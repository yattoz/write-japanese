package dmeeuwis.nakama.views.translations;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.util.List;

import dmeeuwis.Kana;
import dmeeuwis.Translation;
import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.ClueExtractor;
import dmeeuwis.nakama.views.FuriganaSwitcher;
import dmeeuwis.nakama.views.FuriganaTextView;

public class ClueCard extends CardView {

    public interface ClueTypeChangeListener {
        void onClueTypeChane(ClueType c);
    }

    final static private String SHARED_PREFS_CLUE_TYPE_KEY = "clueType";

    final static private boolean DEBUG = BuildConfig.DEBUG && false;

    private View reviewBug, srsBug;

    public enum ClueType { MEANING, READING, TRANSLATION }

    // ui references

    protected ImageView otherClueButton;

    private View meaningsLayout;
    private TextSwitcher instructionsLabel;
    private TextSwitcher target;
    protected ImageView otherMeaningsButton;

    private View readingsLayout;
    private TextSwitcher readingsInstructionLabel;
    private TextSwitcher readingsTarget;
    protected ImageView otherReadingsButton;

    private View translationsLayout;
    private TextSwitcher translationInstructionsLabel;
    private FuriganaSwitcher translationTarget;
    private TextSwitcher translationEnglish;
    protected ImageView otherTranslationsButton;


    // state data
    private Character currentCharacter;
    private int currentMeaningsClueIndex = 0;
    private int currentReadingsClueIndex = 0;
    private int currentTranslationsClueIndex = 0;
    private ClueTypeChangeListener clueTypeChangeListener;

    private ClueExtractor clueExtractor;

    private class SimpleInstructionsLabel implements ViewSwitcher.ViewFactory {
        int maxLines = 1;
        int fontSizeDp = 1;

        public SimpleInstructionsLabel(int fontSizeDp, int maxLines){
            this.fontSizeDp = fontSizeDp;
            this.maxLines = maxLines;
        }

