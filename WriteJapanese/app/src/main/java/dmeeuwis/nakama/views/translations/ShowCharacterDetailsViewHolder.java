package dmeeuwis.nakama.views.translations;

import android.support.v7.widget.RecyclerView;
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
