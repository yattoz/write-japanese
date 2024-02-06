package dmeeuwis.kanjimaster.ui.sections.seteditor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.app.NavUtils;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.data.UncaughtExceptionLogger;
import dmeeuwis.kanjimaster.ui.billing.LockChecker;
import dmeeuwis.kanjimaster.ui.sections.primary.LockCheckerHolder;
import dmeeuwis.kanjimaster.ui.sections.primary.KanjiMasterActivity;
import dmeeuwis.kanjimaster.ui.billing.LockCheckerInAppBillingService;

/**
 * An activity representing a single CharacterSet detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link CharacterSetListActivity}.
 */
public class CharacterSetDetailActivity extends AppCompatActivity implements LockCheckerHolder {

    public static String CHARSET_SWITCH_BUNDLE_KEY = "CHARSET_SWITCH";

    LockChecker lockChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_characterset_detail);

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Edit Custom Character Set");

        this.lockChecker = new LockCheckerInAppBillingService(this);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(CharacterSetDetailFragment.CHARSET_ID,
                    getIntent().getStringExtra(CharacterSetDetailFragment.CHARSET_ID));

            CharacterSetDetailFragment fragment = new CharacterSetDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.characterset_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.charset_edit_menu, menu);

        return true;
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
            NavUtils.navigateUpTo(this, new Intent(this, CharacterSetListActivity.class));
            return true;
        } else if(id == R.id.menu_save_character_set){
            CharacterSetDetailFragment f = (CharacterSetDetailFragment) getSupportFragmentManager().findFragmentById(R.id.characterset_detail_container);
            if(f != null){
                boolean saveSuccess = f.save();
                if(saveSuccess) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor ed = prefs.edit();
                    ed.putString(CHARSET_SWITCH_BUNDLE_KEY, f.getEditingSet().pathPrefix);
                    ed.commit();

                    Intent i = new Intent(this, KanjiMasterActivity.class);
                    NavUtils.navigateUpTo(this, i);
                    return true;
                }

            } else {
                Toast.makeText(this, "Error connecting to fragment", Toast.LENGTH_SHORT);
                UncaughtExceptionLogger.backgroundLogError("Error connecting to CharsetDetailFragment", new RuntimeException());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public LockChecker getLockChecker() {
        return this.lockChecker;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(lockChecker != null){
            lockChecker.dispose();
        }
    }
}