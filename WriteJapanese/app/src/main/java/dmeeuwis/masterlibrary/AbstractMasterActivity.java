package dmeeuwis.masterlibrary;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher.ViewFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import dmeeuwis.Translation;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.masterlibrary.CharacterStudySet.LockLevel;
import dmeeuwis.masterlibrary.KanjiTranslationListAsyncTask.AddTranslation;
import dmeeuwis.masterlibrary.PurchaseDialog.DialogMessage;
import dmeeuwis.nakama.data.AssetFinder;
import dmeeuwis.nakama.helpers.DictionarySet;
import dmeeuwis.nakama.kanjidraw.Criticism;
import dmeeuwis.nakama.kanjidraw.Drawing;
import dmeeuwis.nakama.kanjidraw.Glyph;
import dmeeuwis.nakama.kanjidraw.PathComparator;
import dmeeuwis.nakama.library.Constants;
import dmeeuwis.nakama.views.Animatable;
import dmeeuwis.nakama.views.AnimatedCurveView;
import dmeeuwis.nakama.views.AnimatedCurveView.DrawTime;
import dmeeuwis.nakama.views.DrawView;
import dmeeuwis.nakama.views.FloatingActionButton;
import dmeeuwis.util.Util;

// import com.nhaarman.listviewanimations.swinginadapters.prepared.ScaleInAnimationAdapter;

public abstract class AbstractMasterActivity extends ActionBarActivity implements ActionBar.OnNavigationListener, LockCheckerHolder {
	
	public enum State { DRAWING, REVIEWING, CORRECT_ANSWER, INCORRECT_ANSWER }
	
	protected CharacterStudySet currentCharacterSet;
	
	protected StoryDataHelper db;
	
	// ui references
    protected DrawView drawPad;
    protected AnimatedCurveView correctAnimation;
    protected AnimatedCurveView playbackAnimation;
    
    protected IncorrectScreenView incorrectScreen;
    
    protected AnimatedCurveView correctDrawnView;
    protected AnimatedCurveView correctKnownView;
    
    protected KanjiTranslationListAsyncTask vocabAsync;
    protected RecyclerView correctVocabList;
    protected KanjiVocabRecyclerAdapter correctVocabArrayAdapter;
    
    protected ViewFlipper flipper;
	protected FlipperAnimationListener flipperAnimationListener;
	protected View maskView;
    
    protected View drawFrame, reviewFrame;
    
    protected LinearLayout correctionsAnimationArea;
    protected FloatingActionButton remindStoryButton;
    protected ImageView otherMeaningsButton;
    protected ListView criticism;
    protected ArrayAdapter<String> criticismArrayAdapter;
    
    protected ImageView reviewBug;
    
    protected TextSwitcher instructionsLabel;
    protected TextSwitcher target;
   
    protected String[] currentCharacterSvg;
    
	protected SharedPreferences prefs;

	protected DictionarySet dictionarySet;
	
	protected LockChecker lockChecker;
	
	protected LinkedHashMap<String, CharacterStudySet> characterSets = new LinkedHashMap<String, CharacterStudySet>();
	
	private int currentCharacterClueIndex = 0;
	
	static private final int BLUE_COLOR = 0xffD7FFD7;
	static private final int GREEN_COLOR = 0xffE4E8FF;
	
	static private final int DONE_BUTTON_COLOR = GREEN_COLOR;
	static private final int NEXT_BUTTON_COLOR = GREEN_COLOR;
	static private final int PRACTICE_BUTTON_COLOR = BLUE_COLOR;
	static private final int REMIND_BUTTON_COLOR = BLUE_COLOR;
	static private final int TEACH_BUTTON_COLOR = BLUE_COLOR;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        long startTime = System.currentTimeMillis();
		Log.i("nakama", "MainActivity: onCreate starting.");
        super.onCreate(savedInstanceState);
        
