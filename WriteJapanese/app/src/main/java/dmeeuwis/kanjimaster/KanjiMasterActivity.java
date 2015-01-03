package dmeeuwis.kanjimaster;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import dmeeuwis.masterlibrary.AbstractMasterActivity;
import dmeeuwis.masterlibrary.CharacterSets;
import dmeeuwis.masterlibrary.CharacterStudySet;
import dmeeuwis.masterlibrary.CharacterStudySet.LockLevel;
import dmeeuwis.masterlibrary.ProgressActivity;
import dmeeuwis.masterlibrary.PurchaseDialog;
import dmeeuwis.nakama.library.Constants;

public class KanjiMasterActivity extends AbstractMasterActivity implements ActionBar.OnNavigationListener, UncaughtExceptionHandler {
	public static CharacterStudySet joyouG1, joyouG2, joyouG3, joyouG4, joyouG5, joyouG6; // , joyouSS;
	public static CharacterStudySet hiraganaCharacterSet, katakanaCharacterSet;
	
    @Override public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

    	Thread.setDefaultUncaughtExceptionHandler(this);

    	hiraganaCharacterSet = CharacterSets.hiragana(lockChecker);
    	katakanaCharacterSet = CharacterSets.katakana(lockChecker);
    	joyouG1 = CharacterSets.joyouG1(this.dictionarySet.kanjiFinder(), lockChecker);
    	joyouG2 = CharacterSets.joyouG2(this.dictionarySet.kanjiFinder(), lockChecker);
    	joyouG3 = CharacterSets.joyouG3(this.dictionarySet.kanjiFinder(), lockChecker);
    	joyouG4 = CharacterSets.joyouG4(this.dictionarySet.kanjiFinder(), lockChecker);
    	joyouG5 = CharacterSets.joyouG5(this.dictionarySet.kanjiFinder(), lockChecker);
    	joyouG6 = CharacterSets.joyouG6(this.dictionarySet.kanjiFinder(), lockChecker);
    	
    	this.characterSets.put("hiragana", hiraganaCharacterSet);
    	this.characterSets.put("katakana", katakanaCharacterSet);
    	this.characterSets.put("j1", joyouG1);
    	this.characterSets.put("j2", joyouG2);
    	this.characterSets.put("j3", joyouG3);
    	this.characterSets.put("j4", joyouG4);
    	this.characterSets.put("j5", joyouG5);
    	this.characterSets.put("j6", joyouG6);
    	
