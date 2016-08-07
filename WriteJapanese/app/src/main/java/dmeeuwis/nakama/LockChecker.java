package dmeeuwis.nakama;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.appcompat.BuildConfig;
import android.util.Log;
import android.widget.Toast;

import com.android.vending.billing.util.IabHelper;
import com.android.vending.billing.util.IabHelper.OnConsumeFinishedListener;
import com.android.vending.billing.util.IabHelper.OnIabPurchaseFinishedListener;
import com.android.vending.billing.util.IabHelper.OnIabSetupFinishedListener;
import com.android.vending.billing.util.IabHelper.QueryInventoryFinishedListener;
import com.android.vending.billing.util.IabResult;
import com.android.vending.billing.util.Inventory;
import com.android.vending.billing.util.Purchase;

import dmeeuwis.nakama.data.CharacterStudySet.LockLevel;

public class LockChecker implements OnIabSetupFinishedListener, OnIabPurchaseFinishedListener, OnConsumeFinishedListener {

	private static final String GOOGLE_PLAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu/V3i9u59KOUHFYLUch6MDhSIqrRmj44iQNf5zIwlldj+3oL4QNeB0xI44XgKW/D4Uomg/dma0zQqfWMqen1BjAdt9bXyoSaGbHy8sPPMGrZqbAagz59ms2PzyP+o/Y+FEr2/OAsUxBG9CMUCo1cM4YktNDNQ5wRUXTURLmW4b9bhxksX/PFEZFmGA8wH5eHAJFTlnOUmVqsCePVgh6mKBxublfi9xwrQlHYReVbX05whRb8UI8UCZpKQasYbeskwbYGw61F0Z6K3TNAlip+20Ad18rH2VoBHxM5RXnItx+GBPE3f/Uj3QUsshD09IuqSpapl344f9pNUS+yiq/XqwIDAQAB";
	private static final String LICENSE_SKU = "write_japanese_unlock"; // "android.test.purchased";
	private static final int REQUEST_CODE = 4389743;
	
	private static final String PREFS_KEY = "unlockKey";
	
	private IabHelper iab = null;
	private boolean iabHelperSetupFinished = false;
	final private ActionBarActivity parentActivity;
	final private Runnable refreshUICommand;
	
	List<Runnable> queuedCommands;
	
	public LockChecker(ActionBarActivity parentActivity, Runnable refreshUICommand){
		Log.i("nakama", "New LockChecker: about to start iab setup.");
		this.parentActivity = parentActivity;
		this.refreshUICommand = refreshUICommand;
		this.queuedCommands = new ArrayList<Runnable>();
		
		Log.i("nakama", "LockChecker: about to run iabStartSetup");
		try {
			this.iab = new IabHelper(parentActivity, GOOGLE_PLAY_PUBLIC_KEY);
			iab.enableDebugLogging(true);
			iab.startSetup(this);
		} catch(Throwable t){
			Log.e("nakama", "Error when starting IabHelper", t);
		}
		this.queuedCommands.add(new Runnable(){
			@Override public void run() {
				checkForPurchase(Action.UNLOCK);
			}
			
		});
	}
	
	@Override public void onIabSetupFinished(IabResult result) {
		Log.i("nakama", "LockChecker: IAB onIabSetupFinish listener.");
		if(result.isSuccess()){
			Log.i("nakama", "LockChecker: IabSetup onIabSetupFinish: result success! Will run queue.");
			iabHelperSetupFinished = true;
			runQueue();
		} else {
			Log.i("nakama", "LockChecker: IabSetup onIabSetupFinish: result failure!");
		}
	}
	
	private void runQueue(){
		if(!iabHelperSetupFinished){
			Log.i("nakama", "LockChecker: runQueue: ending early, iabHelperSetup is not finished.");
			return;
		}
		Log.i("nakama", "LockChecker: runQueue: will run queue.");
		
		Runnable r = null;
		synchronized(queuedCommands){
			if(queuedCommands.size() > 0){
				r = queuedCommands.remove(0);
			}
		}
		if(r != null){
			Log.i("nakama", "LockChecker: runQueue: will run queue.");
			r.run();
			runQueue();
		}
	}

	
	// === purchase flow =====
	public void runPurchase(){
		synchronized(queuedCommands){
			queuedCommands.add(new Runnable(){
				@Override public void run() {
					try {
						Log.d("nakama", "LockChecker: runPurchase");
						iab.launchPurchaseFlow(parentActivity, LICENSE_SKU, REQUEST_CODE, LockChecker.this);
					} catch(IabHelper.IabAsyncInProgressException|IllegalStateException e){
						Toast.makeText(parentActivity, "Error contacting Google Play for unlock.", Toast.LENGTH_LONG).show();
						Log.e("nakama", "LockChecker: Error in launchPurchaseFlow", e);
					}
				}
			});
		}
		runQueue();
	}
	
	@Override public void onIabPurchaseFinished(IabResult result, Purchase info) {
		Log.d("nakama", "LockChecker: onIabPurchaseFinished");
		
		if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
			return;
		}
		
