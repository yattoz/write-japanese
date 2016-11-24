package dmeeuwis.nakama;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.appcompat.BuildConfig;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.vending.billing.util.FixedIabHelper;
import com.android.vending.billing.util.IabHelper;
import com.android.vending.billing.util.IabHelper.OnConsumeFinishedListener;
import com.android.vending.billing.util.IabHelper.OnIabPurchaseFinishedListener;
import com.android.vending.billing.util.IabHelper.OnIabSetupFinishedListener;
import com.android.vending.billing.util.IabHelper.QueryInventoryFinishedListener;
import com.android.vending.billing.util.IabResult;
import com.android.vending.billing.util.Inventory;
import com.android.vending.billing.util.Purchase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dmeeuwis.nakama.data.CharacterStudySet.LockLevel;
import dmeeuwis.nakama.data.UncaughtExceptionLogger;

public class LockCheckerIabHelper extends LockChecker implements OnIabSetupFinishedListener, OnIabPurchaseFinishedListener, OnConsumeFinishedListener {

	private FixedIabHelper iab = null;
	private boolean iabHelperSetupFinished = false;
	private boolean iabHelperInventoryRefreshing = false;
	final private ActionBarActivity parentActivity;

	final private List<String> iabDebugLog = new ArrayList<>();
	final private DateFormat df = new SimpleDateFormat("dd-MMM-yyyy E hh:mm a z");

	final private long startTime;

	List<Runnable> queuedCommands;
	
