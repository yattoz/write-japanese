package dmeeuwis.kanjimaster.ui.sections.progress;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import java.util.Set;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.Constants;
import dmeeuwis.kanjimaster.logic.data.CharacterSets;
import dmeeuwis.kanjimaster.logic.data.CharacterStudySet;
import dmeeuwis.kanjimaster.logic.data.ProgressTracker;
import dmeeuwis.kanjimaster.ui.billing.LockChecker;
import dmeeuwis.kanjimaster.ui.sections.teaching.TeachingActivity;
import dmeeuwis.kanjimaster.ui.views.AppColors;
import dmeeuwis.kanjimaster.ui.views.PurchaseDialog;
import dmeeuwis.kanjimaster.ui.views.SingleBarChart;

/**
 * A placeholder fragment containing a simple view.
 */
public class ProgressFragment extends Fragment implements AdapterView.OnItemClickListener {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    char[] chars;
    String[] strings;
    CharacterStudySet.SetProgress scores;

    LockChecker lc;

    String characterList;
    CharacterStudySet charSet;

    GridView characterGrid;
    ProgressActivity.CharacterGridAdapter gridAdapter;
    SingleBarChart chart;
    TextView chartLegend;

    String callingClass;
    String callingPath;

    public static ProgressFragment newInstance(int index) {
        ProgressFragment fragment = new ProgressFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);

