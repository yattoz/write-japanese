package dmeeuwis.kanjimaster.ui.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ShareStoriesDialog {

    public static void show(final Context context, final Runnable chooseYes, final Runnable chooseNo){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("Story Sharing");
        builder.setMessage("Story sharing shares your kanji stories with other users, and lets you see stories from users around the globe." +
                            "\n\nStories are inspected for content before being made visible to other users.\n\nYou can opt in or out at any time in the settings menu.");

        builder.setPositiveButton("Share", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean("share", true);
                edit.apply();
                dialog.dismiss();
                chooseYes.run();
            }
        });

        builder.setNegativeButton("Don't share", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean("share", false);
                edit.apply();
                dialog.dismiss();
                chooseNo.run();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
}
