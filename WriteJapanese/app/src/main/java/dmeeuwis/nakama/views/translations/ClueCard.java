package dmeeuwis.nakama.views.translations;

import android.content.Context;
import android.graphics.Color;
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

import dmeeuwis.Kana;
import dmeeuwis.Translation;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.ClueExtractor;
import dmeeuwis.nakama.views.AdvancedFuriganaTextView;

public class ClueCard extends CardView {

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
    private TextView translationInstructionsLabel;
    private AdvancedFuriganaTextView translationTarget;
    private TextSwitcher translationEnglish;
    protected ImageView otherTranslationsButton;


    // state data
    private CharacterStudySet currentCharacterSet;
    private int currentMeaningsClueIndex = 0;
    private int currentReadingsClueIndex = 0;
    private int currentTranslationsClueIndex = 0;

    private ClueExtractor clueExtractor;

    private class SimpleInstructionsLabel implements ViewSwitcher.ViewFactory {
        @Override
        public View makeView() {
            TextView t = new TextView(getContext());
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            t.setSingleLine();
            t.setMaxLines(1);
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

        this.readingsLayout = findViewById(R.id.clue_readings_layout);
        this.readingsInstructionLabel = (TextSwitcher)findViewById(R.id.readingsInstructionsLabel);
        this.readingsTarget = (TextSwitcher)findViewById(R.id.readingsTarget);

        this.translationsLayout = findViewById(R.id.clue_translation_layout);
        this.translationInstructionsLabel = (TextView) findViewById(R.id.translationInstructionsLabel);
        this.translationTarget = (AdvancedFuriganaTextView) findViewById(R.id.translationTarget);
        this.translationEnglish = (TextSwitcher)findViewById(R.id.translationEnglish);

        target.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        target.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));