		if(result.isSuccess() && info != null && info.getPurchaseState() == 0){
			Log.d("nakama", "LockChecker: onIabPurcaseFinished isSuccess!");
			Toast.makeText(parentActivity, "Thank you, your purchase completed! You have full access to all features of Write Japanese. Good luck in your studies!", Toast.LENGTH_LONG).show();
			coreUnlock();
			tryToRefreshUI();
		} else {
			Log.d("nakama", "LockChecker: onIabPurcaseFinished NOT isSuccess! Purchase info: " + info + "; getPurchaseState: " + (info == null ? "" : info.getPurchaseState()));
			Toast.makeText(parentActivity, "Unfortunately, there was an error while processing the unlock: " + result.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	
	// === consume flow =====
	public void startConsume(){
		Log.d("nakama", "LockChecker: startConsume");
		synchronized(this.queuedCommands){
			this.queuedCommands.add(new Runnable(){
				@Override public void run() {
					Log.d("nakama", "LockChecker: startConsume runnable");
					checkForPurchase(Action.CONSUME);
				}
			});
		}
		runQueue();
	}
	
	@Override
	public void onConsumeFinished(Purchase purchase, IabResult result) {
		if(result.isFailure()){
			Log.e("nakama", "Failed to consume purchase: " + result.getMessage());
			Toast.makeText(parentActivity, "Failed to consume! " + result.getMessage(), Toast.LENGTH_SHORT).show();
		} else if(result.isSuccess()){
			Toast.makeText(parentActivity, "Succeeded in consuming!", Toast.LENGTH_SHORT).show();
			Log.i("nakama", "Succeeded in consuming purchase! " + result.getMessage());
			
		}
	}
	
	// === utility flow =====
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("nakama", "LockChecker: handleActivityResult");
		return iab.handleActivityResult(requestCode, resultCode, data);
    }
    
	
	public void coreUnlock(){
		Log.d("nakama", "LockChecker: coreUnlock");

		SharedPreferences prefs = getSharedPrefs();
		Editor ed = prefs.edit();
		ed.putString(PREFS_KEY, unlockKey());
		ed.apply();
	}
	
	private static String unlockKey(){
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		String id = Secure.ANDROID_ID != null ? Secure.ANDROID_ID : "alkdjsklj9q90adsadsa0";
		byte[] bytes = (id + "_WRITE_JAPANESE").getBytes();
		md.update(bytes, 0, bytes.length);
		byte[] digest = md.digest();
		return bytesToHex(digest);
	}
	
	// http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	private static String bytesToHex( byte[] bytes ) {
	    char[] hexChars = new char[ bytes.length * 2 ];
	    for( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[ j ] & 0xFF;
	        hexChars[ j * 2 ] = hexArray[ v >>> 4 ];
	        hexChars[ j * 2 + 1 ] = hexArray[ v & 0x0F ];
	    }
	    return new String( hexChars );
	}	

	private enum Action { UNLOCK, CONSUME }
	private void checkForPurchase(final Action action) {
		Log.d("nakama", "LockChecker: checkForPurchase: about to query async inventory");
		try {
			iab.queryInventoryAsync(new QueryInventoryFinishedListener() {
                @Override public void onQueryInventoryFinished(IabResult result, Inventory inv) throws IabHelper.IabAsyncInProgressException {
                    Log.d("nakama", "LockChecker: got checkForPurchase asnyc inventory query result: success=" + result.isFailure() + " : " + result.getMessage());
                    if(inv == null){
                        Log.i("nakama", "LockChecker: No Inventory listing found!");
                        return;
                    }

                    Purchase license = inv.getPurchase(LICENSE_SKU);

                    if(license != null){
                        if(action == Action.UNLOCK){
                            Log.i("nakama", "LockChecker: checkForPurchase found existing purchase! Will Unlock.");
                            coreUnlock();
                            tryToRefreshUI();
                        }
                        if(action == Action.CONSUME){
                            Log.i("nakama", "LockChecker: checkForPurchase found existing purchase! Will CONSUME.");
                            iab.consumeAsync(license, LockChecker.this);
                        }
                    } else {
                        Log.i("nakama", "LockChecker: Check for previous purchase failed! Application is still locked!! No remote license found.");
                    }
                }
            });
		} catch (IabHelper.IabAsyncInProgressException e) {
			if(BuildConfig.DEBUG) {
				Toast.makeText(parentActivity, "Error contacting Google Play for unlock.", Toast.LENGTH_LONG).show();
			}
			Log.e("nakama", "LockChecker: Error in checkForPurchase", e);
		}
	}
	
	private SharedPreferences getSharedPrefs(){
		return PreferenceManager.getDefaultSharedPreferences(parentActivity.getApplicationContext());
	}

	public LockLevel getPurchaseStatus(){
		SharedPreferences prefs = getSharedPrefs();
		return getPurchaseStatus(prefs);
	}

	public static LockLevel getPurchaseStatus(SharedPreferences prefs){
		String unlocked = prefs.getString(PREFS_KEY, null);
		// Log.d("nakama", "LockChecker: Unlock key from SharedPreferences: " + unlocked);
		if(unlocked != null){
			if(unlocked.equals(unlockKey())){
				// Log.d("nakama", "LockChecker: getPurchaseStatus: Unlock key matched: UNLOCKED");
				return LockLevel.UNLOCKED;
			}
		}
		// Log.d("nakama", "LockChecker: getPurchaseStatus: Unlock key didn't match: LOCKED");
		return LockLevel.LOCKED;

	}

	private void tryToRefreshUI(){
		try {
			refreshUICommand.run();
		} catch(Throwable t){
			Log.e("nakama", "LockChecker: Exception when refreshing UI", t);
		}
	}

    public void dispose(){
        try {
            this.iab.dispose();
        } catch(Throwable t){
            Log.e("nakama", "Caught error shutting down iab helper", t);
        }
    }
}