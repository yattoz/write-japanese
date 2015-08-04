package dmeeuwis.nakama.primary;

import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.LockChecker;
import dmeeuwis.nakama.data.CharacterProgressDataHelper;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.DictionarySet;

public class CharsetInfoActivity extends ActionBarActivity implements CharacterSetStatusFragment.OnFragmentInteractionListener {

    DictionarySet dictionarySet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charset_info);

        this.dictionarySet = DictionarySet.get(this.getApplicationContext());
        String charsetName = getIntent().getExtras().getString("charset");
        CharacterStudySet charset = CharacterSets.fromName(charsetName, this.dictionarySet.kanjiFinder(), new LockChecker(this, null));

        CharacterSetStatusFragment frag = (CharacterSetStatusFragment) getFragmentManager().findFragmentById(R.id.charset_fragment);
        frag.setCharset(charset);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_charset_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.i("nakama", "CharsetInfoActivity.onFragmentInteraction: " + uri);
    }
}
