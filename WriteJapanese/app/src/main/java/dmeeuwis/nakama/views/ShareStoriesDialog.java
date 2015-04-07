package dmeeuwis.nakama.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ShareStoriesDialog {

    public static void show(final Context context, final Runnable after){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("Story Sharing");
        builder.setMessage("Story sharing shares you kanji stories with others, and lets you see stories from other users around the globe." +
                            "\nStories are inspected for content before being shared. You can opt in or out at any time in the settings menu.");

        builder.setPositiveButton("Share", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean("share", true);
                edit.commit();
                dialog.dismiss();
                after.run();
            }
        });

        builder.setNegativeButton("Don't share", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean("share", false);
                edit.commit();
                dialog.dismiss();
                after.run();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
}
