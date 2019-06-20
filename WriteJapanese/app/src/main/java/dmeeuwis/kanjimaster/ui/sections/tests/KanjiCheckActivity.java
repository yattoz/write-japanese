package dmeeuwis.kanjimaster.ui.sections.tests;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import dmeeuwis.Kanji;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.data.AndroidInputStreamGenerator;
import dmeeuwis.kanjimaster.logic.data.AssetFinder;
import dmeeuwis.kanjimaster.logic.drawing.Criticism;
import dmeeuwis.kanjimaster.logic.drawing.CurveDrawing;
import dmeeuwis.kanjimaster.ui.views.AnimatedCurveView;

public class KanjiCheckActivity extends AppCompatActivity {

    private static class KanjiViewHolder extends RecyclerView.ViewHolder {

        public AnimatedCurveView curve;
        public AnimatedCurveView points;
        public TextView info;

        public KanjiViewHolder(View itemView) {
            super(itemView);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kanji_check);

        final AssetManager am = this.getAssets();
        final AndroidInputStreamGenerator is = new AndroidInputStreamGenerator(am);
        final AssetFinder af = new AssetFinder(is);

        RecyclerView rv = (RecyclerView)this.findViewById(R.id.kanji_check_list);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemLayout = LayoutInflater.from(parent.getContext()).inflate(R.layout.kanji_check_item, parent, false);
                KanjiViewHolder kh = new KanjiViewHolder(itemLayout);
                kh.curve = (AnimatedCurveView)itemLayout.findViewById(R.id.kanji_check_curve);
                kh.points = (AnimatedCurveView)itemLayout.findViewById(R.id.kanji_check_points);
                kh.info = (TextView) itemLayout.findViewById(R.id.kanji_check_text);

                return kh;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                CurveDrawing cv = null;
                if(position < Kanji.JOUYOU_G1.length()) {
                    try {
                        cv = af.findGlyphForCharacter(Kanji.JOUYOU_G1.charAt(position));
                    } catch (IOException e) {
                        Toast.makeText(KanjiCheckActivity.this, "Error finding glyph for " + Kanji.JOUYOU_G1.charAt(position), Toast.LENGTH_SHORT).show();
                    }
                }

                if(cv != null) {
                    KanjiViewHolder kh = (KanjiViewHolder) holder;
                    kh.curve.setDrawing(cv, AnimatedCurveView.DrawTime.STATIC, Criticism.SKIP_LIST);
                    kh.points.setDrawing(cv.toDrawing(), AnimatedCurveView.DrawTime.STATIC, Criticism.SKIP_LIST);
                    kh.info.setText("Get sharp points");
                }
            }

            @Override
            public int getItemCount() {
                return Kanji.JOUYOU_G1.length();
            }
        });
    }
}
