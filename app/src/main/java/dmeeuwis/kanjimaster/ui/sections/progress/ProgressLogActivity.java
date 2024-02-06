package dmeeuwis.kanjimaster.ui.sections.progress;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import java.util.List;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.Constants;
import dmeeuwis.kanjimaster.logic.data.UncaughtExceptionLogger;
import dmeeuwis.kanjimaster.ui.billing.LockChecker;
import dmeeuwis.kanjimaster.ui.billing.LockCheckerInAppBillingService;
import dmeeuwis.kanjimaster.ui.sections.primary.KanjiMasterActivity;
import dmeeuwis.kanjimaster.ui.sections.primary.LockCheckerHolder;
import dmeeuwis.kanjimaster.ui.util.KanjiMasterUncaughtExceptionHandler;

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

        Thread.setDefaultUncaughtExceptionHandler(new KanjiMasterUncaughtExceptionHandler());

        setContentView(R.layout.activity_progress_log);
        FragmentManager man = getSupportFragmentManager();
        sectionsPagerAdapter = new SectionsPagerAdapter(this, man);
        sectionsPagerAdapter.addFragment(ProgressFragment.newInstance(0));
        sectionsPagerAdapter.addFragment(PracticeLogFragment.newInstance(1));

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        lc = new LockCheckerInAppBillingService(this);

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

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

        if(plf == null){
            UncaughtExceptionLogger.backgroundLogError("Error: null PracticeLogFragment", new RuntimeException());
            return;
        }
        plf.setData(logs);
    }
}