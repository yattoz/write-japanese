package dmeeuwis.kanjimaster.ui.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class SetInfoDialog {

    public static void show(final Context context, String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(title);
        builder.setMessage(message);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
}