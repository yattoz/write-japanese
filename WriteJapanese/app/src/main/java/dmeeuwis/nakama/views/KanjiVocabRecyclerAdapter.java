package dmeeuwis.nakama.views;

import android.app.Activity;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dmeeuwis.Kana;
import dmeeuwis.Kanji;
import dmeeuwis.Translation;
import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.kanjimaster.R;
import uk.co.deanwild.flowtextview.FlowTextView;

public class KanjiVocabRecyclerAdapter extends RecyclerView.Adapter<KanjiVocabRecyclerAdapter.ViewHolder> {

    private final float engTextSize;

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final AdvancedFuriganaTextView furigana;
        private final FlowTextView englishText;
        private final TextView character_1, character_1_meanings, character_1_other_meanings, character_1_yomi, character_1_commoness;
        private final TextView character_2, character_2_meanings, character_2_other_meanings, character_2_yomi, character_2_commoness;

        private final View expandButton;

        private final TextView[][] textViews;

        ViewHolder(View view){
            super(view);
            this.furigana = (AdvancedFuriganaTextView)view.findViewById(R.id.kanji);
            this.englishText = (FlowTextView)view.findViewById(R.id.english);

            this.character_1 = (TextView) view.findViewById(R.id.kanji_character_1);
            this.character_1_meanings = (TextView) view.findViewById(R.id.kanji_meanings_1);
            this.character_1_commoness = (TextView) view.findViewById(R.id.kanji_commoness_1);
            this.character_1_other_meanings = (TextView) view.findViewById(R.id.kanji_other_meanings_1);
            this.character_1_yomi = (TextView) view.findViewById(R.id.kanji_yomi_1);

            this.character_2 = (TextView) view.findViewById(R.id.kanji_character_2);
            this.character_2_meanings = (TextView) view.findViewById(R.id.kanji_meanings_2);
            this.character_2_other_meanings = (TextView) view.findViewById(R.id.kanji_other_meanings_2);
            this.character_2_commoness = (TextView) view.findViewById(R.id.kanji_commoness_2);
            this.character_2_yomi = (TextView) view.findViewById(R.id.kanji_yomi_2);

            this.textViews = new TextView[][] {
                    new TextView[] { character_1, character_1_meanings, character_1_other_meanings, character_1_commoness, character_1_yomi },
                    new TextView[] { character_2, character_2_meanings, character_2_other_meanings, character_2_commoness, character_2_yomi } };

            this.expandButton = view.findViewById(R.id.translation_card_expand);
            this.expandButton.setOnClickListener(new View.OnClickListener() {
                     @Override
                     public void onClick(View view) {
                         for(TextView[] tvs: textViews){
                             for(TextView t: tvs){
                                 t.setVisibility(View.VISIBLE);
                             }
                         }
                         expandButton.setVisibility(View.GONE);
                     }
                 }
            );

        }
    }

    private final Activity context;
    private final KanjiFinder kanjiFinder;
    private final List<Translation> translations;

    public KanjiVocabRecyclerAdapter(Activity context, KanjiFinder kanjiFinder) {
        super();
        this.context = context;
        this.kanjiFinder = kanjiFinder;
        this.translations = new ArrayList<>();

        Resources r = context.getResources();
        this.engTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
    }

    @Override
    public KanjiVocabRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = this.context.getLayoutInflater();
        View view = inflater.inflate(R.layout.translation_slide, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Translation t = this.translations.get(position);
        holder.englishText.setTextSize(engTextSize);
        holder.englishText.setText(t.toEnglishString());
        holder.furigana.setTranslation(t, this.kanjiFinder);

        holder.expandButton.setVisibility(View.VISIBLE);

        String kanji = t.getFirstKanjiElement().kanji;
        for(int i = 0, stringIndex = 0; i < holder.textViews.length; i++){
            TextView[] tset = holder.textViews[i];

            tset[0].setVisibility(View.GONE);
            tset[1].setVisibility(View.GONE);
            tset[2].setVisibility(View.GONE);
            tset[3].setVisibility(View.GONE);
            tset[4].setVisibility(View.GONE);

            try {
                Kanji k = null;
                while(stringIndex < kanji.length()) {
                    char c = kanji.charAt(stringIndex);
                    if (Kana.isKanji(c)) {
                        k = this.kanjiFinder.find(c);
                        break;
                    }
                    stringIndex++;
                }
                if(k == null){
                    continue;
                }

                tset[0].setText(String.valueOf(k.kanji));
                tset[1].setText(k.meanings[0]);
                tset[2].setText("Other meanings: " +
                        TextUtils.join(", ", Arrays.copyOfRange(k.meanings, 1, k.meanings.length)));
                tset[3].setText("Frequency: " + k.freq);
                tset[4].setText(
                        "Kunyomi: " + TextUtils.join(", ", k.onyomi) +
                        "\nOnyomi:  "  + TextUtils.join(", ", k.kunyomi));

                stringIndex++;

            } catch (IOException e) {
                Log.e("nakama", "Error finding first kanji for " + kanji);
            }
        }
    }

    @Override
    public int getItemCount() {
        return this.translations.size();
    }

    public void add(Translation t){
        this.translations.add(t);
        this.notifyItemChanged(this.translations.size()-1);
    }

    public void clear(){
        this.translations.clear();
        this.notifyDataSetChanged();
    }
}
