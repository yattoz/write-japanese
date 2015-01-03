package dmeeuwis.masterlibrary;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import android.content.Context;
import android.util.Log;
import dmeeuwis.util.Util;

/**
 * Holds a set of characters to study, and all current user study progress on those characters.
 */
public abstract class CharacterStudySet implements Iterable<Character> {
	
	public static final String PREFS_KEY = "kanjiProgress";
	public static enum Order { IN_ORDER, RANDOM_ORDER }
	public static enum LockLevel { NULL_LOCK, LOCKED, UNLOCKABLE, UNLOCKED }

	final public Set<Character> freeCharactersSet;
	final public Set<Character> allCharactersSet;
	final public String name;
	
	final private LockChecker lockChecker;
	final private Random random = new Random();

	private ProgressTracker tracker;
	private boolean shuffling = false;
	private Character currentChar;
	private boolean reviewing = false;
	private LockLevel locked;
	public final String pathPrefix;
	
	public String toString(){
		return String.format("%s (%d)", this.name, this.allCharactersSet.size());
	}

	public CharacterStudySet(String name, String pathPrefix, LockLevel locked, String allCharacters, String freeCharacters, LockChecker lockChecker){
		this.name = name;
		this.locked = locked;

		this.freeCharactersSet = Collections.unmodifiableSet(new HashSet<Character>(Collections.unmodifiableList(Util.stringToCharList(freeCharacters))));
		this.allCharactersSet = Collections.unmodifiableSet(new HashSet<Character>(Collections.unmodifiableList(Util.stringToCharList(allCharacters))));

		this.pathPrefix = pathPrefix;
		this.tracker = new ProgressTracker(this.allCharactersSet);
		this.lockChecker = lockChecker;
		
		nextCharacter();
	}
	
	public boolean locked(){
		boolean globalLock = lockChecker.getPurchaseStatus() == LockLevel.LOCKED;
		boolean localLock = this.locked == LockLevel.LOCKED;
		Log.d("nakama", "CharacterStudySet: globalLock: " + globalLock + "; localLock: " + localLock + " => " + (globalLock && localLock));
		return globalLock && localLock;
	}
	
	public Set<Character> availableCharactersSet(){
		return locked() ? freeCharactersSet : allCharactersSet;
	}

	public String charactersAsString(){
		return Util.join("", allCharactersSet);
	}
	
	abstract public String label();
	abstract public String[] currentCharacterClues();
	
	public void back() {
	}
	
	public boolean passedAllCharacters(){
		return this.tracker.passedAllCharacters(availableCharactersSet());
	}
	
	public Character currentCharacter(){
		return this.currentChar;
	}
	
	@Override
	public Iterator<Character> iterator() {
		return (availableCharactersSet()).iterator();
	}

	public Map<Character, ProgressTracker.Progress> getAllRatings(){
    	return this.tracker.getAllScores();
	}
	
	public void markCurrent(boolean pass){
		Character c = currentCharacter();
		try {
			if(pass){
				this.tracker.markSuccess(c);
			} else {
				this.tracker.markFailure(c);
			}
		} catch(Throwable t){
			Log.e("nakama", "Error when marking character " + c + " from character set " + Util.join(", ", this.allCharactersSet) + "; tracker is " + tracker);
			throw new RuntimeException(t);
		}
	}
	
	public void markCurrentAsUnknown(){
		this.tracker.markFailure(currentCharacter());
		nextCharacter();
	}
	
	public void setShuffle(boolean isShuffle){
		this.shuffling = isShuffle;
	}
	
	public boolean isShuffling(){
		return this.shuffling;
	}
	
	public void progressReset(Context context){
		this.tracker.progressReset();
		
        CharacterProgressDataHelper cdb = new CharacterProgressDataHelper(context);
		try {
			cdb.clearProgress(pathPrefix);
		} finally { 
			cdb.close();
		}
	}
	
	public void skipTo(Character character){
		if(availableCharactersSet().contains(character)){
			this.currentChar = character;
		}
	}
	
	public void nextCharacter(){
        Log.i("nakama", "Selection: nextCharacter");
		double ran = random.nextDouble();
        
        Log.i("nakama", "Selection: Next character ran is " + ran);

        this.reviewing = true; 
       	int charactersSeen = tracker.getAllScores().size();
       	this.currentChar = null;
        if(tracker.charactersNotYetSeen(availableCharactersSet()).size() > 0){

        	try {
        		// 35% chance of mistaken character
        		if(ran <= 0.35){
        			this.currentChar = tracker.randomMistakenNext(availableCharactersSet());
        			Log.i("nakama", "Still learning progression: reviewing mistaken character " + this.currentChar);
        		
       			// 15% chance of reviewing character
        		} else if(ran <= 0.40){
        			this.currentChar = tracker.randomReviewingNext(availableCharactersSet());
        			Log.i("nakama", "Still learning progression: reviewing review character " + this.currentChar);
        		}
        	} finally {
        		// 50% chance of new character
        		if(this.currentChar == null){
        			this.reviewing = false; 		// this is only case of not-reviewing
        			this.currentChar = this.shuffling ? 
        					tracker.shuffleNext(availableCharactersSet()) :
       						tracker.standardNext(availableCharactersSet());
        			Log.i("nakama", "Still learning progression: introducing new character " + this.currentChar);
        		}
        	}
        	
        } else {

        	try {
        		// 40% chance of previously mistaken character
        		if(ran <= 0.40 && charactersSeen > 0){
        			this.currentChar = tracker.randomReviewingNext(availableCharactersSet());
        			Log.i("nakama", "Known set progression: reviewing mistaken character " + this.currentChar);
        		
       			// 15% chance of reviewing character
        		} else if(ran <= 0.80 && charactersSeen > 0){
        			this.currentChar = tracker.randomReviewingNext(availableCharactersSet());
        			Log.i("nakama", "Known set progression: reviewing review character " + this.currentChar);
        	
        		}
        	} finally {
        		if(this.currentChar == null){
        			this.currentChar = tracker.randomCorrectNext(availableCharactersSet());
        			Log.i("nakama", "Known set progression: reviewing previously correct character " + this.currentChar);
        		}
        	}
        }
	}
	
	public boolean isReviewing(){
		return this.reviewing;
	}

	public void save(Context context){
		String progressAsString = tracker.saveToString();

        CharacterProgressDataHelper cdb = new CharacterProgressDataHelper(context);
		try {
			//Log.i("nakama", "For charset " + pathPrefix + " writing existing progress as: " + progressAsString);
			cdb.recordProgress(pathPrefix, progressAsString);
		} finally {
			cdb.close();
		}
	}

	public void load(Context context){
		String existingProgress;
        CharacterProgressDataHelper cdb = new CharacterProgressDataHelper(context);
		try {
			existingProgress = cdb.getExistingProgress(pathPrefix);
			cdb.close();
		} finally {
			cdb.close();
		}
		// Log.i("nakama", "For charset " + pathPrefix + " read existing progress as: " + existingProgress);

		if(existingProgress == null){
			tracker = new ProgressTracker(this.allCharactersSet);
		} else {
			tracker = ProgressTracker.loadFromString(existingProgress);
		}
	}
}
