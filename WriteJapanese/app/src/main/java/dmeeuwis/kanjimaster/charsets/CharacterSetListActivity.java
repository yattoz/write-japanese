package dmeeuwis.kanjimaster.charsets;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.CustomCharacterSetDataHelper;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.primary.KanjiMasterActivity;

/**
 * An activity representing a list of CharacterSets. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link CharacterSetDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class CharacterSetListActivity extends ActionBarActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    DictionarySet set;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_characterset_list);

        set = DictionarySet.get(getApplicationContext());

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Character Study Sets");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mTwoPane){
                    Bundle arguments = new Bundle();
                    arguments.putString(CharacterSetDetailFragment.CHARSET_ID, "create");
                    CharacterSetDetailFragment fragment = new CharacterSetDetailFragment();
                    fragment.setArguments(arguments);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.characterset_detail_container, fragment)
                            .commit();
                } else {
                    Intent intent = new Intent(CharacterSetListActivity.this, CharacterSetDetailActivity.class);
                    intent.putExtra(CharacterSetDetailFragment.CHARSET_ID, "create");
                    startActivity(intent);
                }
            }
        });

        View recyclerView = findViewById(R.id.characterset_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);

        if (findViewById(R.id.characterset_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, new Intent(this, KanjiMasterActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        List<CharacterStudySet> sets = new CustomCharacterSetDataHelper(this).getSets();
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(sets));
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<CharacterStudySet> sets;

        public SimpleItemRecyclerViewAdapter(List<CharacterStudySet> items) {
            sets = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.characterset_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = sets.get(position);
            holder.nameField.setText(sets.get(position).name);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putString(CharacterSetDetailFragment.CHARSET_ID, holder.mItem.pathPrefix);
                        CharacterSetDetailFragment fragment = new CharacterSetDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.characterset_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, CharacterSetDetailActivity.class);
                        intent.putExtra(CharacterSetDetailFragment.CHARSET_ID, holder.mItem.pathPrefix);
                        context.startActivity(intent);
                    }
                }
            });

            boolean sysSet = sets.get(position).systemSet;
            holder.editButton.setVisibility(sysSet ? View.GONE : View.VISIBLE);
            holder.deleteButton.setVisibility(sysSet ? View.GONE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return sets.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            final View mView;
            final TextView nameField;
            final View editButton, copyButton, deleteButton;
            CharacterStudySet mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                nameField = (TextView) view.findViewById(R.id.charset_list_name);

                editButton = view.findViewById(R.id.charset_edit);
                deleteButton = view.findViewById(R.id.charset_delete);
                copyButton = view.findViewById(R.id.charset_copy);
            }
        }
    }
}
