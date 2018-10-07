package dmeeuwis.nakama.primary;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.JsonWriter;
import android.util.Log;
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

import org.threeten.bp.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
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
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.data.PracticeLogSync;
import dmeeuwis.nakama.data.ProgressTracker;
import dmeeuwis.nakama.data.SRSQueue;
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
import dmeeuwis.nakama.views.*;
import dmeeuwis.nakama.views.translations.CharacterTranslationListAsyncTask;
import dmeeuwis.nakama.views.translations.ClueCard;
import dmeeuwis.nakama.views.translations.KanjiVocabRecyclerAdapter;

import static dmeeuwis.nakama.primary.IntroActivity.USE_SRS_SETTING_NAME;
import static dmeeuwis.nakama.views.OverrideDialog.OverideType.OVERRIDE_TO_CORRECT;
import static dmeeuwis.nakama.views.OverrideDialog.OverideType.OVERRIDE_TO_INCORRECT;

public class KanjiMasterActivity extends AppCompatActivity implements ActionBar.OnNavigationListener,
            LockCheckerHolder, OnFragmentInteractionListener, OnGoalPickListener,
            ActivityCompat.OnRequestPermissionsResultCallback, GradingOverrideListener {

    public enum State {DRAWING, REVIEWING, CORRECT_ANSWER}

    private static final boolean DEBUG_SCORES = false;
    private static final boolean DEBUG_MENU = true;

    private enum Frequency {ALWAYS, ONCE_PER_SESSION}

    public static final String CHAR_SET = "currCharSet";
    public static final String CHAR_SET_CHAR = "currCharSetChar";
    public static final String SKIP_INTRO_CHECK = "skipIntro";

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
    protected FloatingActionButton remindStoryButton, doneButton, undoStrokeButton, teachMeButton;
    protected ListView criticism;           // TODO: to RecyclerView
    protected ArrayAdapter<String> criticismArrayAdapter;
    protected ColorDrawable actionBarBackground;
    protected String lastGradingRow;

    private List<Character> studySessionHistory = new ArrayList<>();

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
        long start = System.currentTimeMillis();
        Log.i("nakama-intro", "MainActivity: onCreate starting.");
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new KanjiMasterUncaughtHandler());
        lockChecker = new LockCheckerInAppBillingService(this);
        setContentView(R.layout.main);          // pretty heavy, ~900ms
        this.dictionarySet = DictionarySet.get(this.getApplicationContext());

        Log.i("nakama-timing", "MainActivity: timing 1 " + (System.currentTimeMillis() - start) + "ms");

        Animation outToLeft = AnimationUtils.loadAnimation(this, R.anim.screen_transition_out);
        flipper = findViewById(R.id.viewflipper);
        if (flipper != null) {
            flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.screen_transition_in));
            flipper.setOutAnimation(outToLeft);
            flipper.setAnimationCacheEnabled(true);

            flipperAnimationListener = new FlipperAnimationListener();
            outToLeft.setAnimationListener(flipperAnimationListener);
        }
        Log.i("nakama-timing", "MainActivity: timing 1.5 " + (System.currentTimeMillis() - start) + "ms loaded animation");

        maskView = findViewById(R.id.maskView);

        // Draw Frame init
        drawPad = findViewById(R.id.drawPad);
        drawPad.setBackgroundColor(DrawView.BACKGROUND_COLOR);

        correctKnownView = findViewById(R.id.correctKnownView);
        correctDrawnView = findViewById(R.id.correctDrawnView);

        doneButton = findViewById(R.id.finishedButton);
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

                ComparisonAsyncTask comp = new ComparisonAsyncTask(KanjiMasterActivity.this, currentCharacterSet.currentCharacter(), currentCharacterSet.pathPrefix, comparator, challenger, known, new ComparisonAsyncTask.OnCriticismDone(){
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

        teachMeButton = findViewById(R.id.teachButton);
        teachMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                char c = currentCharacterSet.currentCharacter();
                mark(currentCharacterSet.currentCharacter(), currentCharacterSet.pathPrefix, null, false);
                goToTeachingActivity(c);
            }
        });


        FloatingActionButton overrideButton = findViewById(R.id.overrideButton);
        overrideButton.setOnClickListener(
                new View.OnClickListener() {
                     @Override
                     public void onClick(View v) {
                         OverrideDialog od = OverrideDialog.make(OVERRIDE_TO_CORRECT);
                         if(!od.isAdded()) {
                             od.show(KanjiMasterActivity.this.getSupportFragmentManager(), "override");
                         }
                     }
                 });

        FloatingActionButton correctOverrideButton = findViewById(R.id.correctOverrideButton);
        correctOverrideButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        OverrideDialog od = OverrideDialog.make(OVERRIDE_TO_INCORRECT);
                        if(!od.isAdded()) {
                            od.show(KanjiMasterActivity.this.getSupportFragmentManager(), "override");
                        }
                    }
                });

        correctAnimation = findViewById(R.id.animatedKnownReplay);
        playbackAnimation = findViewById(R.id.animatedDrawnReplay);

        criticism = findViewById(R.id.criticism);
        criticismArrayAdapter = new ArrayAdapter<>(this, R.layout.critique_list_item, R.id.critique_label, new ArrayList<String>(0));
        criticism.setAdapter(criticismArrayAdapter);

        FloatingActionButton next = findViewById(R.id.nextButton);
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

        final FloatingActionButton correctNext = findViewById(R.id.correctNextButton);
        correctNext.setOnClickListener(nextButtonListener);

        final FloatingActionButton practiceButton = findViewById(R.id.practiceButton);
        practiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToTeachingActivity(currentCharacterSet.currentCharacter());
            }
        });

        correctVocabList = findViewById(R.id.correctExamples);
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

        Log.i("nakama-timing", "MainActivity: timing 2 " + (System.currentTimeMillis() - start) + "ms, done onClickListeners");

        DisplayMetrics dm = getResources().getDisplayMetrics();
        animateSlide = Math.max(dm.heightPixels, dm.widthPixels);

        correctCard = findViewById(R.id.correctCard);
        incorrectCard = findViewById(R.id.incorrectCard);
        charsetCard = findViewById(R.id.charsetInfoCard);
        instructionCard = findViewById(R.id.clueCard);
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

        Log.i("nakama-timing", "MainActivity: timing 3 " + (System.currentTimeMillis() - start) + "ms");

        initializeCharacterSets(CharacterProgressDataHelper.ProgressCacheFlag.USE_CACHE);
        Log.i("nakama-timing", "MainActivity: timing 4 " + (System.currentTimeMillis() - start) + "ms");

        ReminderManager.scheduleRemindersFor(getApplicationContext());
        Log.i("nakama-timing", "MainActivity: timing 5 " + (System.currentTimeMillis() - start) + "ms");

        Boolean srsEnabled = Settings.getSRSEnabled(getApplicationContext());
        boolean srsAsked = srsEnabled != null;
        Settings.SyncStatus syncStatus = Settings.getCrossDeviceSyncEnabled(getApplicationContext());

        boolean skipIntro = false;
        try {
            skipIntro = getIntent().getExtras().getBoolean(SKIP_INTRO_CHECK, false);
        } catch(Throwable t){ /* ignore */ }

        if((!srsAsked || !syncStatus.asked ) && !skipIntro){
            Log.i("nakama-intro", "Launch IntroActivity from KanjiMasterActivity");
            startActivity(new Intent(this, IntroActivity.class));
        }

        Log.d("nakama-timing", "onCreate took " + (System.currentTimeMillis() - start) + "ms");
    }


    public CharacterStudySet.GradingResult mark(Character currChar, String setId, PointDrawing drawn, boolean pass) {
        // Record against all sets (sets ignore chars that don't have.)
        // Take the soonest SRS review as next.
        SRSQueue.SRSEntry s = null;
        for(CharacterStudySet c: characterSets.values()){
            SRSQueue.SRSEntry srs = c.markCurrent(currChar, drawn, pass);
            if(srs != null){
                if(s == null) {
                    s = srs;
                }  else {
                    if(srs.nextPractice.isBefore(s.nextPractice)){
                        s = srs;
                    }
                }
            }
        }

        // record only once in db, not once per set.
        CharacterProgressDataHelper dbHelper = new CharacterProgressDataHelper(getApplicationContext(), Iid.get(getApplicationContext()));
        String rowId = dbHelper.recordPractice(setId, currChar.toString(), drawn, pass ? 100 : -100);

        return new CharacterStudySet.GradingResult(rowId, s);
    }

    private void initializeCharacterSets(CharacterProgressDataHelper.ProgressCacheFlag progressCacheFlag){
        Log.d("nakama-progress", "Initializing character sets!");
        long start = System.currentTimeMillis();

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

        SRSQueue.registerSetsForGlobalSRS(characterSets.values());

        reloadPracticeLogs(CharacterStudySet.LoadProgress.NO_LOAD_SET_PROGRESS, progressCacheFlag);

        long time = System.currentTimeMillis() - start;
        Log.i("nakama", "Loading character sets took: " + time + "ms");
        if(BuildConfig.DEBUG){
            Toast.makeText(this, "Load took: " + time + "ms", Toast.LENGTH_LONG).show();
        }
    }

    private void reloadPracticeLogs(CharacterStudySet.LoadProgress loadType, CharacterProgressDataHelper.ProgressCacheFlag progressCacheFlag){
        List<ProgressTracker> trackers = new ArrayList<>(characterSets.size());
        for(CharacterStudySet c: this.characterSets.values()){
            trackers.add(c.load(this.getApplicationContext(), loadType));
        }
        CharacterProgressDataHelper ch = new CharacterProgressDataHelper(this.getApplicationContext(), Iid.get(getApplicationContext()));
        ch.loadProgressTrackerFromDB(trackers, progressCacheFlag);
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

    private Character vocabChar = null;
    public void backgroundLoadTranslations(){
        if(vocabChar != null && vocabChar.equals(currentCharacterSet.currentCharacter())){
            return;
        }

        correctVocabList.scrollToPosition(0);
        correctVocabArrayAdapter.clear();
        Character c = currentCharacterSet.currentCharacter();
        Log.i("nakama-vocab", "Background loading vocab for: " + c);
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
        vocabAsync = new CharacterTranslationListAsyncTask(adder, getApplicationContext(), c);
        vocabAsync.execute();
        vocabChar = c;
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

        if(currentState == State.DRAWING) {
            queuedNextCharLoad = true;
        }
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

    private void setting(JsonWriter jw, String key, Object b) throws IOException {
        jw.name(key);
        if(b == null){
            jw.value("<null>");
        } else {
            if(b instanceof Boolean){
                jw.value((Boolean)b);
            } else if(b instanceof String){
                jw.value((String)b);
            } else if(b instanceof Long){
                jw.value((Long)b);
            } else if(b instanceof Double){
                jw.value((Double)b);
            } else {
                jw.value(b.toString());
            }

        }
    }

    private String stateLog() throws IOException {
        Context ctx = getApplicationContext();

        StringWriter sb = new StringWriter();
        JsonWriter jw = new JsonWriter(sb);
        jw.setIndent("    ");

        jw.beginObject();

        setting(jw, "version", BuildConfig.VERSION_NAME + " " + BuildConfig.VERSION_CODE);
        setting(jw, "iid", Iid.get(ctx));
        setting(jw, "lockLevel", lockChecker.getPurchaseStatus().toString());
        setting(jw, "device", Build.MODEL);
        setting(jw, "localDate", LocalDate.now().toString());
        setting(jw, "localDateTime", LocalDateTime.now().toString());

        setting(jw, "srsEnabled", Settings.getSRSEnabled(ctx));
        setting(jw, "srsGlobal", Settings.getSRSAcrossSets(ctx));

        jw.name("sets");
        jw.beginArray();
        for(Map.Entry<String, CharacterStudySet> e: characterSets.entrySet()){
            jw.beginObject();
            CharacterStudySet s = e.getValue();

            setting(jw, "id", s.pathPrefix);
            setting(jw, "name", s.name);
            setting(jw, "chars", s.charactersAsString());
            setting(jw, "systemSet", s.systemSet);

            setting(jw, "srsString", s.getSrsScheduleString());

            jw.name("srs");
            s.getProgressTracker().getSrsQueue().serializeOut(jw);
            jw.endObject();
        }
        jw.endArray();

        // session history is static in ProgressTracker, so can get it from any charset.
        jw.name("sessionHistory");
        characterSets.get("hiragana").getProgressTracker().debugHistory(jw);

        jw.name("srsGlobalSet");
        SRSQueue.getglobalQueue().serializeOut(jw);

        jw.name("recentPracticeLogs");
        new CharacterProgressDataHelper(this, Iid.get(this)).debugLastNLogs(jw, 50);

        jw.endObject();
        return sb.toString();
    }

    private boolean loadedInitialVocab = false;
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
            ProgressTracker.StudyRecord r = currentCharacterSet.nextCharacter();
            if(r.type == ProgressTracker.StudyType.SRS && !r.setId.equals(currentCharacterSet.pathPrefix)){
                try {
                    Toast.makeText(this, "Cross-set SRS review from " + characterSets.get(r.setId).name, Toast.LENGTH_LONG).show();
                } catch(Throwable t){
                    UncaughtExceptionLogger.backgroundLogError("Error displaying Toast!", t);
                }
            }

            if(currentCharacterSet.currentCharacter().equals(priorCharacter)) {
                try {
                    UncaughtExceptionLogger.backgroundLogError("Error: increment nextCharacter is same as current! " + currentCharacterSet.currentCharacter() + "\n" +
                            currentCharacterSet.getProgressTracker().debugHistory(), new RuntimeException());
                } catch(Throwable t){
                    // never die because of this!
                }
            }

            studySessionHistory.add(currentCharacterSet.currentCharacter());
            backgroundLoadTranslations();
            Log.d("nakama", "Incremented to next character " + currentCharacterSet.currentCharacter());
        } else if(!loadedInitialVocab){
            backgroundLoadTranslations();
        }

        {
            //Log.i("nakama-progression", "Setting reviewBug visibility to " + currentCharacterSet.isReviewing());
            int reviewBugVisibility = currentCharacterSet.isReviewing() == ProgressTracker.StudyType.REVIEW ? View.VISIBLE : View.GONE;
            if (reviewBug != null) {
                reviewBug.setVisibility(reviewBugVisibility);
            } else {
                instructionCard.setReviewBugVisibility(reviewBugVisibility);
            }
        }

        {
            int srsBugVisibility = currentCharacterSet.isReviewing() == ProgressTracker.StudyType.SRS ? View.VISIBLE : View.GONE;
            //Log.i("nakama-progression", "Setting srsBug visibility to " + (srsBugVisibility == View.VISIBLE));
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
        ed.putBoolean("shuffleEnabled", currentCharacterSet.isShuffling());
        ed.putString(CHAR_SET, this.currentCharacterSet.pathPrefix);
        ed.putString(CHAR_SET_CHAR, Character.toString(this.currentCharacterSet.currentCharacter()));
        ed.apply();
    }

    /**
     * Loads any newly custom created characters sets, removes deleted. Does not load the practice logs
     * for these sets.
     */
    private void resumeCharacterSets() {
        List<CharacterStudySet> sets = new CustomCharacterSetDataHelper(getApplicationContext()).getSets();
        // load any newly created sets
        for (CharacterStudySet s : sets) {
            if (!characterSets.containsKey(s.pathPrefix)) {
                Log.i("nakama-sets", "Loading new character set " + s.pathPrefix + ": " + s.name);
                characterSets.put(s.pathPrefix, s);
                customSets.add(s);
                s.load(this.getApplicationContext(), CharacterStudySet.LoadProgress.NO_LOAD_SET_PROGRESS);
            }
        }
        Set<String> priorSets = new HashSet<>();
        for (CharacterStudySet c : customSets) {
            priorSets.add(c.pathPrefix);
        }
        Set<String> currentSets = new HashSet<>();
        for (CharacterStudySet c : sets) {
            currentSets.add(c.pathPrefix);
        }
        // remove any sets that were deleted in other activity
        for (String inPrior : priorSets) {
            if (!currentSets.contains(inPrior)) {
                customSets.remove(characterSets.get(inPrior));
                CharacterStudySet s = characterSets.remove(inPrior);
                Log.i("nakama-sets", "Removing deleted set: " + s.name);
            }
        }

        List<ProgressTracker> trackers = new ArrayList<>(characterSets.size());
        for (CharacterStudySet c : this.characterSets.values()) {
            trackers.add(c.getProgressTracker());
        }

        try {
            invalidateOptionsMenu();
        } catch (Throwable t) {
            Log.d("nakama", "Ignore error during invalidateOptionsMenu; must be older android device.");
            // ignore for older devices.
        }
    }

    /**
     * Loads all needed practice logs for this set.
     */
    private void resumeCurrentCharacterSet() {

        // current character set
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String set = prefs.getString(CHAR_SET, "j1");
        this.currentCharacterSet = this.characterSets.get(set);

        if (this.currentCharacterSet == null) {
            Log.e("nakama", "Invalid character set: " + set + "; defaulting to j1");
            this.currentCharacterSet = this.characterSets.get("j1");
        }
        Log.i("nakama", "resume CurrentCharacterSet: setting to " + set + " (" + this.currentCharacterSet.pathPrefix + ")");

        // current character
        String currChar = prefs.getString(CHAR_SET_CHAR, null);
        if (currChar != null) {
            this.currentCharacterSet.skipTo(currChar.charAt(0));
            studySessionHistory.add(currChar.charAt(0));

            prefs.edit().remove(CHAR_SET_CHAR).apply();
        }

    }


    private void resumePracticeLogsCurrentCharset(){
        // shuffle setting
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean shuffle = prefs.getBoolean("shuffleEnabled", false);
        currentCharacterSet.setShuffle(shuffle);

        List<ProgressTracker> pts =  new ArrayList<>(characterSets.size());
        for(CharacterStudySet st: characterSets.values()) {
            pts.add(st.getProgressTracker());
        }
        new CharacterProgressDataHelper(this, Iid.get(this)).resumeProgressTrackerFromDB(pts);
    }

    @Override
    public void onResume() {
        long start = System.currentTimeMillis();
        Log.i("nakama", "KanjiMasterActivity.onResume");

        this.charSetFrag = (CharacterSetStatusFragment) getSupportFragmentManager().findFragmentById(R.id.charSetInfoFragment);
        if (this.charSetFrag != null) {
            this.charSetFrag.setCharset(characterSets.get("j1"));
        }

        String charsetSwitch = getIntent().getStringExtra("CHARSET_SWITCH");
        if(charsetSwitch != null){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Editor ed = prefs.edit();
            ed.putString(CHAR_SET, charsetSwitch);
            ed.apply();
        }

        SRSQueue.useSRSGlobal = Settings.getSRSAcrossSets(getApplicationContext());
        if(BuildConfig.DEBUG){
            //Toast.makeText(this, "Global SRS: " + SRSQueue.useSRSGlobal, Toast.LENGTH_LONG).show();
        }

        resumeCharacterSets();
        resumeCurrentCharacterSet();
        resumePracticeLogsCurrentCharset();

        LockableArrayAdapter characterSetAdapter = new LockableArrayAdapter(this, new ArrayList<>(this.characterSets.values()));
        characterSetAdapter.setDropDownViewResource(R.layout.locked_list_item_spinner_layout);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setListNavigationCallbacks(characterSetAdapter, this);


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
        backgroundLoadTranslations();
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
        Log.d("nakama-timing", "onResume took " + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public void onPause() {
        Log.i("nakama", "KanjiMasterActivity.onPause: saving state.");
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
                UncaughtExceptionLogger.backgroundLogError("Error caching progress on " + c.pathPrefix + " onPause", t);
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
            menu.add("DEBUG:ClearLogCache");
            menu.add("DEBUG:DebugHistory");
            menu.add("DEBUG:DebugJSON");
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

        } else if (item.getItemId() == R.id.menu_bug) {
            // show criticism selection fragment
            showReportBugDialog();


        } else if(item.getTitle().equals("Recalculate Progress")) {
            new CharacterProgressDataHelper(getApplicationContext(), Iid.get(getApplicationContext())).clearPracticeRecord();
            initializeCharacterSets(CharacterProgressDataHelper.ProgressCacheFlag.USE_RAW_LOGS);
        }

        if (BuildConfig.DEBUG) {

            if (item.getTitle().equals("DEBUG:DrawTest")) {
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
            } else if(item.getTitle().equals("DEBUG:ClearLogCache")){

                WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this);
                SQLiteDatabase sqlite = db.getWritableDatabase();
                try {
                    Toast.makeText(this, "Recalculating Progress!", Toast.LENGTH_SHORT).show();
                    db.clearPracticeLogCache(sqlite);
                    reloadPracticeLogs(CharacterStudySet.LoadProgress.LOAD_SET_PROGRESS, CharacterProgressDataHelper.ProgressCacheFlag.USE_RAW_LOGS);
                    Toast.makeText(this, "Recalculated!", Toast.LENGTH_LONG).show();
                } finally {
                    sqlite.close();
                }

            } else if(item.getTitle().equals("DEBUG:DebugHistory")){
                Log.d("nakama-debug", currentCharacterSet.getProgressTracker().debugHistory());
                Toast.makeText(this, currentCharacterSet.getProgressTracker().debugHistory(), Toast.LENGTH_LONG * 5).show();

            } else if(item.getTitle().equals("DEBUG:DebugJSON")){
                try {
                    Log.i("nakama-debug", stateLog());
                } catch (IOException e) {
                    Log.e("nakama-debug", "Error doing debug JSON", e);
                }
            }
        }

        return true;
    }

    private void showReportBugDialog() {
        FragmentManager fm = getSupportFragmentManager();
        ReportBugDialog d = new ReportBugDialog();
        Bundle b = new Bundle();
        try {
            b.putString("debugData", stateLog());
        } catch (Throwable e) {
            UncaughtExceptionLogger.backgroundLogError("Error generating debug state", e);
        }
        d.setArguments(b);
        if(!isFinishing()) {
            d.show(fm, "fragment_report_bug");
        }
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


    private int currentNavigationItem = -1;
    private ClueCard.ClueType currentSetClueType = ClueCard.ClueType.MEANING;

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Log.i("nakama", "onNavigationItemSelected: " + itemPosition);
        if(currentNavigationItem == itemPosition){
            return false;
        }

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

        if(itemPosition >= 14 &&  itemPosition < 14 + customSets.size()){
            int customSetIndex = itemPosition - 14;
            this.currentCharacterSet = customSets.get(customSetIndex);
        }


        this.currentSetClueType = Settings.getCharsetClueType(this.currentCharacterSet.pathPrefix, getApplicationContext());
        instructionCard.setClueType(this.currentSetClueType);
        instructionCard.setClueTypeChangeListener(new ClueCard.ClueTypeChangeListener() {
            @Override public void onClueTypeChange(ClueCard.ClueType c) {
                Log.i("nakama", "Setting clue type for set " + currentCharacterSet.name + " to " + c);
                Settings.setCharsetClueType(currentCharacterSet.pathPrefix, c, getApplicationContext());
            }

        });

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

        // force a next recalculation due to SRS global. Otherwise might get stuck
        // redoing same char you just did in previous set.
        if(currentNavigationItem != -1 && currentState == State.DRAWING) {
            loadNextCharacter(true);
        }


        if (this.charSetFrag != null) {
            this.charSetFrag.setCharset(this.currentCharacterSet);
        }

        if (!(itemPosition == 0 || itemPosition == 2 || itemPosition == 9 || itemPosition >= 14)) {
            raisePurchaseDialog(PurchaseDialog.DialogMessage.START_OF_LOCKED_SET, Frequency.ONCE_PER_SESSION);
        }

        resumePracticeLogsCurrentCharset();

        // this can happen when rotating, after onCreate, but before onResume, this method is called. In future,
        /// move loadNextCharacter calls to end of onCreate?
        if(currentCharacterSet.currentCharacter() == null){
            loadNextCharacter(false);
        }

        saveCurrentCharacterSet();


        drawPad.clear();
        setUiState(State.DRAWING);

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
    public void overRide(OverrideDialog.OverideType type, boolean logData) {
        if(BuildConfig.DEBUG) Toast.makeText(this, "Override " + lastGradingRow, Toast.LENGTH_LONG).show();
        String practiceLogId = currentCharacterSet.overRideLast();

        if(type == OVERRIDE_TO_CORRECT){
            setUiState(State.CORRECT_ANSWER);
        }
        if(type == OVERRIDE_TO_INCORRECT){
            setUiState(State.REVIEWING);
        }

        if(logData) {
            try {
                StringWriter sw = new StringWriter();
                JsonWriter j = new JsonWriter(sw);
                j.setLenient(true);

                j.beginObject();

                j.name("logId").value(practiceLogId);
                j.name("character").value(currentCharacterSet.currentCharacter().toString());
                j.name("setId").value(currentCharacterSet.pathPrefix);
                j.name("setName").value(currentCharacterSet.name);
                j.name("overrideType").value(type.toString());

                j.name("drawn");
                drawPad.getDrawing().serialize(j);

                j.endObject();

                UncaughtExceptionLogger.backgroundLogOverride(sw.toString());

            } catch (Throwable t) {
                UncaughtExceptionLogger.backgroundLogError("Error generating override log", t);
            }
        }
    }

    @Override
    public void setGoal(int year, int month, int day) {
        charSetFrag.setGoal(year, month, day);
    }
}
