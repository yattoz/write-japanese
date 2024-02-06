package dmeeuwis.kanjimaster.ui.sections.primary;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.kanjimaster.ui.sections.credits.ReleaseNotesActivity;

public class UpdateNotifier {

    private static final String LAST_VERSION_NOTIFIED_KEY = "notified";
    private static final boolean DEBUG_NOTIFIER = BuildConfig.DEBUG && false;

    public static void updateNotifier(final AppCompatActivity parent, final View view){
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(parent.getApplicationContext());
        String notified = shared.getString(LAST_VERSION_NOTIFIED_KEY, "");

        if(!BuildConfig.VERSION_NAME.equalsIgnoreCase(notified) || DEBUG_NOTIFIER){
            Snackbar snack = Snackbar.make(view, "Application updated to " + BuildConfig.VERSION_NAME + "!", Snackbar.LENGTH_LONG);
            snack.setDuration(10_000);
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

    public static void debugClearNotified(AppCompatActivity parent){
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(parent.getApplicationContext());
        SharedPreferences.Editor ed = shared.edit();
        ed.remove(LAST_VERSION_NOTIFIED_KEY);
        ed.apply();
    }
}