        Log.i("nakama", "ProgressActivity onCreate finished.");
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_progress, container, false);

        characterGrid = root.findViewById(R.id.character_grid);
        chart = root.findViewById(R.id.barChart);
        chartLegend = root.findViewById(R.id.chartLegend);

        return root;
    }

    @Override public void onResume(){
        super.onResume();

        Bundle params = getArguments();
        callingClass = params.getString("parent");
        callingPath = params.getString(Constants.KANJI_PATH_PARAM);

        charSet = CharacterSets.fromName(callingPath, lc);
        charSet.load(CharacterStudySet.LoadProgress.LOAD_SET_PROGRESS);
        scores = charSet.getProgress();
        characterList = charSet.charactersAsString();

        Log.i("nakama", "Seeing SRS schedule as: " + charSet.getSrsScheduleString());

        chars = characterList.toCharArray();
        strings = new String[chars.length];
        for(int i = 0; i < chars.length; i++){
            strings[i] = Character.toString(chars[i]);
        }

        gridAdapter = new ProgressActivity.CharacterGridAdapter(getContext(), characterList, charSet.availableCharactersSet(), scores);
        characterGrid.setAdapter(gridAdapter);
        characterGrid.setOnItemClickListener(this);

        final int gridFontSizeDp = 48 + 6;
        Resources res = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, gridFontSizeDp , res.getDisplayMetrics()); // 12 * 2 = 24 padding
        characterGrid.setColumnWidth((int)px);

        chart.setPercents(
                new SingleBarChart.BarChartEntry((int)(100*(float)scores.passed / characterList.length() + 0.5), AppColors.PASSED_BORDER, AppColors.PASSED_COLOR, "Passed"),
                new SingleBarChart.BarChartEntry((int)(100*(float)scores.timedReviewing / characterList.length() + 0.5), AppColors.TIMED_REVIEW_BORDER, AppColors.TIMED_REVIEW_COLOR, "Timed Review"),
                new SingleBarChart.BarChartEntry((int)(100*(float)scores.reviewing / characterList.length() + 0.5), AppColors.TRAINING_BORDER, AppColors.TRAINING_COLOR, "Reviewing"),
                new SingleBarChart.BarChartEntry((int)(100*(float)scores.failing / characterList.length() + 0.5), AppColors.FAILED_BORDER, AppColors.FAILED_COLOR, "Failed"),
                new SingleBarChart.BarChartEntry((int)(100*(float)scores.unknown / characterList.length() + 0.5), AppColors.UNKNOWN_BORDER, AppColors.UNKNOWN_COLOR, "Untested")
        );

        chartLegend.setText(Html.fromHtml(
                "<font color='" + AppColors.PASSED_BORDER + "'>Passed</font> " +
                        "<font color='" + AppColors.TIMED_REVIEW_BORDER + "'>Timed Review</font> " +
                        "<font color='" + AppColors.TRAINING_BORDER + "'>Reviewing</font> " +
                        "<font color='" + AppColors.FAILED_BORDER + "'>Failed</font> " +
                        "<font color='" + AppColors.UNKNOWN_BORDER + "'>Untested</font>"));
    }

    @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Character selected = chars[position];

        if(charSet.availableCharactersSet().contains(selected)){


            PopupMenu popup = new PopupMenu(getContext(), view);
            //Inflating the Popup using xml file
            popup.getMenuInflater().inflate(R.menu.activity_progress_popup_menu, popup.getMenu());

            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {

                    if(item.getItemId() == R.id.popup_set_failed_progress) {
                        charSet.resetTo(selected, ProgressTracker.Progress.FAILED);
                        gridAdapter.updateScores(charSet.getProgress());

                    } else if(item.getItemId() == R.id.popup_set_reviewing_progress) {
                        charSet.resetTo(selected, ProgressTracker.Progress.REVIEWING);
                        gridAdapter.updateScores(charSet.getProgress());

                    } else if(item.getItemId() == R.id.popup_set_timed_reviewing_progress) {
                        charSet.resetTo(selected, ProgressTracker.Progress.TIMED_REVIEW);
                        gridAdapter.updateScores(charSet.getProgress());

                    } else if(item.getItemId() == R.id.popup_set_known) {
                        charSet.resetTo(selected, ProgressTracker.Progress.PASSED);
                        gridAdapter.updateScores(charSet.getProgress());

                    } else if(item.getItemId() == R.id.popup_see_study_screen) {
                        Intent teachIntent = new Intent(getContext(), TeachingActivity.class);
                        Bundle passParams = new Bundle();
                        passParams.putString("parent", callingClass);
                        passParams.putChar(Constants.KANJI_PARAM, selected);
                        passParams.putString(Constants.KANJI_PATH_PARAM, callingPath);
                        Log.d("nakama", "ProgressActivity: passing path " + callingPath + " to TeachingActivity.");
                        teachIntent.putExtras(passParams);
                        startActivity(teachIntent);
                    }
                    return true;
                }
            });

            popup.show();//showing popup menu


        } else {
            PurchaseDialog pd = PurchaseDialog.make(PurchaseDialog.DialogMessage.LOCKED_CHARACTER);
            pd.show(this.getActivity().getSupportFragmentManager(), "purchase");
        }
    }

    public static class CharacterGridAdapter extends BaseAdapter {

        final private Context context;
        final private String characterList;
        final private Set<Character> unlockedCharacterList;
        final private GridView.LayoutParams params = new GridView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        private CharacterStudySet.SetProgress scores;
        final private int textSize = 48;
        final private char[] chars;

        public CharacterGridAdapter(Context context, String characterList, Set<Character> unlockedCharacterList, CharacterStudySet.SetProgress scores) {
            this.context = context;
            this.characterList = characterList;
            this.unlockedCharacterList = unlockedCharacterList;
            this.scores = scores;
            this.chars = characterList.toCharArray();

            Log.d("nakama", "Making CharacterGridAdapter: characterList size is " + this.characterList.length() + " vs unlocked set size " + this.unlockedCharacterList.size());
        }

        public void updateScores(CharacterStudySet.SetProgress newScores){
            this.scores = newScores;
            notifyDataSetChanged();
        }

        public int getCount() {
            return characterList.length();
        }

        public Object getItem(int position) {
            return Character.valueOf(chars[position]);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            Character character = chars[position];
            ProgressTracker.Progress score = scores.perChar.get(character);

            if (convertView == null) {
                tv = new TextView(context);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
                tv.setLayoutParams(params);
            } else {
                tv = (TextView) convertView;
            }


            boolean showLock = (unlockedCharacterList.size() != characterList.length()) && !unlockedCharacterList.contains(character);
            if(showLock){
                tv.setBackgroundResource(R.drawable.ic_lock_gray);
                Drawable bg = tv.getBackground();
                bg.setAlpha(50);
            } else {
                if(score == ProgressTracker.Progress.FAILED){
                    tv.setBackgroundColor(AppColors.FAILED_COLOR);

                } else if (score == ProgressTracker.Progress.TIMED_REVIEW){
                    tv.setBackgroundColor(AppColors.TIMED_REVIEW_COLOR);

                } else if (score == ProgressTracker.Progress.REVIEWING){
                    tv.setBackgroundColor(AppColors.TRAINING_COLOR);

                } else if(score == ProgressTracker.Progress.PASSED) {
                    tv.setBackgroundColor(AppColors.PASSED_COLOR);
                }  else {
                    tv.setBackgroundColor(0xa0ffffff);
                }
            }

            tv.setText(String.valueOf(chars[position]));
            return tv;
        }
    }


}