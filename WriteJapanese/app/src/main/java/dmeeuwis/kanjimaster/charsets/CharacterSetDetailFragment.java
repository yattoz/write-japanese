package dmeeuwis.kanjimaster.charsets;

import android.app.Activity;
import android.graphics.Color;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;


import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

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
                appBarLayout.setTitle("Edit Character Set");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.characterset_detail, container, false);

        if (studySet != null) {
            ((EditText) rootView.findViewById(R.id.characterset_name)).setText(studySet.name);
            ((EditText) rootView.findViewById(R.id.characterset_detail)).setText(studySet.description);

            AutofitRecyclerView grid = (AutofitRecyclerView) rootView.findViewById(R.id.charset_detail_grid);
            grid.setAdapter(new CharacterGridAdapter(
                    CharacterSets.all(
                            new LockCheckerInAppBillingService(getActivity()),
                            Iid.get(getActivity().getApplicationContext()))));
        }

        return rootView;
    }

    public static class CharacterGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final int CHARACTER_TYPE = 0;
        private final int HEADER_TYPE = 1;

        private final String asLongString;
        private final BitSet selected;
        public final Map<Integer, String> headers;

        private class CharacterViewHolder extends RecyclerView.ViewHolder {
            public TextView text;
            int currentPosition;

            CharacterViewHolder(View itemView) {
                super(itemView);
                this.text = (TextView)itemView.findViewById(R.id.character_grid_text);

                this.text.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                       if(selected.get(currentPosition)){
                           selected.set(currentPosition, false);
                           text.setBackgroundColor(Color.WHITE);
                       } else {
                           selected.set(currentPosition, true);
                           text.setBackgroundColor(Color.GREEN);
                       }
                    }
                });
            }
        }

        private class HeaderViewHolder extends RecyclerView.ViewHolder {
            public TextView text;
            public HeaderViewHolder(View itemView) {
                super(itemView);
                this.text = (TextView)itemView.findViewById(R.id.character_grid_header);
            }
        }

        CharacterGridAdapter(CharacterStudySet[] sets){
            headers = new HashMap<>();
            StringBuilder sb = new StringBuilder();
            for(CharacterStudySet s: sets){
                headers.put(sb.length(), s.name);
                sb.append(" "); // header
                sb.append(s.charactersAsString());
            }
            this.asLongString = sb.toString();
            this.selected = new BitSet(asLongString.length());
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if(viewType == CHARACTER_TYPE) {
                View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_character_grid_layout, parent, false);
                CharacterViewHolder vh = new CharacterViewHolder(mView);
                return vh;
            } else {
                View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_character_header_layout, parent, false);
                HeaderViewHolder vh = new HeaderViewHolder(mView);
                return vh;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if(asLongString.charAt(position) == ' '){
                HeaderViewHolder ch = (HeaderViewHolder) holder;
                ch.text.setText(headers.get(position));
            } else {
                CharacterViewHolder ch = (CharacterViewHolder) holder;
                ch.text.setText(String.valueOf(asLongString.charAt(position)));
                ch.currentPosition = position;

                if(selected.get(position)){
                    ch.text.setBackgroundColor(Color.GREEN);
                } else {
                    ch.text.setBackgroundColor(Color.WHITE);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            // Just as an example, return 0 or 2 depending on position
            // Note that unlike in ListView adapters, types don't have to be contiguous
            return asLongString.charAt(position) == ' ' ? HEADER_TYPE : CHARACTER_TYPE;
        }

        @Override
        public int getItemCount() {
            return asLongString.length();
        }
    }
}
