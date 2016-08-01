package dmeeuwis.nakama;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.primary.KanjiMasterActivity;

public class ReleaseNotesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_release_notes);

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        WebView v = (WebView) findViewById(R.id.release_notes_webview);
        v.loadUrl("file:///android_asset/release-notes.html");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent backUp = new Intent(this, KanjiMasterActivity.class);
            startActivity(backUp);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
