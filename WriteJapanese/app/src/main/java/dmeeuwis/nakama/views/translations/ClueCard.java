package dmeeuwis.nakama.views.translations;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.TextSwitcher;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.views.AdvancedFuriganaTextView;

public class ClueCard extends CardView {

    private TextSwitcher instructionLabel;
    private TextSwitcher target;

    private TextSwitcher readingsInstructionLabel;
    private TextSwitcher readingsTarget;

    private TextSwitcher translationInstructionsLabel;
    private AdvancedFuriganaTextView translationTarget;
    private TextSwitcher translationEnglish;


    public ClueCard(Context context) {
        super(context);
        init();
    }

    public ClueCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClueCard(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.clue_card_layout, this);

        this.instructionLabel = (TextSwitcher)findViewById(R.id.instructionsLabel);
        this.target = (TextSwitcher)findViewById(R.id.target;
        
    }
}
