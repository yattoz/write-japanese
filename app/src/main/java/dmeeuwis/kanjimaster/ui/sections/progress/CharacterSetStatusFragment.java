package dmeeuwis.kanjimaster.ui.sections.progress;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.ui.sections.primary.OnFragmentInteractionListener;
import dmeeuwis.kanjimaster.logic.data.CharacterStudySet;
import dmeeuwis.kanjimaster.ui.sections.primary.OnGoalPickListener;
import dmeeuwis.kanjimaster.ui.views.AppColors;

/**
 * Shows information and overall user progress on a particular character set.
 */
public class CharacterSetStatusFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, OnGoalPickListener {

    private enum CharsetColor {
        HIRAGANA(R.color.BlueGrey), KATAKANA(R.color.DarkSlateBlue),
        J1(R.color.DarkCyan), J2(R.color.RoyalBlue), J3(R.color.DarkSlateGray),
        J4(R.color.IndianRed), J5(R.color.DarkGreen), J6(R.color.BlueGrey2),
        JHS(R.color.DarkCyan),

        JLPT5(R.color.DarkCyan),
        JLPT4(R.color.RoyalBlue),
        JLPT3(R.color.DarkGreen),
        JLPT2(R.color.Gray),
        JLPT1(R.color.DarkGray),

        CUSTOM(R.color.DarkGoldenrod);

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
    private PieChart progressPieChart;

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
        charSet.saveGoals();
        updateProgress();
        updateGoals();
    }

    public void updateProgress() {
        updateProgress(0);
    }

