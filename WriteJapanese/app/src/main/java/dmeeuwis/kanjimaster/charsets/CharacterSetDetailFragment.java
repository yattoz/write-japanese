package dmeeuwis.kanjimaster.charsets;

import android.app.Activity;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.primary.Iid;
import dmeeuwis.nakama.views.AutofitRecyclerView;
import dmeeuwis.nakama.views.LockCheckerInAppBillingService;

/**
 * A fragment representing a single CharacterSet detail screen.
 * This fragment is either contained in a {@link CharacterSetListActivity}
 * in two-pane mode (on tablets) or a {@link CharacterSetDetailActivity}
 * on handsets.
 */
public class CharacterSetDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private CharacterStudySet studySet;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CharacterSetDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // is it safe?
        DictionarySet ds = DictionarySet.get(getActivity().getApplicationContext());

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            String name = getArguments().getString(ARG_ITEM_ID);
            studySet = CharacterSets.fromName(name, ds.kanjiFinder(), new LockCheckerInAppBillingService(getActivity()), Iid.get(getActivity().getApplicationContext()));

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(studySet.name);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.characterset_detail, container, false);

        if (studySet != null) {
            ((TextView) rootView.findViewById(R.id.characterset_detail)).setText(studySet.description);

            AutofitRecyclerView grid = (AutofitRecyclerView) rootView.findViewById(R.id.charset_detail_grid);
            grid.setAdapter(new CharacterGridAdapter(studySet));
        }

        return rootView;
    }

    private static class CharacterGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final CharacterStudySet set;

        private class CharacterViewHolder extends RecyclerView.ViewHolder {
            public TextView text;
            public CharacterViewHolder(View itemView) {
                super(itemView);
                this.text = (TextView)itemView.findViewById(R.id.character_grid_text);
            }
        }

        CharacterGridAdapter(CharacterStudySet set){
           this.set = set;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_character_grid_layout, parent, false);
            CharacterViewHolder vh = new CharacterViewHolder(mView);
            return vh;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            CharacterViewHolder ch = (CharacterViewHolder)holder;
            ch.text.setText(String.valueOf(set.charactersAsString().charAt(position)));
        }

        @Override
        public int getItemCount() {
            return set.charactersAsString().length();
        }
    }
}
