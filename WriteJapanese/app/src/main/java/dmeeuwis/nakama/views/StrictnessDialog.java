package dmeeuwis.nakama.views;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import dmeeuwis.kanjimaster.R;

public class StrictnessDialog extends DialogFragment {

    interface OnStrictnessChangeListener {
        void onChange(boolean strict);
    }

    public StrictnessDialog(){
        // empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_strictness_dialog, container);

        RadioGroup r = (RadioGroup)view.findViewById(R.id.strictness_radio_group);

        return view;
    }
}