        @Override
        public View makeView() {
            TextView t = new TextView(getContext());
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeDp);
            t.setMaxLines(maxLines);
            if(maxLines == 1) {
                t.setSingleLine();
                t.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                t.setSingleLine(false);
            }
            t.setGravity(Gravity.CENTER);
            return t;
        }
    }


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

        this.otherClueButton = (ImageView) findViewById(R.id.other_clue);

        this.meaningsLayout = findViewById(R.id.clue_meanings_layout);
        this.instructionsLabel = (TextSwitcher)findViewById(R.id.instructionsLabel);
        this.target = (TextSwitcher)findViewById(R.id.target);
        this.reviewBug = findViewById(R.id.clue_card_review_bug);
        this.srsBug = findViewById(R.id.clue_card_srs_bug);

        this.readingsLayout = findViewById(R.id.clue_readings_layout);
        this.readingsInstructionLabel = (TextSwitcher)findViewById(R.id.readingsInstructionsLabel);
        this.readingsTarget = (TextSwitcher)findViewById(R.id.readingsTarget);

        this.translationsLayout = findViewById(R.id.clue_translation_layout);
        this.translationInstructionsLabel = (TextSwitcher) findViewById(R.id.translationInstructionsLabel);
        this.translationTarget = (FuriganaSwitcher) findViewById(R.id.translationTarget);
        this.translationEnglish = (TextSwitcher)findViewById(R.id.translationEnglish);

        translationTarget.setFactory(
                new ViewSwitcher.ViewFactory() {
                     @Override public View makeView() {
                         FuriganaTextView f = new FuriganaTextView(getContext());
                         f.setTextAndReadingSizesDp(32, 16);
                         return f;
                     }
                 }
        );
        translationTarget.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        translationTarget.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));

        target.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        target.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));

        readingsTarget.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        readingsTarget.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));

        instructionsLabel.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        instructionsLabel.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));

        translationInstructionsLabel.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        translationInstructionsLabel.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));

        translationEnglish.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        translationEnglish.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));

        this.readingsInstructionLabel.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        this.readingsInstructionLabel.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));

        target.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                final TextView t = new TextView(getContext());
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
                t.setSingleLine();
                t.setEllipsize(TextUtils.TruncateAt.END);
                t.setGravity(Gravity.CENTER);
                t.setTextColor(Color.BLACK);
                t.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Layout layout = t.getLayout();
                        if (layout != null && layout.getLineCount() > 0) {
                            if (layout.getEllipsisCount(0) > 0) {
                                String[] clues = clueExtractor.meaningsClues(currentCharacter);
                                Toast.makeText(getContext(), clues[currentMeaningsClueIndex], Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
                return t;
            }
        });

        readingsTarget.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                final TextView t = new TextView(getContext());
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
                t.setSingleLine();
                t.setEllipsize(TextUtils.TruncateAt.END);
                t.setGravity(Gravity.CENTER);
                t.setTextColor(Color.BLACK);
                t.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Layout layout = t.getLayout();
                        if (layout != null && layout.getLineCount() > 0) {
                            if (layout.getEllipsisCount(0) > 0) {
                                String[] clues = clueExtractor.readingClues(currentCharacter);
                                Toast.makeText(getContext(), clues[currentMeaningsClueIndex], Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
                return t;
            }
        });

        instructionsLabel.setFactory(new SimpleInstructionsLabel(16, 1));

        String tag = (String)(findViewById(R.id.clue_meanings_layout)).getTag();
        if("land".equals(tag)){
            translationEnglish.setFactory(new SimpleInstructionsLabel(16, 3));
            translationTarget.setTextAndReadingSizesDp(38, 16);
            translationInstructionsLabel.setFactory(new SimpleInstructionsLabel(14, 1));
        } else {
            translationEnglish.setFactory(new SimpleInstructionsLabel(16, 1));
            translationInstructionsLabel.setFactory(new SimpleInstructionsLabel(16, 1));
        }
        readingsInstructionLabel.setFactory(new SimpleInstructionsLabel(16, 1));

        otherMeaningsButton = (ImageView) findViewById(R.id.other_meanings);
        otherMeaningsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("nakama-clue", "Other meanings switch!");
                String[] clues = clueExtractor.meaningsClues(currentCharacter);
                currentMeaningsClueIndex = (currentMeaningsClueIndex + 1) % clues.length;
                target.setText(clues[currentMeaningsClueIndex]);
                instructionsLabel.setText(clueExtractor.meaningsInstructionsText(currentCharacter, currentMeaningsClueIndex));
            }
        });

        otherReadingsButton = (ImageView) findViewById(R.id.other_readings);
        otherReadingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("nakama-clue", "Other readings switch!");
                String[] clues = clueExtractor.readingClues(currentCharacter);
                currentReadingsClueIndex = (currentReadingsClueIndex + 1) % clues.length;
                readingsTarget.setText(clues[currentReadingsClueIndex]);
                readingsInstructionLabel.setText(clueExtractor.readingsInstructionsText(clues, currentReadingsClueIndex));
            }
        });

        otherTranslationsButton = (ImageView) findViewById(R.id.other_translations);
        otherTranslationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("nakama-clue", "Other translations switch! " + currentTranslationsClueIndex);
                currentTranslationsClueIndex++;
                updateToTranslation(currentTranslationsClueIndex);
            }
        });

        otherClueButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ClueType c = null;
                if(meaningsLayout.getVisibility() == View.VISIBLE){
                    Log.i("nakama-clue", "ClueCard other clue click, hiding meanings, showing readings");
                    switchToReadings();
                    c = ClueType.MEANING;
                } else if(readingsLayout.getVisibility() == View.VISIBLE){
                    Log.i("nakama-clue", "ClueCard other clue click, hiding readings, showing translations");
                    switchToTranslations();
                    c = ClueType.READING;
                } else if(translationsLayout.getVisibility() == View.VISIBLE){
                    Log.i("nakama-clue", "ClueCard other clue click, hiding translations, showing meanings");
                    switchToMeanings();
                    c = ClueType.TRANSLATION;
                }
                setCurrentCharacter(clueExtractor, currentCharacter, true);
                if(clueTypeChangeListener != null){
                    clueTypeChangeListener.onClueTypeChane(c);
                }
            }
        });
    }

    public void setClueType(ClueType type){
        if(type == ClueType.MEANING){
            switchToMeanings();
        } else if(type == ClueType.READING){
            switchToReadings();
        } else  if(type == ClueType.TRANSLATION){
            switchToTranslations();
        } else {
            throw new IllegalArgumentException("Unknown ClueType: " + type);
        }
    }

    private void switchToTranslations(){
        readingsLayout.setVisibility(View.GONE);
        meaningsLayout.setVisibility(View.GONE);
        translationsLayout.setVisibility(View.VISIBLE);
    }

    private void switchToMeanings(){
        readingsLayout.setVisibility(View.GONE);
        translationsLayout.setVisibility(View.GONE);
        meaningsLayout.setVisibility(View.VISIBLE);
    }

    private void switchToReadings(){
        meaningsLayout.setVisibility(View.GONE);
        translationsLayout.setVisibility(View.GONE);
        readingsLayout.setVisibility(View.VISIBLE);
    }

    public void onResume(Context ctx){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String typeStr = prefs.getString(SHARED_PREFS_CLUE_TYPE_KEY, ClueType.MEANING.name());
        ClueType type = ClueType.valueOf(typeStr);

        if(type == ClueType.TRANSLATION && translationsLayout.getVisibility() != View.VISIBLE){
            translationsLayout.setVisibility(View.VISIBLE);
            readingsLayout.setVisibility(View.GONE);
            meaningsLayout.setVisibility(View.GONE);
        } else if(type == ClueType.READING && readingsLayout.getVisibility() != View.VISIBLE){
            translationsLayout.setVisibility(View.GONE);
            readingsLayout.setVisibility(View.VISIBLE);
            meaningsLayout.setVisibility(View.GONE);
        } else if(type == ClueType.MEANING && meaningsLayout.getVisibility() != View.VISIBLE){
            translationsLayout.setVisibility(View.GONE);
            readingsLayout.setVisibility(View.GONE);
            meaningsLayout.setVisibility(View.VISIBLE);
        }
        setCurrentCharacter(clueExtractor, currentCharacter, true);
    }

    public void setCurrentCharacter(ClueExtractor clueExtractor, Character currentCharacter, boolean immediate) {
        if(this.currentCharacter != null && this.currentCharacter.equals(currentCharacter)){
            return;
        }

        this.currentCharacter = currentCharacter;
        this.clueExtractor = clueExtractor;

        currentMeaningsClueIndex = 0;
        currentReadingsClueIndex = 0;
        currentTranslationsClueIndex = 0;


        // =============== meanings =====================
        String[] meaningClues = clueExtractor.meaningsClues(currentCharacter);
        if(DEBUG) Log.d("nakama-clue", "Setting meanings to " + TextUtils.join(", ", meaningClues));
        if (meaningsLayout.getVisibility() == View.VISIBLE && meaningClues.length > 1) {
            otherMeaningsButton.setVisibility(View.VISIBLE);
        } else {
            otherMeaningsButton.setVisibility(View.GONE);
        }
        setTextImmediate(target, immediate, meaningClues[currentMeaningsClueIndex]);

        setTextImmediate(instructionsLabel, immediate, clueExtractor.meaningsInstructionsText(currentCharacter, currentMeaningsClueIndex));

        if(Kana.isKana(currentCharacter)){
            meaningsLayout.setVisibility(View.VISIBLE);
            readingsLayout.setVisibility(View.GONE);
            translationsLayout.setVisibility(View.GONE);
            otherClueButton.setVisibility(View.GONE);
            otherReadingsButton.setVisibility(View.GONE);
            otherMeaningsButton.setVisibility(View.GONE);
            otherTranslationsButton.setVisibility(View.GONE);
            return;
        }

        otherClueButton.setVisibility(View.VISIBLE);

        // =============== readings =====================
        String[] readingsClues = clueExtractor.readingClues(currentCharacter);
        if(DEBUG)Log.d("nakama-clue", "Setting readings to " + TextUtils.join(", ", readingsClues));
        if(readingsLayout.getVisibility() == View.VISIBLE && readingsClues.length > 1){
            otherReadingsButton.setVisibility(View.VISIBLE);
        } else {
            otherReadingsButton.setVisibility(View.GONE);
        }

        setTextImmediate(readingsTarget, immediate, readingsClues[currentReadingsClueIndex]);
        setTextImmediate(readingsInstructionLabel, immediate, clueExtractor.readingsInstructionsText(readingsClues, currentMeaningsClueIndex).toString());

        // =============== translations =====================
        updateToTranslation(currentTranslationsClueIndex, immediate);
    }

    private void updateToTranslation(int i){
        updateToTranslation(i, false);
    }

    private void setTextImmediate(TextSwitcher t, boolean immediate, String text){
        if(immediate){
            t.setCurrentText(text);
        } else {
            t.setText(text);
        }
    }

    private void updateToTranslation(int i, boolean immediate){
        Translation t = clueExtractor.translationsClue(currentCharacter, i);
        if(t != null){
            if(DEBUG) Log.d("nakama-clue", "Updating " + i + "-th translation to: " + t.toKanjiString());
            if(immediate) {
                translationTarget.setCurrentTranslationQuiz(t, currentCharacter, clueExtractor.getDictionarySet().kanjiFinder());
            } else {
                translationTarget.setTranslationQuiz(t, currentCharacter, clueExtractor.getDictionarySet().kanjiFinder());
            }

            List<String> englishTrans = t.allGlosses();
            List<String> subset = englishTrans.subList(0, Math.min(englishTrans.size(), 3));
            setTextImmediate(translationEnglish, immediate, TextUtils.join("; ", subset));
            setTextImmediate(translationInstructionsLabel, immediate, clueExtractor.translationsInstructionsText(i));
        } else {
            if(DEBUG) Log.d("nakama-clue", "Clearing translation " + i);
            if(i == 0){
                Log.e("nakama-clue", "Error: cannot find a 0th translation for " + currentCharacter);
            } else {
                currentTranslationsClueIndex = 0;
                updateToTranslation(currentTranslationsClueIndex, immediate);
            }
        }

        Translation next = clueExtractor.translationsClue(currentCharacter, i+1);
        boolean nextVisible = (next != null || i != 0) && translationsLayout.getVisibility() == View.VISIBLE;
        if(DEBUG) Log.d("nakama-clue", "Updating to translation " + i + "; next was found " + next + "; setting next button visibility to " + nextVisible);
        otherTranslationsButton.setVisibility(nextVisible ? View.VISIBLE : View.GONE);
    }

    public void saveCurrentClueType(Context ctx){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor ed = prefs.edit();
        String v = meaningsLayout.getVisibility() == View.VISIBLE ? ClueType.MEANING.name() :
                    readingsLayout.getVisibility() == View.VISIBLE ? ClueType.READING.name() :
                            ClueType.TRANSLATION.name();
        ed.putString(SHARED_PREFS_CLUE_TYPE_KEY, v);

        ed.apply();
    }

    public void setReviewBugVisibility(int reviewBugVisibility) {
        if(this.reviewBug != null) {
            this.reviewBug.setVisibility(reviewBugVisibility);
        }
    }

    public void setSRSBugVisibility(int srsBugVisibility) {
        if(this.srsBug != null) {
            this.srsBug.setVisibility(srsBugVisibility);
        }
    }

    public void setClueTypeChangeListener(ClueTypeChangeListener c){
        this.clueTypeChangeListener = c;
    }


}
