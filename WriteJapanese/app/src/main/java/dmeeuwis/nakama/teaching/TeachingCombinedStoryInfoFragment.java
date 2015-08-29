package dmeeuwis.nakama.teaching;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dmeeuwis.Kanji;
import dmeeuwis.Translation;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.data.LoadRadicalsFile;
import dmeeuwis.nakama.data.RadicalAdapter;
import dmeeuwis.nakama.data.StoryDataHelper;
import dmeeuwis.nakama.primary.Iid;
import dmeeuwis.nakama.views.AdvancedFuriganaTextView;
import dmeeuwis.nakama.views.KanjiTranslationListAsyncTask;
import dmeeuwis.nakama.views.NetworkStoriesAsyncTask;
import dmeeuwis.nakama.views.NetworkStorySaveAsyncTask;
import dmeeuwis.nakama.views.TallGridView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TeachingCombinedStoryInfoFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TeachingCombinedStoryInfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TeachingCombinedStoryInfoFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    private char character;
    private UUID iid;

    private LinearLayout combinedExamplesLayout, combinedStoriesLayout;
    private TallGridView radicalsGrid;
    private EditText storyEditor;
    private CardView combinedStoriesCard;

    private KanjiTranslationListAsyncTask searchTask;
    private NetworkStoriesAsyncTask networkStoriesAsyncTask;
    private OnFragmentInteractionListener mListener;

    ArrayAdapter<Kanji> radicalAdapter;
    LoadRadicalsFile loadFileTask;
    View radicalsCard, examplesCard;

    List<String> networkStories = new ArrayList<>();

    public static TeachingCombinedStoryInfoFragment newInstance(String charsetPath) {
        TeachingCombinedStoryInfoFragment fragment = new TeachingCombinedStoryInfoFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public TeachingCombinedStoryInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teaching_combined_story_info, container, false);

        this.combinedStoriesLayout = (LinearLayout)v.findViewById(R.id.combined_stories);
        this.combinedStoriesCard = (CardView)v.findViewById(R.id.combined_stories_card);
        this.examplesCard = v.findViewById(R.id.combined_examples_card);
        this.combinedExamplesLayout = (LinearLayout)v.findViewById(R.id.combined_examples);
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
            final DictionarySet dictionarySet = new DictionarySet(getActivity());
            Kanji k = dictionarySet.kanjiFinder().find(this.character);

            KanjiTranslationListAsyncTask.AddTranslation adder = new KanjiTranslationListAsyncTask.AddTranslation(){
                public void add(Translation t){
                    View newTranslation = View.inflate(getActivity(), R.layout.translation_slide_contents, null);

                    AdvancedFuriganaTextView af = (AdvancedFuriganaTextView) newTranslation.findViewById(R.id.kanji);
                    af.setTranslation(t, dictionarySet.kanjiFinder());
                    TextView eng = (TextView) newTranslation.findViewById(R.id.english);
                    eng.setText(t.toEnglishString());
                    combinedExamplesLayout.addView(newTranslation);

                    examplesCard.setVisibility(View.VISIBLE);
                }
            };

            if(searchTask == null && k != null){
                this.searchTask = new KanjiTranslationListAsyncTask(adder, dictionarySet, k.kanji);
                this.searchTask.execute();
            }

            if(this.radicalAdapter != null && k != null) {
                this.radicalAdapter.clear();
                this.loadFileTask = new LoadRadicalsFile(this.getActivity(), this.character, this.radicalAdapter, this.radicalsCard);
                this.loadFileTask.execute();

                this.loadRemoteStories(this.character);
            }

        } catch (IOException e) {
            Log.e("nakama", "Error finding kanji support for: " + this.character);
        }

        super.onResume();
    }

    public void setCharacter(char c, final Activity parent){
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
        super.onDetach();
        mListener = null;
    }

    private UUID getIid(){
        if(this.iid == null) {
            this.iid = Iid.get(getActivity().getApplication());
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }
}
