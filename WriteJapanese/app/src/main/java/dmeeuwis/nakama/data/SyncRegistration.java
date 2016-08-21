package dmeeuwis.nakama.data;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.nakama.primary.KanjiMasterActivity;

public class SyncRegistration {
    public static final int REQUEST_CODE_PICK_ACCOUNT = 255;
    public static final String HAVE_ASKED_ABOUT_SYNC_KEY = "ASKED_SYNC";

    public enum RegisterRequest {
        REQUESTED("Do you want to enable progress sync among all Android devices authenticated with your Google account?"),
        PROMPTED( "Do you want to enable progress sync among all Android devices authenticated with your Google account?");

        final String message;
        RegisterRequest(String message) {
            this.message = message;
        }
    }

    public static boolean checkIsRegistered(Activity activity) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        String auth = pref.getString(KanjiMasterActivity.AUTHCODE_SHARED_PREF_KEY, null);
        return auth != null;
    }

    public static void registerAccount(final RegisterRequest request, final Activity activity, final boolean force) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        String auth = pref.getString(KanjiMasterActivity.AUTHCODE_SHARED_PREF_KEY, null);

        if (auth != null && !force) {
            Log.i("nakama-sync", "Found existing authcode, already registered for server sync");
        } else {
            Log.i("nakama-sync", "No existing authcode, launch process to register server sync");
            boolean haveAsked = pref.getBoolean(
                    HAVE_ASKED_ABOUT_SYNC_KEY, false);

            if (force || !haveAsked) {
                // shouldShowRequestPermissionRationale returns false on first run!
                Log.i("nakama-sync", "shouldShowRequest returns true, prompting before requesting permission");

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(request.message);
                builder.setTitle("Enable Device Sync?");
                builder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(activity.getApplicationContext(), "Inter-device sync is not enabled. You may enable it at any time from the menu.", Toast.LENGTH_LONG).show();
                            }
                        });
                builder.setPositiveButton("Enable",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i("nakama-register", "Broadcasting AccountManager intent");
                                activity.startActivityForResult(
                                        AccountManager.newChooseAccountIntent(
                                                null, null, new String[]{"com.google"}, false, null, null, null, null),
                                        REQUEST_CODE_PICK_ACCOUNT);
                                return;
                            }
                        }
                );
                builder.create().show();
                SharedPreferences.Editor e = pref.edit();
                e.putBoolean(HAVE_ASKED_ABOUT_SYNC_KEY, true);
                e.apply();
            } else {
                Log.i("nakama", "Skipping automatted registration attempt, user has not opted in.");
            }
        }
    }

    static public void onAccountSelection(Activity activity, int requestCode, int resultCode, Intent data){
        Log.i("nakama-auth", "Got activity result for request account pick!");

        // Receiving a result from the AccountPicker
        if (resultCode == Activity.RESULT_OK) {
            String mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            Log.i("nakama-auth", "Account selected was: " + mEmail);

            accountFound(activity, mEmail);

        } else if (resultCode == Activity.RESULT_CANCELED) {
            // The account picker dialog closed without selecting an account.
            // Notify users that they must pick an account to proceed.
            Toast.makeText(activity, "No Google account exists, or selection was cancelled. Device sync can be enabled at any time from the settings menu.", Toast.LENGTH_LONG).show();
        }
    }

    static private void accountFound(final Activity activity, final String accountName){
        Log.i("nakama-auth", "Found account as: " + accountName);

        GetAccountTokenAsync getter = new GetAccountTokenAsync(activity, accountName,
            new GetAccountTokenAsync.RunWithAuthcode(){
                @Override public void exec(String authcode) {
                    recordAuthToken(authcode, activity);
                    scheduleSyncs(activity, accountName);
                }
            });
        getter.execute();
    }

    private static void recordAuthToken(String authcode, Activity activity){
        Log.i("nakama-auth", "Recording authcode to shared prefs: " + authcode);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(KanjiMasterActivity.AUTHCODE_SHARED_PREF_KEY, authcode);
        ed.apply();
    }

    public static final boolean DEBUG_SYNC = false;

    static private void scheduleSyncs(Activity activity, String accountName){
        final String authority = "dmeeuwis.com";

        Account account = new Account(accountName, "com.google");
        //AccountManager accountManager = AccountManager.get(activity.getApplicationContext());
        //accountManager.addAccountExplicitly(account, null, new Bundle());

        ContentResolver.setIsSyncable(account, authority, 1);
        ContentResolver.setSyncAutomatically(account, authority, true);
        if(BuildConfig.DEBUG && DEBUG_SYNC){
            Log.i("nakama-sync", "Scheduling 60 second DEBUG sync for account " + account.name + "!");
            ContentResolver.addPeriodicSync(account, authority, Bundle.EMPTY, 60);
        } else {
            Log.i("nakama-sync", "Scheduling bi-daily sync for account " + account.name + "!");
            ContentResolver.addPeriodicSync(account, authority, Bundle.EMPTY, KanjiMasterActivity.SYNC_INTERVAL);
        }
/*      // DEBUGGING, requires additional permissions
        {
            List<SyncInfo> syncs = ContentResolver.getCurrentSyncs();
            Log.i("nakama-sync", "Found " + syncs.size() + " syncs!");
            for (SyncInfo s : syncs) {
                Log.i("nakama-sync", "Looking at SyncInfo: " + s);
            }
        }

        {
            List<PeriodicSync> psyncs = ContentResolver.getPeriodicSyncs(account, "dmeeuwis.com");
            Log.i("nakama-sync", "After scheduling, found " + psyncs.size() + " syncs!");
            for (PeriodicSync s : psyncs) {
                Log.i("nakama-sync", "Looking at SyncInfo: " + s);
            }
        }
*/
    }
}