	private LockCheckerIabHelper(ActionBarActivity parentActivity){
		super(parentActivity);

		Log.i("nakama", "New LockCheckerIabHelper: about to start iab setup.");
		this.startTime = System.currentTimeMillis();
		this.parentActivity = parentActivity;
		this.queuedCommands = new ArrayList<Runnable>();
		
		Log.i("nakama", "LockCheckerIabHelper: about to run iabStartSetup");
		try {
			this.iab = new FixedIabHelper(parentActivity, GOOGLE_PLAY_PUBLIC_KEY);
			iab.enableDebugLogging(true);
			iab.startSetup(this);
			iabLog("iab start setup");

			/* For debugging queuing
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(20000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					iab.startSetup(LockCheckerIabHelper.this);
				}
			}).start();
			*/

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
		Log.i("nakama", "LockCheckerIabHelper: IAB onIabSetupFinish listener. Took " + (System.currentTimeMillis() - startTime) + "ms from startup.");
		if(result.isSuccess()){
			Log.i("nakama", "LockCheckerIabHelper: IabSetup onIabSetupFinish: result success! Will run queue.");
			iabLog("iab setup finished SUCCESS in " + (System.currentTimeMillis() - startTime) + "ms");
			iabHelperSetupFinished = true;
			runQueue();
		} else {
			iabLog("iab setup finished FAILURE! " + (System.currentTimeMillis() - startTime) + "ms");
			log("LockCheckerIabHelper: iabSetup onIabSetupFinish: result failure: " +
					result.getMessage() + " " + result.getResponse(),
				new RuntimeException("onIabSetupFinished error: " + result.getMessage()));
		}
	}
	
	private void runQueue(){
		if(!iabHelperSetupFinished){
			Log.i("nakama", "LockCheckerIabHelper: runQueue: ending early, iabHelperSetup is not finished.");
			return;
		}

		if(iabHelperInventoryRefreshing){
			Log.i("nakama", "LockCheckerIabHelper: runQueue: ending early, iabHelper is refreshing.");
			return;
		}

		Log.i("nakama", "LockCheckerIabHelper: runQueue: will run queue.");
		
		Runnable r = null;
		synchronized(queuedCommands){
			if(queuedCommands.size() > 0){
				r = queuedCommands.remove(0);
			}
		}
		if(r != null){
			Log.i("nakama", "LockCheckerIabHelper: runQueue: will run queue.");
			r.run();
			runQueue();
		}
	}

	
	// === purchase flow =====
	@Override
	public void runPurchase(){
		iabDebugLog.add(df.format(new Date()) + " iab run purchase flow queued");
		synchronized(queuedCommands){
			queuedCommands.add(new Runnable(){
				@Override public void run() {
					try {
						Log.d("nakama", "LockCheckerIabHelper: runPurchase");
						iabDebugLog.add(df.format(new Date()) + " iab run purchase flow dequeued and launched!");
						iab.launchPurchaseFlow(parentActivity, LICENSE_SKU, REQUEST_CODE, LockCheckerIabHelper.this);
					} catch(Throwable e){
						Toast.makeText(parentActivity, "Error contacting Google Play for unlock. Please try again later.", Toast.LENGTH_LONG).show();
						log("LockCheckerIabHelper: Error in launchPurchaseFlow", e);
					}
				}
			});
		}
		runQueue();
	}
	
	@Override public void onIabPurchaseFinished(IabResult result, Purchase info) {
		Log.d("nakama", "LockCheckerIabHelper: onIabPurchaseFinished");
		iabDebugLog.add(df.format(new Date()) + " iab onPurchaseFinished CANCELLED");

		if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED){
			Log.d("nakama", "LockCheckerIabHelper: onIabPurchaseFinished user cancelled");
			iabDebugLog.add(df.format(new Date()) + " iab onPurchaseFinished CANCELLED");
			return;
		}

		if (result.getMessage().startsWith("Null data in IAB result")) {
			iabDebugLog.add(df.format(new Date()) + " iab onPurchaseFinished NULL DATA");
			Log.e("nakama", "Hiding known error from launching new activity while existing IAB purchase flow is active.");
			return;
		}
		
		if(result.isSuccess() && info != null && info.getPurchaseState() == 0){
			iabDebugLog.add(df.format(new Date()) + " iab onPurchaseFinished SUCCESS");
			Log.d("nakama", "LockCheckerIabHelper: onIabPurchaseFinished isSuccess!");
			Toast.makeText(parentActivity, "Thank you, your purchase completed! You have full access to all features of Write Japanese. Good luck in your studies!", Toast.LENGTH_LONG).show();
			coreUnlock();
			parentActivity.recreate();
		} else {
			log("LockCheckerIabHelper.onIabPurchaseFinished NOT isSuccess! Result: " + result.getResponse() + " " + result.getMessage() + ". Purchase info: " + info + "; getPurchaseState: " + ( info == null ? "" : info.getPurchaseState()),
					new RuntimeException());
			Toast.makeText(parentActivity, "Unfortunately, there was an error while processing the unlock: " + result.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private void log(String message, Throwable t){
		String log = TextUtils.join(" -> ", iabDebugLog);
		UncaughtExceptionLogger.backgroundLogError(message + "\n" + log, t, parentActivity.getApplicationContext());
	}


	// === consume flow =====
	@Override
	public void startConsume(){
		Log.d("nakama", "LockCheckerIabHelper: startConsume");
		synchronized(this.queuedCommands){
			this.queuedCommands.add(new Runnable(){
				@Override public void run() {
					Log.d("nakama", "LockCheckerIabHelper: startConsume runnable");
					checkForPurchase(Action.CONSUME);
				}
			});
		}
		runQueue();
	}
	
	@Override
	public void onConsumeFinished(Purchase purchase, IabResult result) {
		Log.i("nakama", "onConsumeFinished " + purchase + " " + result);
		if(result.isFailure()){
			Log.e("nakama", "Failed to consume purchase: " + result.getMessage());
			Toast.makeText(parentActivity, "Failed to consume! " + result.getMessage(), Toast.LENGTH_SHORT).show();
		} else if(result.isSuccess()){
			Toast.makeText(parentActivity, "Succeeded in consuming!", Toast.LENGTH_SHORT).show();
			Log.i("nakama", "Succeeded in consuming purchase! " + result.getMessage());
			coreLock();
			parentActivity.recreate();
		}
	}
	
	// === utility flow =====
    @Override
	public boolean handleActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if(requestCode != REQUEST_CODE){ return false; }

		Log.d("nakama", "LockCheckerIabHelper: handleActivityResult " + requestCode + " " + resultCode + " " + data);
		iabLog("handleActivityResult: " + requestCode + " " + resultCode + " " + data);

		if(!iabHelperSetupFinished){
			iabLog("LockCheckerIabHelper: iabHelper not finished! Queuing response");
			queuedCommands.add(new Runnable() {
				@Override
				public void run() {
					Log.d("nakama", "LockCheckerIabHelper: doing delayed handleActivityResult");
					iabLog("running delayed handleActivityResult");
					doHandleActivityResultWork(requestCode, resultCode, data);
				}
			});
			runQueue();
			return true;
		}

		return doHandleActivityResultWork(requestCode, resultCode, data);
    }

	private boolean doHandleActivityResultWork(int requestCode, int resultCode, Intent data){
		try {
			// hack iab listener so that if orientation changed during payment, current LockCheckerIabHelper's
			// iabPurchaseListener still works
			iab.forcePurchaseListener(requestCode, LockCheckerIabHelper.this);

			return iab.handleActivityResult(requestCode, resultCode, data);

		} catch(Throwable e){
			UncaughtExceptionLogger.backgroundLogError("Error in LockCheckerIabHelper.handleActivityResult processing\n" +
					TextUtils.join("\n", iabDebugLog), e, parentActivity);
			Toast.makeText(parentActivity, "Error processing response from Google Play. Please try again later.", Toast.LENGTH_LONG).show();
			return false;
		}
	}

	private void iabLog(String message){
		iabDebugLog.add(df.format(new Date()) + ": " + message);
		Log.d("nakama", message);
	}

	private enum Action { UNLOCK, CONSUME }
	private void checkForPurchase(final Action action) {
		iabLog("LockCheckerIabHelper: checkForPurchase: about to query async inventory");

		// in normal application, action should always be UNLOCK. Only for debug builds where consume
		// is an option might it be CONSUME.
		if(action == Action.UNLOCK && getPurchaseStatus() == LockLevel.UNLOCKED){
			Log.i("nakama", "LockCheckerIabHelper: skipping checkForPurchase, found registration key.");
			return;
		}

		try {
			iabHelperInventoryRefreshing = true;
			iab.queryInventoryAsync(new QueryInventoryFinishedListener() {
                @Override public void onQueryInventoryFinished(IabResult result, Inventory inv) throws IabHelper.IabAsyncInProgressException {
                    iabLog("LockCheckerIabHelper: got checkForPurchase asnyc inventory query result: success=" + result.isFailure() + " : " + result.getMessage());
                    if(inv == null){
                        Log.i("nakama", "LockCheckerIabHelper: No Inventory listing found!");
                        return;
                    }

                    Purchase license = inv.getPurchase(LICENSE_SKU);
					Log.i("nakama", "LockCheckerIabHelper: checkForPurchase found license " + license);

                    if(license != null){
                        if(action == Action.UNLOCK){
                            Log.i("nakama", "LockCheckerIabHelper: checkForPurchase found existing purchase! Will Unlock.");
                            coreUnlock();
							Log.i("nakama", "LockCheckerIabHelper: unlocked! Will restart!");
                            parentActivity.recreate();
                        }
                        if(action == Action.CONSUME){
                            Log.i("nakama", "LockCheckerIabHelper: checkForPurchase found existing purchase! Will CONSUME.");
                            iab.consumeAsync(license, LockCheckerIabHelper.this);
                        }
                    } else {
                        Log.i("nakama", "LockCheckerIabHelper: Check for previous purchase failed! Application is still locked!! No remote license found.");
                    }

					iabHelperInventoryRefreshing = false;
					runQueue();
                }
            });
		} catch (IabHelper.IabAsyncInProgressException e) {
			if(BuildConfig.DEBUG) {
				Toast.makeText(parentActivity, "Error contacting Google Play for unlock.", Toast.LENGTH_LONG).show();
			}
			UncaughtExceptionLogger.backgroundLogError("LockCheckerIabHelper: Error in checkForPurchase", e, parentActivity);
		}
	}
	
    @Override
	public void dispose() {
        try {
            this.iab.dispose();
        } catch(IabHelper.IabAsyncInProgressException t){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Log.e("nakama", "Ignoring interrupted exception on dispose delay");
			}
			try {
				this.iab.dispose();
			} catch(IabHelper.IabAsyncInProgressException t2){
				try {
					Thread.sleep(1500);
					this.iab.dispose();
				} catch (Throwable e) {
					Log.e("nakama", "Caught error shutting down iab helper", e);
				}

			}
		} catch(Throwable t){
			Log.e("nakama", "Caught error shutting down iab helper", t);
		}
    }
}