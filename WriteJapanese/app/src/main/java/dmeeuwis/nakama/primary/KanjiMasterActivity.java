package dmeeuwis.nakama.primary;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.renderscript.Script;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.threeten.bp.LocalDate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dmeeuwis.Kana;
import dmeeuwis.Translation;
import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.charsets.CharacterSetDetailActivity;
import dmeeuwis.kanjimaster.charsets.CharacterSetDetailFragment;
import dmeeuwis.kanjimaster.charsets.CharacterSetListActivity;
import dmeeuwis.nakama.Constants;
import dmeeuwis.nakama.CreditsActivity;
import dmeeuwis.nakama.DrawViewTestActivity;
import dmeeuwis.nakama.KanjiCheckActivity;
import dmeeuwis.nakama.LockChecker;
import dmeeuwis.nakama.LockCheckerHolder;
import dmeeuwis.nakama.OnFragmentInteractionListener;
import dmeeuwis.nakama.ProgressActivity;
import dmeeuwis.nakama.ReleaseNotesActivity;
import dmeeuwis.nakama.ReminderManager;
import dmeeuwis.nakama.SpenDrawActivity;
import dmeeuwis.nakama.TestDrawActivity;
import dmeeuwis.nakama.data.AndroidInputStreamGenerator;
import dmeeuwis.nakama.data.AssetFinder;
import dmeeuwis.nakama.data.CharacterProgressDataHelper;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.CharacterStudySet.LockLevel;
import dmeeuwis.nakama.data.ClueExtractor;
import dmeeuwis.nakama.data.CustomCharacterSetDataHelper;
import dmeeuwis.nakama.data.DataHelper;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.data.PracticeLogSync;
import dmeeuwis.nakama.data.ProgressTracker;
import dmeeuwis.nakama.data.SRSScheduleHtmlGenerator;
import dmeeuwis.nakama.data.Settings;
import dmeeuwis.nakama.data.StoryDataHelper;
import dmeeuwis.nakama.data.SyncRegistration;
import dmeeuwis.nakama.data.UncaughtExceptionLogger;
import dmeeuwis.nakama.data.WriteJapaneseOpenHelper;
import dmeeuwis.nakama.kanjidraw.Comparator;
import dmeeuwis.nakama.kanjidraw.ComparisonAsyncTask;
import dmeeuwis.nakama.kanjidraw.ComparisonFactory;
import dmeeuwis.nakama.kanjidraw.Criticism;
import dmeeuwis.nakama.kanjidraw.CurveDrawing;
import dmeeuwis.nakama.kanjidraw.PointDrawing;
import dmeeuwis.nakama.teaching.TeachingActivity;
import dmeeuwis.nakama.teaching.TeachingStoryFragment;
import dmeeuwis.nakama.views.Animatable;
import dmeeuwis.nakama.views.AnimatedCurveView;
import dmeeuwis.nakama.views.DrawView;
import dmeeuwis.nakama.views.LockCheckerInAppBillingService;
import dmeeuwis.nakama.views.OverrideDialog;
import dmeeuwis.nakama.views.PurchaseDialog;
import dmeeuwis.nakama.views.ShareStoriesDialog;
import dmeeuwis.nakama.views.StrictnessDialog;
import dmeeuwis.nakama.views.translations.CharacterTranslationListAsyncTask;
import dmeeuwis.nakama.views.translations.ClueCard;
import dmeeuwis.nakama.views.translations.KanjiVocabRecyclerAdapter;

import static dmeeuwis.nakama.primary.IntroActivity.USE_SRS_SETTING_NAME;

