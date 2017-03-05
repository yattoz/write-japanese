package dmeeuwis.nakama.primary;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import dmeeuwis.kanjimaster.R;

public class ProgressSettingsDialog extends DialogFragment {


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.setRetainInstance(true);

        final FrameLayout frameView = new FrameLayout(getActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle("Set Progression Options")
                .setView(frameView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog d = builder.create();

        LayoutInflater inflater = d.getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.fragment_character_progression, frameView);

        {
            Spinner s = (Spinner) dialogLayout.findViewById(R.id.max_incorrect_spinner);
            s.setAdapter(ArrayAdapter.createFromResource(getActivity(), R.array.characters_spinner, android.R.layout.simple_spinner_dropdown_item));
        }

        {
            Spinner s = (Spinner) dialogLayout.findViewById(R.id.incorrect_group_leave_after_spinner);
            s.setAdapter(ArrayAdapter.createFromResource(getActivity(), R.array.leave_after_spinner, android.R.layout.simple_spinner_dropdown_item));
        }

        {
            Spinner s = (Spinner) dialogLayout.findViewById(R.id.max_reviewing_spinner);
            s.setAdapter(ArrayAdapter.createFromResource(getActivity(), R.array.characters_spinner, android.R.layout.simple_spinner_dropdown_item));
        }

        {
            Spinner s = (Spinner) dialogLayout.findViewById(R.id.reviewing_group_leave_after_spinner);
            s.setAdapter(ArrayAdapter.createFromResource(getActivity(), R.array.leave_after_spinner, android.R.layout.simple_spinner_dropdown_item));
        }

        {
            Spinner s = (Spinner) dialogLayout.findViewById(R.id.when_to_introduce_spinner);
            s.setAdapter(ArrayAdapter.createFromResource(getActivity(), R.array.when_to_introduce_spinner, android.R.layout.simple_spinner_dropdown_item));
        }
        // Create the AlertDialog object and return it
        return d;
    }
}
