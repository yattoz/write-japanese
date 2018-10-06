package dmeeuwis.nakama.views;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.*;
import android.widget.*;

import dmeeuwis.kanjimaster.*;
import dmeeuwis.nakama.primary.GradingOverrideListener;

import static android.content.DialogInterface.BUTTON_POSITIVE;

public class OverrideDialog extends DialogFragment implements DialogInterface.OnClickListener {

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


    OverideType type;
    CheckBox c;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.setRetainInstance(true);

        Bundle b = getArguments();
        type = OverideType.valueOf(b.getString("type", OverideType.INCORRECT_OVERRIDE.toString()));
        String message = type == OverideType.INCORRECT_OVERRIDE ? incorrectOverrideMessage : correctOverrideMessage;

        final FrameLayout frameView = new FrameLayout(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Dialog d = builder.setTitle("Override Grading?")
                .setMessage(message)
                .setView(frameView)
                .setPositiveButton("Override", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }).create();

        View mView = d.getLayoutInflater().inflate(R.layout.override_fragment_check_data_submit, frameView);
        c = mView.findViewById(R.id.submit_char_data_on_override);

        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == BUTTON_POSITIVE){
            ((GradingOverrideListener)getActivity()).overRide(type, c.isChecked());
        }

    }
}
