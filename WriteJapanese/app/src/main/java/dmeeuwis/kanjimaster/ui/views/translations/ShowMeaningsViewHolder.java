package dmeeuwis.kanjimaster.ui.views.translations;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import dmeeuwis.kanjimaster.R;

public class ShowMeaningsViewHolder  extends RecyclerView.ViewHolder {

    public final TextView meanings;

    public ShowMeaningsViewHolder(View itemView) {
        super(itemView);
        this.meanings = (TextView)itemView.findViewById(R.id.translation_meanings);
    }
}
