package dmeeuwis.kanjimaster.ui.sections.tests;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.ui.views.DrawView;
import dmeeuwis.kanjimaster.ui.views.SquareSignatureView;

public class DrawViewTestActivity extends AppCompatActivity {

    DrawView dv;
    SquareSignatureView sv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_view_test);

        this.dv = (DrawView)this.findViewById(R.id.testDrawView);
        this.sv = (SquareSignatureView)this.findViewById(R.id.testAlternate);
    }

    @Override
    public void onBackPressed() {
        this.dv.clear();
        this.sv.clear();
    }
}
