package dmeeuwis.nakama.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ViewSwitcher;

import dmeeuwis.Translation;
import dmeeuwis.indexer.KanjiFinder;

public class FuriganaSwitcher extends ViewSwitcher {
    public FuriganaSwitcher(Context context) {
        super(context);
    }

    public FuriganaSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setTranslationQuiz(Translation t, Character targetChar, KanjiFinder finder){
        FuriganaTextView next = (FuriganaTextView) getNextView();
        next.setTranslationQuiz(t, targetChar, finder);
        showNext();
    }

    public void setTextAndReadingSizesDp(int mainSize, int furiganaSize){
        ((FuriganaTextView)getCurrentView()).setTextAndReadingSizesDp(mainSize, furiganaSize);
        ((FuriganaTextView)getNextView()).setTextAndReadingSizesDp(mainSize, furiganaSize);
    }

    public void setCurrentTranslationQuiz(Translation t, Character targetChar, KanjiFinder finder) {
        FuriganaTextView next = (FuriganaTextView) getCurrentView();
        next.setTranslationQuiz(t, targetChar, finder);
    }
}
