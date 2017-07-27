package dmeeuwis.nakama.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import java.util.prefs.PreferenceChangeEvent;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.ReminderManager;
import dmeeuwis.nakama.data.CharacterProgressDataHelper;
import dmeeuwis.nakama.primary.KanjiMasterActivity;

public class SRSDialog {

    public static final String SRS_DIALOG_SHOWN_TRIGGER = "srs_dialog_shown";
    private final static boolean DEBUG = true;

    public static void show(final Context context, boolean force){

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.srs_dialog, null, false);

        final CheckBox showNotifications = (CheckBox)layout.findViewById(R.id.srs_dialog_show_notifications_checkbox);
        final CheckBox acrossSets = (CheckBox)layout.findViewById(R.id.srs_dialog_across_charsets_checkbox);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if(prefs.getBoolean(SRS_DIALOG_SHOWN_TRIGGER, false) && !force && !DEBUG){
            return;
        }

        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean(SRS_DIALOG_SHOWN_TRIGGER, true);
        ed.apply();

        // the alert dialog
        AlertDialog alert = new AlertDialog.Builder(context).setView(layout)
                .setTitle("Time Repetition (SRS)")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor ed = prefs.edit();
                        ed.putBoolean("srs_notification", showNotifications.isChecked());
                        ed.putBoolean("srs_across_sets", acrossSets.isChecked());
                        dialog.cancel();
                    }
                }).create();
        alert.show();
    }
}
