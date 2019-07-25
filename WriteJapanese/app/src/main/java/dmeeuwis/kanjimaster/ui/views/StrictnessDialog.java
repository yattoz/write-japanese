package dmeeuwis.kanjimaster.ui.views;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.data.Settings;
import dmeeuwis.kanjimaster.logic.data.SettingsFactory;

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
        radioGroup = dialogLayout.findViewById(R.id.strictness_radio_group);

        {
            TextView casualDesc = dialogLayout.findViewById(R.id.strictness_casual_description);
            casualDesc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    radioGroup.check(R.id.strictness_casual_button);
                }
            });
        }

        {
            TextView casualOrderedDesc = dialogLayout.findViewById(R.id.strictness_casual_ordered_description);
            casualOrderedDesc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    radioGroup.check(R.id.strictness_casual_ordered_button);
                }
            });
        }

        {
            TextView strictDesc = dialogLayout.findViewById(R.id.strictness_strict_description);
            strictDesc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    radioGroup.check(R.id.strictness_strict_button);
                }
            });
        }

        Settings.Strictness selected = SettingsFactory.get().getStrictness();
        if(selected == Settings.Strictness.CASUAL) {
            radioGroup.check(R.id.strictness_casual_button);
        } else if(selected == Settings.Strictness.CASUAL_ORDERED){
            radioGroup.check(R.id.strictness_casual_ordered_button);
        } else {
            radioGroup.check(R.id.strictness_strict_button);
        }

        return d;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if(radioGroup.getCheckedRadioButtonId() == R.id.strictness_casual_button){
            SettingsFactory.get().setStrictness(Settings.Strictness.CASUAL);
        } else  if(radioGroup.getCheckedRadioButtonId() == R.id.strictness_casual_ordered_button){
            SettingsFactory.get().setStrictness(Settings.Strictness.CASUAL_ORDERED);
        } else {
            SettingsFactory.get().setStrictness(Settings.Strictness.STRICT);
        }
    }
}
