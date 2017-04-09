package dmeeuwis.nakama.primary;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.view.View;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.nakama.ReleaseNotesActivity;

public class UpdateNotifier {

    private static final String LAST_VERSION_NOTIFIED_KEY = "notified";

    public static void updateNotifier(final Activity parent, final View view){
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(parent.getApplicationContext());
        String notified = shared.getString(LAST_VERSION_NOTIFIED_KEY, "");

        if(!BuildConfig.VERSION_NAME.equalsIgnoreCase(notified)){
            Snackbar snack = Snackbar.make(view, "Application updated to " + BuildConfig.VERSION_NAME + "!", Snackbar.LENGTH_LONG);
            snack.setAction("View Release Notes", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    parent.startActivity(new Intent(parent, ReleaseNotesActivity.class));
                }
            });
            snack.show();

            SharedPreferences.Editor ed = shared.edit();
            ed.putString(LAST_VERSION_NOTIFIED_KEY, BuildConfig.VERSION_NAME);
            ed.apply();
        }
    }

    public static void debugClearNotified(Activity parent){
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(parent.getApplicationContext());
        SharedPreferences.Editor ed = shared.edit();
        ed.remove(LAST_VERSION_NOTIFIED_KEY);
        ed.apply();
    }
}
