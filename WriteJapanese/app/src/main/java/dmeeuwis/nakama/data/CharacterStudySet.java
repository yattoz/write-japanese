package dmeeuwis.nakama.data;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import java.text.DateFormat;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import dmeeuwis.nakama.LockChecker;
import dmeeuwis.nakama.kanjidraw.PointDrawing;
import dmeeuwis.util.Util;

/**
 * Holds a set of characters to study, and all current user study progress on those characters.
 */
public abstract class CharacterStudySet implements Iterable<Character> {

	public String currentCharacterCluesText(int currentCharacterClueIndex){
		return currentCharacterClueIndex == 0 ?
						"Draw the " + label() + " for" :
                        "which can also mean";
	}

	public abstract String currentReadingCluesText(int currentMeaningsClueIndex);

	public enum LockLevel { NULL_LOCK, LOCKED, UNLOCKABLE, UNLOCKED }

	final public Set<Character> freeCharactersSet;
	final public Set<Character> allCharactersSet;
	final public String name, shortName, description;

	final LockChecker LockChecker;
    ProgressTracker tracker;
	final Random random = new Random();
    final UUID iid;

	boolean shuffling = false;
	Character currentChar;
	boolean reviewing = false;
	LockLevel locked;
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
        return (int)Math.ceil(Math.abs(a.getTimeInMillis() - b.getTimeInMillis()) / (1000.0 * 60 * 60 * 24));
    }

    public static class GoalProgress {
        public final GregorianCalendar goal, goalStarted;
        public final int passed, remaining, daysLeft;
        public final int neededPerDay, scheduledPerDay;

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
            this.neededPerDay = (int)Math.ceil(1.0f * remaining / daysLeft);
            this.scheduledPerDay = (int)Math.ceil((1.0 * s.failing + s.unknown + s.reviewing + s.passed) / daysLeft);

            DateFormat df = DateFormat.getDateInstance();
            Log.i("nakama", "Goal Calcs; Start: " + df.format(goalStarted.getTime()) + ", goal: " + df.format(goal.getTime()) + "; remaining: " + remaining + "; daysLeft: " + daysLeft + "; remaining: " + remaining);
        }
    }

	public int length(){
		return this.allCharactersSet.size();
	}

	public String toString(){
		return String.format("%s (%d)", this.name, this.allCharactersSet.size());
	}

	public CharacterStudySet(String name, String shortName, String description, String pathPrefix, LockLevel locked, String allCharacters, String freeCharacters, LockChecker LockChecker, UUID iid){
		this.name = name;
		this.shortName = shortName;
        this.description = description;
		this.locked = locked;
        this.iid = iid;

		this.freeCharactersSet = Collections.unmodifiableSet(new LinkedHashSet<>(Util.stringToCharList(freeCharacters)));
		this.allCharactersSet = Collections.unmodifiableSet(new LinkedHashSet<>(Util.stringToCharList(allCharacters)));

		this.pathPrefix = pathPrefix;
		this.tracker = new ProgressTracker(this.allCharactersSet);
		this.LockChecker = LockChecker;
	}


    public boolean hasStudyGoal(){
        return this.studyGoal != null;
    }

    public void setStudyGoal(GregorianCalendar g){
        this.studyGoal = g;
        this.goalStarted = new GregorianCalendar();
    }

    public void clearStudyGoal(){
        this.studyGoal = null;
        this.goalStarted = null;
    }

    public GoalProgress getGoalProgress(){
        if(this.studyGoal == null){ return null; }
        SetProgress s = this.getProgress();
        return new GoalProgress(this.goalStarted, this.studyGoal, s, new GregorianCalendar());
    }


    public SetProgress getProgress(){
        return this.tracker.calculateProgress();
    }

	public boolean locked(){
        if(LockChecker == null){ return false; }
		boolean globalLock = LockChecker.getPurchaseStatus() == LockLevel.LOCKED;
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
		if(this.currentChar == null){
			nextCharacter();
		}
		return this.currentChar;
	}

	@Override
	public Iterator<Character> iterator() {
		return (availableCharactersSet()).iterator();
	}

	public void markCurrent(PointDrawing d, boolean pass, Context context){
		Character c = currentCharacter();
		try {
			if(pass){
				this.tracker.markSuccess(c);
			} else {
				this.tracker.markFailure(c);
			}
            CharacterProgressDataHelper cdb = new CharacterProgressDataHelper(context, iid);
            cdb.recordPractice(pathPrefix, currentCharacter().toString(), d, pass ? 100 : -100);
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

        CharacterProgressDataHelper cdb = new CharacterProgressDataHelper(context, iid);
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
        Set<Character> availSet = new LinkedHashSet<>(availableCharactersSet());
        availSet.remove(this.currentChar);
        Character next = null;
		Log.i("nakama", "CharacterStudySet " + this.shortName + ": progression: ran is " + ran);
        if(tracker.charactersNotYetSeen(availSet).size() > 0){

       		// 35% chance of mistaken character
       		if(ran <= 0.35) {
				next = tracker.randomMistakenNext(availSet);
				Log.i("nakama", "CharacterStudySet " + this.shortName + ": A Still learning progression: reviewing mistaken character " + next);
			}

			// 20% chance of reviewing character
        	if(next == null && ran <= 0.55){
        		next = tracker.randomReviewingNext(availSet);
        		Log.i("nakama", "CharacterStudySe " + this.shortName + ": B Still learning progression: reviewing review character " + next);
        	}

        	//  chance of new character
        	if(next == null){
        		this.reviewing = false; 		// this is only case of not-reviewing
        		next = this.shuffling ?
        				tracker.shuffleNext(availSet) :
       					tracker.standardNext(availSet);
        		Log.i("nakama", "CharacterStudySe " + this.shortName + ": C Still learning progression: introducing new character " + next);
        	}

        } else {
		// if all character in set have been seen

       		// 40% chance of previously mistaken character
        	if(ran <= 0.40) {
				next = tracker.randomMistakenNext(availSet);
				Log.i("nakama", "CharacterStudySet " + this.shortName + ": D Known set progression: reviewing mistaken character " + next);
			}

			// if no prev mistaken, then finish up reviewing
			if (next == null & ran < 0.8) {
				next = tracker.randomReviewingNext(availSet);
				Log.i("nakama", "CharacterStudySet " + this.shortName + ": E Known set progression: reviewing mistaken character " + next);
			}

       		// 15% chance of reviewing character
			if(next == null){
				next = tracker.randomCorrectNext(availSet);
				Log.i("nakama", "CharacterStudySe " + this.shortName + ": F Known set progression: reviewing review character " + next);
			}

			if(next == null){
				Log.i("nakama", "CharacterStudySe " + this.shortName + ": G Known set progression: falling back to random next " + next);
        		next = tracker.randomNext(availSet);
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
        CharacterProgressDataHelper cdb = new CharacterProgressDataHelper(context, iid);
        cdb.recordGoals(pathPrefix, goalStarted, studyGoal);
	}

	public void load(Context context){
        CharacterProgressDataHelper cdb = new CharacterProgressDataHelper(context, iid);
        Pair<GregorianCalendar, GregorianCalendar> goals = cdb.getExistingGoals(pathPrefix);
        if(goals != null) {
            this.goalStarted = goals.first;
            this.studyGoal = goals.second;
        }
        Map<Character, Integer> existing = cdb.getRecordSheetForCharset(this.pathPrefix);
        Map<Character, Integer> freshSheet = new LinkedHashMap<>();

        for(Character c: this.allCharactersSet){
            if(existing.containsKey(c)){
                freshSheet.put(c, existing.get(c));
            } else {
                freshSheet.put(c, null);
            }
        }
		tracker = new ProgressTracker(freshSheet);
	}

    public Map<Character, ProgressTracker.Progress> getRecordSheet(){
        return this.tracker.getAllScores();
    }

	public void debugMarkAllPassed(){
		this.tracker.debugMarkAllSuccess();
	}
}
