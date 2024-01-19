package dmeeuwis.kanjimaster.ui.views.translations;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import dmeeuwis.kanjimaster.R;

class ShowCharacterDetailsViewHolder extends RecyclerView.ViewHolder {

    final TextView onyomi, kunyomi;

    ShowCharacterDetailsViewHolder(View view) {
        super(view);

        this.onyomi = (TextView) view.findViewById(R.id.kanji_onyomi);
        this.kunyomi = (TextView) view.findViewById(R.id.kanji_kunyomi);
    }
}
