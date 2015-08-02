package dmeeuwis.nakama.data;

import android.content.Context;
import android.util.Log;

import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import dmeeuwis.nakama.LockChecker;
import dmeeuwis.util.Util;

/**
 * Holds a set of characters to study, and all current user study progress on those characters.
 */
public abstract class CharacterStudySet implements Iterable<Character> {

	public enum LockLevel { NULL_LOCK, LOCKED, UNLOCKABLE, UNLOCKED }

	final public Set<Character> freeCharactersSet;
	final public Set<Character> allCharactersSet;
	final public String name, description;

	final private LockChecker lockChecker;
    private ProgressTracker tracker;
	final private Random random = new Random();

	private boolean shuffling = false;
	private Character currentChar;
	private boolean reviewing = false;
	private LockLevel locked;
	public final String pathPrefix;

	private GregorianCalendar studyGoal, goalStarted;

    public static class SetProgress {
        public final int passed;
        public final int reviewing;
        public final int failing;
        public final int unknown;

        public SetProgress(int passed, int reviewing, int failing, int unknown){
            this.passed = passed;
            this.reviewing = reviewing;
            this.failing = failing;
            this.unknown = unknown;
        }
    }

    public static int daysDifference(GregorianCalendar a, GregorianCalendar b){
        return (int)(Math.abs(a.getTimeInMillis() - b.getTimeInMillis()) / (1000 * 60 * 60 * 24));
    }

    public static class GoalProgress {
        public final GregorianCalendar goal, goalStarted;
        public final int passed, remaining, daysLeft;
        public final int perDay, expected;

        public GoalProgress(GregorianCalendar goalStarted, GregorianCalendar goal, SetProgress s, GregorianCalendar today){
            this.goalStarted = goalStarted;
            this.remaining = s.failing + s.reviewing + s.unknown;
            this.passed = s.passed;
            this.goal = goal;

            if(this.goal.before(today)){
                daysLeft = 0;
            } else {
                daysLeft = Math.max(1, daysDifference(goal, today));
            }
            this.perDay = remaining / daysLeft;

            int totalDays = daysDifference(goalStarted, goal);
            this.expected = (s.failing + s.unknown + s.reviewing + s.passed) / totalDays;

            Log.i("nakama", "Goal Calcs; Start: " + goalStarted + ", goal: " + goal + "; remaining: " + remaining + "; daysLeft: " + daysLeft + "; remaining: " + remaining);
        }
    }


	public String toString(){
		return String.format("%s (%d)", this.name, this.allCharactersSet.size());
	}

	public CharacterStudySet(String name, String description, String pathPrefix, LockLevel locked, String allCharacters, String freeCharacters, LockChecker lockChecker){
		this.name = name;
        this.description = description;
		this.locked = locked;

		this.freeCharactersSet = Collections.unmodifiableSet(new LinkedHashSet<>(Util.stringToCharList(freeCharacters)));
		this.allCharactersSet = Collections.unmodifiableSet(new LinkedHashSet<>(Util.stringToCharList(allCharacters)));

		this.pathPrefix = pathPrefix;
		this.tracker = new ProgressTracker(this.allCharactersSet);
		this.lockChecker = lockChecker;

		nextCharacter();
	}


    public boolean hasStudyGoal(){
        return this.studyGoal != null;
    }

    public void setStudyGoal(GregorianCalendar g){
        this.studyGoal = g;
        this.goalStarted = new GregorianCalendar();
    }

    public GoalProgress getGoalProgress(){
        if(this.studyGoal == null){ return null; }
        SetProgress s = this.getProgress();
        return new GoalProgress(this.goalStarted, this.studyGoal, s, new GregorianCalendar());
    }


    public SetProgress getProgress(){
        return this.tracker.calculateProgress();
    }

    public void setProgressNotifications(boolean enable){

    }

	public boolean locked(){
        if(lockChecker == null){ return false; }
		boolean globalLock = lockChecker.getPurchaseStatus() == LockLevel.LOCKED;
		boolean localLock = this.locked == LockLevel.LOCKED;
		// Log.d("nakama", "CharacterStudySet: globalLock: " + globalLock + "; localLock: " + localLock + " => " + (globalLock && localLock));
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
        cdb.clearProgress(pathPrefix);
	}

	public void skipTo(Character character){
		if(availableCharactersSet().contains(character)){
			this.currentChar = character;
		}
	}

	public void nextCharacter(){
		double ran = random.nextDouble();
        this.reviewing = true;
       	int charactersSeen = tracker.getAllScores().size();
        Set<Character> availSet = new LinkedHashSet<>(availableCharactersSet());
        availSet.remove(this.currentChar);
        Character next = null;
        if(tracker.charactersNotYetSeen(availSet).size() > 0){

        	try {
        		// 35% chance of mistaken character
        		if(ran <= 0.35){
        			next = tracker.randomMistakenNext(availSet);
        			Log.i("nakama", "CharacterStudySe: Still learning progression: reviewing mistaken character " + next);

       			// 15% chance of reviewing character
        		} else if(ran <= 0.40){
        			next = tracker.randomReviewingNext(availSet);
        			Log.i("nakama", "CharacterStudySe: Still learning progression: reviewing review character " + next);
        		}
        	} finally {
        		// 50% chance of new character
        		if(next == null){
        			this.reviewing = false; 		// this is only case of not-reviewing
        			next = this.shuffling ?
        					tracker.shuffleNext(availSet) :
       						tracker.standardNext(availSet);
        			Log.i("nakama", "CharacterStudySe: Still learning progression: introducing new character " + next);
        		}
        	}

        } else {

        	try {
        		// 40% chance of previously mistaken character
        		if(ran <= 0.40 && charactersSeen > 0){
                    next = tracker.randomMistakenNext(availSet);

                    // if no prev mistaken, then finish up reviewing
                    if(next == null){
                        next = tracker.randomReviewingNext(availSet);
                    }
        			Log.i("nakama", "CharacterStudySe: Known set progression: reviewing mistaken character " + next);

       			// 15% chance of reviewing character
        		} else if(ran <= 0.80 && charactersSeen > 0){
        			next = tracker.randomReviewingNext(availSet);
        			Log.i("nakama", "CharacterStudySe: Known set progression: reviewing review character " + next);

        		} else {
                    next = tracker.randomCorrectNext(availSet);
                    Log.i("nakama", "CharacterStudySe: Known set progression: reviewing previously correct character " + next);
                }
        	} finally {
        		if(next == null){
                    Log.i("nakama", "CharacterStudySe: Known set progression: falling back to random next " + next);
                    next = tracker.randomNext(availSet);
        		}
        	}
        }

        if(next == null){
            throw new RuntimeException("Error: currentChar should never be null at the end of this method.");
        }

        this.currentChar = next;
	}

	public boolean isReviewing(){
		return this.reviewing;
	}

	public void save(Context context){
		String progressAsString = tracker.saveToString();

        CharacterProgressDataHelper cdb = new CharacterProgressDataHelper(context);
		cdb.recordProgress(pathPrefix, progressAsString);
	}

	public void load(Context context){
		String existingProgress;
        CharacterProgressDataHelper cdb = new CharacterProgressDataHelper(context);
		existingProgress = cdb.getExistingProgress(pathPrefix);
		tracker.updateFromString(existingProgress);
	}
}