        readingsTarget.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        readingsTarget.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));

        instructionsLabel.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        instructionsLabel.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));

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
                                String[] clues = currentCharacterSet.currentCharacterClues();
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
                                String[] clues = currentCharacterSet.currentCharacterClues();
                                Toast.makeText(getContext(), clues[currentMeaningsClueIndex], Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
                return t;
            }
        });

        instructionsLabel.setFactory(new SimpleInstructionsLabel());
        translationEnglish.setFactory(new SimpleInstructionsLabel());
        readingsInstructionLabel.setFactory(new SimpleInstructionsLabel());

        otherMeaningsButton = (ImageView) findViewById(R.id.other_meanings);
        otherMeaningsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("nakama-clue", "Other meanings switch!");
                String[] clues = clueExtractor.meaningsClues(currentCharacterSet.currentCharacter());
                currentMeaningsClueIndex = (currentMeaningsClueIndex + 1) % clues.length;
                target.setText(clues[currentMeaningsClueIndex]);
                instructionsLabel.setText(currentCharacterSet.currentCharacterCluesText(currentMeaningsClueIndex));
            }
        });

        otherReadingsButton = (ImageView) findViewById(R.id.other_readings);
        otherReadingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("nakama-clue", "Other readings switch!");
                String[] clues = clueExtractor.readingClues(currentCharacterSet.currentCharacter());
                currentMeaningsClueIndex = (currentMeaningsClueIndex + 1) % clues.length;
                readingsTarget.setText(clues[currentMeaningsClueIndex]);
                readingsInstructionLabel.setText(currentCharacterSet.currentCharacterCluesText(currentMeaningsClueIndex));
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
                if(meaningsLayout.getVisibility() == View.VISIBLE){
                    Log.i("nakama-clue", "ClueCard other clue click, hiding meanings, showing readings");
                    meaningsLayout.setVisibility(View.GONE);
                    translationsLayout.setVisibility(View.GONE);
                    otherMeaningsButton.setVisibility(View.GONE);
                    otherTranslationsButton.setVisibility(View.GONE);

                    readingsLayout.setVisibility(View.VISIBLE);
                    otherReadingsButton.setVisibility(View.VISIBLE);
                } else if(readingsLayout.getVisibility() == View.VISIBLE){
                    Log.i("nakama-clue", "ClueCard other clue click, hiding readings, showing translations");
                    readingsLayout.setVisibility(View.GONE);
                    meaningsLayout.setVisibility(View.GONE);
                    otherMeaningsButton.setVisibility(View.GONE);
                    otherReadingsButton.setVisibility(View.GONE);

                    translationsLayout.setVisibility(View.VISIBLE);
                    otherTranslationsButton.setVisibility(View.VISIBLE);
                } else if(translationsLayout.getVisibility() == View.VISIBLE){
                    Log.i("nakama-clue", "ClueCard other clue click, hiding translations, showing meanings");
                    readingsLayout.setVisibility(View.GONE);
                    translationsLayout.setVisibility(View.GONE);
                    otherTranslationsButton.setVisibility(View.GONE);

                    meaningsLayout.setVisibility(View.VISIBLE);
                    otherMeaningsButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void setCurrentCharacter(ClueExtractor clueExtractor, CharacterStudySet currentCharacter) {
        this.currentCharacterSet = currentCharacter;
        this.clueExtractor = clueExtractor;

        currentMeaningsClueIndex = 0;
        currentReadingsClueIndex = 0;
        currentTranslationsClueIndex = 0;

        if(Kana.isKana(currentCharacter.currentCharacter())){
            // setup kana

            return;
        }

        // =============== meanings =====================
        String[] meaningClues = clueExtractor.meaningsClues(currentCharacter.currentCharacter());
        Log.i("nakama-clue", "Setting meanings to " + TextUtils.join(", ", meaningClues));
        if(meaningsLayout.getVisibility() == View.VISIBLE && meaningClues.length > 1){
            otherMeaningsButton.setVisibility(View.VISIBLE);
        } else {
            otherMeaningsButton.setVisibility(View.GONE);
        }
        target.setCurrentText(meaningClues[currentMeaningsClueIndex]);
        instructionsLabel.setCurrentText(currentMeaningsClueIndex == 0 ?
                    "Character means " :
                    "or mean");

        // =============== readings =====================
        String[] readingsClues = clueExtractor.readingClues(currentCharacter.currentCharacter());
        Log.i("nakama-clue", "Setting readings to " + TextUtils.join(", ", readingsClues));
        if(readingsLayout.getVisibility() == View.VISIBLE && readingsClues.length > 1){
            otherReadingsButton.setVisibility(View.VISIBLE);
        } else {
            otherReadingsButton.setVisibility(View.GONE);
        }
        readingsTarget.setCurrentText(readingsClues[currentReadingsClueIndex]);
        readingsInstructionLabel.setCurrentText(currentReadingsClueIndex == 0 ?
                    "Character can be read" :
                    "or read as");

        // =============== translations =====================
        updateToTranslation(currentTranslationsClueIndex);
    }

    private void updateToTranslation(int i){
        Translation t = clueExtractor.translationsClue(this.currentCharacterSet.currentCharacter(), i);
        if(t != null){
            Log.i("nakama-clue", "Updating " + i + "-th translation to: " + t.toKanjiString());
            translationTarget.setTranslationQuiz(t, this.currentCharacterSet.currentCharacter(), clueExtractor.getDictionarySet().kanjiFinder());
            translationTarget.setTextAndReadingSizesDp(32, 16);
            translationEnglish.setText(t.toEnglishString());

            if(i == 0) {
                translationInstructionsLabel.setText("Write the kanji used in");
            } else {
                translationInstructionsLabel.setText("also used in");
            }
        } else {
            Log.i("nakama-clue", "Clearing translation " + i);
            if(i == 0){
                Log.e("nakama-clue", "Error: cannot find a 0th translation for " + currentCharacterSet.currentCharacter());
                return;
            }
            currentTranslationsClueIndex = 0;
            updateToTranslation(currentTranslationsClueIndex);
        }

        Translation next = clueExtractor.translationsClue(this.currentCharacterSet.currentCharacter(), i+1);
        if(next != null){
            Log.i("nakama-clue", "Next translation " + i + " " + next.toKanjiString());
            otherTranslationsButton.setVisibility(View.VISIBLE);
        } else {
            if(i == 0) {
                Log.i("nakama-clue", "No " + (i + 1) + " translation found, hiding next button");
                otherTranslationsButton.setVisibility(View.GONE);
            } else {
                // no next found, will loop back
            }
        }
    }

}