        this.prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        
		Log.i("nakama", "MainActivity: onCreate about to make LockChecker.");
        lockChecker = new LockChecker(this, 
        		new Runnable(){
        			@Override public void run() {
        				Log.i("nakama",  "PurchaseDialog: onDismissRunnable from raisePurchaseDialog.");
						if(lockChecker.getPurchaseStatus() == LockLevel.UNLOCKED){
							Log.i("nakama",  "PurchaseDialog: onDismissRunnable from raisePurchaseDialog: saw unlocked result, updating menu.");
							getSupportActionBar().setSelectedNavigationItem(2);
							supportInvalidateOptionsMenu();
						}
					}
				});
		Log.i("nakama", "MainActivity: onCreate made LockChecker.");
        
        setContentView(R.layout.main);
        

        this.dictionarySet = DictionarySet.singleton(this);
		Log.i("nakama", "MainActivity: onCreate, loading dictionary set took " + (System.currentTimeMillis() - startTime) + "ms.");
       
		Animation outToLeft = AnimationUtils.loadAnimation(this,  R.anim.screen_transition_out);
        
        flipper = (ViewFlipper)findViewById(R.id.viewflipper);
		flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.screen_transition_in));
		flipper.setOutAnimation(outToLeft); 
		flipper.setAnimationCacheEnabled(true);
        
        flipperAnimationListener = new FlipperAnimationListener();
        outToLeft.setAnimationListener(flipperAnimationListener);
        
        maskView = (View)findViewById(R.id.maskView);
        
        // Draw Frame init
        drawFrame = findViewById(R.id.drawingFrame); 
        drawPad = (DrawView)findViewById(R.id.drawPad);
        drawPad.setBackgroundColor(DrawView.BACKGROUND_COLOR);
        
        reviewBug = (ImageView)findViewById(R.id.reviewBug);
       
        target = (TextSwitcher)findViewById(R.id.target);
        target.setInAnimation(AnimationUtils.loadAnimation(this,android.R.anim.slide_in_left));
        target.setOutAnimation(AnimationUtils.loadAnimation(this,android.R.anim.slide_out_right));
        
        correctKnownView = (AnimatedCurveView)findViewById(R.id.correctKnownView);
        correctDrawnView = (AnimatedCurveView)findViewById(R.id.correctDrawnView);

        instructionsLabel = (TextSwitcher)findViewById(R.id.instructionsLabel); 
        instructionsLabel.setInAnimation(AnimationUtils.loadAnimation(this,android.R.anim.slide_in_left));
        instructionsLabel.setOutAnimation(AnimationUtils.loadAnimation(this,android.R.anim.slide_out_right));

        
        target.setFactory(new ViewFactory(){
			@Override public View makeView() {
				final TextView t = new TextView(AbstractMasterActivity.this);
				t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
				t.setSingleLine();
				t.setEllipsize(TruncateAt.END);
				t.setGravity(Gravity.CENTER);
                t.setTextColor(Color.BLACK);
				t.setOnClickListener(new OnClickListener() {
					@Override public void onClick(View v) {
						Layout layout = t.getLayout();
						if(layout != null && layout.getLineCount() > 0){
							if(layout.getEllipsisCount(0) > 0){
								String[] clues = currentCharacterSet.currentCharacterClues();
								Toast.makeText(AbstractMasterActivity.this, clues[currentCharacterClueIndex], Toast.LENGTH_LONG).show();
							}
						}
					}
				});
				return t;
			}
        });
        
        instructionsLabel.setFactory(new ViewFactory(){
			@Override public View makeView() {
				TextView t = new TextView(AbstractMasterActivity.this);
				t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
				t.setSingleLine();
				t.setMaxLines(1);
				t.setGravity(Gravity.CENTER);
				return t;
			}
        });


        final FloatingActionButton doneButton = (FloatingActionButton)findViewById(R.id.finishedButton);
        doneButton.hideInstantly();
        doneButton.setFloatingActionButtonColor(getResources().getColor(R.color.DarkGreen));
        doneButton.setFloatingActionButtonDrawable(getResources().getDrawable(R.drawable.ic_check_mark));
        doneButton.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View arg0) {
				if(drawPad.getStrokeCount() == 0){
					return;
				}
				
				final Drawing challenger = drawPad.getDrawing();
				final Glyph known = new Glyph(currentCharacterSvg);
				
				PathComparator comparator = new PathComparator(currentCharacterSet.currentCharacter(), known, challenger,  new AssetFinder(AbstractMasterActivity.this.getAssets()));
				final Criticism critique = comparator.compare();
				
				currentCharacterSet.markCurrent(critique.pass);
				
				if(critique.pass){
					correctKnownView.setDrawing(known, DrawTime.STATIC);
					correctDrawnView.setDrawing(challenger, DrawTime.STATIC);
					
					setUiState(State.CORRECT_ANSWER);

					if(vocabAsync != null){
						vocabAsync.cancel(true);
						correctVocabArrayAdapter.clear();
					}
					Log.i("nakama", "VOCAB: Starting vocab async task.");

					AddTranslation adder = new AddTranslation(){
						public void add(Translation t){
							correctVocabArrayAdapter.add(t);
						}
					};
					vocabAsync = new KanjiTranslationListAsyncTask(adder, dictionarySet, currentCharacterSet.currentCharacter());
					vocabAsync.execute();
					
					
				} else {
					Log.d("nakama", "Setting up data for incorrect results critique.");
					correctAnimation.setDrawing(known, DrawTime.ANIMATED);
					playbackAnimation.setDrawing(challenger, DrawTime.ANIMATED);
                        
					incorrectScreen.setInformation(known, challenger, currentCharacterSet.currentCharacter());
					
					criticismArrayAdapter.clear();
					for(String c: critique.critiques){
						criticismArrayAdapter.add(c);
					}

					setUiState(State.INCORRECT_ANSWER);
				}
			}
		});
        
        db = new StoryDataHelper(getApplicationContext());
        remindStoryButton = (FloatingActionButton)findViewById(R.id.remindStoryButton);
        remindStoryButton.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				Toast.makeText(getApplicationContext(), db.getStory(currentCharacterSet.currentCharacter()), Toast.LENGTH_LONG).show();
			}
		});
        
        otherMeaningsButton = (ImageView)findViewById(R.id.other_meanings);
        otherMeaningsButton.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				String[] clues = currentCharacterSet.currentCharacterClues();
				currentCharacterClueIndex = (currentCharacterClueIndex + 1) % clues.length;
				target.setText(clues[currentCharacterClueIndex]);
		        instructionsLabel.setText(currentCharacterClueIndex == 0 ? 
	        			"Draw the " + currentCharacterSet.label() + " for" :
	        			"which can also mean");

			}
		});
        
        final FloatingActionButton teachMeButton = (FloatingActionButton)findViewById(R.id.teachButton);
        teachMeButton.setFloatingActionButtonColor(getResources().getColor(R.color.Blue));
        teachMeButton.setFloatingActionButtonDrawable(getResources().getDrawable(R.drawable.ic_question_mark));
        teachMeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                char c = currentCharacterSet.currentCharacter();
                currentCharacterSet.markCurrentAsUnknown();
                goToTeachingActivity(c);
            }
        });

        // Review Frame init
        reviewFrame = findViewById(R.id.reviewingFrame);
        
        correctAnimation = (AnimatedCurveView)findViewById(R.id.animatedKnownReplay);
        playbackAnimation = (AnimatedCurveView)findViewById(R.id.animatedDrawnReplay);
        
        correctAnimation.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				playbackAnimation.startAnimation(0);
			}
		});
        playbackAnimation.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				correctAnimation.startAnimation(0);
			}
		});
        
        criticism = (ListView)findViewById(R.id.criticism);
        criticismArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<String>(0));
        criticism.setAdapter(criticismArrayAdapter);
        
        OnClickListener nextButtonListener = new OnClickListener() {
			@Override public void onClick(View arg0) {
		        drawPad.clear();
				setUiState(State.DRAWING);
				loadNextCharacter(true);
			}
             };

        final FloatingActionButton next = (FloatingActionButton)findViewById(R.id.nextButton);
        next.setOnClickListener(nextButtonListener);
        next.setFloatingActionButtonColor(getResources().getColor(R.color.DarkGreen));
        next.setFloatingActionButtonDrawable(getResources().getDrawable(R.drawable.ic_right_arrow));

        final FloatingActionButton correctNext = (FloatingActionButton)findViewById(R.id.correctNextButton);
        correctNext.setFloatingActionButtonColor(getResources().getColor(R.color.DarkGreen));
        correctNext.setFloatingActionButtonDrawable(getResources().getDrawable(R.drawable.ic_right_arrow));
        correctNext.setOnClickListener(nextButtonListener);
        
        final FloatingActionButton practiceButton = (FloatingActionButton)findViewById(R.id.practiceButton);
        practiceButton.setFloatingActionButtonColor(getResources().getColor(R.color.Blue));
        practiceButton.setFloatingActionButtonDrawable(getResources().getDrawable(R.drawable.ic_question_mark));
        practiceButton.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				goToTeachingActivity(currentCharacterSet.currentCharacter());
			}
		});
       

        incorrectScreen = (IncorrectScreenView)findViewById(R.id.incorrectFrame);
 
        correctVocabList = (RecyclerView)findViewById(R.id.correctExamples);
        correctVocabList.setLayoutManager(new LinearLayoutManager(this));
  		correctVocabArrayAdapter = new KanjiVocabRecyclerAdapter(this, this.dictionarySet.kanjiFinder());
        correctVocabList.setAdapter(correctVocabArrayAdapter);

        drawPad.setOnStrokeListener(new DrawView.OnStrokeListener(){
            @Override public void onStroke(List<Point> stroke){
                if(drawPad.getStrokeCount() == 1){
                    doneButton.showFloatingActionButton();
                    teachMeButton.hideFloatingActionButton();
                }
            }
        });

        drawPad.setOnClearListener(new DrawView.OnClearListener(){
            @Override public void onClear(){
                if(teachMeButton.isHidden())
                    teachMeButton.showFloatingActionButton();
                if(!doneButton.isHidden())
                    doneButton.hideFloatingActionButton();

                String story = db.getStory(currentCharacterSet.currentCharacter());
                if(story != null && !"".equals(story.trim()))
                    remindStoryButton.setVisibility(View.VISIBLE);
                else
                    remindStoryButton.setVisibility(View.GONE);
            }
        });

        // child implementation needs to call loadNextCharacter(false) to start data loading.
		Log.i("nakama", "MainActivity: onCreate finishing. Took " + (System.currentTimeMillis() - startTime) + "ms.");
    }
    
	public void setUiState(State requestedState){
		Log.i("nakama", "setUiState " + requestedState);
		
		if(requestedState.ordinal() == flipper.getDisplayedChild())
			return;
        
		if(requestedState == State.DRAWING){
	        correctAnimation.stopAnimation();
			playbackAnimation.stopAnimation();
			
			drawPad.clear();
			
			flipper.setDisplayedChild(State.DRAWING.ordinal());
		
		} else if(requestedState == State.CORRECT_ANSWER){
			Log.d("nakama", "In CORRECT_ANSWER state change; starting flip");
			flipperAnimationListener.transferTo = null;
			flipperAnimationListener.incrementProgress = true;
			flipper.setDisplayedChild(State.CORRECT_ANSWER.ordinal());
		
		} else if(requestedState == State.INCORRECT_ANSWER){
			Log.d("nakama", "In INCORRECT_ANSWER state change; starting flip");
			flipperAnimationListener.transferTo = State.REVIEWING;
			flipper.setDisplayedChild(State.INCORRECT_ANSWER.ordinal());
	        
		} else if(requestedState == State.REVIEWING){
			Log.d("nakama", "In REVIEWING state change; starting flip");
			flipperAnimationListener.animateOnFinish = new Animatable[] { correctAnimation, playbackAnimation };
			flipper.setDisplayedChild(State.REVIEWING.ordinal());
		}
	}
	
	public void goToTeachingActivity(char character){
		Intent teachIntent = new Intent(this, TeachingActivity.class);
		Bundle params = new Bundle();
		params.putString("parent", this.getClass().getName());
		params.putChar(Constants.KANJI_PARAM, character);
		params.putString(Constants.KANJI_PATH_PARAM, currentCharacterSet.pathPrefix);
		teachIntent.putExtras(params);
		startActivity(teachIntent);
	}
   
    @Override public void onBackPressed(){
    	State currentUiState = State.values()[flipper.getDisplayedChild()];
    	
    	if(currentUiState == State.DRAWING){
	    	if(this.drawPad.getStrokeCount() > 0){
	    		this.drawPad.undo();
	    	} else {
                super.onBackPressed();
	    	}
    	} else {
    		this.setUiState(State.DRAWING);
    	}
    }


	static boolean showedEndOfSetDialog = false;
	static boolean showedStartOfSetDialog = false;
	
	public enum Frequency { ALWAYS, ONCE_PER_SESSION }
	
    PurchaseDialog pd;
    public void raisePurchaseDialog(PurchaseDialog.DialogMessage message, Frequency freq){
    	if(message == DialogMessage.START_OF_LOCKED_SET){
    		if(showedStartOfSetDialog){
    			return;
    		} else {
    			showedStartOfSetDialog = true;
    		}
    	}
    	
    	if(message == DialogMessage.END_OF_LOCKED_SET){
    		if(showedEndOfSetDialog){
    			return;
    		} else {
    			showedEndOfSetDialog = true;
    		}
    	}
    	
		if(lockChecker.getPurchaseStatus() == LockLevel.LOCKED && (freq == Frequency.ALWAYS || (freq == Frequency.ONCE_PER_SESSION && pd == null))){
			try {
				if(pd == null){
					pd = PurchaseDialog.make(message);
				}
				pd.show(this.getSupportFragmentManager(), "purchase");
			} catch(Throwable t){
				Log.e("nakama", "Caught fragment error when trying to show PurchaseDialog.", t);
			}
		} else {
			Log.i("nakama", "Skipping purchase fragment.");
		}
    }

	public void loadNextCharacter(boolean increment){
		Log.i("nakama", "loadNextCharacter(" + increment + ")");
		
		// push before-next character onto back-stack
		Character priorCharacter = currentCharacterSet.currentCharacter();
	
		if(priorCharacter == null && !increment){
			loadNextCharacter(true);
			return;
		}

        // TODO: improve this... show a page, give congratulations...? 
		if(increment && currentCharacterSet.locked() && currentCharacterSet.passedAllCharacters()){
			if(currentCharacterSet.locked() && lockChecker.getPurchaseStatus() == LockLevel.LOCKED){
				raisePurchaseDialog(PurchaseDialog.DialogMessage.END_OF_LOCKED_SET, Frequency.ONCE_PER_SESSION);
			} else {
				Toast.makeText(this, "You have completed all the kana in this set!", Toast.LENGTH_LONG).show();
			}
		}
		
        if(increment){
	    	this.reviewBug.setVisibility(View.GONE);
        	currentCharacterSet.nextCharacter();
        	if(currentCharacterSet.isReviewing())
        		this.reviewBug.setVisibility(View.VISIBLE);
        	Log.d("nakama", "Incremented to next character " + currentCharacterSet.currentCharacter());
        }
        
        String story = db.getStory(currentCharacterSet.currentCharacter());
        if(story == null || "".equals(story.trim())){
        	remindStoryButton.setVisibility(View.GONE);
        } else {
        	remindStoryButton.setVisibility(View.VISIBLE);
        }
        
        this.loadDrawDetails();
	}
	
	private void loadDrawDetails(){
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
        if(clues.length == 1){
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
	
	public static final String CHAR_SET = "currCharSet";
	public static final String CHAR_SET_CHAR = "currCharSetChar";
	private void saveCurrentUsingCharacterSet(){
		Editor ed = this.prefs.edit();
		Log.i("nakama", "AbstractMaster: saveCurrentUsingCharacterSet : writing " + CHAR_SET + " to " + this.currentCharacterSet.pathPrefix);
		ed.putString(CHAR_SET, this.currentCharacterSet.pathPrefix);
		ed.putString(CHAR_SET_CHAR, Character.toString(this.currentCharacterSet.currentCharacter()));
		ed.commit();
	}
	
	@SuppressLint("NewApi")
	private void loadCurrentCharacterSet(){
		
		// current character set
		String set = this.prefs.getString(CHAR_SET, "j1");
		this.currentCharacterSet = this.characterSets.get(set);
		Log.i("nakama", "loadCurrentCharacterSet: setting to " + set + " (" + this.currentCharacterSet.pathPrefix + ")");
		
		// current character
		String currChar = this.prefs.getString(CHAR_SET_CHAR, null);
		if(currChar != null){
			this.currentCharacterSet.skipTo(currChar.charAt(0));
		}
		
		// shuffle setting
        currentCharacterSet.setShuffle(this.prefs.getBoolean("shuffleEnabled", false));
        
        try {
        	invalidateOptionsMenu();
        } catch(Throwable t){
        	Log.d("nakama", "Ignore error during invalidateOptionsMenu; must be older android device.");
        	// ignore for older devices.
        }
	}

	@Override public void onResume(){
		Log.i("nakama", "AbstractMaster.onResume");
		
		loadCurrentCharacterSet();
		currentCharacterSet.load(this);

		// update tab navigation dropdown to selected characterset
		String[] charSetNames = this.characterSets.keySet().toArray(new String[0]);
		for(int i = 0; i < this.characterSets.keySet().size(); i++){
			String currCharsetName = characterSets.get(charSetNames[i]).pathPrefix;
			if(currCharsetName.equals(this.currentCharacterSet.pathPrefix)){
				getSupportActionBar().setSelectedNavigationItem(i);
				break;
			}
		}
		loadNextCharacter(false);
		
		super.onResume();
	}

	@Override 
	public void onPause(){
		Log.i("nakama", "AbstractMaster.onPause: saving state.");
		drawPad.stopAnimation();
		Editor ed = this.prefs.edit();
        ed.putBoolean("shuffleEnabled", currentCharacterSet.isShuffling());
        ed.commit();
		currentCharacterSet.save(this);
		saveCurrentUsingCharacterSet();
		if(pd != null){
			pd.dismiss();
		}
		db.close();
		super.onPause();
	}
	
	protected class FlipperAnimationListener implements AnimationListener {
		public State transferTo = State.DRAWING;
		public boolean incrementProgress = false;
		
		public Animatable[] animateOnFinish = new Animatable[0];
		public Runnable runOnAnimationEnd;
		
		@Override public void onAnimationStart(Animation animation) {
			maskView.setOnTouchListener(new OnTouchListener() {
				@SuppressLint("ClickableViewAccessibility") 
				@Override public boolean onTouch(View v, MotionEvent event) {
					Log.i("nakama", "Ignoring touch event due to flipper animation.");
					return true;
				}
			});
		}

		@Override public void onAnimationEnd(Animation animation) {
			Log.i("nakama", "Flip animation finished.");
			
			maskView.setOnTouchListener(new OnTouchListener() {
				@SuppressLint("ClickableViewAccessibility") 
				@Override public boolean onTouch(View v, MotionEvent event) {
					return false;
				}
			});
		
			if(runOnAnimationEnd != null){
				runOnAnimationEnd.run();
				runOnAnimationEnd = null;
			}
			
			for(Animatable a: animateOnFinish){
				a.startAnimation(100);
			}
			animateOnFinish = new Animatable[0];
			
			if(incrementProgress){
				loadNextCharacter(true);
				incrementProgress = false;
			}
			
			if(transferTo != null){
				setUiState(transferTo);
				transferTo = null;
			}
		}

		@Override public void onAnimationRepeat(Animation animation) {
			// nothing to do
		} 
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
    
    public LockChecker getLockChecker(){
    	return this.lockChecker;
    }
}
