package dmeeuwis.kanjimaster.ui.sections.credits;

import android.content.Intent;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.ui.sections.primary.KanjiMasterActivity;

public class ReleaseNotesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_release_notes);

        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        WebView v = (WebView) findViewById(R.id.release_notes_webview);
        v.loadUrl("file:///android_asset/release-notes.html");

        v.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });
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
