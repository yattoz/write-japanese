package dmeeuwis.nakama.teaching;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import dmeeuwis.Kanji;
import dmeeuwis.KanjiRadicalFinder;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.StoryDataHelper;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.kanjidraw.CurveDrawing;
import dmeeuwis.nakama.primary.Iid;
import dmeeuwis.nakama.primary.KanjiMasterActivity;
import dmeeuwis.nakama.views.AnimatedCurveView;
import dmeeuwis.nakama.views.KanjiWithMeaningView;
import dmeeuwis.nakama.views.NetworkStoriesAsyncTask;
import dmeeuwis.nakama.views.NetworkStorySaveAsyncTask;
import dmeeuwis.nakama.views.ShareStoriesDialog;
import dmeeuwis.util.Util;

public class TeachingStoryFragment extends Fragment {
    public static final String STORY_SHARING_KEY = "storySharing";

	char character;
	Kanji kanji;        // may be null for kana
	EditText storyEditor;
	TextView kanjiLabel;
	AnimatedCurveView kanim;
	GridView gridView;
	ArrayAdapter<Kanji> radicalAdapter;
	LoadRadicalsFile loadFileTask;
    LinearLayout storiesCard;
	View radicalsCard;

    UUID iid;

    List<String> networkStories = new ArrayList<>();

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser){
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            String value = prefs.getString(STORY_SHARING_KEY, null);
            if("true".equals(value)) {
                loadRemoteStories();
            } else if("false".equals(value)){
               // do nothing
            } else {
                ShareStoriesDialog.show(getActivity(), new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences.Editor e = prefs.edit();
                        e.putString(STORY_SHARING_KEY, "true");
                        e.apply();
                        loadRemoteStories();
                    }
                }, new Runnable(){

                    @Override
                    public void run() {
                        SharedPreferences.Editor e = prefs.edit();
                        e.putString(STORY_SHARING_KEY, "false");
                        e.apply();
                    }
                });
            }
       }
    }

    @Override
    public void onResume() {
        //Log.i("nakama", "TeachingStoryFragment lifecycle: onResume; getView=" + getView());
        final TeachingActivity parent = (TeachingActivity)getActivity();

        this.character = parent.getCharacter().charAt(0);
        this.kanji = parent.getKanji();

        this.iid = Iid.get(parent.getApplication());
        //Log.i("nakama", "TeachingStoryFragment: init iid to " + this.iid);

        StoryDataHelper db = new StoryDataHelper(parent);
        String s = db.getStory(this.character);
        if(s == null){ s = ""; }

        View view = getView();
        this.storyEditor = (EditText)view.findViewById(R.id.storyEditor);
        this.kanim = (AnimatedCurveView)view.findViewById(R.id.kanji_animation);
        this.kanjiLabel = (TextView)view.findViewById(R.id.bigkanji);
        this.radicalsCard = view.findViewById(R.id.radicalsCard);
        this.gridView = (GridView)view.findViewById(R.id.radicalsGrid);

        CurveDrawing currentCharacterCurve = new CurveDrawing(parent.getCurrentCharacterSvg());
        this.storyEditor.setText(s, TextView.BufferType.EDITABLE);
        this.kanjiLabel.setText(Character.toString(this.character), TextView.BufferType.EDITABLE);
        this.kanim.setDrawing(currentCharacterCurve, AnimatedCurveView.DrawTime.ANIMATED);
        this.kanjiLabel.setText(Character.toString(this.character), TextView.BufferType.EDITABLE);

        this.kanim.setDrawing(currentCharacterCurve, AnimatedCurveView.DrawTime.ANIMATED);

        radicalAdapter = new RadicalAdapter(parent, android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<Kanji>());
        gridView.setAdapter(radicalAdapter);

        loadFileTask = new LoadRadicalsFile(parent);
        loadFileTask.execute();

        this.storiesCard = (LinearLayout)view.findViewById(R.id.networkStoriesCard);
        startAnimation();
        super.onResume();
    }

    public void loadRemoteStories(){
        final Resources r = this.getResources();
        final int paddingPx = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, this.getResources().getDisplayMetrics());
        NetworkStoriesAsyncTask loadRemoteStories = new NetworkStoriesAsyncTask(this.character, this.iid, new NetworkStoriesAsyncTask.AddString() {

            @Override public void add(final String s) {
                Log.d("nakama", "Adding story as view: " + s);
                TextView tv = new TextView(getActivity());
                int tid = Util.generateViewId();
                tv.setText(s);
                tv.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

                ImageView iv = new ImageView(getActivity());
                iv.setImageDrawable(r.getDrawable(R.drawable.ic_story_for_white_bg));
                iv.setClickable(true);
                iv.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
                int imageWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, r.getDisplayMetrics());
                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        storyEditor.setText(s);
                        Toast.makeText(getActivity(), "Your story for this character has been updated.", Toast.LENGTH_SHORT).show();
                    }
                });

                FrameLayout layout = new FrameLayout(getActivity());
                {
                    FrameLayout.LayoutParams ll = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    ll.setMargins(0, 0, imageWidth, 0);
                    layout.addView(tv, ll);
                }

                {
                    FrameLayout.LayoutParams llv = new FrameLayout.LayoutParams(imageWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
                    llv.gravity = Gravity.RIGHT;
                    layout.addView(iv, llv);
                }

                storiesCard.setVisibility(View.VISIBLE);
                storiesCard.addView(layout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                Log.d("nakama", "Added story as view: " + s);
            }
        });
        loadRemoteStories.execute();
    }

    public void clear(){
        this.kanim.clear();
    }

    public void startAnimation(){
        if(this.kanim != null) {
            this.kanim.startAnimation(500);
        }
    }

    public void focusAway(Activity parent){
        if(storyEditor != null) {
            storyEditor.clearFocus();

            InputMethodManager imm = (InputMethodManager)parent.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(storyEditor.getWindowToken(), 0);
        }

    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Log.i("nakama", "TeachingStoryFragment lifecycle: onCreateView");
		return inflater.inflate(R.layout.fragment_story, container, false);
	}
	
	private class RadicalAdapter extends ArrayAdapter<Kanji> {
		public RadicalAdapter(Context context, int resource, int textViewResourceId, List<Kanji> objects) {
			super(context, resource, textViewResourceId, objects);
		}

		public View getView(int position, View convertView, ViewGroup parentViewgroup){
			//Log.i("nakama", "RadicalAdapter.getView " + convertView);
			if(convertView == null){
				convertView = new KanjiWithMeaningView(this.getContext());
			}
			Kanji k = getItem(position);
			((KanjiWithMeaningView)convertView).setKanjiAndMeaning(String.valueOf(k.kanji), k.meanings[0]);
			return convertView;
		}

	}

	public void saveStory(Activity act) {
		if (storyEditor != null && storyEditor.getText() != null && !storyEditor.getText().toString().trim().equals("")){
            StoryDataHelper db = new StoryDataHelper(act);
            String story = storyEditor.getText().toString();
			db.recordStory(this.character, story);

            // check if story matches a network story exactly, and optimize out one network request.
            // server protects itself from duplicates, so fine if one accidentally goes out, just uses user bandwidth.
            for(String s: this.networkStories){
                if(s != null && story.equals(s)){
                    return;
                }
            }

            NetworkStorySaveAsyncTask saveRemove =
                new NetworkStorySaveAsyncTask(this.character, story, iid);
            saveRemove.execute();
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
       				//Log.i("nakama", "Adding results to radicalAdapter: " + k);
       				radicalAdapter.add(k); 
       			}
       			radicalsCard.setVisibility(View.VISIBLE);
       			radicalAdapter.notifyDataSetChanged();
       		}
        }
    }
}