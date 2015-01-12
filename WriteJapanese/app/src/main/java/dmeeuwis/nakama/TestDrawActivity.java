package dmeeuwis.nakama;

import android.graphics.Color;
import android.graphics.Point;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.kanjidraw.DrawingComparator;
import dmeeuwis.nakama.kanjidraw.PathCalculator;
import dmeeuwis.nakama.views.AnimatedCurveView;
import dmeeuwis.nakama.views.DrawView;

public class TestDrawActivity extends ActionBarActivity {

    DrawView dv;
    AnimatedCurveView av;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_draw);
        dv = (DrawView)this.findViewById(R.id.drawView);
        dv.setBackgroundColor(Color.LTGRAY);
        av = (AnimatedCurveView)this.findViewById(R.id.displayView);

        dv.setOnStrokeListener(new DrawView.OnStrokeListener() {
            @Override public void onStroke(List<Point> stroke) {
               av.setDrawing(dv.getDrawing(), AnimatedCurveView.DrawTime.STATIC);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (dv.getStrokeCount() == 0){
            super.onBackPressed();
        } else {
            dv.clear();
            av.clear();
        }
    }
}