    private void updateProgress(int animationDelay) {
        Log.i("nakama", "Updating progress UI");

        CharacterStudySet.SetProgress sp = charSet.getProgress();
        if(sp == null){
            Log.d("nakama", "CharacterSetStatusFragment.updateProgress: progress not yet set, returning.");
            return;
        }

        if(progressText == null){
            Log.d("nakama", "CharacterSetStatusFragment.updateProgress: UI not instantiated, returning.");
            return;
        }

        progressText.setText(Html.fromHtml(
                String.format("<div style='text-align: center; width: 100%%;'>" +
                              "<span style='color: %s;'>%5d Unknown</span>" +
                              "<span style='color: %s;'>%5d Incorrect</span> <br />" +
                              "<span style='color: %s;'>%5d Reviewing</span> " +
                              "<span style='color: %s;'>%5d Timed Reviewing</span> " +
                              "<span style='color: %s;'>%5d Completed</span> " +
                              "</div>",
                        AppColors.UNKNOWN_BORDER_HEX, sp.unknown,
                        AppColors.FAILED_BORDER_HEX, sp.failing,
                        AppColors.TRAINING_COLOR_HEX, sp.reviewing,
                        AppColors.TIMED_REVIEW_BORDER_HEX, sp.timedReviewing,
                        AppColors.PASSED_BORDER_HEX, sp.passed)));

        if(progressPieChart != null){
            List<PieEntry> values = new ArrayList<>();
            int[] colours = new int[5];
            int colour_i = 0;
            float total = sp.failing + sp.passed + sp.reviewing + sp.timedReviewing + sp.unknown;

            if(sp.passed > 0) {
                values.add(new PieEntry(100 * sp.passed / total, "Completed"));
                colours[colour_i] = ColorTemplate.rgb(AppColors.PASSED_BORDER_HEX);
                colour_i += 1;
            }

            if(sp.failing > 0) {
                values.add(new PieEntry(100 * sp.failing / total,  "Incorrect"));
                colours[colour_i] = ColorTemplate.rgb(AppColors.FAILED_BORDER_HEX);
                colour_i += 1;
            }

            if(sp.reviewing > 0) {
                values.add(new PieEntry(100 * sp.reviewing / total,  "Reviewing"));
                colours[colour_i] = ColorTemplate.rgb(AppColors.TRAINING_BORDER_HEX);
                colour_i += 1;
            }

            if(sp.timedReviewing > 0) {
                values.add(new PieEntry(100 * sp.timedReviewing / total,  "Timed Review"));
                colours[colour_i] = ColorTemplate.rgb(AppColors.TIMED_REVIEW_BORDER_HEX);
                colour_i += 1;
            }

            if(sp.unknown > 0){
                values.add(new PieEntry(100 * sp.unknown / total, "Untested"));
                colours[colour_i] = ColorTemplate.rgb(AppColors.UNKNOWN_BORDER_HEX);
                colour_i += 1;
            }

            final PieDataSet pieData = new PieDataSet(values, "Study Progress");
            int[] usedColours = new int[colour_i];
            for(int i = 0; i < colour_i; i++){
                usedColours[i] = colours[i];
            }
            pieData.setColors(usedColours);
            pieData.setSliceSpace(3f);

            pieData.setValueFormatter(new PercentFormatter(new DecimalFormat("###")));
            pieData.setValueTextSize(11f);
            pieData.setValueTextColor(Color.WHITE);
            //pieData.setValueTypeface(mTfLight);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    progressPieChart.setData(new PieData(pieData));
                    progressPieChart.invalidate();
                }
            }, animationDelay);
        }
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

            notifications.setChecked(checkIfReminderExists(getActivity().getApplicationContext(), this.charSet));

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
        setCharset(charSet, 0);
    }

    public void setCharset(CharacterStudySet charSet, int animationDelay) {
        this.charSet = charSet;

        if(charLabel == null){
            return;
        }

        charLabel.setText(charSet.name);
        descLabel.setText(charSet.description);

        int color;
        if(charSet.systemSet) {
            CharsetColor cc = CharsetColor.valueOf(charSet.pathPrefix.toUpperCase(Locale.ENGLISH));
            color = cc.color;
        } else {
            color = R.color.BlueGrey;
        }
        charLabel.setBackgroundColor(getResources().getColor(color));
        descLabel.setBackgroundColor(getResources().getColor(color));
        charLabel.setTextColor(Color.WHITE);
        descLabel.setTextColor(Color.WHITE);

        updateProgress(animationDelay);

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
                charSet.saveGoals();
                updateGoals();
            }
        });

        notifications = (CheckBox) view.findViewById(R.id.goal_notifications_enabled);

        progressPieChart = (PieChart)view.findViewById(R.id.charset_progress_chart);
        if(progressPieChart != null){
            progressPieChart.setDescription(new Description());

            progressPieChart.setDrawHoleEnabled(true);
            progressPieChart.setHoleRadius(50f);
            progressPieChart.setHoleColor(Color.TRANSPARENT);
            progressPieChart.setCenterText("Study Progress");

            // color and alpha for inner lining of pie (not inside hole)
            progressPieChart.setTransparentCircleColor(Color.WHITE);
            progressPieChart.setTransparentCircleAlpha(110);

            progressPieChart.setRotationEnabled(false);

            progressPieChart.setMaxAngle(180);
            progressPieChart.setRotationAngle(180);
            progressPieChart.setCenterTextOffset(0, -20);

            // entry label styling
            progressPieChart.setEntryLabelColor(Color.WHITE);
            //progressPieChart.setEntryLabelTypeface(mTfRegular);
            progressPieChart.setEntryLabelTextSize(12f);

            progressPieChart.setCenterTextOffset(0, -20);
            progressPieChart.animateY(800, Easing.EasingOption.EaseInOutQuad);
            progressPieChart.setHighlightPerTapEnabled(false);
        }

        if(this.charSet != null){
            this.setCharset(this.charSet);
        }

        return view;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i("nakama", "onCheckedChanged listener fired; checked is " + isChecked);
        if (isChecked) {
            scheduleReminder(charSet);
        } else {
            descheduleReminder(charSet);
        }
    }

    private void scheduleReminder(CharacterStudySet charSet){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean(prefName(charSet), true);
        ed.apply();
    }

    private void descheduleReminder(CharacterStudySet set){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean(prefName(charSet), false);
        ed.apply();
    }

    public static boolean checkIfReminderExists(Context context, CharacterStudySet charSet) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(prefName(charSet), false);
    }

    private static String prefName(CharacterStudySet set){
        return "remind_" + set.pathPrefix;
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