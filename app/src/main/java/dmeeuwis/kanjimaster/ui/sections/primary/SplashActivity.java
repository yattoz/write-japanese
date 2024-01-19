package dmeeuwis.kanjimaster.ui.sections.primary;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, KanjiMasterActivity.class);
        startActivity(intent);
        finish();
    }
}
