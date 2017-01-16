package dmeeuwis.kanjimaster.charsets;

import android.graphics.Color;
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
import dmeeuwis.nakama.data.CustomCharacterSetDataHelper;
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
    public static final String CHARSET_ID = "item_id";

    private CharacterStudySet studySet;
    private AutofitRecyclerView grid;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CharacterSetDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(CHARSET_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            String name = getArguments().getString(CHARSET_ID);

            // for small devices, this fragment gets loaded into a otherwise-empty activity, and "create"
            // is passed as id. On large layouts, this fragment is beside the list, and setCharacterStudySet is called instead
            if(name.equals("create")){
                studySet = CharacterSets.createCustom(Iid.get(getActivity().getApplicationContext()));
            } else {
                studySet = CharacterSets.fromName(name, new LockCheckerInAppBillingService(getActivity()), Iid.get(getActivity().getApplicationContext()));
            }
        }
    }

    public void save(){
        new CustomCharacterSetDataHelper(getActivity()).recordEdit(studySet.pathPrefix, studySet.name, studySet.description, studySet.charactersAsString());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.characterset_detail, container, false);

        if (studySet != null) {

            grid = (AutofitRecyclerView) rootView.findViewById(R.id.charset_detail_grid);
            grid.setAdapter(new CharacterGridAdapter(studySet.name, studySet.description,
                    CharacterSets.all(
                            new LockCheckerInAppBillingService(getActivity()),
                            Iid.get(getActivity().getApplicationContext()))));
        }

        return rootView;
    }

    public void setCharacterStudySet(CharacterStudySet characterStudySet) {
        this.studySet = characterStudySet;
        grid.setAdapter(new CharacterGridAdapter(studySet.name, studySet.description,
                CharacterSets.all(
                        new LockCheckerInAppBillingService(getActivity()),
                        Iid.get(getActivity().getApplicationContext()))));
    }

    public static class CharacterGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final int CHARACTER_TYPE = 0;
        private final int HEADER_TYPE = 1;
        private final int METADATA_TYPE = 2;

        private final String asLongString, setName, setDesc;
        private final BitSet selected;
        public final Map<Integer, String> headers;


        private class MetadataViewHolder extends RecyclerView.ViewHolder {
            final EditText nameInput;
            final EditText descInput;

            MetadataViewHolder(View itemView) {
                super(itemView);
                nameInput = ((EditText) itemView.findViewById(R.id.characterset_name));
                descInput = ((EditText) itemView.findViewById(R.id.characterset_detail));
            }
        }

        private class CharacterViewHolder extends RecyclerView.ViewHolder {
            public TextView text;

            CharacterViewHolder(View itemView) {
                super(itemView);
                this.text = (TextView)itemView.findViewById(R.id.character_grid_text);

                this.text.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                       int currentPosition = getAdapterPosition();
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

        CharacterGridAdapter(String setName, String setDescription, CharacterStudySet[] sets){
            headers = new HashMap<>();
            headers.put(0, "Meta");
            StringBuilder sb = new StringBuilder();
            sb.append('X');             // represents edit metadata header
            for(CharacterStudySet s: sets){
                headers.put(sb.length(), s.name);
                sb.append(" "); // header
                sb.append(s.charactersAsString());
            }
            this.setName = setName;
            this.setDesc = setDescription;
            this.asLongString = sb.toString();
            this.selected = new BitSet(asLongString.length());
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if(viewType == CHARACTER_TYPE) {
                View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_character_grid_layout, parent, false);
                CharacterViewHolder vh = new CharacterViewHolder(mView);
                return vh;
            } else if(viewType == METADATA_TYPE) {
                View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_character_grid_metadata, parent, false);
                MetadataViewHolder vh = new MetadataViewHolder(mView);
                return vh;
            } else {
                View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_character_header_layout, parent, false);
                HeaderViewHolder vh = new HeaderViewHolder(mView);
                return vh;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if(asLongString.charAt(position) == 'X') {
                MetadataViewHolder ch = (MetadataViewHolder) holder;
                ch.nameInput.setText(setName);
                ch.descInput.setText(setDesc);

            } else if(asLongString.charAt(position) == ' '){
                HeaderViewHolder ch = (HeaderViewHolder) holder;
                ch.text.setText(headers.get(position));
            } else {
                CharacterViewHolder ch = (CharacterViewHolder) holder;
                ch.text.setText(String.valueOf(asLongString.charAt(position)));

                if(selected.get(position)){
                    ch.text.setBackgroundColor(Color.GREEN);
                } else {
                    ch.text.setBackgroundColor(Color.WHITE);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            switch (asLongString.charAt(position)){
                case ' ': return HEADER_TYPE;
                case 'X': return METADATA_TYPE;
                default:  return CHARACTER_TYPE;
            }
        }

        @Override
        public int getItemCount() {
            return asLongString.length();
        }
    }
}
