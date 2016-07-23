package dmeeuwis.nakama.primary;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.LockChecker;
import dmeeuwis.nakama.OnFragmentInteractionListener;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.DictionarySet;

public class CharsetInfoActivity extends ActionBarActivity implements OnGoalPickListener, OnFragmentInteractionListener {

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
        String charsetName = getIntent().getExtras().getString("charset");
        charset = CharacterSets.fromName(charsetName, dictionarySet.kanjiFinder(), new LockChecker(this, null), Iid.get(this.getApplicationContext()));
        charset.load(this.getApplicationContext());

        CharacterSetStatusFragment frag = (CharacterSetStatusFragment) getSupportFragmentManager().findFragmentById(R.id.charset_holder);
        frag.setCharset(charset);

        super.onResume();
    }

    public void setGoal(int year, int month, int day){
        CharacterSetStatusFragment frag = (CharacterSetStatusFragment) getSupportFragmentManager().findFragmentById(R.id.charset_holder);
        frag.setGoal(year, month, day);
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
