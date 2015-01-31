package dmeeuwis.nakama.teaching;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;

import java.io.IOException;
import java.io.InputStream;

import dmeeuwis.Kana;
import dmeeuwis.Kanji;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.Constants;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.util.Util;

public class TeachingActivity extends ActionBarActivity implements ViewPager.OnPageChangeListener {
	ActionBar actionBar;
	
	private String character;
	private String[] currentCharacterSvg;
    private Kanji kanji;
	
	String callingClass;

    FragmentPagerAdapter kanjiAdapter, kanaAdapter;

    MyViewPager pager;
    PagerSlidingTabStrip tabStrip;
	TeachingStoryFragment storyFragment;
	TeachingDrawFragment drawFragment;
    TeachingInfoFragment infoFragment;

	DictionarySet dictSet;
	
	public void setupCharacter(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
    	Bundle params = getIntent().getExtras();
		Character kanjiIn;
		String kanjiPath;
		if(params.size() > 0){
			callingClass = params.getString("parent");
			kanjiIn = params.getChar(Constants.KANJI_PARAM);
			kanjiPath = params.getString(Constants.KANJI_PATH_PARAM);

			Editor ed = prefs.edit();
			ed.putString("character", kanjiIn.toString());
			ed.putString("path", kanjiPath);
			ed.apply();

		} else {
			String kanjiInStr = prefs.getString("character", null);
			if(kanjiInStr == null || kanjiInStr.length() == 0){
				try {
					startActivity(new Intent(this, Class.forName("dmeeuwis.nakama.primary.KanjiMasterActivity")));
					return;
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
			kanjiIn = prefs.getString("character", null).charAt(0);            
			kanjiPath = prefs.getString("path", null);
		}
    	
		this.character = kanjiIn.toString();

        DictionarySet sd = DictionarySet.get(this);
        try {
            this.kanji = sd.kanjiFinder().find(getCharacter().charAt(0));
        } catch (IOException e) {
            Log.e("nakama", "Error: can't find kanji for: " + this.kanji, e);
            Toast.makeText(this, "Internal Error: can't find kanji information for: " + this.kanji, Toast.LENGTH_LONG).show();
        }

        int unicodeValue = kanjiIn;
        String path = kanjiPath + "/" + Integer.toHexString(unicodeValue) + ".path";
        AssetManager assets = getAssets();
        try {
	        InputStream is = assets.open(path);
	        try {
				currentCharacterSvg = Util.slurp(is).split("\n");
	        } finally {
	        	is.close();
	        }
			Log.d("nakama", "Read path hints as: \n" + Util.join("\n", currentCharacterSvg));
		} catch (IOException e) {
			Log.e("nakama", "Error loading path: " + path + " for character " + kanjiIn + " (" + unicodeValue + ")");
			throw new RuntimeException(e);
		}
	}
	
	public String getCharacter(){
		return this.character;
	}

    public Kanji getKanji(){
        return this.kanji;
    }

	public String[] getCurrentCharacterSvg(){
		return this.currentCharacterSvg;
	}

	@Override public void onCreate(Bundle saveInstanceState) {
        long startTime = System.currentTimeMillis();
        super.onCreate(saveInstanceState);

        Log.i("nakama", "TeachingActivity: onCreate starting.");
        this.setContentView(R.layout.fragment_container);
        this.dictSet = DictionarySet.get(this);

        Log.i("nakama", "TeachingActivity: before setupCharacter: " + (System.currentTimeMillis() - startTime));
        setupCharacter();
        Log.i("nakama", "TeachingActivity: after setupCharacter: " + (System.currentTimeMillis() - startTime));

        actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        pager = (MyViewPager)findViewById(R.id.teachingViewPager);
        final FragmentManager fm = getSupportFragmentManager();

        drawFragment = new TeachingDrawFragment();
        storyFragment = new TeachingStoryFragment();
        infoFragment = new TeachingInfoFragment();

        kanjiAdapter = new MyFragmentPagerAdapter(fm,
                new Fragment[] { drawFragment, storyFragment, infoFragment },
                new String[] { "Draw", "Story", "Usage" });

        kanaAdapter = new MyFragmentPagerAdapter(fm,
                new Fragment[] { drawFragment, storyFragment },
                new String[] { "Draw", "Story" });

        pager.setAdapter(kanjiAdapter);
        pager.setMotionEnabled(false);

        tabStrip = (PagerSlidingTabStrip)findViewById(R.id.teachingTabStrip);
        tabStrip.setIndicatorColor(getResources().getColor(R.color.actionbar_main));
        tabStrip.setShouldExpand(true);
        tabStrip.setViewPager(pager);
        tabStrip.setOnPageChangeListener(this);

        passCharacterDataToUi();
        Log.i("nakama", "TeachingActivity: onCreate finishing. Took " + (System.currentTimeMillis() - startTime) + "ms.");
    }

    private void passCharacterDataToUi(){
    	char kanjiIn = this.character.charAt(0);
    	if(Kana.isKanji(kanjiIn)){
    		try {
    			Kanji k = dictSet.kanjiFinder().find(kanjiIn);
    			actionBar.setTitle("Studying " + k.meanings[0]);
                pager.setAdapter(kanjiAdapter);
                tabStrip.setViewPager(pager);
    		} catch(IOException e){
    			throw new RuntimeException(e);
    		}
    	} else {
    		actionBar.setTitle("Studying " + Kana.kana2Romaji(String.valueOf(kanjiIn)));
            pager.setAdapter(kanaAdapter);
            tabStrip.setViewPager(pager);
    	}

        this.drawFragment.updateCharacter(this);
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
	public void onBackPressed(){
		if(drawFragment.undo()){
			return;
		}
		finish();
	}
	
	@Override 
	public void onPause(){
		Log.i("nakama", "TeachingActivity: onPause starting.");
		storyFragment.saveStory(this);
		Log.i("nakama", "TeachingActivity: onPause passing to super.");
		super.onPause();
	}
	
	@Override public void onResume(){
		Log.i("nakama", "TeachingActivity: onResume");
        onPageSelected(pager.getCurrentItem());
		super.onResume();
	}

    @Override protected void onNewIntent(Intent intent){
        Log.i("nakama", "TeachingActivity: onNewIntent");
        this.setIntent(intent);
        setupCharacter();
        passCharacterDataToUi();
    }

    @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { /* nothing */  }
    @Override public void onPageScrollStateChanged(int state) { /* nothing */  }

    @Override
    public void onPageSelected(int position) {
        Log.i("nakama", "TeachingActivity: onPageSelected " + position);
        pager.setMotionEnabled(position != 0);

        if(position == 0) {
            drawFragment.startAnimation();
        } else if(position == 1){
            storyFragment.startAnimation();
        } else if (position == 2){
            infoFragment.startAnimation();
        }
    }


    private static class MyFragmentPagerAdapter extends FragmentPagerAdapter {
        private final Fragment[] fragments;
        private final String[] titles;

        public MyFragmentPagerAdapter(FragmentManager fm, Fragment[] fragments, String[] titles) {
            super(fm);
            Log.e("nakama", "In MyFragmentPagerAdapter, FragmentManager is " + fm);
            this.fragments = fragments;
            this.titles = titles;
        }

        @Override public Fragment getItem(int position) { return fragments[position]; }
        @Override public int getCount() { return fragments.length; }
        @Override public String getPageTitle(int position){ return titles[position]; }
    }
}
