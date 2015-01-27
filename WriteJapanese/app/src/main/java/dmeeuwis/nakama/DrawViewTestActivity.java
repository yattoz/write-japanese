package dmeeuwis.nakama;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.views.DrawView;
import dmeeuwis.nakama.views.SquareSignatureView;

public class DrawViewTestActivity extends ActionBarActivity {

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
