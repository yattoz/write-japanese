package dmeeuwis.nakama.teaching;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.ViewGroup;
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

public class TeachingActivity extends ActionBarActivity implements ViewPager.OnPageChangeListener, TeachingCombinedStoryInfoFragment.OnFragmentInteractionListener {
	ActionBar actionBar;
	
	private String character;
	private String[] currentCharacterSvg;
    private Kanji kanji;
	
	String callingClass;

    MyFragmentPagerAdapter kanjiAdapter, kanaAdapter;
    MyFragmentPagerAdapter adapter;

    TeachingCombinedStoryInfoFragment combinedFragment;
    TeachingDrawFragment drawFragment;

    MyViewPager pager;
    PagerSlidingTabStrip tabStrip;

	DictionarySet dictSet;
	
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
        Log.i("nakama", "TeachingActivity lifecycle: onCreate");
        long startTime = System.currentTimeMillis();
        super.onCreate(saveInstanceState);

        this.setContentView(R.layout.fragment_container);
        this.dictSet = DictionarySet.get(this);

        actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        // small layout
        pager = (MyViewPager)findViewById(R.id.teachingViewPager);
        if(pager != null) {
            pager.setOffscreenPageLimit(2);

            final FragmentManager fm = getSupportFragmentManager();
            kanjiAdapter = new MyFragmentPagerAdapter(fm,
                    new String[] { "Draw", "Story", "Usage" });

            kanaAdapter = new MyFragmentPagerAdapter(fm,
                    new String[] { "Draw", "Story" });

            this.adapter = this.kanji == null ? kanaAdapter : kanjiAdapter;


            tabStrip = (PagerSlidingTabStrip)findViewById(R.id.teachingTabStrip);
            tabStrip.setIndicatorColor(getResources().getColor(R.color.actionbar_main));
            tabStrip.setShouldExpand(true);
            tabStrip.setOnPageChangeListener(this);
        }

        // large layout
        combinedFragment = (TeachingCombinedStoryInfoFragment) getSupportFragmentManager().findFragmentById(R.id.combined_fragment);
        drawFragment = (TeachingDrawFragment) getSupportFragmentManager().findFragmentById(R.id.teaching_draw_fragment);

        Log.i("nakama", "TeachingActivity: onCreate finishing. Took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    @Override public void onResume(){
        Log.i("nakama", "TeachingActivity lifecycle: onResume");

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
            kanjiIn = kanjiInStr.charAt(0);
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
        } catch (IOException e) {
            Log.e("nakama", "Error loading path: " + path + " for character " + kanjiIn + " (" + unicodeValue + ")");
            throw new RuntimeException(e);
        }

        if(Kana.isKanji(kanjiIn)){
            try {
                Kanji k = dictSet.kanjiFinder().find(kanjiIn);
                actionBar.setTitle("Studying " + k.meanings[0]);
                adapter = kanjiAdapter;
            } catch(IOException e){
                throw new RuntimeException(e);
            }
        } else {
            actionBar.setTitle("Studying " + Kana.kana2Romaji(String.valueOf(kanjiIn)));
            adapter = kanaAdapter;
        }

        if(pager != null) {
            pager.setAdapter(adapter);
            tabStrip.setViewPager(pager);
        }

        if(combinedFragment != null){
            combinedFragment.setCharacter(this.getCharacter().charAt(0));
        }

        super.onResume();
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
        if(adapter != null) {
            TeachingDrawFragment drawFragment = (TeachingDrawFragment) adapter.getRegisteredFragment(0);
            if (drawFragment.undo()){
                return;
            }
        }
		finish();
	}
	
	@Override 
	public void onPause(){
        try {
            if(adapter != null) {
                TeachingStoryFragment storyFragment = (TeachingStoryFragment) adapter.getRegisteredFragment(1);
                storyFragment.saveStory(this);
            }

            if(combinedFragment != null){
                combinedFragment.saveStory(this);
            }
        } catch(NullPointerException e){
            Log.i("nakama", "Ignoring null fragment at onPause.");
        }
		super.onPause();
	}
	
    @Override protected void onNewIntent(Intent intent){
        Log.i("nakama", "TeachingActivity lifecycle onNewIntent");
        this.setIntent(intent);
    }

    @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { /* nothing */  }

    int previousScrollState = ViewPager.SCROLL_STATE_IDLE;
    @Override public void onPageScrollStateChanged(int state) {
        TeachingDrawFragment drawFragment = (TeachingDrawFragment)adapter.getRegisteredFragment(0);
        TeachingStoryFragment storyFragment = (TeachingStoryFragment)adapter.getRegisteredFragment(1);
        TeachingInfoFragment infoFragment = (TeachingInfoFragment)adapter.getRegisteredFragment(2);

        int position = pager.getCurrentItem();

        if(previousScrollState == ViewPager.SCROLL_STATE_IDLE && state == ViewPager.SCROLL_STATE_SETTLING) {
            storyFragment.clear();
            infoFragment.clear();
            drawFragment.clear();
        } else if(previousScrollState == ViewPager.SCROLL_STATE_SETTLING && state == ViewPager.SCROLL_STATE_IDLE){
            if(position == 0) {
                drawFragment.startAnimation(300);
            } else if(position == 1){
                storyFragment.startAnimation();
            } else if (position == 2){
                infoFragment.startAnimation();
            }
        }
        previousScrollState = state;
    }

    @Override
    public void onPageSelected(int position) {
        pager.setMotionEnabled(position != 0);

    /* Maybe not desirable? Certainly not on the tablet layout....
        if(storyFragment != null && (position == 0 || position == 2)) {
            storyFragment.focusAway(this);
        }
    */
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.i("nakama", "TeachingActivity.onFragmentInteraction: " + uri);
    }


    private static class MyFragmentPagerAdapter extends FragmentPagerAdapter {
        private final String[] titles;

        SparseArray<Fragment> registeredFragments = new SparseArray<>();

        public MyFragmentPagerAdapter(FragmentManager fm, String[] titles) {
            super(fm);
            this.titles = titles;
        }

        @Override public Fragment getItem(int position) {
            Log.i("nakama", "TeachingActivity adapter.getItem " + position);
            if(position == 0){
                return new TeachingDrawFragment();
            } else if(position == 1){
                return new TeachingStoryFragment();
            } else if (position == 2){
                return new TeachingInfoFragment();
            }
            return null;
        }

        @Override public int getCount() {
            //Log.i("nakama", "TeachingActivity adapter.getCount; will return " + titles.length);
            return titles.length;
        }

        @Override public String getPageTitle(int position){
            //Log.i("nakama", "TeachingActivity adapter.getPageTitle " + position);
            return titles[Math.min(titles.length, position)];
        }


        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            //Log.i("nakama", "TeachingActivity adapter.instantiateItem " + position);
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            //Log.i("nakama", "TeachingActivity adapter.destroyItem " + position);
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getRegisteredFragment(int position) {
            //Log.i("nakama", "TeachingActivity adapter.getRegisteredFragment " + position);
            return registeredFragments.get(position);
        }
    }
}
