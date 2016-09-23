package dmeeuwis.nakama.views;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioGroup;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.Settings;

public class StrictnessDialog extends DialogFragment implements DialogInterface.OnClickListener{
    RadioGroup radioGroup;

    public StrictnessDialog(){
        // empty constructor
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final FrameLayout frameView = new FrameLayout(getActivity());
        Dialog d = new AlertDialog.Builder(getActivity())
                .setTitle("Set Grading Strictness")
                .setView(frameView)
                .setPositiveButton("OK", this)
                .setNegativeButton("Cancel", null).create();

        LayoutInflater inflater = d.getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.fragment_strictness_dialog, frameView);
        radioGroup = (RadioGroup)dialogLayout.findViewById(R.id.strictness_radio_group);

        Settings.Strictness selected = Settings.getStrictness(getActivity().getApplicationContext());
        if(selected == Settings.Strictness.CASUAL){
            radioGroup.check(R.id.strictness_casual_button);
        } else {
            radioGroup.check(R.id.strictness_strict_button);
        }

        return d;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if(radioGroup.getCheckedRadioButtonId() == R.id.strictness_casual_button){
            Settings.setStrictness(Settings.Strictness.CASUAL, getActivity().getApplicationContext());
        } else {
            Settings.setStrictness(Settings.Strictness.STRICT, getActivity().getApplicationContext());
        }
    }
}
