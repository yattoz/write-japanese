package dmeeuwis.nakama.primary;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.LockChecker;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.DictionarySet;

public class CharsetInfoActivity extends ActionBarActivity implements CharacterSetStatusFragment.OnFragmentInteractionListener {

    DictionarySet dictionarySet;
    CharacterStudySet charset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charset_info);

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        this.dictionarySet = DictionarySet.get(this.getApplicationContext());
        String charsetName = getIntent().getExtras().getString("charset");
        charset = CharacterSets.fromName(charsetName, this.dictionarySet.kanjiFinder(), new LockChecker(this, null));
        charset.load(this.getApplicationContext());

        CharacterSetStatusFragment frag = (CharacterSetStatusFragment) getFragmentManager().findFragmentById(R.id.charset_fragment);
        frag.setCharset(charset);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.i("nakama", "CharsetInfoActivity.onFragmentInteraction: " + uri);
    }

    @Override
    public void onPause() {
        charset.save(this.getApplicationContext());
        super.onPause();
    }
}
