package dmeeuwis.kanjimaster.charsets;

import android.app.Activity;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import java.util.HashMap;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.ProgressActivity;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.data.ProgressTracker;
import dmeeuwis.nakama.primary.Iid;
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
    private CharacterStudySet mItem;

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
            mItem = CharacterSets.fromName(name, ds.kanjiFinder(), new LockCheckerInAppBillingService(getActivity()), Iid.get(getActivity().getApplicationContext()));

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(mItem.name);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.characterset_detail, container, false);

        // Show the dummy content as text in a TextView.
        if (mItem != null) {
            ((TextView) rootView.findViewById(R.id.characterset_detail)).setText(mItem.description);

            GridView grid = (GridView)rootView.findViewById(R.id.charset_detail_grid);
            grid.setAdapter(new ProgressActivity.CharacterGridAdapter(
                    this.getActivity(), mItem.charactersAsString(), mItem.availableCharactersSet(), new HashMap<Character, ProgressTracker.Progress>()));
        }

        return rootView;
    }
}
