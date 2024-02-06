package dmeeuwis.kanjimaster.ui.sections.teaching;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.core.Kanji;
import dmeeuwis.kanjimaster.logic.data.IidFactory;
import dmeeuwis.kanjimaster.logic.data.SettingsFactory;
import dmeeuwis.kanjimaster.logic.data.StoryDataHelper;
import dmeeuwis.kanjimaster.logic.drawing.Criticism;
import dmeeuwis.kanjimaster.logic.drawing.CurveDrawing;
import dmeeuwis.kanjimaster.ui.data.LoadRadicalsFile;
import dmeeuwis.kanjimaster.ui.data.RadicalAdapter;
import dmeeuwis.kanjimaster.ui.views.AnimatedCurveView;
import dmeeuwis.kanjimaster.ui.views.NetworkStoriesAsyncTask;
import dmeeuwis.kanjimaster.ui.views.NetworkStorySaveAsyncTask;
import dmeeuwis.kanjimaster.ui.views.ShareStoriesDialog;

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
    LinearLayout storiesLayout;
    View storiesCard;
	View radicalsCard;

    UUID iid;

    List<String> networkStories = new ArrayList<>();

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser){
            String value = SettingsFactory.get().getStorySharing();
            if("true".equals(value)) {
                loadRemoteStories();
            } else if("false".equals(value)){
               // do nothing
            } else {
                ShareStoriesDialog.show(getActivity(), new Runnable() {
                    @Override
                    public void run() {
                        SettingsFactory.get().setStorySharing("true");
                        loadRemoteStories();
                    }
                }, new Runnable(){

                    @Override
                    public void run() {
                        SettingsFactory.get().setStorySharing("false");
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

        this.iid = IidFactory.get();
        //Log.i("nakama", "TeachingStoryFragment: init iid to " + this.iid);

        StoryDataHelper db = new StoryDataHelper();
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
        this.kanim.setDrawing(currentCharacterCurve, AnimatedCurveView.DrawTime.ANIMATED, Criticism.SKIP_LIST);
        this.kanjiLabel.setText(Character.toString(this.character), TextView.BufferType.EDITABLE);

        this.kanim.setDrawing(currentCharacterCurve, AnimatedCurveView.DrawTime.ANIMATED, Criticism.SKIP_LIST);

        radicalAdapter = new RadicalAdapter(parent, android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<Kanji>());
        gridView.setAdapter(radicalAdapter);

        loadFileTask = new LoadRadicalsFile(parent, character, radicalAdapter, radicalsCard);
        loadFileTask.execute();

        this.storiesCard = view.findViewById(R.id.networkStoriesCard);
        this.storiesLayout = (LinearLayout) view.findViewById(R.id.networkStoriesLayout);
        startAnimation();
        super.onResume();
    }

    NetworkStoriesAsyncTask loadStoriesAsync;
    public void loadRemoteStories(){
        if(loadStoriesAsync != null){
            Log.d("nakama", "Not re-requesting stories, already loaded.");
            return;
        }
        final Resources r = this.getResources();
        final int paddingPx = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, this.getResources().getDisplayMetrics());
        loadStoriesAsync = new NetworkStoriesAsyncTask(this.character, this.iid, new NetworkStoriesAsyncTask.AddString() {

            @Override public void add(final String s) {
                Log.d("nakama", "Adding story as view: " + s);
                if(getView() == null){
                    Log.d("nakama", "on async return getView is null! Aborting story additions.");
                    return;
                }

                TextView tv = new TextView(getActivity());
                tv.setText(s);
                tv.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

                ImageView iv = new ImageView(getActivity());
                iv.setImageDrawable(r.getDrawable(R.drawable.ic_story_for_white_bg));
                iv.setClickable(true);
                iv.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

                // hide icon if 'story' is a network error message
                iv.setVisibility(!s.startsWith("Network error") ? View.VISIBLE : View.GONE);

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
                storiesLayout.addView(layout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                Log.d("nakama", "Added story as view: " + s);
            }
        });
        loadStoriesAsync.execute();
    }

    public void clear(){
        if(this.kanim != null){
            this.kanim.clear();
        }
    }

    public void startAnimation(){
        if(this.kanim != null) {
            this.kanim.startAnimation(500);
        }
    }

    public void focusAway(AppCompatActivity parent){
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
	
	public void saveStory(AppCompatActivity act) {
		if (storyEditor != null && storyEditor.getText() != null && !storyEditor.getText().toString().trim().equals("")){
            StoryDataHelper db = new StoryDataHelper();
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
}