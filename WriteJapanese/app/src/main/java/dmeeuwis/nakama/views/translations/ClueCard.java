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

import dmeeuwis.indexer.KanjiFinder;
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
    private TextSwitcher translationInstructionsLabel;
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
        this.translationInstructionsLabel = (TextSwitcher)findViewById(R.id.translationInstructionsLabel);
        this.translationTarget = (AdvancedFuriganaTextView) findViewById(R.id.translationTarget);
        this.translationEnglish = (TextSwitcher)findViewById(R.id.translationEnglish);

        target.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        target.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));

        instructionsLabel.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left));
        instructionsLabel.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right));

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
        readingsInstructionLabel.setFactory(new SimpleInstructionsLabel());

        otherMeaningsButton = (ImageView) findViewById(R.id.other_meanings);
        otherMeaningsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("nakama", "Other meanings switch!");
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
                Log.i("nakama", "Other readings switch!");
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
                Log.i("nakama", "Other translations switch!");
                /*Translation t = clueExtractor.TranslationsClue(currentCharacterSet.currentCharacter(), currentTranslationsClueIndex+1);
                currentMeaningsClueIndex = (currentMeaningsClueIndex + 1) % clues.length;
                target.setText(clues[currentMeaningsClueIndex]);
                instructionsLabel.setText(currentCharacterSet.currentCharacterCluesText(currentMeaningsClueIndex));
                */
            }
        });

        otherClueButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(meaningsLayout.getVisibility() == View.VISIBLE){
                    Log.i("nakama", "ClueCard other clue click, hiding meanings, showing readings");
                    meaningsLayout.setVisibility(View.GONE);
                    translationsLayout.setVisibility(View.GONE);
                    otherMeaningsButton.setVisibility(View.GONE);
                    otherTranslationsButton.setVisibility(View.GONE);

                    readingsLayout.setVisibility(View.VISIBLE);
                    otherReadingsButton.setVisibility(View.VISIBLE);
                } else if(readingsLayout.getVisibility() == View.VISIBLE){
                    Log.i("nakama", "ClueCard other clue click, hiding readings, showing translations");
                    readingsLayout.setVisibility(View.GONE);
                    meaningsLayout.setVisibility(View.GONE);
                    otherMeaningsButton.setVisibility(View.GONE);
                    otherReadingsButton.setVisibility(View.GONE);

                    translationsLayout.setVisibility(View.VISIBLE);
                    otherTranslationsButton.setVisibility(View.VISIBLE);
                } else if(translationsLayout.getVisibility() == View.VISIBLE){
                    Log.i("nakama", "ClueCard other clue click, hiding translations, showing meanings");
                    readingsLayout.setVisibility(View.GONE);
                    translationsLayout.setVisibility(View.GONE);
                    otherTranslationsButton.setVisibility(View.GONE);

                    meaningsLayout.setVisibility(View.VISIBLE);
                    otherMeaningsButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void setCurrentCharacter(KanjiFinder kanjiFinder, CharacterStudySet currentCharacter) {
        this.currentCharacterSet = currentCharacter;

        clueExtractor = new ClueExtractor(kanjiFinder);

        // =============== meanings =====================
        String[] meaningClues = clueExtractor.meaningsClues(currentCharacter.currentCharacter());
        Log.i("nakama", "Setting meanings to " + TextUtils.join(", ", meaningClues));
        if(meaningsLayout.getVisibility() == View.VISIBLE && meaningClues.length > 1){
            otherMeaningsButton.setVisibility(View.VISIBLE);
        } else {
            otherMeaningsButton.setVisibility(View.GONE);
        }
        currentMeaningsClueIndex = 0;
        target.setCurrentText(meaningClues[currentMeaningsClueIndex]);
        instructionsLabel.setCurrentText(currentMeaningsClueIndex == 0 ?
                    "Character means " :
                    "can also mean");

        // =============== readings =====================
        String[] readingsClues = clueExtractor.readingClues(currentCharacter.currentCharacter());
        Log.i("nakama", "Setting readings to " + TextUtils.join(", ", readingsClues));
        if(readingsLayout.getVisibility() == View.VISIBLE && readingsClues.length > 1){
            otherReadingsButton.setVisibility(View.VISIBLE);
        } else {
            otherReadingsButton.setVisibility(View.GONE);
        }
        currentReadingsClueIndex = 0;
        readingsTarget.setCurrentText(readingsClues[currentReadingsClueIndex]);
        readingsInstructionLabel.setCurrentText(currentReadingsClueIndex == 0 ?
                    "Character can be read" :
                    "can also be read as");

        // =============== translations =====================
    }
}
