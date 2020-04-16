package dmeeuwis.kanjimaster.ui.sections.progress;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.ui.billing.LockChecker;
import dmeeuwis.kanjimaster.ui.sections.primary.OnFragmentInteractionListener;
import dmeeuwis.kanjimaster.logic.data.CharacterSets;
import dmeeuwis.kanjimaster.logic.data.CharacterStudySet;
import dmeeuwis.kanjimaster.ui.billing.LockCheckerInAppBillingService;
import dmeeuwis.kanjimaster.ui.sections.primary.OnGoalPickListener;
import dmeeuwis.kanjimaster.ui.util.KanjiMasterUncaughtExceptionHandler;

/**
 * On small devices, holds the CharacterSetStatusFragment standardSets by itself.
 */
public class CharsetInfoActivity extends AppCompatActivity implements OnGoalPickListener, OnFragmentInteractionListener {
    private final String CHARSET_SAVED_INSTANCE = "charset";

    CharacterStudySet charset;
    LockChecker lockChecker;
    boolean justCreated = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charset_info);

        Thread.setDefaultUncaughtExceptionHandler(new KanjiMasterUncaughtExceptionHandler());

        if (savedInstanceState == null) {
            CharacterSetStatusFragment frag = new CharacterSetStatusFragment();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.charset_holder, frag);
            ft.commit();
        }

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String charsetName;
        Intent i = getIntent();
        String intentCharset = i == null ? null : i.getStringExtra("charset");
        if(intentCharset == null) {
            charsetName = prefs.getString(CHARSET_SAVED_INSTANCE, null);
        } else {
            charsetName = i.getStringExtra(CHARSET_SAVED_INSTANCE);
            SharedPreferences.Editor e = prefs.edit();
            e.putString(CHARSET_SAVED_INSTANCE, charsetName);
            e.apply();
        }

        lockChecker = new LockCheckerInAppBillingService(this);
        charset = CharacterSets.fromName(charsetName, lockChecker);
        charset.load(CharacterStudySet.LoadProgress.LOAD_SET_PROGRESS);

        CharacterSetStatusFragment frag = (CharacterSetStatusFragment) getSupportFragmentManager().findFragmentById(R.id.charset_holder);
        frag.setCharset(charset, justCreated ? 200 : 0);

        justCreated = false;

        super.onResume();
    }

    public void setGoal(int year, int month, int day){
        CharacterSetStatusFragment frag = (CharacterSetStatusFragment) getSupportFragmentManager().findFragmentById(R.id.charset_holder);
        if(frag != null) {
            frag.setGoal(year, month, day);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
       if(lockChecker != null){
           lockChecker.dispose();
       }
       super.onDestroy();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.i("nakama", "onFragmentInteration: " + uri.toString());
    }
}
