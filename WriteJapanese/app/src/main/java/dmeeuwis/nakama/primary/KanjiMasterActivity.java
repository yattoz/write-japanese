package dmeeuwis.nakama.primary;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher;

import dmeeuwis.Translation;
import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.nakama.CreditsActivity;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.DrawViewTestActivity;
import dmeeuwis.nakama.KanjiCheckActivity;
import dmeeuwis.nakama.SpenDrawActivity;
import dmeeuwis.nakama.TestDrawActivity;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.CharacterStudySet.LockLevel;
import dmeeuwis.nakama.kanjidraw.DrawingComparator;
import dmeeuwis.nakama.kanjidraw.PointDrawing;
import dmeeuwis.nakama.teaching.TeachingStoryFragment;
import dmeeuwis.nakama.views.KanjiTranslationListAsyncTask;
import dmeeuwis.nakama.views.KanjiVocabRecyclerAdapter;
import dmeeuwis.nakama.LockChecker;
import dmeeuwis.nakama.LockCheckerHolder;
import dmeeuwis.nakama.ProgressActivity;
import dmeeuwis.nakama.views.PurchaseDialog;
import dmeeuwis.nakama.data.StoryDataHelper;
import dmeeuwis.nakama.teaching.TeachingActivity;
import dmeeuwis.nakama.data.AssetFinder;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.kanjidraw.Criticism;
import dmeeuwis.nakama.kanjidraw.CurveDrawing;
import dmeeuwis.nakama.Constants;
import dmeeuwis.nakama.views.Animatable;
import dmeeuwis.nakama.views.AnimatedCurveView;
import dmeeuwis.nakama.views.DrawView;
import dmeeuwis.nakama.views.FloatingActionButton;
import dmeeuwis.nakama.views.SetInfoDialog;
import dmeeuwis.nakama.views.ShareStoriesDialog;
import dmeeuwis.util.Util;

public class KanjiMasterActivity extends ActionBarActivity implements ActionBar.OnNavigationListener, LockCheckerHolder, CharacterSetStatusFragment.OnFragmentInteractionListener {
    public enum State {DRAWING, REVIEWING, CORRECT_ANSWER}
    public enum Frequency {ALWAYS, ONCE_PER_SESSION}

    public static final String CHAR_SET = "currCharSet";
    public static final String CHAR_SET_CHAR = "currCharSetChar";

    protected DictionarySet dictionarySet;
    protected LockChecker lockChecker;

    public CharacterStudySet joyouG1, joyouG2, joyouG3, joyouG4, joyouG5, joyouG6; // , joyouSS;
    public CharacterStudySet hiraganaCharacterSet, katakanaCharacterSet;
    protected CharacterStudySet currentCharacterSet;
    protected StoryDataHelper db;

    protected LinkedHashMap<String, CharacterStudySet> characterSets = new LinkedHashMap<>();
    protected boolean showedEndOfSetDialog = false;
    protected boolean showedStartOfSetDialog = false;
    protected boolean queuedNextCharLoad = false;   // when switching to teaching activity, queue a next char load for onResume
    protected PurchaseDialog pd;
    private int currentCharacterClueIndex = 0;

    // ui references
    protected DrawView drawPad;
    protected AnimatedCurveView correctAnimation;
    protected AnimatedCurveView playbackAnimation;
    protected AnimatedCurveView correctDrawnView;
    protected AnimatedCurveView correctKnownView;
    protected KanjiTranslationListAsyncTask vocabAsync;
    protected RecyclerView correctVocabList;
    protected KanjiVocabRecyclerAdapter correctVocabArrayAdapter;
    protected ViewFlipper flipper;
    protected FlipperAnimationListener flipperAnimationListener;
    protected View maskView;
    protected FloatingActionButton remindStoryButton;
    protected ImageView otherMeaningsButton;
    protected ListView criticism;           // TODO: to RecyclerView
    protected ArrayAdapter<String> criticismArrayAdapter;
    protected ImageView reviewBug;
    protected TextSwitcher instructionsLabel;
    protected TextSwitcher target;
    protected ColorDrawable actionBarBackground;

    protected TextView introCharsetLabel, introCharsetDesc;

