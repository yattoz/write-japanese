package dmeeuwis.nakama.primary;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.OnFragmentInteractionListener;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.views.LockCheckerIInAppBillingService;

/**
 * On small devices, holds the CharacterSetStatusFragment all by itself.
 */
public class CharsetInfoActivity extends ActionBarActivity implements OnGoalPickListener, OnFragmentInteractionListener {
    private final String CHARSET_SAVED_INSTANCE = "charset";

    CharacterStudySet charset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charset_info);

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
        DictionarySet dictionarySet = DictionarySet.get(this.getApplicationContext());
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

        charset = CharacterSets.fromName(charsetName, dictionarySet.kanjiFinder(), new LockCheckerIInAppBillingService(this), Iid.get(this.getApplicationContext()));
        charset.load(this.getApplicationContext());

        CharacterSetStatusFragment frag = (CharacterSetStatusFragment) getSupportFragmentManager().findFragmentById(R.id.charset_holder);
        frag.setCharset(charset);

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
        charset.save(this.getApplicationContext());
        super.onPause();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.i("nakama", "onFragmentInteration: " + uri.toString());
    }
}
