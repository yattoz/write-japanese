package dmeeuwis.kanjimaster.charsets;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.billing.LockChecker;
import dmeeuwis.nakama.LockCheckerHolder;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.CustomCharacterSetDataHelper;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.primary.KanjiMasterActivity;
import dmeeuwis.nakama.billing.LockCheckerInAppBillingService;

/**
 * An activity representing a list of CharacterSets. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link CharacterSetDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class CharacterSetListActivity extends AppCompatActivity implements LockCheckerHolder {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    DictionarySet set;
    LockChecker lockChecker;

    FloatingActionButton fab_new, fab_save, fab_cancel;
    CharacterStudySet lastCancelled = null;
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_characterset_list);

        set = DictionarySet.get(getApplicationContext());

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Character Study Sets");

        fab_new = (FloatingActionButton) findViewById(R.id.fab_new_charset);
        fab_new.setOnClickListener(new View.OnClickListener() {
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

                    fab_save.show();
                    fab_cancel.show();
                    fab_new.hide();
                } else {
                    Intent intent = new Intent(CharacterSetListActivity.this, CharacterSetDetailActivity.class);
                    intent.putExtra(CharacterSetDetailFragment.CHARSET_ID, "create");
                    startActivity(intent);
                }
            }
        });

        fab_save = (FloatingActionButton) findViewById(R.id.fab_save);
        fab_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharacterSetDetailFragment f = (CharacterSetDetailFragment) getSupportFragmentManager().findFragmentById(R.id.characterset_detail_container);
                if(f.save()) {
                    getSupportFragmentManager().beginTransaction()
                            .remove(f)
                            .commit();
                    fab_save.hide();
                    fab_cancel.hide();
                    fab_new.show();

                    setupRecyclerView(recyclerView);
                }
            }
        });

        fab_cancel = (FloatingActionButton) findViewById(R.id.fab_cancel);
        fab_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharacterSetDetailFragment f = (CharacterSetDetailFragment) getSupportFragmentManager().findFragmentById(R.id.characterset_detail_container);
                lastCancelled = f.cancel();
                getSupportFragmentManager().beginTransaction()
                        .remove(f)
                        .commit();
                fab_save.hide();
                fab_cancel.hide();
                fab_new.show();
            }
        });


        fab_cancel = (FloatingActionButton) findViewById(R.id.fab_cancel);

        recyclerView = (RecyclerView) findViewById(R.id.characterset_list);
        assert recyclerView != null;
        setupRecyclerView(recyclerView);

        if (findViewById(R.id.characterset_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        this.lockChecker = new LockCheckerInAppBillingService(this);
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


    @Override
    public void onDestroy(){
        if(lockChecker != null){
            lockChecker.dispose();
        }
        super.onDestroy();
    }

    @Override
    public LockChecker getLockChecker() {
        return this.lockChecker;
    }

    @Override
    public void onResume(){
        super.onResume();

        CharacterSetDetailFragment f = (CharacterSetDetailFragment) getSupportFragmentManager().findFragmentById(R.id.characterset_detail_container);
        if(f != null) {
            fab_save.show();
            fab_cancel.show();
            fab_new.hide();
        }
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<CharacterStudySet> sets;
        private View editButton, deleteButton;

        public SimpleItemRecyclerViewAdapter(List<CharacterStudySet> items) {
            sets = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.characterset_list_content, parent, false);

            final ViewHolder viewHolder = new ViewHolder(view);

            editButton = view.findViewById(R.id.charset_edit);
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mTwoPane){
                        Bundle arguments = new Bundle();
                        arguments.putString(CharacterSetDetailFragment.CHARSET_ID, sets.get(viewHolder.getAdapterPosition()).pathPrefix);
                        CharacterSetDetailFragment fragment = new CharacterSetDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.characterset_detail_container, fragment)
                                .commit();

                        fab_save.show();
                        fab_cancel.show();
                        fab_new.hide();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, CharacterSetDetailActivity.class);
                        intent.putExtra(CharacterSetDetailFragment.CHARSET_ID, sets.get(viewHolder.getAdapterPosition()).pathPrefix);
                        context.startActivity(intent);
                        context.startActivity(intent);
                    }
                }
            });

            deleteButton = view.findViewById(R.id.charset_delete);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = viewHolder.getAdapterPosition();
                    final CharacterStudySet doomed = sets.get(position);
                    new CustomCharacterSetDataHelper(getApplicationContext()).delete(doomed);
                    sets.remove(position);
                    notifyDataSetChanged();

                    CharacterSetDetailFragment f = (CharacterSetDetailFragment) getSupportFragmentManager().findFragmentById(R.id.characterset_detail_container);
                    if(f != null && f.getEditingSet().pathPrefix.equals(doomed.pathPrefix)){
                        getSupportFragmentManager().beginTransaction()
                                .remove(f)
                                .commit();
                    }

                    if(sets.size() == 0){
                        Context context = v.getContext();
                        Intent intent = new Intent(context, KanjiMasterActivity.class);
                        context.startActivity(intent);
                    }

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor ed = prefs.edit();
                    ed.remove(KanjiMasterActivity.CHAR_SET);
                    ed.apply();

                    Snackbar.make(view, "Deleted character set '" + doomed.name + "'", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Undo", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    CustomCharacterSetDataHelper h = new CustomCharacterSetDataHelper(getApplication());
                                    h.unDelete(doomed);
                                    sets.clear();
                                    sets.addAll(h.getSets());
                                    SimpleItemRecyclerViewAdapter.this.notifyDataSetChanged();

                                }
                            })
                            .show();
                }
            });

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putString(CharacterSetDetailFragment.CHARSET_ID, sets.get(viewHolder.getAdapterPosition()).pathPrefix);
                        CharacterSetDetailFragment fragment = new CharacterSetDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.characterset_detail_container, fragment)
                                .commit();

                        fab_save.show();
                        fab_cancel.show();
                        fab_new.hide();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, CharacterSetDetailActivity.class);
                        intent.putExtra(CharacterSetDetailFragment.CHARSET_ID, sets.get(viewHolder.getAdapterPosition()).pathPrefix);
                        context.startActivity(intent);
                    }
                }
            });

            deleteButton = view.findViewById(R.id.charset_delete);

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = sets.get(position);
            holder.nameField.setText(sets.get(position).name);

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
            final View editButton, deleteButton;
            CharacterStudySet mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                nameField = (TextView) view.findViewById(R.id.charset_list_name);

                editButton = view.findViewById(R.id.charset_edit);
                deleteButton = view.findViewById(R.id.charset_delete);
            }
        }
    }

}