public class KanjiMasterActivity extends AppCompatActivity implements ActionBar.OnNavigationListener,
            LockCheckerHolder, OnFragmentInteractionListener, OnGoalPickListener,
            ActivityCompat.OnRequestPermissionsResultCallback, GradingOverrideListener {

    public enum State {DRAWING, REVIEWING, CORRECT_ANSWER}

    private static final boolean DEBUG_SCORES = false;
    private static final boolean DEBUG_MENU = true;

    private enum Frequency {ALWAYS, ONCE_PER_SESSION}

    public static final String CHAR_SET = "currCharSet";
    public static final String CHAR_SET_CHAR = "currCharSetChar";

    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long SYNC_INTERVAL_IN_MINUTES = 60L * 12;
    public static final long SYNC_INTERVAL = SYNC_INTERVAL_IN_MINUTES * SECONDS_PER_MINUTE;

    protected DictionarySet dictionarySet;
    protected LockChecker lockChecker;

    protected CharacterStudySet currentCharacterSet;
    protected LinkedHashMap<String, CharacterStudySet> characterSets = new LinkedHashMap<>();
    protected List<CharacterStudySet> customSets = null;

    protected StoryDataHelper db;
    protected boolean showedEndOfSetDialog = false;
    protected boolean showedStartOfSetDialog = false;
    protected boolean queuedNextCharLoad = false;   // when switching to teaching activity, queue a next char load for onResume
    protected PurchaseDialog pd;

    private int animateSlide = 2000;    // replace by max screen width/height onResume

    // ui references
    protected DrawView drawPad;
    protected ClueCard instructionCard;
    protected AnimatedCurveView correctAnimation;
    protected AnimatedCurveView playbackAnimation;
    protected AnimatedCurveView correctDrawnView;
    protected AnimatedCurveView correctKnownView;
    protected CharacterTranslationListAsyncTask vocabAsync;
    protected RecyclerView correctVocabList;
    protected View reviewBug, srsBug;
    protected KanjiVocabRecyclerAdapter correctVocabArrayAdapter;
    protected ViewFlipper flipper;
    protected FlipperAnimationListener flipperAnimationListener;
    protected View maskView;
    protected FloatingActionButton overrideButton, remindStoryButton, doneButton, undoStrokeButton, teachMeButton;
    protected ListView criticism;           // TODO: to RecyclerView
    protected ArrayAdapter<String> criticismArrayAdapter;
    protected ColorDrawable actionBarBackground;
    protected String lastGradingRow;

    protected View correctCard, charsetCard, incorrectCard;

    protected String[] currentCharacterSvg;

    protected CharacterSetStatusFragment charSetFrag;

    private class KanjiMasterUncaughtHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(final Thread thread, final Throwable ex) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            UncaughtExceptionLogger.logError(thread, "Uncaught top level error: ", ex, KanjiMasterActivity.this.getApplicationContext());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("nakama", "KanjiMasterActivity.onActivityResult(" + requestCode + "," + resultCode + "," + data);

        lockChecker.handleActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("nakama-intro", "MainActivity: onCreate starting.");
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new KanjiMasterUncaughtHandler());

        lockChecker = new LockCheckerInAppBillingService(this);

        setContentView(R.layout.main);

        this.dictionarySet = DictionarySet.get(this.getApplicationContext());

        Animation outToLeft = AnimationUtils.loadAnimation(this, R.anim.screen_transition_out);
        flipper = (ViewFlipper) findViewById(R.id.viewflipper);
        if (flipper != null) {
            flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.screen_transition_in));
            flipper.setOutAnimation(outToLeft);
            flipper.setAnimationCacheEnabled(true);

            flipperAnimationListener = new FlipperAnimationListener();
            outToLeft.setAnimationListener(flipperAnimationListener);
        }

        maskView = findViewById(R.id.maskView);

        // Draw Frame init
        drawPad = (DrawView) findViewById(R.id.drawPad);
        drawPad.setBackgroundColor(DrawView.BACKGROUND_COLOR);

        correctKnownView = (AnimatedCurveView) findViewById(R.id.correctKnownView);
        correctDrawnView = (AnimatedCurveView) findViewById(R.id.correctDrawnView);

        doneButton = (FloatingActionButton) findViewById(R.id.finishedButton);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.d("nakama", "Finished button clicked");
                doneButton.hide();
                if (drawPad.getStrokeCount() == 0) {
                    return;
                }

                final PointDrawing challenger = drawPad.getDrawing();

                CurveDrawing curve;
                try {
                    curve = new CurveDrawing(currentCharacterSvg);
                } catch(Throwable t){
                    // should never happen, but error logs indicate it can.

                    UncaughtExceptionLogger.backgroundLogError("Caught error parsing svg CurveDrawing for " + currentCharacterSet.currentCharacter(), t, KanjiMasterActivity.this);
                    Toast.makeText(KanjiMasterActivity.this, "Error: failed to parse curve data for " + currentCharacterSet.currentCharacter(), Toast.LENGTH_LONG).show();

                    loadNextCharacter(true);
                    KanjiMasterActivity.this.recreate();

                    return;
                }
                final CurveDrawing known = curve;

                AndroidInputStreamGenerator is = new AndroidInputStreamGenerator(KanjiMasterActivity.this.getAssets());
                Comparator comparator = ComparisonFactory.getUsersComparator(getApplicationContext(), new AssetFinder(is));

                ComparisonAsyncTask comp = new ComparisonAsyncTask(getApplicationContext(), comparator, currentCharacterSet, challenger, known, new ComparisonAsyncTask.OnCriticismDone(){
                    public void run(Criticism critique, CharacterStudySet.GradingResult entry) {

                        // to support grading override
                        lastGradingRow = entry.rowId;

                        // with the override mechanism, users can switch back and forth between correct and incorrect screens,
                        // so initialize both now.

                        // set correct screen displays
                        {
                            if (correctKnownView == null) {
                                Log.i("nakama", "Setting challenger/drawing in recyclerview adapter");
                                correctVocabArrayAdapter.addKnownAndDrawnHeader(known, challenger);
                            } else {
                                Log.i("nakama", "Setting challenger/drawing in layouts");
                                correctKnownView.setDrawing(known, AnimatedCurveView.DrawTime.STATIC, critique.knownPaintInstructions);
                                correctDrawnView.setDrawing(challenger, AnimatedCurveView.DrawTime.STATIC, critique.drawnPaintInstructions);
                            }

                            if (entry.srs != null) {
                                correctVocabArrayAdapter.addNextSrsHeader(entry.srs);
                            }
                        }

                        // set incorrect displays
                        {
                            correctAnimation.setDrawing(known, AnimatedCurveView.DrawTime.ANIMATED, critique.knownPaintInstructions);
                            playbackAnimation.setDrawing(challenger, AnimatedCurveView.DrawTime.ANIMATED, critique.drawnPaintInstructions);

                            criticismArrayAdapter.clear();
                            for (String c : critique.critiques) {
                                criticismArrayAdapter.add(c);
                            }
                        }

                        if (critique.pass) {
                            setUiState(State.CORRECT_ANSWER);
                        } else {
                            setUiState(State.REVIEWING);
                        }

                        // apply new grading to all other sets. Otherwise might see old already done chars when switching sets.
                        Context appContext = getApplicationContext();
                        for(Map.Entry<String, CharacterStudySet> s: characterSets.entrySet()) {
                            CharacterStudySet set = s.getValue();
                            new CharacterProgressDataHelper(appContext, Iid.get(appContext))
                                    .resumeProgressTrackerFromDB(Arrays.asList(set.getProgressTracker()));
                        }
                    }
                });
                comp.execute();

            }
        });

        undoStrokeButton = findViewById(R.id.undoStrokeButton);
        undoStrokeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawPad.undo();
                if(drawPad.getStrokeCount() == 0){
                    setUiState(State.DRAWING);
                }
            }
        });

        db = new StoryDataHelper(getApplicationContext());
        remindStoryButton = findViewById(R.id.remindStoryButton);
        remindStoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), db.getStory(currentCharacterSet.currentCharacter()), Toast.LENGTH_LONG).show();
            }
        });

        teachMeButton = (FloatingActionButton) findViewById(R.id.teachButton);
        teachMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                char c = currentCharacterSet.currentCharacter();
                currentCharacterSet.markCurrentAsUnknown(getApplicationContext());
                goToTeachingActivity(c);
            }
        });


        overrideButton = (FloatingActionButton) findViewById(R.id.overrideButton);
        overrideButton.setOnClickListener(
                new View.OnClickListener() {
                     @Override
                     public void onClick(View v) {
                         OverrideDialog od = OverrideDialog.make();
                         if(!od.isAdded()) {
                             od.show(KanjiMasterActivity.this.getSupportFragmentManager(), "override");
                         }
                     }
                 });

        correctAnimation = (AnimatedCurveView) findViewById(R.id.animatedKnownReplay);
        playbackAnimation = (AnimatedCurveView) findViewById(R.id.animatedDrawnReplay);

        criticism = (ListView) findViewById(R.id.criticism);
        criticismArrayAdapter = new ArrayAdapter<>(this, R.layout.critique_list_item, R.id.critique_label, new ArrayList<String>(0));
        criticism.setAdapter(criticismArrayAdapter);

        FloatingActionButton next = (FloatingActionButton) findViewById(R.id.nextButton);
        View.OnClickListener nextButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.i("nakama", "nextButton Clicked!");
                drawPad.clear();
                setUiState(State.DRAWING);
                loadNextCharacter(true);

                delayedStartBackgroundLoadTranslations();
            }
        };

        next.setOnClickListener(nextButtonListener);

        final FloatingActionButton correctNext = (FloatingActionButton) findViewById(R.id.correctNextButton);
        correctNext.setOnClickListener(nextButtonListener);

        final FloatingActionButton practiceButton = (FloatingActionButton) findViewById(R.id.practiceButton);
        practiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToTeachingActivity(currentCharacterSet.currentCharacter());
            }
        });

        correctVocabList = (RecyclerView) findViewById(R.id.correctExamples);
        correctVocabList.setLayoutManager(new LinearLayoutManager(this));
        correctVocabArrayAdapter = new KanjiVocabRecyclerAdapter(KanjiMasterActivity.this, dictionarySet.kanjiFinder());
        correctVocabList.setAdapter(correctVocabArrayAdapter);

        drawPad.setOnStrokeListener(new DrawView.OnStrokeListener() {
            @Override
            public void onStroke(List<Point> stroke) {
                if (drawPad.getStrokeCount() == 1) {
                    doneButton.show();
                    undoStrokeButton.show();
                    teachMeButton.hide();
                    remindStoryButton.hide();
                }
            }
        });

        drawPad.setOnClearListener(new DrawView.OnClearListener() {
            @Override
            public void onClear() {
                if (!teachMeButton.isShown()){
                    teachMeButton.show();
                }
                storyButtonUpdate();

                if (doneButton.isShown()) {
                    doneButton.hide();
                    undoStrokeButton.hide();
                }
            }
        });


        DisplayMetrics dm = getResources().getDisplayMetrics();
        animateSlide = Math.max(dm.heightPixels, dm.widthPixels);

        correctCard = findViewById(R.id.correctCard);
        incorrectCard = findViewById(R.id.incorrectCard);
        charsetCard = findViewById(R.id.charsetInfoCard);
        instructionCard = (ClueCard) findViewById(R.id.clueCard);
        reviewBug = findViewById(R.id.reviewBug);
        if(correctCard != null){
            correctCard.setTranslationY(-1 * animateSlide);
            incorrectCard.setTranslationY(-1 * animateSlide);
        }

        srsBug = findViewById(R.id.srsBug);

        ActionBar actionBar = getSupportActionBar();
        this.actionBarBackground = new ColorDrawable(getResources().getColor(R.color.actionbar_main));
        actionBar.setBackgroundDrawable(this.actionBarBackground);

        LockableArrayAdapter characterSetAdapter = new LockableArrayAdapter(this, new ArrayList<>(this.characterSets.values()));
        characterSetAdapter.setDropDownViewResource(R.layout.locked_list_item_spinner_layout);
        actionBar.setListNavigationCallbacks(characterSetAdapter, this);

        actionBar.show();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        initializeCharacterSets();

        ReminderManager.scheduleRemindersFor(getApplicationContext());

        Boolean srsEnabled = Settings.getSRSEnabled(getApplicationContext());
        boolean srsAsked = srsEnabled != null;
        Settings.SyncStatus syncStatus = Settings.getCrossDeviceSyncEnabled(getApplicationContext());

        Log.i("nakama", "srsEnabled=" + srsEnabled + "; syncStatus=" + syncStatus);
        if(!srsAsked || !syncStatus.asked){
            Log.i("nakama-intro", "Launch IntroActivity from KanjiMasterActivity");
            startActivity(new Intent(this, IntroActivity.class));
        }
    }

    private void initializeCharacterSets(){
        Log.d("nakama-progress", "Initializing character sets!");
        this.customSets = new ArrayList<>();
        this.characterSets.clear();
        this.characterSets.put("hiragana", CharacterSets.hiragana(lockChecker, getApplicationContext()));
        this.characterSets.put("katakana", CharacterSets.katakana(lockChecker, getApplicationContext()));

        this.characterSets.put("j1", CharacterSets.joyouG1(lockChecker, getApplicationContext()));
        this.characterSets.put("j2", CharacterSets.joyouG2(lockChecker, getApplicationContext()));
        this.characterSets.put("j3", CharacterSets.joyouG3(lockChecker, getApplicationContext()));
        this.characterSets.put("j4", CharacterSets.joyouG4(lockChecker, getApplicationContext()));
        this.characterSets.put("j5", CharacterSets.joyouG5(lockChecker, getApplicationContext()));
        this.characterSets.put("j6", CharacterSets.joyouG6(lockChecker, getApplicationContext()));
        this.characterSets.put("jhs", CharacterSets.joyouHS(lockChecker, getApplicationContext()));

        this.characterSets.put("jlpt5", CharacterSets.jlptN5(lockChecker, getApplicationContext()));
        this.characterSets.put("jlpt4", CharacterSets.jlptN4(lockChecker, getApplicationContext()));
        this.characterSets.put("jlpt3", CharacterSets.jlptN3(lockChecker, getApplicationContext()));
        this.characterSets.put("jlpt2", CharacterSets.jlptN2(lockChecker, getApplicationContext()));
        this.characterSets.put("jlpt1", CharacterSets.jlptN1(lockChecker, getApplicationContext()));

        CustomCharacterSetDataHelper helper = new CustomCharacterSetDataHelper(this);
        for(CharacterStudySet c: helper.getSets()){
            this.customSets.add(c);
            this.characterSets.put(c.pathPrefix, c);
        }

        List<ProgressTracker> trackers = new ArrayList<>(characterSets.size());
        for(CharacterStudySet c: this.characterSets.values()){
            trackers.add(c.load(this.getApplicationContext(), CharacterStudySet.LoadProgress.NO_LOAD_SET_PROGRESS));
        }
        new CharacterProgressDataHelper(this.getApplicationContext(), Iid.get(getApplicationContext()))
                .loadProgressTrackerFromDB(trackers);
    }

    private void resumeCharacterSets(){
        List<ProgressTracker> trackers = new ArrayList<>(characterSets.size());
        for(CharacterStudySet c: this.characterSets.values()){
            trackers.add(c.getProgressTracker());
        }
        new CharacterProgressDataHelper(this.getApplicationContext(), Iid.get(getApplicationContext()))
                .resumeProgressTrackerFromDB(trackers);
    }

    private void resumeCurrentCharacterSet(){
        List<ProgressTracker> singleTracker = new ArrayList<>(1);
        singleTracker.add(currentCharacterSet.getProgressTracker());
        new CharacterProgressDataHelper(this.getApplicationContext(), Iid.get(getApplicationContext()))
                .resumeProgressTrackerFromDB(singleTracker);
        loadNextCharacter(false);
    }

    public void delayedStartBackgroundLoadTranslations(){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                backgroundLoadTranslations();
            }
        }, 500L);
    }

    public void backgroundLoadTranslations(){
        correctVocabList.scrollToPosition(0);
        correctVocabArrayAdapter.clear();
        Character c = currentCharacterSet.currentCharacter();
        if(Kana.isKanji(c)) {
            correctVocabArrayAdapter.addReadingsHeader(c);
            try {
                correctVocabArrayAdapter.addMeaningsHeader(
                        TextUtils.join(", ", dictionarySet.kanjiFinder().find(c).meanings));
            } catch (IOException e) {
                correctVocabArrayAdapter.removeMeaningsHeader();
            }
        } else {
            correctVocabArrayAdapter.removeReadingsHeader();
            correctVocabArrayAdapter.removeMeaningsHeader();
        }
        CharacterTranslationListAsyncTask.AddTranslation adder = new CharacterTranslationListAsyncTask.AddTranslation() {
            public void add(Translation t) {
                correctVocabArrayAdapter.add(t);
            }
        };

        if (vocabAsync != null) {
            vocabAsync.cancel(true);
        }
        vocabAsync = new CharacterTranslationListAsyncTask(adder, getApplicationContext(), currentCharacterSet.currentCharacter());
        vocabAsync.execute();
    }

    public void animateActionBar(Integer colorTo) {
        Integer colorFrom = this.actionBarBackground.getColor();
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                actionBarBackground.setColor((Integer) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();
    }


    public void setUiState(State requestedState) {
        if (flipper != null) {
            setSmallUiState(requestedState);
        } else {
            setLargeUiState(requestedState);
        }
    }


    public void setSmallUiState(State requestedState) {
        if (requestedState.ordinal() == flipper.getDisplayedChild())
            return;

        if (requestedState == State.DRAWING) {
            correctAnimation.stopAnimation();
            playbackAnimation.stopAnimation();
            animateActionBar(getResources().getColor(R.color.actionbar_main));

            drawPad.clear();
            undoStrokeButton.hide();

            flipper.setDisplayedChild(State.DRAWING.ordinal());
            currentState = State.DRAWING;

        } else if (requestedState == State.CORRECT_ANSWER) {
            Log.d("nakama", "In CORRECT_ANSWER state change; starting flip");
            flipper.setDisplayedChild(State.CORRECT_ANSWER.ordinal());
            animateActionBar(getResources().getColor(R.color.actionbar_correct));
            currentState = State.CORRECT_ANSWER;
            undoStrokeButton.hide();

        } else if (requestedState == State.REVIEWING) {
            Log.d("nakama", "In REVIEWING state change; starting flip");
            flipperAnimationListener.animateOnFinish = new Animatable[]{correctAnimation, playbackAnimation};
            flipper.setDisplayedChild(State.REVIEWING.ordinal());
            animateActionBar(getResources().getColor(R.color.actionbar_incorrect));
            currentState = State.REVIEWING;
            undoStrokeButton.hide();
        }
    }

    private State currentState = State.DRAWING;
    Set<Integer> slideInIds = new HashSet<>();
    Set<Integer> slideOutIds = new HashSet<>();

    private void slideIn(View ... views){
        for(View v: views) {
            if(slideInIds.contains(v.getId())){ return; }

            if("slideDown".equals(v.getTag())){
                v.animate().translationYBy(-1 * animateSlide);
            } else {
                v.animate().translationYBy(animateSlide);
            }
            slideInIds.add(v.getId());

            if(slideOutIds.contains(v.getId())){
                slideOutIds.remove(v.getId());
            } else {
                slideInIds.add(v.getId());
            }
        }
    }
    private void slideOut(View ... views){
        for(View v: views) {
            if(slideOutIds.contains(v.getId())){ return; }

            if("slideDown".equals(v.getTag())){
                v.animate().translationYBy(animateSlide);
            } else {
                v.animate().translationYBy(-1 * animateSlide);
            }

            if(slideInIds.contains(v.getId())){
                slideInIds.remove(v.getId());
            } else {
                slideOutIds.add(v.getId());
            }
        }
    }

    public void setLargeUiState(State requestedState) {
        if (requestedState == currentState) {
            return;
        }
        if (requestedState == State.DRAWING) {
            Log.d("nakama", "In DRAWING state change; starting flip");
            animateActionBar(getResources().getColor(R.color.actionbar_main));
            drawPad.clear();

            if(correctCard.getY() >= 0) slideOut(correctCard);
            if(incorrectCard.getY() >= 0) slideOut(incorrectCard);

            slideIn(instructionCard, charsetCard);
            teachMeButton.show();

            currentState = State.DRAWING;

        } else if (requestedState == State.CORRECT_ANSWER) {
            Log.d("nakama", "In CORRECT_ANSWER state change; starting flip");
            animateActionBar(getResources().getColor(R.color.actionbar_correct));

            correctCard.animate().translationY(animateSlide);

            slideIn(correctCard);
            slideOut(instructionCard, charsetCard);
            doneButton.hide();
            undoStrokeButton.hide();

            currentState = State.CORRECT_ANSWER;

        } else if (requestedState == State.REVIEWING) {
            Log.d("nakama", "In REVIEWING state change; starting flip");
            animateActionBar(getResources().getColor(R.color.actionbar_incorrect));

            slideIn(incorrectCard);
            slideOut(instructionCard, charsetCard);
            doneButton.hide();
            undoStrokeButton.hide();

            correctAnimation.startAnimation(200);
            playbackAnimation.startAnimation(200);

            currentState = State.REVIEWING;
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
        if (currentState == State.DRAWING) {
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

                if(!pd.isAdded()) {
                    pd.show(this.getSupportFragmentManager(), "purchase");
                }
            } catch (Throwable t) {
                Log.e("nakama", "Caught fragment error when trying to show PurchaseDialog.", t);
            }
        } else {
            Log.i("nakama", "Skipping purchase fragment.");
        }
    }

    public void loadNextCharacter(boolean increment) {
        // push before-next character onto back-stack
        Character priorCharacter = currentCharacterSet.currentCharacter();

        if (priorCharacter == null && !increment) {
            increment = true;
        }

        // TODO: improve this... show a page, give congratulations...?
        if (increment && currentCharacterSet.locked() && currentCharacterSet.passedAllCharacters()) {
            if (currentCharacterSet.locked() && lockChecker.getPurchaseStatus() == LockLevel.LOCKED) {
                raisePurchaseDialog(PurchaseDialog.DialogMessage.END_OF_LOCKED_SET, Frequency.ONCE_PER_SESSION);
            } else {
                Toast.makeText(this, "You have completed standardSets the characters in this set!", Toast.LENGTH_LONG).show();
            }
        }

        if (increment) {
            currentCharacterSet.nextCharacter();
            Log.d("nakama", "Incremented to next character " + currentCharacterSet.currentCharacter());

        }

        {
            Log.i("nakama-progression", "Setting reviewBug visibility to " + currentCharacterSet.isReviewing());
            int reviewBugVisibility = currentCharacterSet.isReviewing() == ProgressTracker.StudyType.REVIEW ? View.VISIBLE : View.GONE;
            if (reviewBug != null) {
                reviewBug.setVisibility(reviewBugVisibility);
            } else {
                instructionCard.setReviewBugVisibility(reviewBugVisibility);
            }
        }

        {
            int srsBugVisibility = currentCharacterSet.isReviewing() == ProgressTracker.StudyType.SRS ? View.VISIBLE : View.GONE;
            Log.i("nakama-progression", "Setting srsBug visibility to " + (srsBugVisibility == View.VISIBLE));
            if (srsBug != null) {
                //if(BuildConfig.DEBUG && srsBugVisibility == View.VISIBLE) { Toast.makeText(this, "SRS Repetition!", Toast.LENGTH_LONG).show(); }
                srsBug.setVisibility(srsBugVisibility);
            } else {
                instructionCard.setSRSBugVisibility(srsBugVisibility);
            }
        }


        storyButtonUpdate();
        this.loadDrawDetails(increment);

        if(this.charSetFrag != null){
            this.charSetFrag.updateProgress();
        }

        if(BuildConfig.DEBUG && DEBUG_SCORES){
            Integer curScore = currentCharacterSet.getScoreSheet().get(currentCharacterSet.currentCharacter());
            if(curScore != null) {
                Toast.makeText(this, "Current character score " + curScore, Toast.LENGTH_SHORT).show();
                Log.d("nakama", "Current character " + currentCharacterSet.currentCharacter() + " score " + curScore);
            }

        }
    }

    private void storyButtonUpdate() {
        String story = db.getStory(currentCharacterSet.currentCharacter());
        if (story != null && !story.trim().equals("")) {
            remindStoryButton.show();
        } else {
            remindStoryButton.hide();
        }
    }

    private void loadDrawDetails(boolean increment) {
        //Log.i("nakama", "loadDrawDetails()");
        Character first = this.currentCharacterSet.currentCharacter();

        String path = AssetFinder.findPathFile(this.currentCharacterSet.currentCharacter());

        try {
            this.currentCharacterSvg = new AssetFinder(new AndroidInputStreamGenerator(getAssets()))
                            .findSvgForCharacter(this.currentCharacterSet.currentCharacter());
        } catch (IOException e) {
            Log.e("nakama", "Error loading path: " + path + " for character " + first + " (" + currentCharacterSet.currentCharacter().charValue() + ")");
            Toast.makeText(this.getBaseContext(), "Internal Error loading stroke for " + first + "; " + path, Toast.LENGTH_SHORT).show();
            return;
        }

        ClueExtractor clueExtractor = new ClueExtractor(dictionarySet);
        instructionCard.setCurrentCharacter(clueExtractor, currentCharacterSet.currentCharacter(), !increment);
    }


    private void saveCurrentCharacterSet() {
        if (currentCharacterSet == null || currentCharacterSet.currentCharacter() == null) {
            return;         // TODO: fix this. Should never be null, how is it happening?
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Editor ed = prefs.edit();
        //Log.i("nakama", "KanjiMasterActivity: saveCurrentCharacterSet : writing " + CHAR_SET + " to " + this.currentCharacterSet.pathPrefix);
        ed.putString(CHAR_SET, this.currentCharacterSet.pathPrefix);
        ed.putString(CHAR_SET_CHAR, Character.toString(this.currentCharacterSet.currentCharacter()));
        ed.apply();

        currentCharacterSet.save();
    }

    private void loadCurrentCharacterSet() {

        // current character set
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String set = prefs.getString(CHAR_SET, "j1");
        this.currentCharacterSet = this.characterSets.get(set);

        if (this.currentCharacterSet == null) {
            Log.e("nakama", "Invalid character set: " + set + "; defaulting to j1");
            this.currentCharacterSet = this.characterSets.get("j1");
        }
        Log.i("nakama", "loadCurrentCharacterSet: setting to " + set + " (" + this.currentCharacterSet.pathPrefix + ")");

        // current character
        String currChar = prefs.getString(CHAR_SET_CHAR, null);
        if (currChar != null) {
            this.currentCharacterSet.skipTo(currChar.charAt(0));
        }

        // shuffle setting
        boolean shuffle = prefs.getBoolean("shuffleEnabled", false);
        Log.i("nakama", "Setting shuffle on " + currentCharacterSet.name + " to " + shuffle);
        currentCharacterSet.setShuffle(shuffle);

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

        //initializeCharacterSets();      // should do nothing?
        resumeCharacterSets();

        this.charSetFrag = (CharacterSetStatusFragment) getSupportFragmentManager().findFragmentById(R.id.charSetInfoFragment);
        if (this.charSetFrag != null) {
            this.charSetFrag.setCharset(characterSets.get("j1"));
        }

        LockableArrayAdapter characterSetAdapter = new LockableArrayAdapter(this, new ArrayList<>(this.characterSets.values()));
        characterSetAdapter.setDropDownViewResource(R.layout.locked_list_item_spinner_layout);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setListNavigationCallbacks(characterSetAdapter, this);

        String charsetSwitch = getIntent().getStringExtra("CHARSET_SWITCH");
        if(charsetSwitch != null){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Editor ed = prefs.edit();
            ed.putString(CHAR_SET, charsetSwitch);
            ed.apply();
        }

        loadCurrentCharacterSet();

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

        instructionCard.onResume(getApplicationContext());
        UpdateNotifier.updateNotifier(this, findViewById(R.id.drawingFrame));


        // detect if user changed SRS settings, and need to reload charsets
        Settings.SyncStatus syncStatus = Settings.getCrossDeviceSyncEnabled(getApplicationContext());
        boolean srsAsked = Settings.getBooleanSetting(getApplicationContext(), USE_SRS_SETTING_NAME, null) != null;
        if(syncStatus.asked && srsAsked &&
                currentCharacterSet.srsAcrossSets() != Settings.getSRSAcrossSets(getApplicationContext())){
            Log.i("nakama-intro", "Restarting activity due to change in SRS settings");
            this.recreate();
        }

        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i("nakama", "KanjiMasterActivity.onPause: saving state.");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Editor ed = prefs.edit();
        ed.putBoolean("shuffleEnabled", currentCharacterSet.isShuffling());
        ed.apply();
        saveCurrentCharacterSet();
        instructionCard.saveCurrentClueType(getApplicationContext());

        CharacterProgressDataHelper d = new CharacterProgressDataHelper(this.getApplicationContext(), Iid.get(this.getApplicationContext()));
        for(CharacterStudySet c: this.characterSets.values()){
            try {
                ProgressTracker.ProgressState serialize = c.getProgressTracker().serializeOut();
                if(serialize != null) {
                    d.cachePracticeRecord(c.pathPrefix, serialize.recordSheetJson, serialize.srsQueueJson, serialize.oldestDateTime);
                }
            } catch(Throwable t){
                Log.d("nakama", "Error caching progress on " + c.pathPrefix + " onPause", t);
            }
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.i("nakama", "KanjiMasterActivity.onDestroy");
        this.lockChecker.dispose();
        if(pd != null && pd.isAdded()){
            //pd.dismiss();
        }

        super.onDestroy();
    }

    public LockChecker getLockChecker() {
        return this.lockChecker;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar, menu);

        if (charSetFrag != null) {
            menu.findItem(R.id.menu_set_goals).setVisible(false);
        }

        if (BuildConfig.DEBUG && DEBUG_MENU) {
            menu.add("DEBUG:RecalculateProgress");
            menu.add("DEBUG:DrawTest");
            menu.add("DEBUG:DrawViewComparison");
            menu.add("DEBUG:SpenTest");
            menu.add("DEBUG:KanjiCheck");
            menu.add("DEBUG:Lock");
            menu.add("DEBUG:Unlock");
            menu.add("DEBUG:IabConsume");
            menu.add("DEBUG:ResetStorySharing");
            menu.add("DEBUG:ClearRegisteredAccount");
            menu.add("DEBUG:ClearSharedPrefs");
            menu.add("DEBUG:ClearSyncSettings");
            menu.add("DEBUG:ClearSRSSettings");
            menu.add("DEBUG:PrintSRSQueues");
            menu.add("DEBUG:SRSPassCurrentSet");
            menu.add("DEBUG:SRSAddDay");
            menu.add("DEBUG:ClearSync");
            menu.add("DEBUG:PrintPracticeLog");
            menu.add("DEBUG:SyncNow");
            menu.add("DEBUG:ThrowException");
            menu.add("DEBUG:LogBackgroundException");
            menu.add("DEBUG:ClearSyncTimestamp");
            menu.add("DEBUG:ClearUpdateNotification");
            menu.add("DEBUG:PrintCharsetsAndSRS");
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem shuffleCheck = menu.findItem(R.id.menu_shuffle);
        if (currentCharacterSet != null) {
            Log.i("nakama", "Menu prep: shuffle is " + currentCharacterSet.isShuffling());
            shuffleCheck.setChecked(currentCharacterSet.isShuffling());
        }
        MenuItem lockItem = menu.findItem(R.id.menu_lock);
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

        } else if (item.getItemId() == R.id.menu_srs_settings) {
            Intent i = new Intent(this, IntroActivity.class);
            i.putExtra(IntroActivity.REQUEST_SRS_SETTINGS, true);
            startActivity(i);

/*        } else if (item.getItemId() == R.id.menu_sync_settings) {
            Intent i = new Intent(this, IntroActivity.class);
            i.putExtra(IntroActivity.REQUEST_SYNC_SETTINGS, true);
            startActivity(i);
*/
        } else if (item.getItemId() == R.id.menu_shuffle) {
            boolean currentState = !item.isChecked();
            item.setChecked(currentState);
            currentCharacterSet.setShuffle(currentState);
            Log.i("nakama", "Setting curr charset shuffle to " + item.isChecked());
            for (CharacterStudySet c : characterSets.values()) {
                Log.i("nakama", "Setting charset " + c.name + " shuffle to " + item.isChecked());
                c.setShuffle(currentState);
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
        } else if (item.getItemId() == R.id.menu_network_sync) {
            if(!SyncRegistration.checkIsRegistered(KanjiMasterActivity.this)) {
                Intent i = new Intent(this, IntroActivity.class);
                i.putExtra(IntroActivity.REQUEST_SYNC_SETTINGS, true);
                startActivity(i);
            } else {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Map<String, Integer> counts = new PracticeLogSync(KanjiMasterActivity.this).sync();
                            StringBuffer message = new StringBuffer("Sync completed!");
                            if(counts.get("practiceLogs") != null){
                                message.append(" " + counts.get("practiceLogs") + " practice logs. ");
                            }
                            if(counts.get("charsetEdits") != null){
                                message.append(" " + counts.get("charsetEdits") + " character set edits. ");
                            }
                            if(counts.get("charsetGoals") != null){
                                message.append(" " + counts.get("charsetGoals") + " character set goals.");
                            }
                            final String m = message.toString();

                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(new Runnable() {
                                @Override public void run() {
                                    Toast.makeText(KanjiMasterActivity.this, m, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Throwable e) {
                            UncaughtExceptionLogger.backgroundLogError("Error on menu-option network sync", e, KanjiMasterActivity.this);
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(new Runnable() {
                                @Override public void run() {
                                    Toast.makeText(KanjiMasterActivity.this, "Error while attempting network sync. Please retry later.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }.start();
            }
        } else if (item.getItemId() == R.id.menu_set_goals) {
            String charset = currentCharacterSet.pathPrefix;
            Intent intent = new Intent(this, CharsetInfoActivity.class);
            Bundle params = new Bundle();
            params.putString("charset", charset);
            intent.putExtras(params);
            startActivity(intent);

        } else if (item.getItemId() == R.id.menu_release_notes) {
            startActivity(new Intent(this, ReleaseNotesActivity.class));
        } else if (item.getItemId() == R.id.menu_strictness) {
            // show criticism selection fragment
            showStrictnessDialog();
        } else if (item.getItemId() == R.id.menu_srs){
            Map<LocalDate, List<Character>> schedule = currentCharacterSet.getSrsSchedule();
            SRSScheduleHtmlGenerator.displayScheduleDialog(this, schedule);

        } else if (item.getItemId() == R.id.menu_progression_settings) {
            // show criticism selection fragment
            showProgressionSettingsDialog();
        }

        if (BuildConfig.DEBUG) {
            if(item.getTitle().equals("DEBUG:RecalculateProgress")) {
                new CharacterProgressDataHelper(getApplicationContext(), Iid.get(getApplicationContext())).clearPracticeRecord();
                initializeCharacterSets();

            } else if (item.getTitle().equals("DEBUG:DrawTest")) {
                startActivity(new Intent(this, TestDrawActivity.class));
            } else if (item.getTitle().equals("DEBUG:KanjiCheck")) {
                startActivity(new Intent(this, KanjiCheckActivity.class));
            } else if (item.getTitle().equals("DEBUG:DrawViewComparison")) {
                startActivity(new Intent(this, DrawViewTestActivity.class));
            } else if (item.getTitle().equals("DEBUG:SpenTest")) {
                startActivity(new Intent(this, SpenDrawActivity.class));
            } else if (item.getTitle().equals("DEBUG:Unlock")) {
                getLockChecker().coreUnlock();
                recreate();
            } else if (item.getTitle().equals("DEBUG:Lock")) {
                getLockChecker().coreLock();
                recreate();
            } else if (item.getTitle().equals("DEBUG:IabConsume")) {
                lockChecker.startConsume();

            } else if (item.getTitle().equals("DEBUG:ResetStorySharing")) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                Editor e = prefs.edit();
                e.putString(TeachingStoryFragment.STORY_SHARING_KEY, null);
                e.apply();

            } else if (item.getTitle().equals("DEBUG:ClearSyncSettings")) {
                Settings.clearCrossDeviceSync(getApplicationContext());

            } else if (item.getTitle().equals("DEBUG:ClearSRSSettings")) {
                Settings.clearSRSSettings(getApplicationContext());

            } else if (item.getTitle().equals("DEBUG:PrintSRSQueues")) {
                for(Map.Entry<String, CharacterStudySet> s: characterSets.entrySet()){
                    Log.d("nakama", s.getKey() + ": " + s.getValue().getSrsScheduleString());
                }

            } else if (item.getTitle().equals("DEBUG:SRSPassCurrentSet")) {
                currentCharacterSet.srsForcePassAll();
            } else if (item.getTitle().equals("DEBUG:SRSAddDay")) {
                for(Map.Entry<String, CharacterStudySet> set: characterSets.entrySet()){
                    set.getValue().getProgressTracker().debugAddDayToSRS();
                }

            } else if (item.getTitle().equals("DEBUG:ClearSync")) {
                new PracticeLogSync(KanjiMasterActivity.this).clearSync();
            } else if (item.getTitle().equals("DEBUG:ClearSharedPrefs")) {
                Log.i("nakama", "DEBUG clearing standardSets shared prefs");
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                Editor e = prefs.edit();
                e.clear();
                e.apply();
            } else if (item.getTitle().equals("DEBUG:PrintPracticeLog")) {
                new PracticeLogSync(KanjiMasterActivity.this).debugPrintLog();
            } else if(item.getTitle().equals("DEBUG:ClearRegisteredAccount")){
                SyncRegistration.clearAccount(this);
            } else if(item.getTitle().equals("DEBUG:SyncNow")){
                Bundle bundle = new Bundle();
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_FORCE, true);
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                ContentResolver.requestSync(null, "dmeeuwis.com", bundle);
            } else if(item.getTitle().equals("DEBUG:ThrowException")){
                throw new RuntimeException("Practicing error catching!");
            } else if(item.getTitle().equals("DEBUG:LogBackgroundException")){
                UncaughtExceptionLogger.backgroundLogError("Practicing background error catching!",
                        new RuntimeException("BOOM!", new RuntimeException("CRASH!", new RuntimeException("THUNK!"))), getApplicationContext());
            } else if(item.getTitle().equals("DEBUG:ClearSyncTimestamp")){
                SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                ed.remove(PracticeLogSync.DEVICE_SYNC_PREFS_KEY);
                ed.remove(PracticeLogSync.SERVER_SYNC_PREFS_KEY);
                ed.apply();
            } else if (item.getTitle().equals("DEBUG:ClearUpdateNotification")) {
                UpdateNotifier.debugClearNotified(this);
            } else if(item.getTitle().equals("DEBUG:PrintCharsetsAndSRS")){
                for(CharacterStudySet c: characterSets.values()){
                    Log.d("nakama", "Character set " + c.name);
                    Log.d("nakama", "Character SRS " + c.getSrsScheduleString());

                }


            }
        }

        return true;
    }

    private void showProgressionSettingsDialog() {
        FragmentManager fm = getSupportFragmentManager();
        ProgressSettingsDialog d = new ProgressSettingsDialog();
        if(!isFinishing()) {
            d.show(fm, "fragment_progression_settings");
        }
    }

    private void showStrictnessDialog() {
        FragmentManager fm = getSupportFragmentManager();
        StrictnessDialog strictnessDialog = new StrictnessDialog();
        if(!isFinishing()) {
            strictnessDialog.show(fm, "fragment_strictness");
        }
    }

    public void updateStorySharingPreferences(boolean sharing) {
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
                currentCharacterSet.progressReset(getApplicationContext());
                loadNextCharacter(true);

                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        if(!isFinishing()) {
            alert.show();
        }
    }


    private int currentNavigationItem = 0;

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Log.i("nakama", "onNavigationItemSelected: " + itemPosition);

        saveCurrentCharacterSet();
        CharacterStudySet prevSet = this.currentCharacterSet;

        if (itemPosition == 0) {
            this.currentCharacterSet = characterSets.get("hiragana");
            //this.correctVocabList.setVisibility(View.GONE);
        } else if (itemPosition == 1) {
            this.currentCharacterSet = characterSets.get("katakana");
            //this.correctVocabList.setVisibility(View.GONE);
        } else if (itemPosition == 2) {
            this.currentCharacterSet = characterSets.get("j1");
            //this.correctVocabList.setVisibility(View.VISIBLE);
        } else if (itemPosition == 3) {
            this.currentCharacterSet = characterSets.get("j2");
            //this.correctVocabList.setVisibility(View.VISIBLE);
        } else if (itemPosition == 4) {
            this.currentCharacterSet = characterSets.get("j3");
            //this.correctVocabList.setVisibility(View.VISIBLE);
        } else if (itemPosition == 5) {
            this.currentCharacterSet = characterSets.get("j4");
            //this.correctVocabList.setVisibility(View.VISIBLE);
        } else if (itemPosition == 6) {
            this.currentCharacterSet = characterSets.get("j5");
            //this.correctVocabList.setVisibility(View.VISIBLE);
        } else if (itemPosition == 7) {
            this.currentCharacterSet = characterSets.get("j6");
        } else if (itemPosition == 8) {
            this.currentCharacterSet = characterSets.get("jhs");
        } else if (itemPosition == 9) {
            this.currentCharacterSet = characterSets.get("jlpt5");
        } else if (itemPosition == 10) {
            this.currentCharacterSet = characterSets.get("jlpt4");
        } else if (itemPosition == 11) {
            this.currentCharacterSet = characterSets.get("jlpt3");
        } else if (itemPosition == 12) {
            this.currentCharacterSet = characterSets.get("jlpt2");
        } else if (itemPosition == 13) {
            this.currentCharacterSet = characterSets.get("jlpt1");
        }

        // force a next recalculation due to SRS global. Otherwise might get stucck
        // redoing same char you just did in previous set.
        loadNextCharacter(true);

        if(itemPosition >= 14 &&  itemPosition < 14 + customSets.size()){
            int customSetIndex = itemPosition - 14;
            this.currentCharacterSet = customSets.get(customSetIndex);
        }

        if(prevSet != null && this.currentCharacterSet != null){
            this.currentCharacterSet.setShuffle(prevSet.isShuffling());
        }

        if(itemPosition >= 14 + customSets.size()) {
            if (customSets.size() == 0) {

                getSupportActionBar().setSelectedNavigationItem(currentNavigationItem);

                Intent intent = new Intent(this, CharacterSetDetailActivity.class);
                intent.putExtra(CharacterSetDetailFragment.CHARSET_ID, "create");
                startActivity(intent);
                return true;
            } else {
                startActivity(new Intent(this, CharacterSetListActivity.class));
                return true;
            }
        }

        if (this.charSetFrag != null) {
            this.charSetFrag.setCharset(this.currentCharacterSet);
        }

        if (!(itemPosition == 0 || itemPosition == 2 || itemPosition >= 14)) {
            raisePurchaseDialog(PurchaseDialog.DialogMessage.START_OF_LOCKED_SET, Frequency.ONCE_PER_SESSION);
        }

        resumeCurrentCharacterSet();

        drawPad.clear();
        setUiState(State.DRAWING);
        backgroundLoadTranslations();

        currentNavigationItem = itemPosition;

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
                    Log.d("nakama", "Ignoring touch event due to flipper animation.");
                    return true;
                }
            });
        }

        @Override
        public void onAnimationEnd(Animation animation) {
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

    public void onFragmentInteraction(Uri uri) {
        Log.d("nakama", "KanjiMasterActivity: onFragmentInteraction called, " + uri);
    }


    @Override
    public void overRide() {
        if(BuildConfig.DEBUG) Toast.makeText(this, "Override " + lastGradingRow, Toast.LENGTH_LONG).show();
        currentCharacterSet.overRideLast();
        setUiState(State.CORRECT_ANSWER);
    }

    @Override
    public void setGoal(int year, int month, int day) {
        charSetFrag.setGoal(year, month, day);
    }

}
