package dmeeuwis.nakama.data;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.PeriodicSync;
import android.content.SharedPreferences;
import android.content.SyncInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.nakama.primary.KanjiMasterActivity;

public class SyncRegistration {
    public static final int REQUEST_CODE_PICK_ACCOUNT = 0x8473;
    public static final int MY_PERMISSIONS_REQUEST_ACCOUNT_MANAGER = 123;

    public enum RegisterRequest {
        REQUESTED("In order to sync your study progress between multiple devices, access to the Account Manager will now be requested. This will share your progress between all devices authenticated with your Google account."),
        PROMPTED("Do you want to enable network sync? This will let your progress be shared between all of your current and future Android devices. Access to the Account Manager will be requested, so your progress can be shared between all devices authenticated with your Google account.");

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

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                findAccount(activity);

            } else {
                int permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCOUNT_MANAGER);
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    Log.i("nakama-sync", "Found permission already granted for ACCOUNT_MANAGER");
                    findAccount(activity);
                } else {
                    Log.i("nakama-sync", "Requesting permission ACCOUNT_MANAGER");

                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCOUNT_MANAGER)) {
                        Log.i("nakama-sync", "shouldShowRequest returns true, prompting before requesting permission");

                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setMessage(request.message);
                        builder.setTitle("Enable Device Sync?");
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(activity.getApplicationContext(), "Network sync cancelled. You may enable it at any time from the settings menu.", Toast.LENGTH_LONG).show();
                            }
                        });
                        builder.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(activity,
                                        new String[]{Manifest.permission.ACCOUNT_MANAGER},
                                        MY_PERMISSIONS_REQUEST_ACCOUNT_MANAGER);
                            }
                        });

                    } else {
                        Log.i("nakama-sync", "shouldShowRequest returns false, directly requesting permission ACCOUNT_MANAGER");
                        ActivityCompat.requestPermissions(activity,
                                new String[]{Manifest.permission.ACCOUNT_MANAGER},
                                MY_PERMISSIONS_REQUEST_ACCOUNT_MANAGER);
                    }
                }
            }
        }
    }

    public static void continueRegisterAfterPermission(Activity activity, int resultCode) {
        if(resultCode == Activity.RESULT_OK) {
            findAccount(activity);
        } else {
            Toast.makeText(activity, "Could not continue network sync due to denied permissions.", Toast.LENGTH_LONG).show();
        }
    }

    private static void findAccount(Activity activity){
        AccountManager accountManager = AccountManager.get(activity);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        if (accounts.length == 0) {
            Toast.makeText(activity, "Could not find any Google accounts to sync with.", Toast.LENGTH_LONG).show();
        } else if (accounts.length == 1) {
            Account a = accounts[0];
            Log.i("nakama-auth", "Found only 1 com.google account: " + a.name);

            accountFound(activity, a);
        } else if (accounts.length > 1) {
            Log.i("nakama-auth", "Found multiple google accounts: prompting user");
            String[] accountTypes = new String[]{"com.google"};
            Intent intent = AccountManager.newChooseAccountIntent(null, null,
                    accountTypes, false, null, null, null, null);
            activity.startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
            // show account picker
        }
    }

    static public void onAccountSelection(Activity activity, int requestCode, int resultCode, Intent data){
        Log.i("nakama-auth", "Got activity result for request account pick!");
        // Receiving a result from the AccountPicker
        if (resultCode == Activity.RESULT_OK) {
            String mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            Log.i("nakama-auth", "Account selected was: " + mEmail);

            AccountManager accountManager = AccountManager.get(activity);
            Account[] accounts = accountManager.getAccountsByType("com.google");
            for(Account a: accounts){
                if(a.name.equals(mEmail)){
                    SyncRegistration.accountFound(activity, a);
                }
            }

        } else if (resultCode == Activity.RESULT_CANCELED) {
            // The account picker dialog closed without selecting an account.
            // Notify users that they must pick an account to proceed.
            Toast.makeText(activity, "You must choose a Google account to enable network sync. Device sync can be enabled at any time from the settings menu.", Toast.LENGTH_SHORT).show();
        }
    }

    static private void accountFound(final Activity activity, final Account account){
        Log.i("nakama-auth", "Found account as: " + account.name);

        GetAccountTokenAsync getter = new GetAccountTokenAsync(activity, account.name,
            new GetAccountTokenAsync.RunWithAuthcode(){
                @Override public void exec(String authcode) {
                    recordAuthToken(authcode, activity);
                    scheduleSyncs(account);
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

    static private void scheduleSyncs(Account account){
        final String authority = "dmeeuwis.com";
        ContentResolver.setIsSyncable(account, authority, 1);
        ContentResolver.setSyncAutomatically(account, authority, true);
        if(BuildConfig.DEBUG && KanjiMasterActivity.DEBUG_SYNC){
            Log.i("nakama-sync", "Scheduling 60 second DEBUG sync for account " + account.name + "!");
            ContentResolver.addPeriodicSync(account, authority, Bundle.EMPTY, 60);
        } else {
            Log.i("nakama-sync", "Scheduling bi-daily sync for account " + account.name + "!");
            ContentResolver.addPeriodicSync(account, authority, Bundle.EMPTY, KanjiMasterActivity.SYNC_INTERVAL);
        }

        {
            List<SyncInfo> syncs = ContentResolver.getCurrentSyncs();
            Log.i("nakama-sync", "Found " + syncs.size() + " syncs!");
            for (SyncInfo s : syncs) {
                Log.i("nakama-sync", "Looking at SyncInfo: " + s);
            }
        }

        {
            List<PeriodicSync> psyncs = ContentResolver.getPeriodicSyncs(account, "dmeeuwis.com");
            Log.i("nakama-sync", "Found " + psyncs.size() + " syncs!");
            for (PeriodicSync s : psyncs) {
                Log.i("nakama-sync", "Looking at SyncInfo: " + s);
            }
        }
    }
}