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
import android.widget.TextView;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.Settings;

public class ClueDialog extends DialogFragment implements DialogInterface.OnClickListener{
    RadioGroup radioGroup;

    public ClueDialog(){
        // empty constructor
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final FrameLayout frameView = new FrameLayout(getActivity());
        Dialog d = new AlertDialog.Builder(getActivity())
                .setTitle("Study Clue Type")
                .setView(frameView)
                .setPositiveButton("OK", this)
                .setNegativeButton("Cancel", null).create();

        LayoutInflater inflater = d.getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.fragment_clue_dialog, frameView);
        radioGroup = (RadioGroup)dialogLayout.findViewById(R.id.clue_radio_group);

        {
            TextView casualDesc = (TextView) dialogLayout.findViewById(R.id.clue_meanings_description);
            casualDesc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    radioGroup.check(R.id.clue_meanings_button);
                }
            });
        }

        {
            TextView casualOrderedDesc = (TextView) dialogLayout.findViewById(R.id.clue_vocab_description);
            casualOrderedDesc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    radioGroup.check(R.id.clue_vocab_button);
                }
            });
        }

        Settings.ClueType selected = Settings.getClueType(getActivity().getApplicationContext());
        if(selected == Settings.ClueType.MEANING) {
            radioGroup.check(R.id.clue_meanings_button);
        } else if(selected == Settings.ClueType.VOCAB){
            radioGroup.check(R.id.clue_vocab_button);
        } else {
            radioGroup.check(R.id.clue_meanings_button);
        }

        return d;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if(radioGroup.getCheckedRadioButtonId() == R.id.clue_vocab_button){
            Settings.setClueType(Settings.ClueType.VOCAB, getActivity().getApplicationContext());
        } else {
            Settings.setClueType(Settings.ClueType.MEANING, getActivity().getApplicationContext());
        }
    }
}
