package dmeeuwis.nakama.primary;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.views.PieProgressView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CharacterSetStatusFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CharacterSetStatusFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CharacterSetStatusFragment extends Fragment {
    private static final String ARG_CHARSET = "charset";

    private CharacterStudySet charSet;

    private OnFragmentInteractionListener mListener;

    /**
     * @param charset Name of the CharacterStudySet.
     * @return A new instance of fragment CharacterSetStatusFragment.
     */
    public static CharacterSetStatusFragment newInstance(String charset) {
        CharacterSetStatusFragment fragment = new CharacterSetStatusFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHARSET, charset);
        fragment.setArguments(args);
        return fragment;
    }

    public CharacterSetStatusFragment() {
        // Required empty public constructor
    }

    public void setDate(int year, int month, int day){
        Toast.makeText(this.getActivity(), "Set Date tArget: " + year + ", " + month + ", " + day, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setCharset(CharacterStudySet charSet){
        this.charSet = charSet;
        View v = getView();

        TextView charLabel = (TextView)v.findViewById(R.id.charset_label);
        charLabel.setText(charSet.name);

        TextView descLabel = (TextView)v.findViewById(R.id.charset_desc);
        descLabel.setText(charSet.description);

        PieProgressView pie = (PieProgressView)v.findViewById(R.id.charset_progress_chart);
        pie.setProgressLevels(30, 20, 20, 30);

        TextView progressText = (TextView)v.findViewById(R.id.charset_progress_text);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_character_set_status, container, false);

        Button setDateButton = (Button)view.findViewById(R.id.charset_progress_set_date);
        setDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new DatePickerFragment(CharacterSetStatusFragment.this);
                newFragment.show(getActivity().getFragmentManager(), "datePicker");
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        CharacterSetStatusFragment frag;

        public DatePickerFragment(CharacterSetStatusFragment frag){
           this.frag = frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            frag.setDate(year, month, day);
        }
    }
}