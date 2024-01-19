package dmeeuwis.kanjimaster.ui.views.translations;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.ui.views.AnimatedCurveView;

public class ShowCharacterInfoViewHolder extends RecyclerView.ViewHolder {
    public final AnimatedCurveView anim;
    public final TextView bigKanji;

    ShowCharacterInfoViewHolder(View view) {
        super(view);

        this.bigKanji = (TextView) view.findViewById(R.id.bigkanji);
        this.anim = (AnimatedCurveView) view.findViewById(R.id.kanji_animation);
    }
}
