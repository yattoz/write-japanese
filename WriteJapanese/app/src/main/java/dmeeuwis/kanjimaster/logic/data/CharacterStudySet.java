package dmeeuwis.kanjimaster.logic.data;

import android.util.Log;
import android.util.Pair;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dmeeuwis.kanjimaster.logic.LockChecker;
import dmeeuwis.kanjimaster.logic.drawing.PointDrawing;
import dmeeuwis.kanjimaster.core.util.Util;

/**
 * Holds a set of characters to study, and standardSets current user study progress on those characters.
 */
public class CharacterStudySet implements Iterable<Character> {

    // TODO: change this. Either ProgressTracker exists outside of CharacterStudySet,
    // or its never leaked out.
    public ProgressTracker getProgressTracker() {
        return tracker;
    }

    public enum LockLevel {NULL_LOCK, LOCKED, UNLOCKABLE, UNLOCKED}

    final public Set<Character> freeCharactersSet;
    final public Set<Character> allCharactersSet;
    public String name, shortName, description;

    final private CharacterProgressDataHelper dbHelper;
    final public boolean systemSet;
    final private LockChecker LockChecker;
    private ProgressTracker tracker;

    private boolean shuffling = false;
    private Character currentChar;
    private ProgressTracker.StudyType reviewing = ProgressTracker.StudyType.NEW_CHAR;
    private LockLevel locked;

    public final String pathPrefix;

    private String lastRowId = null;

    private GregorianCalendar studyGoal, goalStarted;

    public static class SetProgress {
        public final int passed;
        public final int reviewing;
        public final int timedReviewing;
        public final int failing;
        public final int unknown;
        public final Map<Character, ProgressTracker.Progress> perChar;

        SetProgress(int passed, int reviewing, int timedReviewing, int failing, int unknown, Map<Character, ProgressTracker.Progress> perChar) {
            this.passed = passed;
            this.reviewing = reviewing;
            this.timedReviewing = timedReviewing;
            this.failing = failing;
            this.unknown = unknown;
            this.perChar = perChar;
        }
    }

    private static int daysDifference(GregorianCalendar a, GregorianCalendar b) {
        return (int) Math.ceil(Math.abs(a.getTimeInMillis() - b.getTimeInMillis()) / (1000.0 * 60 * 60 * 24));
    }

    public static class GoalProgress {
        public final GregorianCalendar goal, goalStarted;
        public final int passed, remaining, daysLeft;
        public final int neededPerDay, scheduledPerDay;

        GoalProgress(GregorianCalendar goalStarted, GregorianCalendar goal, SetProgress s, GregorianCalendar today) {
            this.goalStarted = goalStarted;
            this.remaining = s.failing + s.reviewing + s.unknown;
            this.passed = s.passed;
            this.goal = goal;

            if (this.goal.before(today)) {
                daysLeft = 0;
                this.neededPerDay = remaining;
                this.scheduledPerDay = 0;
            } else {
                daysLeft = Math.max(1, daysDifference(goal, today));
                this.neededPerDay = (int) Math.ceil(1.0f * remaining / daysLeft);
                this.scheduledPerDay = (int) Math.ceil((1.0 * s.failing + s.unknown + s.reviewing + s.passed) / daysLeft);
            }

            DateFormat df = DateFormat.getDateInstance();
            Log.i("nakama", "Goal Calcs; Start: " + df.format(goalStarted.getTime()) + ", goal: " + df.format(goal.getTime()) + "; remaining: " + remaining + "; daysLeft: " + daysLeft + "; remaining: " + remaining);
        }
    }

    public int length() {
        return this.allCharactersSet.size();
    }

    public String toString() {
        return String.format("%s (%d)", this.name, this.allCharactersSet.size());
    }


    public CharacterStudySet(String name, String shortName, String description, String pathPrefix, LockLevel locked, String allCharacters, String freeCharacters, LockChecker LockChecker, UUID iid, boolean systemSet) {
        this(name, shortName, description, pathPrefix, locked, allCharacters, freeCharacters, LockChecker, iid, systemSet, new CharacterProgressDataHelper(iid));
    }

    public CharacterStudySet(String name, String shortName, String description, String pathPrefix, LockLevel locked, String allCharacters, String freeCharacters, LockChecker LockChecker, UUID iid, boolean systemSet, CharacterProgressDataHelper db) {
        this.dbHelper = db;
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.systemSet = systemSet;
        this.locked = locked;

        this.freeCharactersSet = new LinkedHashSet<>(Util.stringToCharList(freeCharacters));
        this.allCharactersSet = new LinkedHashSet<>(Util.stringToCharList(allCharacters));

        this.pathPrefix = pathPrefix;
        this.LockChecker = LockChecker;
    }


    public boolean hasStudyGoal() {
        return this.studyGoal != null;
    }

    public void setStudyGoal(GregorianCalendar g) {
        this.studyGoal = g;
        this.goalStarted = new GregorianCalendar();
    }


    public void resetTo(Character selected, ProgressTracker.Progress progress) {
        this.dbHelper.resetTo(this.pathPrefix, selected.toString(), progress);
        this.tracker.resetTo(selected, progress);
    }


    public void clearStudyGoal() {
        this.studyGoal = null;
        this.goalStarted = null;
    }

    public GoalProgress getGoalProgress() {
        if (this.studyGoal == null) {
            return null;
        }
        SetProgress s = this.getProgress();
        return new GoalProgress(this.goalStarted, this.studyGoal, s, new GregorianCalendar());
    }


    public SetProgress getProgress() {
        return this.tracker.calculateProgress();
    }