    protected String[] currentCharacterSvg;

    protected CharacterSetStatusFragment charSetFrag;

    private static class KanjiMasterUncaughtHandler implements Thread.UncaughtExceptionHandler {
        @Override public void uncaughtException(Thread thread, Throwable ex) {
            Log.e("nakama", "Uncaught exception from thread " + thread, ex);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("nakama", "MainActivity: onCreate starting.");
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new KanjiMasterUncaughtHandler());

        lockChecker = new LockChecker(this,
                new Runnable() {
                    @Override
                    public void run() {
                        Log.i("nakama", "PurchaseDialog: onDismissRunnable from raisePurchaseDialog.");
                        if (lockChecker.getPurchaseStatus() == LockLevel.UNLOCKED) {
                            Log.i("nakama", "PurchaseDialog: onDismissRunnable from raisePurchaseDialog: saw unlocked result, updating menu.");
                            getSupportActionBar().setSelectedNavigationItem(2);
                            supportInvalidateOptionsMenu();
                        }
                    }
                });

        setContentView(R.layout.main);

        this.dictionarySet = DictionarySet.get(this.getApplicationContext());

        Animation outToLeft = AnimationUtils.loadAnimation(this, R.anim.screen_transition_out);

        flipper = (ViewFlipper) findViewById(R.id.viewflipper);
        flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.screen_transition_in));
        flipper.setOutAnimation(outToLeft);
        flipper.setAnimationCacheEnabled(true);

        flipperAnimationListener = new FlipperAnimationListener();
        outToLeft.setAnimationListener(flipperAnimationListener);

        maskView = findViewById(R.id.maskView);

        // Draw Frame init
        drawPad = (DrawView) findViewById(R.id.drawPad);
        drawPad.setBackgroundColor(DrawView.BACKGROUND_COLOR);

        reviewBug = (ImageView) findViewById(R.id.reviewBug);

        target = (TextSwitcher) findViewById(R.id.target);
        target.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        target.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));

        correctKnownView = (AnimatedCurveView) findViewById(R.id.correctKnownView);
        correctDrawnView = (AnimatedCurveView) findViewById(R.id.correctDrawnView);

        instructionsLabel = (TextSwitcher) findViewById(R.id.instructionsLabel);
        instructionsLabel.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        instructionsLabel.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));


        target.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                final TextView t = new TextView(KanjiMasterActivity.this);
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
                                Toast.makeText(KanjiMasterActivity.this, clues[currentCharacterClueIndex], Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
                return t;
            }
        });

        instructionsLabel.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView t = new TextView(KanjiMasterActivity.this);
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                t.setSingleLine();
                t.setMaxLines(1);
                t.setGravity(Gravity.CENTER);
                return t;
            }
        });

        final FloatingActionButton doneButton = (FloatingActionButton) findViewById(R.id.finishedButton);
        doneButton.hideInstantly();
        doneButton.setFloatingActionButtonColor(getResources().getColor(R.color.DarkGreen));
        doneButton.setFloatingActionButtonDrawable(getResources().getDrawable(R.drawable.ic_right_arrow));
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (drawPad.getStrokeCount() == 0) {
                    return;
                }

                final PointDrawing challenger = drawPad.getDrawing();
                final CurveDrawing known = new CurveDrawing(currentCharacterSvg);

                DrawingComparator comparator = new DrawingComparator(currentCharacterSet.currentCharacter(), known, challenger,
                        new AssetFinder(KanjiMasterActivity.this.getAssets()));
                final Criticism critique = comparator.compare();

                currentCharacterSet.markCurrent(critique.pass);

                if (critique.pass) {
                    correctKnownView.setDrawing(known, AnimatedCurveView.DrawTime.STATIC);
                    correctDrawnView.setDrawing(challenger, AnimatedCurveView.DrawTime.STATIC);

                    setUiState(State.CORRECT_ANSWER);

                    if (vocabAsync != null) {
                        vocabAsync.cancel(true);
                        correctVocabArrayAdapter.clear();
                    }
                    Log.i("nakama", "VOCAB: Starting vocab async task.");

                    KanjiTranslationListAsyncTask.AddTranslation adder = new KanjiTranslationListAsyncTask.AddTranslation() {
                        public void add(Translation t) {
                            correctVocabArrayAdapter.add(t);
                        }
                    };
                    vocabAsync = new KanjiTranslationListAsyncTask(adder, dictionarySet, currentCharacterSet.currentCharacter());
                    vocabAsync.execute();


                } else {
                    Log.d("nakama", "Setting up data for incorrect results critique.");
                    correctAnimation.setDrawing(known, AnimatedCurveView.DrawTime.ANIMATED);
                    playbackAnimation.setDrawing(challenger, AnimatedCurveView.DrawTime.ANIMATED);

                    criticismArrayAdapter.clear();
                    for (String c : critique.critiques) {
                        criticismArrayAdapter.add(c);
                    }

                    setUiState(State.REVIEWING);
                }
            }
        });

        db = new StoryDataHelper(getApplicationContext());
        remindStoryButton = (FloatingActionButton) findViewById(R.id.remindStoryButton);
        remindStoryButton.setFloatingActionButtonColor(getResources().getColor(R.color.DarkTurquoise));
        remindStoryButton.setFloatingActionButtonDrawable(getResources().getDrawable(R.drawable.ic_story));
        remindStoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), db.getStory(currentCharacterSet.currentCharacter()), Toast.LENGTH_LONG).show();
            }
        });

        otherMeaningsButton = (ImageView) findViewById(R.id.other_meanings);
        otherMeaningsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] clues = currentCharacterSet.currentCharacterClues();
                currentCharacterClueIndex = (currentCharacterClueIndex + 1) % clues.length;
                target.setText(clues[currentCharacterClueIndex]);
                instructionsLabel.setText(currentCharacterClueIndex == 0 ?
                        "Draw the " + currentCharacterSet.label() + " for" :
                        "which can also mean");

            }
        });

        final FloatingActionButton teachMeButton = (FloatingActionButton) findViewById(R.id.teachButton);
        teachMeButton.setFloatingActionButtonColor(getResources().getColor(R.color.Blue));
        teachMeButton.setFloatingActionButtonDrawable(getResources().getDrawable(R.drawable.ic_learning));
        teachMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                char c = currentCharacterSet.currentCharacter();
                currentCharacterSet.markCurrentAsUnknown();
                goToTeachingActivity(c);
            }
        });

        correctAnimation = (AnimatedCurveView) findViewById(R.id.animatedKnownReplay);
        playbackAnimation = (AnimatedCurveView) findViewById(R.id.animatedDrawnReplay);

        criticism = (ListView) findViewById(R.id.criticism);
        criticismArrayAdapter = new ArrayAdapter<>(this, R.layout.critique_list_item, R.id.critique_label, new ArrayList<String>(0));
        criticism.setAdapter(criticismArrayAdapter);

        View.OnClickListener nextButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                drawPad.clear();
                setUiState(State.DRAWING);
                loadNextCharacter(true);
            }
        };

        final FloatingActionButton next = (FloatingActionButton) findViewById(R.id.nextButton);
        next.setOnClickListener(nextButtonListener);
        next.setFloatingActionButtonColor(getResources().getColor(R.color.DarkGreen));
        next.setFloatingActionButtonDrawable(getResources().getDrawable(R.drawable.ic_right_arrow));

        final FloatingActionButton correctNext = (FloatingActionButton) findViewById(R.id.correctNextButton);
        correctNext.setFloatingActionButtonColor(getResources().getColor(R.color.DarkGreen));
        correctNext.setFloatingActionButtonDrawable(getResources().getDrawable(R.drawable.ic_right_arrow));
        correctNext.setOnClickListener(nextButtonListener);

        final FloatingActionButton practiceButton = (FloatingActionButton) findViewById(R.id.practiceButton);
        practiceButton.setFloatingActionButtonColor(getResources().getColor(R.color.Blue));
        practiceButton.setFloatingActionButtonDrawable(getResources().getDrawable(R.drawable.ic_learning));
        practiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToTeachingActivity(currentCharacterSet.currentCharacter());
            }
        });

        correctVocabList = (RecyclerView) findViewById(R.id.correctExamples);
        correctVocabList.setLayoutManager(new LinearLayoutManager(this));
        correctVocabArrayAdapter = new KanjiVocabRecyclerAdapter(this, this.dictionarySet.kanjiFinder());
        correctVocabList.setAdapter(correctVocabArrayAdapter);

        drawPad.setOnStrokeListener(new DrawView.OnStrokeListener() {
            @Override
            public void onStroke(List<Point> stroke) {
                if (drawPad.getStrokeCount() == 1) {
                    doneButton.showFloatingActionButton();
                    teachMeButton.hideFloatingActionButton();
                    remindStoryButton.hideFloatingActionButton();
                }
            }
        });

        drawPad.setOnClearListener(new DrawView.OnClearListener() {
            @Override
            public void onClear() {
                if (teachMeButton.isHidden()) {
                    teachMeButton.showFloatingActionButton();
                }
                storyButtonUpdate();

                if (!doneButton.isHidden())
                    doneButton.hideFloatingActionButton();
            }
        });

        this.introCharsetLabel = (TextView)findViewById(R.id.introCharsetLabel);
        this.introCharsetDesc = (TextView)findViewById(R.id.introCharsetDescription);

    	hiraganaCharacterSet = CharacterSets.hiragana(lockChecker);
    	katakanaCharacterSet = CharacterSets.katakana(lockChecker);
    	joyouG1 = CharacterSets.joyouG1(this.dictionarySet.kanjiFinder(), lockChecker);
    	joyouG2 = CharacterSets.joyouG2(this.dictionarySet.kanjiFinder(), lockChecker);
    	joyouG3 = CharacterSets.joyouG3(this.dictionarySet.kanjiFinder(), lockChecker);
    	joyouG4 = CharacterSets.joyouG4(this.dictionarySet.kanjiFinder(), lockChecker);
    	joyouG5 = CharacterSets.joyouG5(this.dictionarySet.kanjiFinder(), lockChecker);
    	joyouG6 = CharacterSets.joyouG6(this.dictionarySet.kanjiFinder(), lockChecker);

    	this.characterSets.put("hiragana", hiraganaCharacterSet);
    	this.characterSets.put("katakana", katakanaCharacterSet);
    	this.characterSets.put("j1", joyouG1);
    	this.characterSets.put("j2", joyouG2);
    	this.characterSets.put("j3", joyouG3);
    	this.characterSets.put("j4", joyouG4);
    	this.characterSets.put("j5", joyouG5);
    	this.characterSets.put("j6", joyouG6);

        this.charSetFrag = (CharacterSetStatusFragment)getFragmentManager().findFragmentById(R.id.charSetInfoFragment);
        if(this.charSetFrag != null) {
            this.charSetFrag.setCharset(joyouG1);
        }

    	ActionBar actionBar = getSupportActionBar();
        this.actionBarBackground = new ColorDrawable(getResources().getColor(R.color.actionbar_main));
        actionBar.setBackgroundDrawable(this.actionBarBackground);
    	LockableArrayAdapter characterSetAdapter = new LockableArrayAdapter(this, new ArrayList<>(this.characterSets.values()));
        characterSetAdapter.setDropDownViewResource(R.layout.locked_list_item_spinner_layout);
    	actionBar.setListNavigationCallbacks(characterSetAdapter, this);
    	actionBar.show();
    	actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
    }

    public void animateActionBar(Integer colorTo){
        Integer colorFrom = this.actionBarBackground.getColor();
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator animator) {
                actionBarBackground.setColor((Integer) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();
    }

    public void setUiState(State requestedState) {
        if (requestedState.ordinal() == flipper.getDisplayedChild())
            return;

        if (requestedState == State.DRAWING) {
            correctAnimation.stopAnimation();
            playbackAnimation.stopAnimation();
            animateActionBar(getResources().getColor(R.color.actionbar_main));

            drawPad.clear();

            flipper.setDisplayedChild(State.DRAWING.ordinal());

        } else if (requestedState == State.CORRECT_ANSWER) {
            Log.d("nakama", "In CORRECT_ANSWER state change; starting flip");
            flipper.setDisplayedChild(State.CORRECT_ANSWER.ordinal());
            animateActionBar(getResources().getColor(R.color.actionbar_correct));

        } else if (requestedState == State.REVIEWING) {
            Log.d("nakama", "In REVIEWING state change; starting flip");
            flipperAnimationListener.animateOnFinish = new Animatable[]{correctAnimation, playbackAnimation};
            flipper.setDisplayedChild(State.REVIEWING.ordinal());
            animateActionBar(getResources().getColor(R.color.actionbar_incorrect));
        }
    }

    public void goToTeachingActivity(char character) {
        Intent teachIntent = new Intent(this, TeachingActivity.class);
        Bundle params = new Bundle();
        params.putString("parent", this.getClass().getName());
        params.putChar(Constants.KANJI_PARAM, character);
        params.putString(Constants.KANJI_PATH_PARAM, currentCharacterSet.pathPrefix);
        teachIntent.putExtras(params);
        queuedNextCharLoad = true;
        startActivity(teachIntent);
    }

    @Override
    public void onBackPressed() {
        State currentUiState = State.values()[flipper.getDisplayedChild()];

        if (currentUiState == State.DRAWING) {
            if (this.drawPad.getStrokeCount() > 0) {
                this.drawPad.undo();
            } else {
                super.onBackPressed();
            }
        } else {
            this.setUiState(State.DRAWING);
        }
    }

    public void raisePurchaseDialog(PurchaseDialog.DialogMessage message, Frequency freq) {
        if (message == PurchaseDialog.DialogMessage.START_OF_LOCKED_SET) {
            if (showedStartOfSetDialog) {
                return;
            } else {
                showedStartOfSetDialog = true;
            }
        }

        if (message == PurchaseDialog.DialogMessage.END_OF_LOCKED_SET) {
            if (showedEndOfSetDialog) {
                return;
            } else {
                showedEndOfSetDialog = true;
            }
        }

        if (lockChecker.getPurchaseStatus() == LockLevel.LOCKED && (freq == Frequency.ALWAYS || (freq == Frequency.ONCE_PER_SESSION && pd == null))) {
            try {
                if (pd == null) {
                    pd = PurchaseDialog.make(message);
                }
                pd.show(this.getSupportFragmentManager(), "purchase");
            } catch (Throwable t) {
                Log.e("nakama", "Caught fragment error when trying to show PurchaseDialog.", t);
            }
        } else {
            Log.i("nakama", "Skipping purchase fragment.");
        }
    }

    public void loadNextCharacter(boolean increment) {
        Log.i("nakama", "loadNextCharacter(" + increment + ")");

        // push before-next character onto back-stack
        Character priorCharacter = currentCharacterSet.currentCharacter();

        if (priorCharacter == null && !increment) {
            loadNextCharacter(true);
            return;
        }

        // TODO: improve this... show a page, give congratulations...?
        if (increment && currentCharacterSet.locked() && currentCharacterSet.passedAllCharacters()) {
            if (currentCharacterSet.locked() && lockChecker.getPurchaseStatus() == LockLevel.LOCKED) {
                raisePurchaseDialog(PurchaseDialog.DialogMessage.END_OF_LOCKED_SET, Frequency.ONCE_PER_SESSION);
            } else {
                Toast.makeText(this, "You have completed all the kana in this set!", Toast.LENGTH_LONG).show();
            }
        }

        if (increment) {
            this.reviewBug.setVisibility(View.GONE);
            currentCharacterSet.nextCharacter();
            if (currentCharacterSet.isReviewing())
                this.reviewBug.setVisibility(View.VISIBLE);
            Log.d("nakama", "Incremented to next character " + currentCharacterSet.currentCharacter());
        }
        storyButtonUpdate();
        this.loadDrawDetails();
    }

    private void storyButtonUpdate(){
        String story = db.getStory(currentCharacterSet.currentCharacter());
        if (story != null && !story.trim().equals("")) {
            remindStoryButton.showFloatingActionButton();
        } else {
            remindStoryButton.hideFloatingActionButton();
        }
    }

    private void loadDrawDetails() {
        Log.i("nakama", "loadDrawDetails()");
        Character first = this.currentCharacterSet.currentCharacter();

        int unicodeValue = this.currentCharacterSet.currentCharacter().charValue();
        String path = this.currentCharacterSet.pathPrefix + "/" + Integer.toHexString(unicodeValue) + ".path";

        AssetManager assets = getAssets();
        try {
            InputStream is = assets.open(path);
            try {
                this.currentCharacterSvg = Util.slurp(is).split("\n");
            } finally {
                is.close();
            }
        } catch (IOException e) {
            Log.e("nakama", "Error loading path: " + path + " for character " + first + " (" + unicodeValue + ")");
            Toast.makeText(this.getBaseContext(), "Internal Error loading stroke for " + first + "; " + path, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] clues = currentCharacterSet.currentCharacterClues();
        if (clues.length == 1) {
            otherMeaningsButton.setVisibility(View.GONE);
        } else {
            otherMeaningsButton.setVisibility(View.VISIBLE);
        }
        currentCharacterClueIndex = 0;

        Log.i("nakama", "target: setText to " + clues[currentCharacterClueIndex]);
        target.setCurrentText(clues[currentCharacterClueIndex]);
        instructionsLabel.setCurrentText(currentCharacterClueIndex == 0 ?
                "Draw the " + currentCharacterSet.label() + " for" :
                "which can also mean");
    }

    private void saveCurrentUsingCharacterSet() {
        if(this.currentCharacterSet.currentCharacter() == null){
            return;         // TODO: fix this. Should never be null, how is it happening?
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Editor ed = prefs.edit();
        Log.i("nakama", "KanjiMasterActivity: saveCurrentUsingCharacterSet : writing " + CHAR_SET + " to " + this.currentCharacterSet.pathPrefix);
        ed.putString(CHAR_SET, this.currentCharacterSet.pathPrefix);
        ed.putString(CHAR_SET_CHAR, Character.toString(this.currentCharacterSet.currentCharacter()));
        ed.apply();
    }

    private void loadCurrentCharacterSet() {

        // current character set
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String set = prefs.getString(CHAR_SET, "j1");
        this.currentCharacterSet = this.characterSets.get(set);
        if(this.currentCharacterSet == null){
            Log.e("nakama", "Invalid character set: " + set + "; defaulting to j1");
            this.currentCharacterSet = this.characterSets.get("j1");
        }
        Log.i("nakama", "loadCurrentCharacterSet: setting to " + set + " (" + this.currentCharacterSet.pathPrefix + ")");

        this.introCharsetLabel.setText(this.currentCharacterSet.label());
        this.introCharsetDesc.setText(this.currentCharacterSet.description);

        // current character
        String currChar = prefs.getString(CHAR_SET_CHAR, null);
        if (currChar != null) {
            this.currentCharacterSet.skipTo(currChar.charAt(0));
        }

        // shuffle setting
        currentCharacterSet.setShuffle(prefs.getBoolean("shuffleEnabled", false));

        try {
            invalidateOptionsMenu();
        } catch (Throwable t) {
            Log.d("nakama", "Ignore error during invalidateOptionsMenu; must be older android device.");
            // ignore for older devices.
        }
    }

    @Override
    public void onResume() {
        Log.i("nakama", "KanjiMasterActivity.onResume");

        loadCurrentCharacterSet();
        currentCharacterSet.load(this.getApplicationContext());

        // update tab navigation dropdown to selected character set
        String[] charSetNames = this.characterSets.keySet().toArray(new String[0]);
        for (int i = 0; i < this.characterSets.keySet().size(); i++) {
            String currCharsetName = characterSets.get(charSetNames[i]).pathPrefix;
            if (currCharsetName.equals(this.currentCharacterSet.pathPrefix)) {
                getSupportActionBar().setSelectedNavigationItem(i);
                break;
            }
        }
        loadNextCharacter(queuedNextCharLoad);
        queuedNextCharLoad = false;

        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i("nakama", "KanjiMasterActivity.onPause: saving state.");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        drawPad.stopAnimation();
        Editor ed = prefs.edit();
        ed.putBoolean("shuffleEnabled", currentCharacterSet.isShuffling());
        ed.apply();
        currentCharacterSet.save(this.getApplicationContext());
        saveCurrentUsingCharacterSet();
        if (pd != null) {
            pd.dismiss();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        Log.i("nakama", "KanjiMasterActivity.onDestroy");
        this.lockChecker.dispose();
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("nakama", "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        if (!lockChecker.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d("nakama", "AbstractMasterActivity: onActivityResult handled by IABUtil.");
        }
    }

    public LockChecker getLockChecker() {
        return this.lockChecker;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar, menu);
        if(BuildConfig.DEBUG) {
            menu.add("DEBUG:DrawTest");
            menu.add("DEBUG:DrawViewComparison");
            menu.add("DEBUG:SpenTest");
            menu.add("DEBUG:KanjiCheck");
            menu.add("DEBUG:LockUnlock");
            menu.add("DEBUG:IabConsume");
            menu.add("DEBUG:ResetStorySharing");
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem shuffleCheck = menu.findItem(R.id.menu_shuffle);
        if (currentCharacterSet != null) {
            shuffleCheck.setChecked(currentCharacterSet.isShuffling());
        }
        MenuItem lockItem = menu.findItem(R.id.menu_lock);
        Log.d("nakama", "KanjiMaster.onPrepareOptionsMenus: setting actionbar lock to: " +
                (lockChecker.getPurchaseStatus() != LockLevel.UNLOCKED) + " (" + lockChecker.getPurchaseStatus() + ")");
        lockItem.setVisible(lockChecker.getPurchaseStatus() != LockLevel.UNLOCKED);

        MenuItem shareCheck = menu.findItem(R.id.menu_share_stories);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String sharing = prefs.getString(TeachingStoryFragment.STORY_SHARING_KEY, null);
        shareCheck.setChecked("true".equals(sharing));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.menu_progress) {
            Intent teachIntent = new Intent(this, ProgressActivity.class);
            Bundle params = new Bundle();
            params.putString("parent", this.getClass().getName());
            params.putString(Constants.KANJI_PATH_PARAM, this.currentCharacterSet.pathPrefix);
            params.putString("characters", this.currentCharacterSet.charactersAsString());
            Log.d("nakama", "KanjiMaster: passing charset path " + this.currentCharacterSet.pathPrefix);
            teachIntent.putExtras(params);
            startActivity(teachIntent);
        } else if (item.getItemId() == R.id.menu_info) {
            Intent creditsIntent = new Intent(this, CreditsActivity.class);
            Bundle params = new Bundle();
            params.putString("parent", this.getClass().getName());
            creditsIntent.putExtras(params);
            startActivity(creditsIntent);
        } else if (item.getItemId() == R.id.menu_reset_progress) {
            queryProgressReset();
        } else if (item.getItemId() == R.id.menu_about_set) {
            SetInfoDialog.show(this, currentCharacterSet.name, currentCharacterSet.description);
        } else if (item.getItemId() == R.id.menu_shuffle) {
            item.setChecked(!item.isChecked());

            for (CharacterStudySet c : characterSets.values()) {
                c.setShuffle(item.isChecked());
            }
        } else if (item.getItemId() == R.id.menu_share_stories) {
            ShareStoriesDialog.show(this, new Runnable() {
                @Override
                public void run() {   // yes, share
                    item.setChecked(true);
                    updateStorySharingPreferences(true);
                }
            }, new Runnable() {
                @Override
                public void run() {   // no, don't share
                    item.setChecked(false);
                    updateStorySharingPreferences(false);
                }
            });

        } else if (item.getItemId() == R.id.menu_lock) {
            raisePurchaseDialog(PurchaseDialog.DialogMessage.LOCK_BUTTON, Frequency.ALWAYS);
        }

        if(BuildConfig.DEBUG) {
            if (item.getTitle().equals("DEBUG:DrawTest")) {
                startActivity(new Intent(this, TestDrawActivity.class));
            } else if (item.getTitle().equals("DEBUG:KanjiCheck")) {
                startActivity(new Intent(this, KanjiCheckActivity.class));
            } else if (item.getTitle().equals("DEBUG:DrawViewComparison")) {
                startActivity(new Intent(this, DrawViewTestActivity.class));
            } else if (item.getTitle().equals("DEBUG:SpenTest")) {
                startActivity(new Intent(this, SpenDrawActivity.class));
            } else if (item.getTitle().equals("DEBUG:LockUnlock")) {
                getLockChecker().coreUnlock();
            } else if (item.getTitle().equals("DEBUG:IabConsume")) {
                lockChecker.startConsume();
            } else if (item.getTitle().equals("DEBUG:ResetStorySharing")){
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                Editor e = prefs.edit();
                e.putString(TeachingStoryFragment.STORY_SHARING_KEY, null);
                e.apply();
            }
        }

        return true;
    }

    public void updateStorySharingPreferences(boolean sharing){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Editor e = prefs.edit();
        e.putString(TeachingStoryFragment.STORY_SHARING_KEY, String.valueOf(sharing));
        e.apply();
    }

    public void queryProgressReset() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Confirm");
        builder.setMessage("Are you sure you want to reset your progress in " + currentCharacterSet.name + "?");

        builder.setPositiveButton("Reset Progress", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                Editor ed = prefs.edit();
                ed.remove(currentCharacterSet.pathPrefix);
                ed.apply();
                currentCharacterSet.progressReset(KanjiMasterActivity.this);
                loadNextCharacter(false);

                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        saveCurrentUsingCharacterSet();
        currentCharacterSet.save(this.getApplicationContext());

        if (!(itemPosition == 0 || itemPosition == 2)) {
            raisePurchaseDialog(PurchaseDialog.DialogMessage.START_OF_LOCKED_SET, Frequency.ONCE_PER_SESSION);
        }

        if (itemPosition == 0) {
            this.currentCharacterSet = hiraganaCharacterSet;
            this.correctVocabList.setVisibility(View.GONE);
        } else if (itemPosition == 1) {
            this.currentCharacterSet = katakanaCharacterSet;
            this.correctVocabList.setVisibility(View.GONE);
        } else if (itemPosition == 2) {
            this.currentCharacterSet = joyouG1;
            this.correctVocabList.setVisibility(View.VISIBLE);
        } else if (itemPosition == 3) {
            this.currentCharacterSet = joyouG2;
            this.correctVocabList.setVisibility(View.VISIBLE);
        } else if (itemPosition == 4) {
            this.currentCharacterSet = joyouG3;
            this.correctVocabList.setVisibility(View.VISIBLE);
        } else if (itemPosition == 5) {
            this.currentCharacterSet = joyouG4;
            this.correctVocabList.setVisibility(View.VISIBLE);
        } else if (itemPosition == 6) {
            this.currentCharacterSet = joyouG5;
            this.correctVocabList.setVisibility(View.VISIBLE);
        } else if (itemPosition == 7) {
            this.currentCharacterSet = joyouG6;
            this.correctVocabList.setVisibility(View.VISIBLE);
//		} else if(itemPosition == 8){
//			Toast.makeText(this, "Showing SS", Toast.LENGTH_SHORT);
//			this.currentCharacterSet = this.joyouSS;
        }
        this.currentCharacterSet.load(this.getApplicationContext());
        if(this.charSetFrag != null){
            this.charSetFrag.setCharset(this.currentCharacterSet);
        }
        this.reviewBug.setVisibility(View.GONE);
        loadNextCharacter(false);
        drawPad.clear();
        setUiState(State.DRAWING);
        return true;
    }

    protected class FlipperAnimationListener implements Animation.AnimationListener {
        public Animatable[] animateOnFinish = new Animatable[0];

        @Override
        public void onAnimationStart(Animation animation) {
            maskView.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.i("nakama", "Ignoring touch event due to flipper animation.");
                    return true;
                }
            });
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            Log.i("nakama", "Flip animation finished.");

            maskView.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });

            for (Animatable a : animateOnFinish) {
                a.startAnimation(100);
            }
            animateOnFinish = new Animatable[0];
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            // nothing to do
        }
    }

    public void onFragmentInteraction(Uri uri){
        Log.i("nakama", "KanjiMasterActivity: onFragmeentInteraction called, " + uri);
    }
}
