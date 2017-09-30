package dmeeuwis.nakama.views;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import dmeeuwis.nakama.primary.GradingOverrideListener;

public class OverrideDialog extends DialogFragment {

    public static OverrideDialog make(){
        return new OverrideDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.setRetainInstance(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle("Override Grading?")
                .setMessage("The grading algorithm in Write Japanese can sometimes get things wrong. (Sorry about that!) If you think you drew the character correctly, and wish to override the grading, please click 'Override' below, and you will receive a successful grading.")
                .setPositiveButton("Override", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((GradingOverrideListener)getActivity()).overRide();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Toast.makeText(getActivity(), "Override denied!", Toast.LENGTH_LONG).show();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
