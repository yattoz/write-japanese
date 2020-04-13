package dmeeuwis.kanjimaster.ui.sections.progress;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import java.util.List;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.Constants;
import dmeeuwis.kanjimaster.ui.billing.LockChecker;
import dmeeuwis.kanjimaster.ui.billing.LockCheckerInAppBillingService;
import dmeeuwis.kanjimaster.ui.sections.primary.KanjiMasterActivity;
import dmeeuwis.kanjimaster.ui.sections.primary.LockCheckerHolder;

public class ProgressLogActivity extends AppCompatActivity
        implements LockCheckerHolder, PracticeLogFragment.OnListFragmentInteractionListener, PracticeLogAsyncTask.PracticeLogAsyncCallback {

    LockCheckerInAppBillingService lc;
    String callingClass;
    String callingPath;

    PracticeLogAsyncTask logAsyncTask;
    SectionsPagerAdapter sectionsPagerAdapter;
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_log);
        sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        lc = new LockCheckerInAppBillingService(this);

        ActionBar actionBar = this.getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Bundle params;
        Intent intent = getIntent();
        if(intent == null) {
            startActivity(new Intent(this, KanjiMasterActivity.class));
            return;
        } else {
            params = getIntent().getExtras();
            if (params == null) {
                startActivity(new Intent(this, KanjiMasterActivity.class));
                return;
            }
        }

        callingClass = params.getString("parent");
        callingPath = params.getString(Constants.KANJI_PATH_PARAM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("nakama", "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        if (!lc.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d("nakama", "AbstractMasterActivity: onActivityResult handled by IABUtil.");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        logAsyncTask = new PracticeLogAsyncTask(callingPath, getApplicationContext(), this);
        logAsyncTask.execute();
    }

    @Override
    protected void onDestroy() {
        if(this.lc != null) {
            this.lc.dispose();
        }

        if(logAsyncTask != null){
            this.logAsyncTask.cancel(true);
        }

        super.onDestroy();
    }

    @Override protected void onNewIntent(Intent intent){
        Log.i("nakama", "ProgressActivity lifecycle onNewIntent");
        super.onNewIntent(intent);
        this.setIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("nakama", "ActionBar: in onOptionsItemSelected: " + item.toString());

        if(item.getItemId() == android.R.id.home){
            try {
                Intent backUp = new Intent(this, Class.forName(callingClass));
                startActivity(backUp);
            } catch (ClassNotFoundException e) {
                Log.e("nakama", "ClassNotFoundException while returning to parent. callingClass is " + callingClass, e);
                return false;
            } catch (Throwable t) {
                Log.e("nakama", "Throwable while returning to parent. callingClass is " + callingClass, t);
                return false;
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public LockChecker getLockChecker() {
        return this.lc;
    }

    @Override
    public void onListFragmentInteraction(PracticeLogAsyncTask.PracticeLog item) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onCompletion(List<PracticeLogAsyncTask.PracticeLog> logs) {
        Fragment f = sectionsPagerAdapter.itemAt(1);
        PracticeLogFragment plf = (PracticeLogFragment) f;
        plf.setData(logs);
    }
}