    public boolean locked() {
        if (LockChecker == null) {
            return false;
        }
        boolean globalLock = LockChecker.getPurchaseStatus() == LockLevel.LOCKED;
        boolean localLock = this.locked == LockLevel.LOCKED;
        // Log.d("nakama", "CharacterStudySet: globalLock: " + globalLock + "; localLock: " + localLock + " => " + (globalLock && localLock));
        return globalLock && localLock;
    }

    public Set<Character> availableCharactersSet() {
        return locked() ? freeCharactersSet : allCharactersSet;
    }

    public String charactersAsString() {
        return Util.join("", allCharactersSet);
    }

    public boolean passedAllCharacters() {
        return this.tracker.passedAllCharacters(availableCharactersSet());
    }

    public Character currentCharacter() {
        return this.currentChar;
    }

    @Override
    public Iterator<Character> iterator() {
        return (availableCharactersSet()).iterator();
    }

    public static class GradingResult {
        public final String rowId;
        public final SRSQueue.SRSEntry srs;

        public GradingResult(String rowId, SRSQueue.SRSEntry srs) {
            this.srs = srs;
            this.rowId = rowId;
        }
    }

    public SRSQueue.SRSEntry markCurrent(Character c, PointDrawing d, boolean pass) {
        SRSQueue.SRSEntry result;
        try {
            if (pass) {
                result = this.tracker.markSuccess(c, LocalDateTime.now());
            } else {
                result = this.tracker.markFailure(c);
            }
            return result;
        } catch (Throwable t) {
            Log.e("nakama", "Error when marking character " + c + " from character set " + Util.join(", ", this.allCharactersSet) + "; tracker is " + tracker);
            throw new RuntimeException(t);
        }
    }

    public void setShuffle(boolean isShuffle) {
        this.shuffling = isShuffle;
    }

    public boolean isShuffling() {
        return this.shuffling;
    }

    public void progressReset() {
        this.tracker.progressReset(pathPrefix);
        dbHelper.clearProgress(pathPrefix);
    }

    public void srsForcePassAll() {
        this.dbHelper.srsReset(pathPrefix);
        this.tracker.srsReset(pathPrefix);
    }

    public void skipTo(Character character) {
        if (availableCharactersSet().contains(character)) {
            this.currentChar = character;
            this.reviewing = tracker.isReviewing(character);
        }
        this.tracker.forceCharacterOntoHistory(character);
    }

    public ProgressTracker.StudyRecord nextCharacter() {
        CharacterProgressDataHelper.ProgressionSettings p = Settings.getProgressionSettings();
        return nextCharacter(p);
    }

    public ProgressTracker.StudyRecord nextCharacter(CharacterProgressDataHelper.ProgressionSettings p) {
        try {
            ProgressTracker.StudyRecord i = tracker.nextCharacter(availableCharactersSet(), this.shuffling, p);

            this.currentChar = i.chosenChar;
            this.reviewing = i.type;

            return i;

        } catch (Throwable t) {
            throw new RuntimeException("Error getting next char for charset: " + shortName + "; chars " + charactersAsString(), t);
        }
    }


    public void saveGoals() {
        dbHelper.recordGoals(pathPrefix, goalStarted, studyGoal);
    }

    public enum LoadProgress { LOAD_SET_PROGRESS, NO_LOAD_SET_PROGRESS };


    public ProgressTracker loadEmptyTestingTracker() {
        tracker = new ProgressTracker(allCharactersSet, 2, 2, true, false, true, pathPrefix);
        tracker.clearGlobalState();
        return tracker;
    }

    public ProgressTracker load(LoadProgress loadProgress) {
        CharacterProgressDataHelper.ProgressionSettings p = Settings.getProgressionSettings();
        Pair<GregorianCalendar, GregorianCalendar> goals = dbHelper.getExistingGoals(pathPrefix);
        if (goals != null) {
            this.goalStarted = goals.first;
            this.studyGoal = goals.second;
        }

        Boolean srsEnabled = Settings.getSRSEnabled();
        Boolean srsAcrossSets = Settings.getSRSAcrossSets();

        // these 2 should only happen on first view, and then the IntroActivity should spring up
        // before the user can interact with this charset anyways.
        if(srsEnabled == null){ srsEnabled = true; }
        if(srsAcrossSets == null){ srsAcrossSets = true; }


        tracker = new ProgressTracker(allCharactersSet, p.advanceIncorrect, p.advanceReviewing, srsEnabled, srsAcrossSets, p.skipSRSOnFirstTimeCorrect, pathPrefix);

        if(loadProgress == LoadProgress.LOAD_SET_PROGRESS) {
            dbHelper.loadProgressTrackerFromDB(Arrays.asList(tracker), CharacterProgressDataHelper.ProgressCacheFlag.USE_CACHE);
        }

        return tracker;
    }

    public ProgressTracker load(ProgressTracker tracker){
        this.tracker = tracker;
        dbHelper.loadProgressTrackerFromDB(Arrays.asList(tracker), CharacterProgressDataHelper.ProgressCacheFlag.USE_CACHE);
        return tracker;
    }

    public ProgressTracker.StudyType isReviewing() {
        return reviewing;
    }


    public Map<Character, ProgressTracker.Progress> getRecordSheet() {
        return this.tracker.getAllScores();
    }

    public Map<Character, Integer> getScoreSheet() {
        return this.tracker.getScoreSheet();
    }

    public Map<LocalDate, List<Character>> getSrsSchedule(){
        return tracker.getSrsSchedule();
    }

    public String getSrsScheduleString() {
        return Util.join(getSrsSchedule(), ": ", ", ");
    }

    public Boolean srsAcrossSets() {
        return tracker.useSRSAcrossSets;
    }

    public String overRideLast() {
        this.tracker.overRideLast();
        return this.dbHelper.overRideLast();
    }

    public void correctSRSQueueState() {
        this.tracker.correctSRSQueueState(allCharactersSet);
    }
}
