package dmeeuwis.kanjimaster.ui.sections.seteditor;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.data.CharacterSets;
import dmeeuwis.kanjimaster.logic.data.CharacterStudySet;
import dmeeuwis.kanjimaster.logic.data.CustomCharacterSetDataHelper;
import dmeeuwis.kanjimaster.ui.sections.primary.KanjiMasterActivity;
import dmeeuwis.kanjimaster.ui.views.AutofitRecyclerView;
import dmeeuwis.kanjimaster.ui.billing.LockCheckerInAppBillingService;
import dmeeuwis.kanjimaster.ui.views.PurchaseDialog;

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

    private static final char HEADER_CHAR = ' ';
    private static final char METADATA_CHAR = 'X';

    private CharacterStudySet studySet;
    private AutofitRecyclerView grid;

    private TextView nameEdit, descriptionEdit;

    private static final int SELECT_COLOUR = 0xffbbdefb;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CharacterSetDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);

        if (getArguments().containsKey(CHARSET_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            String name = getArguments().getString(CHARSET_ID);

            // for small devices, this fragment gets loaded into a otherwise-empty activity, and "create"
            // is passed as id. On large layouts, this fragment is beside the list, and setCharacterStudySet is called instead
            if(name.equals("create")){
                studySet = CharacterSets.createCustom();
            } else {
                studySet = CharacterSets.fromName(name, new LockCheckerInAppBillingService(getActivity()));
            }
        }
    }

    public CharacterStudySet cancel(){
        return studySet;
    }

    public boolean save(){
        String editName = nameEdit.getText().toString();
        String editDesc = descriptionEdit.getText().toString();
        String characters = ((CharacterGridAdapter)grid.getAdapter()).getCharacters();

        if(editName.isEmpty()){
            Toast.makeText(getContext(), "Custom character set must have a name.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if(editDesc.isEmpty()){
            Toast.makeText(getContext(), "Custom character set must have a description.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if(characters.length() == 0){
            Toast.makeText(getContext(), "Custom character set must have at least 1 character selected.", Toast.LENGTH_SHORT).show();
            return false;
        }

        new CustomCharacterSetDataHelper().recordEdit(studySet.pathPrefix, editName, editDesc, characters);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(KanjiMasterActivity.CHAR_SET, studySet.pathPrefix);
        ed.apply();

        return true;
    }

    public CharacterStudySet getEditingSet(){
        return studySet;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.characterset_detail, container, false);

        if (studySet != null) {

            grid = rootView.findViewById(R.id.charset_detail_grid);
            grid.setAdapter(new CharacterGridAdapter(
                    CharacterSets.standardSets(
                            new LockCheckerInAppBillingService(getActivity())
                    )));
        }

        return rootView;
    }

    public static void makeColorAnimater(final View view, int color1, int color2){
        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(color1, color2);
        anim.setEvaluator(new ArgbEvaluator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                view.setBackgroundColor((Integer)valueAnimator.getAnimatedValue());
            }
        });

        anim.setDuration(100);
        anim.start();
    }

    public class CharacterGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final int CHARACTER_TYPE = 0;
        private final int HEADER_TYPE = 1;
        private final int METADATA_TYPE = 2;

        private final String asLongString;
        private final BitSet selected;
        public final Map<Integer, String> headers;
        private final BitSet locked;
        private final Drawable lockIcon;


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

                        if(locked.get(currentPosition)){
                            PurchaseDialog pd = PurchaseDialog.make(PurchaseDialog.DialogMessage.LOCKED_CHARACTER);
                            pd.show(getActivity().getSupportFragmentManager(), "purchase");
                            return;
                        }

                       if(selected.get(currentPosition)){
                           selected.set(currentPosition, false);
                           makeColorAnimater(text, SELECT_COLOUR, Color.WHITE);
                       } else {
                           selected.set(currentPosition, true);
                           makeColorAnimater(text, Color.WHITE, SELECT_COLOUR);
                       }

                        studySet.allCharactersSet.clear();
                        studySet.allCharactersSet.addAll(getCharactersAsSet());
                    }
                });
            }
        }

        private class HeaderViewHolder extends RecyclerView.ViewHolder {
            public TextView text, allLink, noneLink;
            public HeaderViewHolder(View itemView) {
                super(itemView);
                this.text = (TextView)itemView.findViewById(R.id.character_grid_header);

                this.allLink = (TextView)itemView.findViewById(R.id.character_set_select_all_link);
                this.allLink.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int i = getAdapterPosition();
                        i++;
                        while(i < asLongString.length() && asLongString.charAt(i) != HEADER_CHAR){
                            if(!locked.get(i) && !selected.get(i)) {
                                selected.set(i, true);
                            }
                            i++;
                        }
                        notifyItemRangeChanged(getAdapterPosition(), i-1);
                        studySet.allCharactersSet.clear();
                        studySet.allCharactersSet.addAll(getCharactersAsSet());
                    }
                });


                this.noneLink = (TextView)itemView.findViewById(R.id.character_set_select_none_link);
                this.noneLink.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int i = getAdapterPosition();
                        Log.i("nakama", "Header type onClick! " + i);
                        i++;
                        while(i < asLongString.length() && asLongString.charAt(i) != HEADER_CHAR){
                            Log.i("nakama", "Selecting " + i);
                            if(selected.get(i)) {
                                selected.set(i, false);
                                notifyItemChanged(i);
                            }
                            i++;
                        }
                        studySet.allCharactersSet.clear();
                        studySet.allCharactersSet.addAll(getCharactersAsSet());
                    }
                });
            }
        }

        CharacterGridAdapter(CharacterStudySet[] sets){
            headers = new HashMap<>();
            headers.put(0, "Meta");
            StringBuilder sb = new StringBuilder();
            sb.append(METADATA_CHAR);             // represents edit metadata header
            for(CharacterStudySet s: sets){
                headers.put(sb.length(), s.name);
                sb.append(String.valueOf(HEADER_CHAR)); // header
                sb.append(s.charactersAsString());
            }
            this.asLongString = sb.toString();
            this.selected = new BitSet(asLongString.length());
            this.locked = new BitSet(asLongString.length());

            if(studySet != null){
                for(char c: studySet.allCharactersSet){
                    int index = asLongString.indexOf(c);
                    if(index != -1) {
                        this.selected.set(index);
                    }
                }
            }

            if(sets[sets.length-1].locked()) {
                HashSet<Character> allFree = new HashSet<>();
                for (CharacterStudySet s : sets) {
                    if(s.locked()) {
                        allFree.addAll(s.freeCharactersSet);
                    } else {
                        allFree.addAll(s.allCharactersSet);
                    }
                }

                for(int i = 0; i < asLongString.length(); i++){
                    if(asLongString.charAt(i) == HEADER_CHAR || asLongString.charAt(i) == METADATA_CHAR){
                        continue;
                    }

                    if(!allFree.contains(asLongString.charAt(i))){
                        locked.set(i);
                    }
                }
            }
            lockIcon = getResources().getDrawable(R.drawable.ic_lock_gray);
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

                nameEdit = (TextView) mView.findViewById(R.id.characterset_name);
                nameEdit.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        studySet.name = charSequence.toString();
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                descriptionEdit = (TextView) mView.findViewById(R.id.characterset_detail);
                descriptionEdit.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        studySet.description = charSequence.toString();
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                return vh;
            } else {
                View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_character_header_layout, parent, false);
                HeaderViewHolder vh = new HeaderViewHolder(mView);
                return vh;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if(asLongString.charAt(position) == METADATA_CHAR) {
                MetadataViewHolder ch = (MetadataViewHolder) holder;
                ch.nameInput.setText(studySet.name);
                ch.descInput.setText(studySet.description);

            } else if(asLongString.charAt(position) == HEADER_CHAR){
                HeaderViewHolder ch = (HeaderViewHolder) holder;
                ch.text.setText(headers.get(position));
            } else {
                CharacterViewHolder ch = (CharacterViewHolder) holder;
                ch.text.setText(String.valueOf(asLongString.charAt(position)));

                if(locked.get(position)) {
                    ch.text.setBackgroundDrawable(lockIcon);
                } else if(selected.get(position)){
                        ch.text.setBackgroundColor(SELECT_COLOUR);
                } else {
                    ch.text.setBackgroundColor(Color.WHITE);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            switch (asLongString.charAt(position)){
                case HEADER_CHAR: return HEADER_TYPE;
                case METADATA_CHAR: return METADATA_TYPE;
                default:  return CHARACTER_TYPE;
            }
        }

        @Override
        public int getItemCount() {
            return asLongString.length();
        }

        public String getCharacters(){
            StringBuilder s = new StringBuilder();
            for(int i = 0; i < selected.length(); i++){
                if(selected.get(i)){
                    s.append(asLongString.charAt(i));
                }
            }
            return s.toString();
        }

        Set<Character> getCharactersAsSet(){
            Set<Character> s = new HashSet<>();
            for(int i = 0; i < selected.length(); i++){
                if(selected.get(i)){
                    s.add(asLongString.charAt(i));
                }
            }
            return s;
        }
    }
}
