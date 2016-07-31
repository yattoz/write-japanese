package dmeeuwis.nakama;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

import dmeeuwis.kanjimaster.R;

public class ReleaseNotesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_release_notes);
        WebView v = (WebView)findViewById(R.id.release_notes_webview);
        v.loadUrl("file:///android_asset/release-notes.html");
    }
}
