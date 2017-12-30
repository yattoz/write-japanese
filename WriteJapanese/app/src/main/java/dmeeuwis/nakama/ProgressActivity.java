package dmeeuwis.nakama;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import java.util.Map;
import java.util.Set;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.ProgressTracker.Progress;
import dmeeuwis.nakama.primary.KanjiMasterActivity;
import dmeeuwis.nakama.teaching.TeachingActivity;
import dmeeuwis.nakama.views.AppColors;
import dmeeuwis.nakama.views.LockCheckerInAppBillingService;
import dmeeuwis.nakama.views.PurchaseDialog;
import dmeeuwis.nakama.views.SingleBarChart;
import dmeeuwis.nakama.views.SingleBarChart.BarChartEntry;


public class ProgressActivity extends AppCompatActivity implements OnItemClickListener, LockCheckerHolder {

	String callingClass;
	String callingPath;
	char[] chars;
	String[] strings;
	Map<Character, Progress> scores;
	
	LockChecker lc;

	String characterList;
	CharacterStudySet charSet;
	
	GridView characterGrid;
	CharacterGridAdapter gridAdapter;
    SingleBarChart chart;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("nakama", "ProgressActivity oncreate starting");
		setContentView(R.layout.activity_progress);

    	ActionBar actionBar = this.getSupportActionBar();
    	actionBar.setDisplayHomeAsUpEnabled(true);

