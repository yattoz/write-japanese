package dmeeuwis.nakama.views.translations;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.views.AnimatedCurveView;

class ShowStrokesViewHolder extends RecyclerView.ViewHolder {
    public final AnimatedCurveView known, drawn;

    ShowStrokesViewHolder(View view) {
        super(view);

        this.known = (AnimatedCurveView) view.findViewById(R.id.adapterCorrectKnownView);
        this.drawn = (AnimatedCurveView) view.findViewById(R.id.adapterCorrectDrawnView);
    }
}
