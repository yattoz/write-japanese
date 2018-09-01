package dmeeuwis.nakama.views;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import dmeeuwis.nakama.primary.GradingOverrideListener;

public class OverrideDialog extends DialogFragment {

    public enum OverideType { INCORRECT_OVERRIDE, CORRECT_OVERRIDE }

    private final String incorrectOverrideMessage = "The grading algorithm in Write Japanese can sometimes get things wrong. (Sorry about that!) If you think you drew the character correctly, and wish to override the grading, please click 'Override' below, and you will receive a successful grading.";
    private final String correctOverrideMessage = "The grading algorithm in Write Japanese can sometimes get things wrong. (Sorry about that!) If you think you drew the character incorrectly, and wish to override the grading, please click 'Override' below, and you will receive a failed grading.";

    public static OverrideDialog make(OverideType type){
        OverrideDialog d = new OverrideDialog();
        Bundle b = new Bundle();
        b.putString("type", type.name());
        d.setArguments(b);
        return d;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.setRetainInstance(true);

        Bundle b = getArguments();
        final OverideType type = OverideType.valueOf(b.getString("type", OverideType.INCORRECT_OVERRIDE.toString()));
        String message = type == OverideType.INCORRECT_OVERRIDE ? incorrectOverrideMessage : correctOverrideMessage;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle("Override Grading?")
                .setMessage(message)
                .setPositiveButton("Override", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((GradingOverrideListener)getActivity()).overRide(type);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
