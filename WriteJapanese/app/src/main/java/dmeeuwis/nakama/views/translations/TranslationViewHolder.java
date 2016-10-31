package dmeeuwis.nakama.views.translations;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.Arrays;

import dmeeuwis.Kana;
import dmeeuwis.Kanji;
import dmeeuwis.KanjiElement;
import dmeeuwis.Translation;
import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.views.AdvancedFuriganaTextView;
import uk.co.deanwild.flowtextview.FlowTextView;

import static dmeeuwis.kanjimaster.R.id.kanji;

class TranslationViewHolder extends RecyclerView.ViewHolder {
    private final AdvancedFuriganaTextView furigana;
    private final FlowTextView englishText;
    private final View view;
    private View expandButton, divisor1, divisor2, divisor3, divisor4, headerSmall, headerBig;
    private final float engTextSize;
    private Translation t;

    private KanjiFinder kanjiFinder;

    int assignedCharacters = 0;

    TranslationViewHolder(View view, float engTextSize) {
        super(view);
        this.view = view;
        this.engTextSize = engTextSize;
        this.furigana = (AdvancedFuriganaTextView) view.findViewById(kanji);
        this.englishText = (FlowTextView) view.findViewById(R.id.english);
        this.headerSmall = view.findViewById(R.id.translation_collapsed_header);
        this.headerBig = view.findViewById(R.id.translation_first_header);

        this.expandButton = view.findViewById(R.id.translation_card_expand);
        View.OnClickListener touch = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                expand();
            }
        };
        view.findViewById(R.id.translation_layout).setOnClickListener(touch);
        view.findViewById(R.id.englishMask).setOnClickListener(touch);
        this.expandButton.setOnClickListener(touch);
        this.furigana.setOnClickListener(touch);
        this.englishText.setOnClickListener(touch);
    }

    void bind(Translation t, KanjiFinder kanjiFinder, int translationIndex) {
        this.kanjiFinder = kanjiFinder;
        this.t = t;

        englishText.setTextSize(engTextSize);
        englishText.setText(t.toEnglishString());
        furigana.setTranslation(t, kanjiFinder);

        expandButton.setVisibility(View.VISIBLE);
        View ex = view.findViewById(R.id.translation_expansion);
        if(ex != null){
            ((RelativeLayout)view.findViewById(R.id.translation_layout)).removeView(ex);
        }

        if(translationIndex == 0){
            headerBig.setVisibility(View.VISIBLE);
            headerSmall.setVisibility(View.GONE);
        } else {
            headerBig.setVisibility(View.GONE);
            headerSmall.setVisibility(View.VISIBLE);
        }

        KanjiElement kanjiObj = t.getFirstKanjiElement();
        if (kanjiObj == null) {
            expandButton.setVisibility(View.GONE);
        }
    }

    private void expand(){
        View expansion = LayoutInflater.from(view.getContext()).inflate(R.layout.translation_expansion, null, false);
        RelativeLayout parent = (RelativeLayout) view.findViewById(R.id.translation_layout);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.BELOW, R.id.translation_frame);
        parent.addView(expansion, params);

        TextView character_1, character_1_meanings, character_1_other_meanings, character_1_yomi, character_1_commoness;
        TextView character_2, character_2_meanings, character_2_other_meanings, character_2_yomi, character_2_commoness;
        TextView character_3, character_3_meanings, character_3_other_meanings, character_3_yomi, character_3_commoness;
        TextView character_4, character_4_meanings, character_4_other_meanings, character_4_yomi, character_4_commoness;


        character_1 = (TextView) expansion.findViewById(R.id.kanji_character_1);
        character_1_meanings = (TextView) expansion.findViewById(R.id.kanji_meanings_1);
        character_1_commoness = (TextView) expansion.findViewById(R.id.kanji_commoness_1);
        character_1_other_meanings = (TextView) expansion.findViewById(R.id.kanji_other_meanings_1);
        character_1_yomi = (TextView) expansion.findViewById(R.id.kanji_yomi_1);

        character_2 = (TextView) expansion.findViewById(R.id.kanji_character_2);
        character_2_meanings = (TextView) expansion.findViewById(R.id.kanji_meanings_2);
        character_2_other_meanings = (TextView) expansion.findViewById(R.id.kanji_other_meanings_2);
        character_2_commoness = (TextView) expansion.findViewById(R.id.kanji_commoness_2);
        character_2_yomi = (TextView) expansion.findViewById(R.id.kanji_yomi_2);

        character_3 = (TextView) expansion.findViewById(R.id.kanji_character_3);
        character_3_meanings = (TextView) expansion.findViewById(R.id.kanji_meanings_3);
        character_3_other_meanings = (TextView) expansion.findViewById(R.id.kanji_other_meanings_3);
        character_3_commoness = (TextView) expansion.findViewById(R.id.kanji_commoness_3);
        character_3_yomi = (TextView) expansion.findViewById(R.id.kanji_yomi_3);

        character_4 = (TextView) expansion.findViewById(R.id.kanji_character_4);
        character_4_meanings = (TextView) expansion.findViewById(R.id.kanji_meanings_4);
        character_4_other_meanings = (TextView) expansion.findViewById(R.id.kanji_other_meanings_4);
        character_4_commoness = (TextView) expansion.findViewById(R.id.kanji_commoness_4);
        character_4_yomi = (TextView) expansion.findViewById(R.id.kanji_yomi_4);

        TextView[][] textViews = new TextView[][]{
                new TextView[]{character_1, character_1_meanings, character_1_other_meanings, character_1_commoness, character_1_yomi},
                new TextView[]{character_2, character_2_meanings, character_2_other_meanings, character_2_commoness, character_2_yomi},
                new TextView[]{character_3, character_3_meanings, character_3_other_meanings, character_3_commoness, character_3_yomi},
                new TextView[]{character_4, character_4_meanings, character_4_other_meanings, character_4_commoness, character_4_yomi}};

        divisor1 = expansion.findViewById(R.id.translation_card_divisor_1);
        divisor2 = expansion.findViewById(R.id.translation_card_divisor_2);
        divisor3 = expansion.findViewById(R.id.translation_card_divisor_3);
        divisor4 = expansion.findViewById(R.id.translation_card_divisor_4);

        divisor1.setVisibility(View.GONE);
        divisor2.setVisibility(View.GONE);
        divisor3.setVisibility(View.GONE);
        divisor4.setVisibility(View.GONE);

        assignedCharacters = 0;
        for (int i = 0; i < textViews.length; i++) {
            TextView[] tset = textViews[i];

            tset[0].setVisibility(View.GONE);
            tset[1].setVisibility(View.GONE);
            tset[2].setVisibility(View.GONE);
            tset[3].setVisibility(View.GONE);
            tset[4].setVisibility(View.GONE);
        }

        KanjiElement kanjiObj = t.getFirstKanjiElement();
        if(kanjiObj == null){
            return;
        }
        String kanji = kanjiObj.kanji;

        assignedCharacters = 0;
        for (int i = 0, stringIndex = 0; i < textViews.length; i++) {
            TextView[] tset = textViews[i];

            try {
                Kanji k = null;
                while (stringIndex < kanji.length()) {
                    char c = kanji.charAt(stringIndex);
                    if (Kana.isKanji(c)) {
                        k = kanjiFinder.find(c);
                        break;
                    }
                    stringIndex++;
                }
                if (k == null) {
                    continue;
                }
                String kanjiChar = String.valueOf(k.kanji);

                String commonnessPrefix = "";
                if (Kanji.JOUYOU_G1.contains(kanjiChar)) {
                    commonnessPrefix = "Joyou Level 1; ";

                } else if (Kanji.JOUYOU_G2.contains(kanjiChar)) {
                    commonnessPrefix = "Joyou Level 2; ";

                } else if (Kanji.JOUYOU_G3.contains(kanjiChar)) {
                    commonnessPrefix = "Joyou Level 3; ";

                } else if (Kanji.JOUYOU_G4.contains(kanjiChar)) {
                    commonnessPrefix = "Joyou Level 4; ";

                } else if (Kanji.JOUYOU_G5.contains(kanjiChar)) {
                    commonnessPrefix = "Joyou Level 5; ";

                } else if (Kanji.JOUYOU_G6.contains(kanjiChar)) {
                    commonnessPrefix = "Joyou Level 6; ";
                }

                tset[0].setText(kanjiChar);

                tset[1].setText(k.meanings[0]);
                if (k.meanings.length > 1) {
                    tset[2].setText("Other meanings: " +
                            TextUtils.join(", ", Arrays.copyOfRange(k.meanings, 1, k.meanings.length)));
                } else {
                    tset[2].setVisibility(View.GONE);
                }

                if (k.freq != null || commonnessPrefix != null) {
                    tset[3].setText(commonnessPrefix + (k.freq == null ? "" : "Frequency: " + k.freq));
                } else {
                    tset[3].setVisibility(View.GONE);
                }

                tset[4].setText(
                        "Kunyomi: " + TextUtils.join(", ", k.onyomi) +
                                "\nOnyomi:  " + TextUtils.join(", ", k.kunyomi));
                assignedCharacters += 1;

                stringIndex++;

            } catch (IOException e) {
                Log.e("nakama", "Error finding first kanji for " + kanji);
            }
        }

        for (int i = 0; i < textViews.length && i < assignedCharacters; i++) {
            TextView[] tvs = textViews[i];
            for (TextView t : tvs) {
                t.setVisibility(View.VISIBLE);
            }
        }

        if (assignedCharacters >= 1) {
            divisor1.setVisibility(View.VISIBLE);
        }
        if (assignedCharacters >= 2) {
            divisor2.setVisibility(View.VISIBLE);
        }
        if (assignedCharacters >= 3) {
            divisor3.setVisibility(View.VISIBLE);
        }
        if (assignedCharacters >= 4) {
            divisor4.setVisibility(View.VISIBLE);
        }

        expandButton.setVisibility(View.GONE);
    }
}
