package dmeeuwis.nakama.primary;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.ReminderManager;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.views.CircleLabel;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CharacterSetStatusFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CharacterSetStatusFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CharacterSetStatusFragment extends Fragment implements CompoundButton.OnCheckedChangeListener{
    private static final String ARG_CHARSET = "charset";

    private CharacterStudySet charSet;

    private OnFragmentInteractionListener mListener;

    private CircleLabel circleLabel;
    private TextView progressText, progressGoalsText, charLabel, descLabel;
    private View goalPresentArea, goalAbsentArea;
    private CheckBox notifications;

    private Map<String, String> charsetCircleLabels;
    private Map<String, String> charsetCircleColors;

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

    private void setDate(int year, int month, int day){
        charSet.setStudyGoal(new GregorianCalendar(year, month, day));
        updateProgress();
        updateGoals();
    }

    private void updateProgress(){
        CharacterStudySet.SetProgress sp = charSet.getProgress();
        progressText.setText("Known: " + sp.passed + "\n" +
                "Reviewing: " + sp.reviewing + "\n" +
                "Mistaken: " + sp.failing + "\n" +
                "Unknown: " + sp.unknown);
    }

    private void updateGoals(){
        if(charSet.hasStudyGoal()){
            CharacterStudySet.GoalProgress gp = charSet.getGoalProgress();
            DateFormat df = DateFormat.getDateInstance();

            GregorianCalendar now = new GregorianCalendar();
            Log.i("nakama", "Now is " + df.format(now.getTime()));
            Log.i("nakama", "Goal is " + df.format(gp.goal.getTime()));

            if(df.format(now.getTime()).equals(df.format(gp.goal.getTime()))){
               progressGoalsText.setText(
                       "Today is the last day for your goal!\n" +
                               "Target date: " + df.format(gp.goal.getTime()) + "\n" +
                               "Kanji Remaining: " + gp.remaining + "\n");
            } else if(gp.goal.before(now)){
               progressGoalsText.setText(
                       "Your study goal has passed!\n" +
                       "Target date: " + df.format(gp.goal.getTime()) + "\n" +
                       "Kanji Remaining: " + gp.remaining + "\n");

            } else {
               progressGoalsText.setText(
                       "Target date: " + df.format(gp.goal.getTime()) + "\n" +
                               "Days Remaining: " + gp.daysLeft + "\n" +
                               "Kanji Needed Per Day: " + gp.neededPerDay + "\n" );
                              // + (gp.neededPerDay == gp.scheduledPerDay ? "" :
                              // "Kanji Needed Per Day: " + gp.neededPerDay));
            }
            goalAbsentArea.setVisibility(View.GONE);
            goalPresentArea.setVisibility(View.VISIBLE);

            notifications.setChecked(
                ReminderManager.reminderExists(this.getActivity().getApplicationContext(), this.charSet));

        } else {
            goalAbsentArea.setVisibility(View.VISIBLE);
            goalPresentArea.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setCharset(CharacterStudySet charSet){
        this.charSet = charSet;

        charLabel.setText(charSet.name);
        descLabel.setText(charSet.description);

        //circleLabel.setLabel(charSet.pathPrefix);
        //circleLabel.setLabel(charSet.pathPrefix);

        updateProgress();

        notifications.setOnCheckedChangeListener(null);
        updateGoals();
        notifications.setOnCheckedChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_character_set_status, container, false);

        //circleLabel = (CircleLabel)view.findViewById(R.id.circleLabel);

        Button setGoalsButton = (Button)view.findViewById(R.id.charset_progress_set_date);
        setGoalsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new DatePickerFragment(CharacterSetStatusFragment.this);
                newFragment.show(getActivity().getFragmentManager(), "datePicker");
            }
        });

        progressText = (TextView)view.findViewById(R.id.charset_progress_text);
        progressGoalsText = (TextView)view.findViewById(R.id.charset_goal_progress_text);

        charLabel = (TextView)view.findViewById(R.id.charset_label);
        descLabel = (TextView)view.findViewById(R.id.charset_desc);

        goalPresentArea = view.findViewById(R.id.goal_present_space);
        goalAbsentArea = view.findViewById(R.id.goal_absent_space);

        Button progressGoalClearButton = (Button)view.findViewById(R.id.charset_progress_clear_goal);
        progressGoalClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                charSet.clearStudyGoal();
                updateGoals();
            }
        });

        notifications = (CheckBox)view.findViewById(R.id.goal_notifications_enabled);

        return view;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i("nakama", "onCheckedChanged listener fired; checked is " + isChecked);
        if(isChecked){
            ReminderManager.scheduleRemindersFor(
                    CharacterSetStatusFragment.this.getActivity().getApplicationContext(),
                    CharacterSetStatusFragment.this.charSet);
        } else {
            ReminderManager.clearReminders(
                    CharacterSetStatusFragment.this.getActivity().getApplicationContext(),
                    CharacterSetStatusFragment.this.charSet);

        }
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