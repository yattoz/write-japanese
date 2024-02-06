package dmeeuwis.kanjimaster.ui.views.translations;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import dmeeuwis.kanjimaster.R;

class SrsNextPracticeViewHolder extends RecyclerView.ViewHolder {

    final TextView nextSrsTextView;

    public SrsNextPracticeViewHolder(View view) {
        super(view);
        this.nextSrsTextView = (TextView)view.findViewById(R.id.srs_next_practice_row_text);
    }
}
