package dmeeuwis.nakama.views;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dmeeuwis.nakama.ILockChecker;

import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_DEVELOPER_ERROR;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_ERROR;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_OK;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE;
import static com.android.vending.billing.util.IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED;

public class LockCheckerIInAppBillingService extends ILockChecker {

    final Activity parent;
    final ServiceConnection mServiceConn;
    final ExecutorService commsExec;

    IInAppBillingService mService;
    boolean badConnection = false;

    public LockCheckerIInAppBillingService(final Activity parent){
        super(parent);
        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
                badConnection = true;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mService = IInAppBillingService.Stub.asInterface(service);
                badConnection = false;

                commsExec.execute(new Runnable() {
                    @Override public void run() {
                        checkPastPurchases();
                    }
                });
            }
        };

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        parent.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
        this.parent = parent;

        commsExec = Executors.newSingleThreadExecutor();
    }

    private void flagBadConnection(){
        this.badConnection = true;
    }

    @Override
    public void runPurchase() {
        commsExec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if(badConnection){
                        Toast.makeText(parent, "Error contacting Google Play; please try again later.", Toast.LENGTH_LONG);
                        return;
                    }

                    Bundle buyIntentBundle = mService.getBuyIntent(3, parent.getPackageName(),
                            LICENSE_SKU, "inapp", GOOGLE_PLAY_PUBLIC_KEY);
                    int responseCode = buyIntentBundle.getInt("RESPONSE_CODE", -1);

                    if(responseCode == BILLING_RESPONSE_RESULT_OK){
                        PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                        parent.startIntentSenderForResult(pendingIntent.getIntentSender(),
                            ILockChecker.REQUEST_CODE, new Intent(), 0, 0, 0);

                    } else if(responseCode == BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE){
                        Toast.makeText(parent, "Could not contact Google Play: Billing response unavailable. Please try again later.", Toast.LENGTH_LONG).show();

                    } else if(responseCode == BILLING_RESPONSE_RESULT_DEVELOPER_ERROR){
                        Toast.makeText(parent, "Could not contact Google Play: Unexpected error. Please try again later.", Toast.LENGTH_LONG).show();

                    } else if(responseCode == BILLING_RESPONSE_RESULT_ERROR){
                        Toast.makeText(parent, "Could not contact Google Play: error response. Please try again later.", Toast.LENGTH_LONG).show();

                    } else if(responseCode == BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED){
                        Toast.makeText(parent, "Google Play reported already purchased; unlocking.", Toast.LENGTH_LONG).show();
                        coreUnlock();

                    } else if (responseCode == BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED){
                        Toast.makeText(parent, "Google Play reported error; item not owned.", Toast.LENGTH_LONG).show();

                    } else if (responseCode == BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE){
                        Toast.makeText(parent, "Google Play reported error; unlock key unavailable.", Toast.LENGTH_LONG).show();

                    } else if (responseCode == BILLING_RESPONSE_RESULT_SERVICE_UNAVAILABLE){
                        Toast.makeText(parent, "Google Play reported error; Google Play service unavailable.", Toast.LENGTH_LONG).show();

                    } else if (responseCode == BILLING_RESPONSE_RESULT_USER_CANCELED){
                        Log.i("nakama", "User cancelled purchase screen.");

                    } else {

                    }
                } catch (IntentSender.SendIntentException|RemoteException e) {
                    Toast.makeText(parent, "Error in contacting Google Play; please try again later.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void startConsume() {

    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("nakama", "IABLockChecker: handleActivityResult " + requestCode + " " + resultCode + " " + data);
        if(requestCode != ILockChecker.REQUEST_CODE){ return false; }

        if(resultCode == Activity.RESULT_OK){
            // String responseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            // logToServer("Successful purchase! " + responseData);
            coreUnlock();
            parent.recreate();
        } else if(resultCode == Activity.RESULT_CANCELED){
            Log.d("nakama", "Purchase cancelled");
        }
        return true;
    }

    @Override
    public void dispose() {
        if(mService != null){
            parent.unbindService(mServiceConn);
        }
        commsExec.shutdown();
    }

    private void checkPastPurchases(){
        try {
            Bundle ownedItems = mService.getPurchases(3, parent.getPackageName(), "inapp", null);
            int response = ownedItems.getInt("RESPONSE_CODE");
            if (response == 0) {
                ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                ArrayList<String>  purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                //ArrayList<String>  signatureList = ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
                //String continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");

                for (int i = 0; i < purchaseDataList.size(); ++i) {
                    //String purchaseData = purchaseDataList.get(i);
                    //String signature = signatureList.get(i);
                    String sku = ownedSkus.get(i);

                    if(sku.equals(ILockChecker.LICENSE_SKU)){
                        coreUnlock();
                    }
                }
            }
        } catch (RemoteException e) {
            flagBadConnection();
        }
    }
}
