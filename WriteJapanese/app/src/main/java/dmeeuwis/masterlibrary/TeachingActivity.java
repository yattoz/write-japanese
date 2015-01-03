package dmeeuwis.masterlibrary;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import dmeeuwis.Kana;
import dmeeuwis.Kanji;
import dmeeuwis.Translation;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.helpers.DictionarySet;
import dmeeuwis.nakama.library.Constants;
import dmeeuwis.util.Util;

public class TeachingActivity extends ActionBarActivity {
	ActionBar actionBar;
	
	private String character;
	private String[] currentCharacterSvg;
	
	List<Translation> usedInTranslations;
	
	LinearLayout examplesLayout;
	
	String callingClass;

	ActionBar.Tab infoTab;
	ActionBar.Tab drawTab;
	ActionBar.Tab storyTab;
	TeachingStoryFragment storyFragment;
	TeachingDrawFragment drawFragment;
	
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
			ed.commit();

		} else {
			String kanjiInStr = prefs.getString("character", null);
			if(kanjiInStr == null || kanjiInStr.length() == 0){
				try {
					startActivity(new Intent(this, Class.forName("dmeeuwis.kanjimaster.KanjiMasterActivity")));
					return;
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
			kanjiIn = prefs.getString("character", null).charAt(0);            
			kanjiPath = prefs.getString("path", null);
		}
    	
		this.character = kanjiIn.toString();
    	
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
		if(this.character == null){
			setupCharacter();
		}
		return this.character;
	}
	
	public String[] getCurrentCharacterSvg(){
		if(this.currentCharacterSvg == null){
			setupCharacter();
		}
		return this.currentCharacterSvg;
	}
	
	@Override public void onCreate(Bundle saveInstanceState){
		long startTime = System.currentTimeMillis();
		super.onCreate(saveInstanceState);
		
		Log.i("nakama", "TeachingActivity: onCreate starting.");
		this.setContentView(R.layout.fragment_container);
		this.dictSet = DictionarySet.singleton(this);
		
		Log.i("nakama", "TeachingActivity: before setupCharacter: " + (System.currentTimeMillis() - startTime));
		setupCharacter();
		Log.i("nakama", "TeachingActivity: after setupCharacter: " + (System.currentTimeMillis() - startTime));
    	
    	actionBar = this.getSupportActionBar();
    	actionBar.setDisplayHomeAsUpEnabled(true);
    	actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    	
    	drawFragment = new TeachingDrawFragment(); 
    	drawTab = actionBar.newTab().setText("Trace"); // .setIcon(R.drawable.ic_action_edit);
    	drawTab.setTabListener(new TabListener() {
			@Override public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
				// Log.d("nakama", "drawTab onTabUnselected");
			}
			
			@Override public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, drawFragment).commit();
			}
			
			@Override public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
				// Log.d("nakama", "drawTab onTabReselected");
			}
		});
    	actionBar.addTab(drawTab);
    	
		Log.i("nakama", "TeachingActivity: after drawTab: " + (System.currentTimeMillis() - startTime));
    	
    	storyFragment = new TeachingStoryFragment(); 
    	storyTab = actionBar.newTab().setText("Story"); //.setIcon(R.drawable.ic_action_edit);
    	storyTab.setTabListener(new TabListener() {
			@Override public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
				storyFragment.clearFocus();
				storyFragment.saveStory();
			}
			
			@Override public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, storyFragment).commit();
			}
			
			@Override public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
				// Log.d("nakama", "storyTab onTabReselected");
			}
		});
    	actionBar.addTab(storyTab);
    	
		Log.i("nakama", "TeachingActivity: after storyTab: " + (System.currentTimeMillis() - startTime));
    	
    	char kanjiIn = this.character.charAt(0);
    	if(Kana.isKanji(kanjiIn)){
    		infoTab = actionBar.newTab().setText("Usage"); // .setIcon(R.drawable.ic_menu_mark);
    		final Fragment infoFragment = new TeachingInfoFragment();
    		infoTab.setTabListener(new TabListener() {
			
    			@Override public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
    				Log.d("nakama", "infoTab onTabUnselected");
    			}
			
    			@Override public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
    				Log.d("nakama", "infoTab onTabSelected");
    				getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, infoFragment).commit();
    			}
			
    			@Override public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
    				Log.d("nakama", "infoTab onTabReselected");
    			}
    		});
    		try {
    			Kanji k = dictSet.kanjiFinder().find(kanjiIn);
    			actionBar.setTitle("Learn " + k.meanings[0]);
    		} catch(IOException e){
    			throw new RuntimeException(e);
    		}
    		actionBar.addTab(infoTab);
    	} else {
    		actionBar.setTitle("Learn " + Kana.kana2Romaji(String.valueOf(kanjiIn)));
    	}
		Log.i("nakama", "TeachingActivity: onCreate finishing. Took " + (System.currentTimeMillis() - startTime) + "ms.");
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
		
		return super.onOptionsItemSelected(item); // super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed(){
		if(actionBar.getSelectedNavigationIndex() == 0 && drawFragment.undo()){
			return;
		}
		finish();
	}
	
	@Override 
	public void onPause(){
		Log.i("nakama", "TeachingActivity: onPause starting.");
		storyFragment.saveStory();
		Log.i("nakama", "TeachingActivity: onPause passing to super.");
		super.onPause();
	}
	
	@Override public void onResume(){
		Log.i("nakama", "TeachingActivity: onResume");
		super.onResume();
	}
}
