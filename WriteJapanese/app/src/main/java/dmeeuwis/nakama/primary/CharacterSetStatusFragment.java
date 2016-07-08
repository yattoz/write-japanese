package dmeeuwis.nakama.primary;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.ReminderManager;
import dmeeuwis.nakama.data.CharacterStudySet;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CharacterSetStatusFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CharacterSetStatusFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, OnGoalPickListener {

    private enum CharsetColor {
        HIRAGANA(R.color.BlueGrey), KATAKANA(R.color.DarkSlateBlue),
        J1(R.color.DarkCyan), J2(R.color.RoyalBlue), J3(R.color.DarkSlateGray),
        J4(R.color.IndianRed), J5(R.color.DarkGreen), J6(R.color.BlueGrey2);

        final int color;

        CharsetColor(int color) {
            this.color = color;
        }
    }

    private static final String ARG_CHARSET = "charset";

    private CharacterStudySet charSet;

    private OnFragmentInteractionListener mListener;

    private TextView progressText, progressGoalsText, charLabel, descLabel;
    private View goalPresentArea, goalAbsentArea;
    private CheckBox notifications;

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

    public void setGoal(int year, int month, int day) {
        charSet.setStudyGoal(new GregorianCalendar(year, month, day));
        charSet.save(this.getActivity().getApplicationContext());
        updateProgress();
        updateGoals();
    }

    public void updateProgress() {
        CharacterStudySet.SetProgress sp = charSet.getProgress();
        progressText.setText(
                String.format("%5d Known\n%5d Reviewing\n%5d Unknown",
                       sp.passed, sp.reviewing + sp.failing, sp.unknown));
    }

    private void updateGoals() {
        if (charSet.hasStudyGoal()) {
            CharacterStudySet.GoalProgress gp = charSet.getGoalProgress();
            DateFormat df = DateFormat.getDateInstance();

            GregorianCalendar now = new GregorianCalendar();
            Log.i("nakama", "Now is " + df.format(now.getTime()));
            Log.i("nakama", "Goal is " + df.format(gp.goal.getTime()));

            if (df.format(now.getTime()).equals(df.format(gp.goal.getTime()))) {
                progressGoalsText.setText(
                        "Today is the last day for your goal!\n" +
                                "Target date: " + df.format(gp.goal.getTime()) + "\n" +
                                "Kanji Remaining: " + gp.remaining + "\n");
            } else if (gp.goal.before(now)) {
                progressGoalsText.setText(
                        "Your study goal has passed!\n" +
                                "Target date: " + df.format(gp.goal.getTime()) + "\n" +
                                "Kanji Remaining: " + gp.remaining + "\n");

            } else {
                progressGoalsText.setText(
                        "Target date: " + df.format(gp.goal.getTime()) + "\n" +
                                "Days Remaining: " + gp.daysLeft + "\n" +
                                "Kanji Needed Per Day: " + gp.neededPerDay + "\n");
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

    public void setCharset(CharacterStudySet charSet) {
        this.charSet = charSet;

        charLabel.setText(charSet.name);
        descLabel.setText(charSet.description);

        CharsetColor color = CharsetColor.valueOf(charSet.pathPrefix.toUpperCase());
        charLabel.setBackgroundColor(getResources().getColor(color.color));
        descLabel.setBackgroundColor(getResources().getColor(color.color));
        charLabel.setTextColor(Color.WHITE);
        descLabel.setTextColor(Color.WHITE);

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

        Button setGoalsButton = (Button) view.findViewById(R.id.charset_progress_set_date);
        setGoalsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new DatePickerFragment();
                newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
            }
        });

        progressText = (TextView) view.findViewById(R.id.charset_progress_text);
        progressGoalsText = (TextView) view.findViewById(R.id.charset_goal_progress_text);

        charLabel = (TextView) view.findViewById(R.id.charset_label);
        descLabel = (TextView) view.findViewById(R.id.charset_desc);

        goalPresentArea = view.findViewById(R.id.goal_present_space);
        goalAbsentArea = view.findViewById(R.id.goal_absent_space);

        Button progressGoalClearButton = (Button) view.findViewById(R.id.charset_progress_clear_goal);
        progressGoalClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                charSet.clearStudyGoal();
                updateGoals();
            }
        });

        notifications = (CheckBox) view.findViewById(R.id.goal_notifications_enabled);

        return view;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i("nakama", "onCheckedChanged listener fired; checked is " + isChecked);
        if (isChecked) {
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
        public DatePickerFragment() {
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
            OnGoalPickListener parent = (OnGoalPickListener) getActivity();
            parent.setGoal(year, month, day);
        }
    }
}