    	ActionBar actionBar = getSupportActionBar();
    	LockableArrayAdapter characterSetAdapter = new LockableArrayAdapter(this, new ArrayList<CharacterStudySet>(this.characterSets.values()));
    	actionBar.setListNavigationCallbacks(characterSetAdapter, this);
    	actionBar.show();
    	actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
    }
    
    public static class LockableArrayAdapter extends ArrayAdapter<CharacterStudySet> {
    	private List<CharacterStudySet> data;
    	
		public LockableArrayAdapter(Context context, List<CharacterStudySet> objects) {
			super(context, R.layout.locked_list_item_layout, R.id.text, objects);
			this.data = objects;
		}
    
		 @Override public View getView(int position, View convertView, ViewGroup parent) {
			 return getCustomView(position, convertView, parent);
		 }
		 
		 @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
			 return getCustomView(position, convertView, parent);
		 }
		 
		 public View getCustomView(int position, View convertView, ViewGroup parent) {
			 View row = convertView;
		        
			 if(row == null) {
				 LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
				 row = inflater.inflate(R.layout.locked_list_item_layout, parent, false);
			 }

			 CharacterStudySet d = data.get(position);
			 ImageView lockIcon = (ImageView)row.findViewById(R.id.lock);
			 lockIcon.getDrawable().setAlpha(255);
			 boolean lockIconVisible = d.locked();
			 lockIcon.setVisibility(lockIconVisible ? View.VISIBLE : View.INVISIBLE);
			 ((TextView)row.findViewById(R.id.text)).setText(d.toString());
			 return row;
		 }
    }

	@Override public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.actionbar, menu);
	    return true;
	}	
	
	@Override public boolean onPrepareOptionsMenu(Menu menu){
		MenuItem shuffleCheck = menu.findItem(R.id.menu_shuffle);
		if(currentCharacterSet != null){
			shuffleCheck.setChecked(currentCharacterSet.isShuffling());
		}
		MenuItem lockItem = menu.findItem(R.id.menu_lock);
		Log.i("nakama", "KanjiMaster.onPrepareOptionsMenus: setting actionbar lock to: " + (lockChecker.getPurchaseStatus() != LockLevel.UNLOCKED) + " (" + lockChecker.getPurchaseStatus() + ")");
		lockItem.setVisible(lockChecker.getPurchaseStatus() != LockLevel.UNLOCKED);
		return true;
	}

	@Override public boolean onOptionsItemSelected(MenuItem item){
	    if(item.getItemId() ==  R.id.menu_progress){
			Intent teachIntent = new Intent(this, ProgressActivity.class);
			Bundle params = new Bundle();
			params.putString("parent", this.getClass().getName());
			params.putString(Constants.KANJI_PATH_PARAM, this.currentCharacterSet.pathPrefix);
			params.putString("characters", this.currentCharacterSet.charactersAsString());
			Log.d("nakama", "KanjiMaster: passing charset path " + this.currentCharacterSet.pathPrefix);
			teachIntent.putExtras(params);
			startActivity(teachIntent);
	    } else if(item.getItemId() ==  R.id.menu_info){
			Intent creditsIntent = new Intent(this, CreditsActivity.class);
			Bundle params = new Bundle();
			params.putString("parent", this.getClass().getName());
			creditsIntent.putExtras(params);
			startActivity(creditsIntent);
	    } else if(item.getItemId() ==  R.id.menu_reset_progress){
	    	queryProgressReset();
	    } else if(item.getItemId() ==  R.id.menu_shuffle){
	    	item.setChecked(!item.isChecked());
	    	
	    	for(CharacterStudySet c: characterSets.values()){
	    		c.setShuffle(item.isChecked());
	    	}
	    } else if(item.getItemId() ==  R.id.menu_lock){
	    	raisePurchaseDialog(PurchaseDialog.DialogMessage.LOCK_BUTTON, Frequency.ALWAYS);
//	    } else if(item.getItemId() ==  R.id.menu_debug_unlock){
//	    	getLockChecker().coreUnlock();
//	    } else if(item.getItemId() ==  R.id.menu_debug_consume){
//	    	lockChecker.startConsume();
	    }
	    return true;
    }
	
	public void queryProgressReset(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

	    builder.setTitle("Confirm");
	    builder.setMessage("Are you sure you want to reset your progress in " + currentCharacterSet.name + "?");

	    builder.setPositiveButton("Reset Progress", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {
	            Editor ed = prefs.edit();
	            ed.remove(currentCharacterSet.pathPrefix);
	            ed.commit();
	            currentCharacterSet.progressReset(KanjiMasterActivity.this);
	            loadNextCharacter(false);
	            
	            dialog.dismiss();
	        }
	    });

	    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {
	            dialog.dismiss();
	        }
	    });

	    AlertDialog alert = builder.create();
	    alert.show();
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if(!(itemPosition == 0 || itemPosition == 2)) {
			raisePurchaseDialog(PurchaseDialog.DialogMessage.START_OF_LOCKED_SET, Frequency.ONCE_PER_SESSION);
		}
		
		
		if(itemPosition == 0){
			this.currentCharacterSet = KanjiMasterActivity.hiraganaCharacterSet;
			this.correctVocabList.setVisibility(View.GONE);
		} else if(itemPosition == 1){
			this.currentCharacterSet = KanjiMasterActivity.katakanaCharacterSet;
			this.correctVocabList.setVisibility(View.GONE);
		} else if(itemPosition == 2){
			this.currentCharacterSet = KanjiMasterActivity.joyouG1;
			this.correctVocabList.setVisibility(View.VISIBLE);
		} else if(itemPosition == 3){
			this.currentCharacterSet = KanjiMasterActivity.joyouG2;
			this.correctVocabList.setVisibility(View.VISIBLE);
		} else if(itemPosition == 4){
			this.currentCharacterSet = KanjiMasterActivity.joyouG3;
			this.correctVocabList.setVisibility(View.VISIBLE);
		} else if(itemPosition == 5){
			this.currentCharacterSet = KanjiMasterActivity.joyouG4;
			this.correctVocabList.setVisibility(View.VISIBLE);
		} else if(itemPosition == 6){
			this.currentCharacterSet = KanjiMasterActivity.joyouG5;
			this.correctVocabList.setVisibility(View.VISIBLE);
		} else if(itemPosition == 7){
			this.currentCharacterSet = KanjiMasterActivity.joyouG6;
			this.correctVocabList.setVisibility(View.VISIBLE);
//		} else if(itemPosition == 8){
//			Toast.makeText(this, "Showing SS", Toast.LENGTH_SHORT);
//			this.currentCharacterSet = this.joyouSS;
		}
		this.reviewBug.setVisibility(View.GONE);
    	loadNextCharacter(false);
		drawPad.clear();
    	setUiState(State.DRAWING);
		return true;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Log.e("nakama", "Uncaught Exception in main activity", ex);
	}
}
