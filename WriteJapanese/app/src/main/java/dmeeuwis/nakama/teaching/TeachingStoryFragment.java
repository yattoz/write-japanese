package dmeeuwis.nakama.teaching;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import dmeeuwis.Kanji;
import dmeeuwis.KanjiRadicalFinder;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.StoryDataHelper;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.kanjidraw.CurveDrawing;
import dmeeuwis.nakama.views.AnimatedCurveView;
import dmeeuwis.nakama.views.KanjiWithMeaningView;

public class TeachingStoryFragment extends Fragment {

	char character;
	Kanji kanji;        // may be null for kana
	String[] currentCharacterSvg;
	EditText storyEditor;
	TextView kanjiLabel;
	AnimatedCurveView kanim;
	GridView gridView;
	ArrayAdapter<Kanji> radicalAdapter;
	LoadRadicalsFile loadFileTask;
	View radicalsCard;
    String story;

	@Override
	public void onAttach(Activity activity) {
		loadFileTask = new LoadRadicalsFile(activity);
		loadFileTask.execute();

		super.onAttach(activity);
	}

    @Override
    public void onResume() {
        this.storyEditor.setText(this.story, TextView.BufferType.EDITABLE);
        this.kanjiLabel.setText(Character.toString(this.character), TextView.BufferType.EDITABLE);
        super.onResume();
    }

    public void updateCharacter(TeachingActivity parent){
        char pc = parent.getCharacter().charAt(0);
        Log.d("nakama", "TeachingStory: updateCharacter " + pc);

        this.character = pc;
        this.kanji = parent.getKanji();
        this.currentCharacterSvg = parent.getCurrentCharacterSvg();

        StoryDataHelper db = new StoryDataHelper(parent);
        String s = db.getStory(this.character);
        if(s == null){ s = ""; }
        this.story = s;

        Log.d("nakama", "Setting story editor to: " + this.story + "; storyEditor is " + this.storyEditor + "; thread is " + Thread.currentThread());
        if(this.storyEditor != null){
            this.storyEditor.setText(this.story, TextView.BufferType.EDITABLE);
        }

        if(this.kanjiLabel != null) {
            this.kanjiLabel.setText(Character.toString(this.character), TextView.BufferType.EDITABLE);
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_story, container, false);
		storyEditor = (EditText) view.findViewById(R.id.storyEditor);
		storyEditor.requestFocus();

		CurveDrawing animCurveDrawing = new CurveDrawing(currentCharacterSvg);
		this.kanim = (AnimatedCurveView)view.findViewById(R.id.kanji_animation);
		this.kanim.setDrawing(animCurveDrawing, AnimatedCurveView.DrawTime.ANIMATED);
		this.kanim.startAnimation(500);
	
		this.kanjiLabel = (TextView)view.findViewById(R.id.bigkanji);
		this.radicalsCard = view.findViewById(R.id.radicalsCard);

		gridView = (GridView) view.findViewById(R.id.radicalsGrid);
		radicalAdapter = new RadicalAdapter(this.getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<Kanji>());
		gridView.setAdapter(radicalAdapter);
		
		return view;
	}
	
	private class RadicalAdapter extends ArrayAdapter<Kanji> {
		public RadicalAdapter(Context context, int resource, int textViewResourceId, List<Kanji> objects) {
			super(context, resource, textViewResourceId, objects);
		}

		public View getView(int position, View convertView, ViewGroup parentViewgroup){
			Log.i("nakama", "RadicalAdapter.getView " + convertView);
			if(convertView == null){
				convertView = new KanjiWithMeaningView(this.getContext());
			}
			Kanji k = getItem(position);
			((KanjiWithMeaningView)convertView).setKanjiAndMeaning(String.valueOf(k.kanji), k.meanings[0]);
			return convertView;
		}

	}

	public void clearFocus() {
		if(isAdded()){
			storyEditor.clearFocus();
			InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(storyEditor.getWindowToken(), 0);
		}
	}

	public void saveStory(Activity act) {
		if (storyEditor != null && storyEditor.getText() != null && !storyEditor.getText().toString().trim().equals("")){
            StoryDataHelper db = new StoryDataHelper(act);
            this.story = storyEditor.getText().toString();
			db.recordStory(this.character, this.story);
		}
	}

	private class LoadRadicalsFile extends AsyncTask<Void, Void, List<Kanji>> {
        Activity parent;
        public LoadRadicalsFile(Activity parent){
            this.parent = parent;
        }

        @Override
        protected List<Kanji> doInBackground(Void... v) {
			Thread.currentThread().setName("LoadRadicalsFile");
            DictionarySet dicts = DictionarySet.get(parent);
			List<Kanji> retRadicals = null;
			try {

				AssetManager asm = parent.getAssets();
				InputStream kif = asm.open("kradfile");
				try {
					KanjiRadicalFinder krf = new KanjiRadicalFinder(kif);
					retRadicals = krf.findRadicalsAsKanji(dicts.kanjiFinder(), character);
				} finally {
					kif.close();
				}
			} catch (IOException e) {
				Log.e("nakama", "Error: could not read kradfile entries to kanji.", e);
				retRadicals = new ArrayList<Kanji>(0);
			}
			
			return retRadicals;
        }
        
        @Override
        protected void onPostExecute(List<Kanji> result) {
       		if(result.size() > 0){
       			for(Kanji k: result){ 
       				Log.i("nakama", "Adding results to radicalAdapter: " + k);
       				radicalAdapter.add(k); 
       			}
       			radicalsCard.setVisibility(View.VISIBLE);
       			radicalAdapter.notifyDataSetChanged();
       		}
        }
    }
}