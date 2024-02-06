package dmeeuwis.kanjimaster.ui.sections.teaching;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.core.Kanji;
import dmeeuwis.kanjimaster.core.Translation;
import dmeeuwis.kanjimaster.logic.data.DictionarySet;
import dmeeuwis.kanjimaster.logic.data.IidFactory;
import dmeeuwis.kanjimaster.logic.data.StoryDataHelper;
import dmeeuwis.kanjimaster.ui.data.DictionarySetAndroid;
import dmeeuwis.kanjimaster.ui.data.LoadRadicalsFile;
import dmeeuwis.kanjimaster.ui.data.RadicalAdapter;
import dmeeuwis.kanjimaster.ui.sections.primary.OnFragmentInteractionListener;
import dmeeuwis.kanjimaster.ui.views.NetworkStoriesAsyncTask;
import dmeeuwis.kanjimaster.ui.views.NetworkStorySaveAsyncTask;
import dmeeuwis.kanjimaster.ui.views.ShareStoriesDialog;
import dmeeuwis.kanjimaster.ui.views.TallGridView;
import dmeeuwis.kanjimaster.ui.views.translations.CharacterTranslationListAsyncTask;
import dmeeuwis.kanjimaster.ui.views.translations.KanjiVocabRecyclerAdapter;

public class TeachingCombinedStoryInfoFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    private char character;
    private UUID iid;

    private RecyclerView combinedExamples;
    private LinearLayout combinedStoriesLayout;
    private TallGridView radicalsGrid;
    private EditText storyEditor;
    private CardView combinedStoriesCard;

    private CharacterTranslationListAsyncTask searchTask;
    private NetworkStoriesAsyncTask networkStoriesAsyncTask;
    private OnFragmentInteractionListener mListener;

    ArrayAdapter<Kanji> radicalAdapter;
    LoadRadicalsFile loadFileTask;
    View radicalsCard;

    List<String> networkStories = new ArrayList<>();

    private float engTextSize;
    private KanjiVocabRecyclerAdapter combinedExamplesAdapter;

    public static TeachingCombinedStoryInfoFragment newInstance(String charsetPath) {
        TeachingCombinedStoryInfoFragment fragment = new TeachingCombinedStoryInfoFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public TeachingCombinedStoryInfoFragment() {
        // Required empty public constructor
    }


    public void checkForStoryAccess() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        String value = prefs.getString(TeachingStoryFragment.STORY_SHARING_KEY, null);
        if("true".equals(value)) {
            this.loadRemoteStories(this.character);
        } else if("false".equals(value)){
            // do nothing
        } else {
            ShareStoriesDialog.show(getActivity(), new Runnable() {
                @Override
                public void run() {
                    SharedPreferences.Editor e = prefs.edit();
                    e.putString(TeachingStoryFragment.STORY_SHARING_KEY, "true");
                    e.apply();
                    loadRemoteStories(TeachingCombinedStoryInfoFragment.this.character);
                }
            }, new Runnable() {

                @Override
                public void run() {
                    SharedPreferences.Editor e = prefs.edit();
                    e.putString(TeachingStoryFragment.STORY_SHARING_KEY, "false");
                    e.apply();
                }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources r = getResources();
        engTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teaching_combined_story_info, container, false);

        this.combinedStoriesLayout = (LinearLayout)v.findViewById(R.id.combined_stories);
        this.combinedStoriesCard = (CardView)v.findViewById(R.id.combined_stories_card);

        DictionarySet df = DictionarySetAndroid.get(getActivity().getApplicationContext());
        this.combinedExamples = (RecyclerView)v.findViewById(R.id.combined_examples);
        this.combinedExamples.setNestedScrollingEnabled(false);
        this.combinedExamplesAdapter = new KanjiVocabRecyclerAdapter(this.getActivity(), df.kanjiFinder());
        this.combinedExamples.setAdapter(combinedExamplesAdapter);
        this.combinedExamples.setLayoutManager(new LinearLayoutManager(this.getActivity()));

        this.radicalsGrid = (TallGridView)v.findViewById(R.id.combinedRadicalsGrid);
        this.storyEditor = (EditText)v.findViewById(R.id.combined_story_edit);

        this.radicalsCard = v.findViewById(R.id.combinedRadicalsCard);
        this.radicalAdapter = new RadicalAdapter(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<Kanji>());
        this.radicalsGrid.setAdapter(this.radicalAdapter);

        return v;
    }

    @Override
    public void onResume() {
        Log.i("nakama", "TeachingCombinedStoryInfoFragment.setCharacter " + this.character);
        try {
            final DictionarySet dictionarySet = new DictionarySetAndroid(getActivity());
            Kanji k = dictionarySet.kanjiFinder().find(this.character);

            CharacterTranslationListAsyncTask.AddTranslation adder = new CharacterTranslationListAsyncTask.AddTranslation(){
                public void add(Translation t){
                    Activity parent = getActivity();
                    if(parent == null){
                        return;
                    }

                    combinedExamplesAdapter.add(t);
                }
            };

            if(searchTask == null){
                this.searchTask = new CharacterTranslationListAsyncTask(adder, getActivity().getApplicationContext(), character);
                this.searchTask.execute();
            }

            if(this.radicalAdapter != null && k != null) {
                this.radicalAdapter.clear();
                this.loadFileTask = new LoadRadicalsFile(this.getActivity(), this.character, this.radicalAdapter, this.radicalsCard);
                this.loadFileTask.execute();
            }

        } catch (IOException e) {
            Log.e("nakama", "Error finding kanji support for: " + this.character);
        }

        checkForStoryAccess();

        super.onResume();
    }

    public void setCharacter(char c){
        Log.i("nakama", "TeachingCombinedStoryInfoFragment.setCharacter " + c);
        this.character = c;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        if(searchTask != null){
            searchTask.cancel(true);
        }
        if(networkStoriesAsyncTask != null){
            networkStoriesAsyncTask.cancel(true);
        }
        super.onDetach();
        mListener = null;
    }

    private UUID getIid(){
        if(this.iid == null) {
            this.iid = IidFactory.get();
        }
        return this.iid;

    }

    public void loadRemoteStories(char character){
        if(networkStoriesAsyncTask != null){
            return;
        }

        final Resources r = this.getResources();
        final int paddingPx = 0;
        networkStoriesAsyncTask = new NetworkStoriesAsyncTask(character, this.getIid(), new NetworkStoriesAsyncTask.AddString() {

            @Override public void add(final String s) {
                Log.d("nakama", "Adding story as view: " + s);
                if (s.startsWith("Network error")) {
                    return;
                }

                TextView tv = new TextView(getActivity());
                tv.setText(s);
                tv.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

                int imageWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, r.getDisplayMetrics());

                FrameLayout layout = new FrameLayout(getActivity());
                {
                    FrameLayout.LayoutParams ll = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    ll.setMargins(0, 0, imageWidth, 0);
                    layout.addView(tv, ll);
                }

                ImageView iv = new ImageView(getActivity());
                iv.setImageDrawable(r.getDrawable(R.drawable.ic_story_for_white_bg));
                iv.setClickable(true);
                iv.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        storyEditor.setText(s);
                        Toast.makeText(getActivity(), "Your story for this character has been updated.", Toast.LENGTH_SHORT).show();
                    }
                });

                {
                    FrameLayout.LayoutParams llv = new FrameLayout.LayoutParams(imageWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
                    llv.gravity = Gravity.RIGHT;
                    layout.addView(iv, llv);
                }

                combinedStoriesCard.setVisibility(View.VISIBLE);
                combinedStoriesLayout.addView(layout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                Log.d("nakama", "Added story as view: " + s);
            }
        });
        networkStoriesAsyncTask.execute();
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
