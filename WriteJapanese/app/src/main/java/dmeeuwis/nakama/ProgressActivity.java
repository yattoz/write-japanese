package dmeeuwis.nakama;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
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
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.data.ProgressTracker.Progress;
import dmeeuwis.nakama.primary.Iid;
import dmeeuwis.nakama.teaching.TeachingActivity;
import dmeeuwis.nakama.views.LockCheckerInAppBillingService;
import dmeeuwis.nakama.views.PurchaseDialog;
import dmeeuwis.nakama.views.SingleBarChart;
import dmeeuwis.nakama.views.SingleBarChart.BarChartEntry;

public class ProgressActivity extends ActionBarActivity implements OnItemClickListener, LockCheckerHolder {

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
	
	static final int PASSED_BORDER = Color.parseColor("#006C02");
	static final int PASSED_COLOR = Color.parseColor("#A0DAFFDD");
	
	static final int TRAINING_BORDER = Color.parseColor("#3743FF");
	static final int TRAINING_COLOR = Color.parseColor("#A0D3D6FF");
	
	static final int FAILED_BORDER = Color.parseColor("#B70000");
	static final int FAILED_COLOR = Color.parseColor("#A0FFD3DB");
	
	static final int UNKNOWN_BORDER = Color.parseColor("#585555");
	static final int UNKNOWN_COLOR = Color.parseColor("#A0E2E2E2");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("nakama", "ProgressActivity oncreate starting");
		setContentView(R.layout.activity_progress);

    	ActionBar actionBar = this.getSupportActionBar();
    	actionBar.setDisplayHomeAsUpEnabled(true);

        characterGrid = (GridView)this.findViewById(R.id.character_grid);
        chart = (SingleBarChart)this.findViewById(R.id.barChart);

    	
		Log.i("nakama", "ProgressActivity oncreate finished.");
	}

    @Override public void onResume(){
        super.onResume();

        Bundle params = getIntent().getExtras();
        callingClass = params.getString("parent");
        callingPath = params.getString(Constants.KANJI_PATH_PARAM);

        DictionarySet dictSet = new DictionarySet(this.getApplicationContext());
        if(lc != null){ lc.dispose(); }
        lc = new LockCheckerInAppBillingService(this);

        charSet = CharacterSets.fromName(this, callingPath, lc);
        charSet.load();
        scores = charSet.getRecordSheet();
        characterList = charSet.charactersAsString();

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

        int passedCount = 0, trainingCount = 0, failedCount = 0;
        for(Map.Entry<Character, Progress> s: scores.entrySet()){
            Progress r = s.getValue();
            //Log.i("nakama-progress", "Progress entry: " + s.getKey() + " has progress " + s.getValue());
            if(r == Progress.PASSED){
                passedCount++;
            } else if(r == Progress.REVIEWING){
                trainingCount++;
            } else if(r == Progress.FAILED){
                failedCount++;
            }
        }
        int unknownCount = characterList.length() - passedCount - trainingCount - failedCount;
        Log.d("nakama", "Counted progress: scores has " + scores.size() + " entries; passed=" + passedCount +
                "; training=" + trainingCount + "; failed=" + failedCount + "; unknwon=" + unknownCount);

        chart.setPercents(new BarChartEntry((int)(100*(float)passedCount / characterList.length()), PASSED_BORDER, PASSED_COLOR, "Passed"),
                new BarChartEntry((int)(100*(float)trainingCount / characterList.length()), TRAINING_BORDER, TRAINING_COLOR, "Reviewing"),
                new BarChartEntry((int)(100*(float)failedCount / characterList.length()), FAILED_BORDER, FAILED_COLOR, "Failed"),
                new BarChartEntry((int)(100*(float)unknownCount / characterList.length()), UNKNOWN_BORDER, UNKNOWN_COLOR, "Untested")
        );

        TextView chartLegend = (TextView)findViewById(R.id.chartLegend);
        chartLegend.setText(Html.fromHtml(
                "<font color='" + PASSED_BORDER + "'>Passed</font> " +
                        "<font color='" + TRAINING_BORDER + "'>Reviewing</font> " +
                        "<font color='" + FAILED_BORDER + "'>Failed</font> " +
                        "<font color='" + UNKNOWN_BORDER + "'>Untested</font>"));
    }

	@Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Character selected = chars[position];

		if(charSet.availableCharactersSet().contains(selected)){
			Intent teachIntent = new Intent(this, TeachingActivity.class);
			Bundle passParams = new Bundle();
			passParams.putString("parent", callingClass);
			passParams.putChar(Constants.KANJI_PARAM, selected);
			passParams.putString(Constants.KANJI_PATH_PARAM, callingPath);
			Log.d("nakama", "ProgressActivity: passing path " + callingPath + " to TeachingActivity.");
			teachIntent.putExtras(passParams);
			startActivity(teachIntent);
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
        final private Map<Character, Progress> scores;
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
            		tv.setBackgroundColor(FAILED_COLOR);
            	} else if (score == Progress.REVIEWING){
            		tv.setBackgroundColor(TRAINING_COLOR);
            	} else if(score == Progress.PASSED) {
            		tv.setBackgroundColor(PASSED_COLOR);
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
        this.lc.dispose();
        super.onDestroy();
    }

    @Override protected void onNewIntent(Intent intent){
        Log.i("nakama", "ProgressActivity lifecycle onNewIntent");
        this.setIntent(intent);
    }
}