        characterGrid = (GridView)this.findViewById(R.id.character_grid);
        chart = (SingleBarChart)this.findViewById(R.id.barChart);

    	
		Log.i("nakama", "ProgressActivity onCreate finished.");
	}

    @Override public void onResume(){
        super.onResume();

		Bundle params;
		Intent intent = getIntent();
		if(intent == null) {
			startActivity(new Intent(this, KanjiMasterActivity.class));
			return;
		} else {
			params = getIntent().getExtras();
			if (params == null) {
				startActivity(new Intent(this, KanjiMasterActivity.class));
				return;
			}
		}

        callingClass = params.getString("parent");
        callingPath = params.getString(Constants.KANJI_PATH_PARAM);

        if(lc != null){ lc.dispose(); }
        lc = new LockCheckerInAppBillingService(this);

        charSet = CharacterSets.fromName(this, callingPath, lc);
        charSet.load(this.getApplicationContext(), CharacterStudySet.LoadProgress.LOAD_SET_PROGRESS);
        scores = charSet.getRecordSheet();
        characterList = charSet.charactersAsString();

        Log.i("nakama", "Seeing SRS schedule as: " + charSet.getSrsScheduleString());

        chars = characterList.toCharArray();
        strings = new String[chars.length];
        for(int i = 0; i < chars.length; i++){
            strings[i] = Character.toString(chars[i]);
        }

        gridAdapter = new CharacterGridAdapter(this, characterList, charSet.availableCharactersSet(), scores);
        characterGrid.setAdapter(gridAdapter);
        characterGrid.setOnItemClickListener(this);

        final int gridFontSizeDp = 48 + 6;
        Resources res = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, gridFontSizeDp , res.getDisplayMetrics()); // 12 * 2 = 24 padding
        characterGrid.setColumnWidth((int)px);

        int passedCount = 0, trainingCount = 0, failedCount = 0, timedReviewCount = 0;
        for(Map.Entry<Character, Progress> s: scores.entrySet()){
            Progress r = s.getValue();
            //Log.i("nakama-progress", "Progress entry: " + s.getKey() + " has progress " + s.getValue());
            if(r == Progress.PASSED){
                passedCount++;
            } else if(r == Progress.REVIEWING){
                trainingCount++;
            } else if(r == Progress.TIMED_REVIEW){
                timedReviewCount++;
            } else if(r == Progress.FAILED){
                failedCount++;
            }
        }
        int unknownCount = characterList.length() - passedCount - trainingCount - failedCount;
        Log.d("nakama", "Counted progress: scores has " + scores.size() + " entries; passed=" + passedCount +
                "; training=" + trainingCount + "; failed=" + failedCount + "; unknwon=" + unknownCount);

        chart.setPercents(
                new BarChartEntry((int)(100*(float)passedCount / characterList.length()), AppColors.PASSED_BORDER, AppColors.PASSED_COLOR, "Passed"),
                new BarChartEntry((int)(100*(float)timedReviewCount / characterList.length()), AppColors.TIMED_REVIEW_BORDER, AppColors.TIMED_REVIEW_COLOR, "Timed Review"),
                new BarChartEntry((int)(100*(float)trainingCount / characterList.length()), AppColors.TRAINING_BORDER, AppColors.TRAINING_COLOR, "Reviewing"),
                new BarChartEntry((int)(100*(float)failedCount / characterList.length()), AppColors.FAILED_BORDER, AppColors.FAILED_COLOR, "Failed"),
                new BarChartEntry((int)(100*(float)unknownCount / characterList.length()), AppColors.UNKNOWN_BORDER, AppColors.UNKNOWN_COLOR, "Untested")
        );

        TextView chartLegend = (TextView)findViewById(R.id.chartLegend);
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


			PopupMenu popup = new PopupMenu(this, view);
            //Inflating the Popup using xml file
            popup.getMenuInflater().inflate(R.menu.activity_progress_popup_menu, popup.getMenu());

            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {

					if(item.getItemId() == R.id.popup_set_failed_progress) {
						charSet.resetTo(selected, Progress.FAILED);
						gridAdapter.updateScores(charSet.getRecordSheet());

					} else if(item.getItemId() == R.id.popup_set_reviewing_progress) {
						charSet.resetTo(selected, Progress.REVIEWING);
						gridAdapter.updateScores(charSet.getRecordSheet());

					} else if(item.getItemId() == R.id.popup_set_timed_reviewing_progress) {
						charSet.resetTo(selected, Progress.TIMED_REVIEW);
						gridAdapter.updateScores(charSet.getRecordSheet());

					} else if(item.getItemId() == R.id.popup_set_known) {
						charSet.resetTo(selected, Progress.PASSED);
						gridAdapter.updateScores(charSet.getRecordSheet());

					} else if(item.getItemId() == R.id.popup_see_study_screen) {
						Intent teachIntent = new Intent(ProgressActivity.this, TeachingActivity.class);
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
			pd.show(this.getSupportFragmentManager(), "purchase");
		}
	}

    @Override
    public LockChecker getLockChecker() {
        return this.lc;
    }

    public static class CharacterGridAdapter extends BaseAdapter {

	    final private Context context;
	    final private String characterList;
	    final private Set<Character> unlockedCharacterList;
	    final private GridView.LayoutParams params = new GridView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        private Map<Character, Progress> scores;
	    final private int textSize = 48;
        final private char[] chars;

	    public CharacterGridAdapter(Context context, String characterList, Set<Character> unlockedCharacterList, Map<Character, Progress> scores) {
	        this.context = context;
	        this.characterList = characterList;
	        this.unlockedCharacterList = unlockedCharacterList;
            this.scores = scores;
            this.chars = characterList.toCharArray();

	        Log.d("nakama", "Making CharacterGridAdapter: characterList size is " + this.characterList.length() + " vs unlocked set size " + this.unlockedCharacterList.size());
	    }

	    public void updateScores(Map<Character, Progress> newScores){
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
	        Progress score = scores.get(character);
	        
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
            	if(score == Progress.FAILED){
            		tv.setBackgroundColor(AppColors.FAILED_COLOR);

				} else if (score == Progress.TIMED_REVIEW){
					tv.setBackgroundColor(AppColors.TIMED_REVIEW_COLOR);

            	} else if (score == Progress.REVIEWING){
            		tv.setBackgroundColor(AppColors.TRAINING_COLOR);

            	} else if(score == Progress.PASSED) {
            		tv.setBackgroundColor(AppColors.PASSED_COLOR);
            	}  else {
            		tv.setBackgroundColor(0xa0ffffff);
                   }
			}

            tv.setText(String.valueOf(chars[position]));
	        return tv;
	    }
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d("nakama", "ActionBar: in onOptionsItemSelected: " + item.toString());
	
		if(item.getItemId() == android.R.id.home){
			try {
				Intent backUp = new Intent(this, Class.forName(callingClass));
				startActivity(backUp);
			} catch (ClassNotFoundException e) {
				Log.e("nakama", "ClassNotFoundException while returning to parent. callingClass is " + callingClass, e);
				return false;
			} catch (Throwable t) {
				Log.e("nakama", "Throwable while returning to parent. callingClass is " + callingClass, t);
				return false;
			}
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d("nakama", "onActivityResult(" + requestCode + "," + resultCode + "," + data);

    	if (!lc.handleActivityResult(requestCode, resultCode, data)) {
    		super.onActivityResult(requestCode, resultCode, data);
    	} else {
    		Log.d("nakama", "AbstractMasterActivity: onActivityResult handled by IABUtil.");
    	}
   	}

    @Override
    protected void onDestroy() {
		if(this.lc != null) {
			this.lc.dispose();
		}
        super.onDestroy();
    }

    @Override protected void onNewIntent(Intent intent){
		Log.i("nakama", "ProgressActivity lifecycle onNewIntent");
		super.onNewIntent(intent);
        this.setIntent(intent);
    }
}
