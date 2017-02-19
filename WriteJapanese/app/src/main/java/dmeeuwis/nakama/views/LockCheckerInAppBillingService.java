package dmeeuwis.nakama.views;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.nakama.LockChecker;
import dmeeuwis.nakama.data.LoggingRunnable;
import dmeeuwis.nakama.data.UncaughtExceptionLogger;

import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_DEVELOPER_ERROR;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_ERROR;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_OK;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED;

public class LockCheckerInAppBillingService extends LockChecker {

    final Activity parent;
    final ServiceConnection mServiceConn;
    final ConcurrentLinkedQueue<Runnable> delayed;

    // only instantiated with billing connection
    ExecutorService commsExec;

    IInAppBillingService mService;
    boolean badConnection = false;
    boolean googlePlayFound = false;
    String purchaseCode;

    public LockCheckerInAppBillingService(final Activity parent){
        super(parent);

        this.delayed = new ConcurrentLinkedQueue<>();
        this.parent = parent;

        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d("nakama-iiab", "LockCheckerIInAppBilling: onServiceDisconnected");
                toast("Could not connect to Google Play Services");
                mService = null;
                badConnection = true;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d("nakama-iiab", "LockCheckerIInAppBilling: onServiceConnected");

                mService = IInAppBillingService.Stub.asInterface(service);
                badConnection = false;

                if(commsExec == null) {
                    commsExec = Executors.newSingleThreadExecutor();
                }

                if(commsExec.isTerminated()){
                    commsExec.shutdown();
                    commsExec = Executors.newSingleThreadExecutor();
                }

                while(!delayed.isEmpty()){
                    Runnable r = delayed.poll();
                    if(r != null) {
                        addJob(r);
                    }
                }

                commsExec.execute(new LoggingRunnable(parent) {
                    public void runCore() {
                        checkPastPurchases();
                    }
                });
            }
        };

        Log.d("nakama-iiab", "LockCheckerIInAppBilling: constructing, starting to bind service");
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        googlePlayFound = parent.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void runPurchase() {
        if(!googlePlayFound){
            toast("Error: Could not contact Google Play for purchasing on this device.");
            return;
        }

        Log.d("nakama-iiab", "LockCheckerIInAppBilling: queuing purchase run");
        addJob(new LoggingRunnable(parent) {
            public void runCore() {
                try {
                    Log.d("nakama-iiab", "LockCheckerIInAppBilling: doing purchase run");
                    if(badConnection){
                        toast("Error contacting Google Play; please try again later.");
                        return;
                    }

                    int result = mService.isBillingSupported(3, parent.getPackageName(), "inapp");
                    if(result != BILLING_RESPONSE_RESULT_OK){
                        toast("Sorry, Google Play in-app purchases are not supported on your device (" + result + ")");
                    }

                    Bundle buyIntentBundle = mService.getBuyIntent(3, parent.getPackageName(),
                            LICENSE_SKU, "inapp", GOOGLE_PLAY_PUBLIC_KEY);
                    int responseCode = buyIntentBundle.getInt("RESPONSE_CODE", -1);
                    Log.d("nakama-iiab", "LockCheckerIInAppBilling: saw response to purchase: " + responseCode);

                    if(responseCode == BILLING_RESPONSE_RESULT_OK){
                        PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                        parent.startIntentSenderForResult(pendingIntent.getIntentSender(),
                            LockChecker.REQUEST_CODE, new Intent(), 0, 0, 0);

                    } else if(responseCode == BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE){
                        toast("Could not contact Google Play: Billing response unavailable. Please try again later.");

                    } else if(responseCode == BILLING_RESPONSE_RESULT_DEVELOPER_ERROR){
                        toast("Could not contact Google Play: Unexpected error. Please try again later.");

                    } else if(responseCode == BILLING_RESPONSE_RESULT_ERROR){
                        toast("Could not contact Google Play: error response. Please try again later.");

                    } else if(responseCode == BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED){
                        toast("Google Play found existing purchase; unlocking.");
                        coreUnlock();
                        recreateActivity();

                    } else if (responseCode == BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED){
                        toast("Google Play reported error; item not owned.");

                    } else if (responseCode == BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE){
                        toast("Google Play reported error; unlock key unavailable.");

                    } else if (responseCode == BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE){
                        toast("Google Play reported error; Google Play service unavailable.");

                    } else if (responseCode == BILLING_RESPONSE_RESULT_USER_CANCELED){
                        Log.i("nakama-iiab", "User cancelled purchase screen.");

                    } else {

                    }
                } catch (IntentSender.SendIntentException|RemoteException e) {
                    UncaughtExceptionLogger.backgroundLogError(e.getMessage(), e, parent);
                    toast("Error in contacting Google Play; please try again later.");
                }
            }
        });
    }

    private void toast(final String msg){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new LoggingRunnable(parent) {
                 public void runCore() {
                     Toast.makeText(parent, msg, Toast.LENGTH_LONG).show();
                 }
            }
        );
    }

    private void recreateActivity(){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new LoggingRunnable(parent) {
                         public void runCore() {
                             parent.recreate();
                         }
                     }
        );
    }

    @Override
    public void startConsume() {
        Log.d("nakama-iiab", "LockCheckerIInAppBilling: debug CONSUMING");
        if(!BuildConfig.DEBUG){
            Toast.makeText(parent, "Only available in debug build", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences prefs = getSharedPrefs();
        String token = prefs.getString("purchase_token", null);

        if(token == null){
            Toast.makeText(parent, "No token found", Toast.LENGTH_LONG).show();

        } else {
            try {
                int consume = mService.consumePurchase(3, parent.getPackageName(), token);
                if(consume == BILLING_RESPONSE_RESULT_OK) {
                    Toast.makeText(parent, "Consumed!", Toast.LENGTH_LONG).show();
                    coreLock();
                    recreateActivity();
                } else if(consume == BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED){
                    Toast.makeText(parent, "Previously Consumed!", Toast.LENGTH_LONG).show();
                    coreLock();
                    recreateActivity();
                } else {
                    Toast.makeText(parent, "Unknown response: " + consume, Toast.LENGTH_LONG).show();
                }
            } catch (RemoteException e) {
                Toast.makeText(parent, "Error consuming purchase: " + e, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("nakama-iiab", "LockCheckerIInAppBilling: handleActivityResult " + requestCode + " " + resultCode + " " + data);
        if(requestCode != LockChecker.REQUEST_CODE){ return false; }

        if(resultCode == Activity.RESULT_OK){
            String responseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            // Log.d("nakama-iiab", "LockCHeckerIInAppBillingService.handleActivityResult Purchase succeeded!" + responseData);
            try {
                savePurchaseTokenFromPurchaseData(responseData);
            } catch (JSONException e) {
                UncaughtExceptionLogger.backgroundLogError("Error parsing JSON response from Google Play: " + responseData, e, parent);
                toast("Error parsing JSON response from Google Play");
                return true;
            }
            toast("Thank you, your purchase completed! You have full access to all features of Write Japanese. Good luck in your studies!");
            parent.recreate();
        } else if(resultCode == Activity.RESULT_CANCELED){
            Log.d("nakama-iiab", "LockCHeckerIInAppBillingService.handleActivityResult Purchase cancelled");
        }
        return true;
    }

    @Override
    public void dispose() {
        if(mService != null){
            parent.unbindService(mServiceConn);
        }
        if(commsExec != null) {
            commsExec.shutdown();
        }
    }

    private void checkPastPurchases(){
        try {
            Log.d("nakama-iiab", "Checking past purchases...");
            Bundle ownedItems = mService.getPurchases(3, parent.getPackageName(), "inapp", null);
            int response = ownedItems.getInt("RESPONSE_CODE");
            if (response == 0) {
                ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                ArrayList<String>  purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                //ArrayList<String>  signatureList = ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
                //String continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");
                Log.d("nakama-iiab", "Found " + ownedSkus.size() + " past purchases.");

                for (int i = 0; i < purchaseDataList.size(); ++i) {
                    String purchaseData = purchaseDataList.get(i);
                    //String signature = signatureList.get(i);
                    String sku = ownedSkus.get(i);
                    //Log.d("nakama-iiab", "Purchase " + i + ": " + purchaseData);
                    if(sku.equals(LockChecker.LICENSE_SKU)){
                        boolean saved = savePurchaseTokenFromPurchaseData(purchaseData);
                        if(saved) {
                            recreateActivity();
                        }
                    }
                }
            } else {
                Log.d("nakama-iiab", "Unknown response code for past purchases: " + response);
            }
        } catch (JSONException e) {
            toast("Error parsing JSON response from Google Play");
        } catch (RemoteException e) {
            badConnection = true;
        }
    }

    private boolean savePurchaseTokenFromPurchaseData(String purchaseData) throws JSONException {
        // Log.i("nakama-iiab", "Attempt to parse JSON: " + purchaseData);
        JSONObject j = new JSONObject(purchaseData);
        String token = j.getString("purchaseToken");
        this.purchaseCode = token;
        SharedPreferences shared = getSharedPrefs();

        // Log.i("nakama-iiab", "Unlock and recording previous purchase token: " + token);
        coreUnlock();

        String existing = shared.getString("purchase_token", "");
        if(existing.equals(token)){
            return false;
        } else {
            SharedPreferences.Editor ed = shared.edit();
            ed.putString("purchase_token", token);
            ed.apply();
            return true;
        }
    }

    private void addJob(Runnable j){
        if(commsExec != null){
            Log.d("nakama-iiab", "Executing job");
            commsExec.execute(j);
        } else {
            toast("Waiting for Google Play connection...");
            Log.d("nakama-iiab", "Delaying job");
            delayed.add(j);
        }
    }
}