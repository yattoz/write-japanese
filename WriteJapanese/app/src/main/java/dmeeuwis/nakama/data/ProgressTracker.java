package dmeeuwis.nakama.data;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;
import android.util.Pair;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.util.Util;

/**
 * Scores:
 *
 * null unknown
 *
 * -2 * setting failing
 * -1 * setting review
 *  0 time review d1
 *  1 time review d3
 *  2 time review d7
 *  3 time review d14
 *  4 time review d30
 */
public class ProgressTracker {

	final static public int MAX_SCORE = SRSQueue.SRSTable.length;
	private static final boolean DEBUG_SRS = BuildConfig.DEBUG && false;
	private static final boolean DEBUG_PROGRESS = BuildConfig.DEBUG && false;

	private final boolean useSRS;
    private final boolean skipSrsIfFirstCorrect;

    // these 2 track last grading to support override functionality
    private Integer lastCharPrevScore = null;
    private boolean lastPassed = false;

    private Character lastChar = null;
    private boolean isReview = false;
    public final String setId;
    public final boolean useSRSAcrossSets;
    private final int advanceIncorrect;
    private final int advanceReview;

    private final Set<Character> specialChars = new HashSet<>();
    {
        specialChars.add('R');
        specialChars.add('S');
    }

	public LinkedHashMap<String, LocalDateTime> oldestLogTimestampByDevice = new LinkedHashMap<>();

	SRSQueue srsQueue;
    Map<Character, Integer> recordSheet;

    // TODO: would be better to take a reference to global history object in constructor, instead of relying
	// on tests to call this clear method.
	public void clearGlobalState() {
		history.clear();
	}

	public void forceCharacterOntoHistory(Character c) {
	    history.add(new StudyRecord(c, null, setId, StudyType.REVIEW, "init" ));
	}

	private static class StudyRecord {
        private final Character chosenChar;
        private final Character previousChar;
        private final String setId;
        private final StudyType type;
        private final String pool;

        private StudyRecord(Character chosen, Character prev, String setId, StudyType type, String pool) {
            this.chosenChar = chosen;
            this.previousChar = prev;
            this.setId = setId;
            this.type = type;
            this.pool = pool;
        }
    }

    private static final List<StudyRecord> history = new ArrayList<>();

    private DateFactory dateFactory = new DateFactory() {
		@Override
		public LocalDateTime nowLocalDateTime() {
			return LocalDateTime.now();
		}

		@Override
		public LocalDate nowLocalDate() {
			return LocalDate.now();
		}
	};

    public Map<Character,Integer> getScoreSheet() {
		return this.recordSheet;
	}

	public void noteTimestamp(Character c, LocalDateTime t, String device) {
		LocalDateTime oldestLogTimestamp = oldestLogTimestampByDevice.get(device);
		if(oldestLogTimestamp == null || t.isAfter(oldestLogTimestamp)){
			//Log.d("nakama-progress", "Oldest timestamp for device " + device + " is now set to " + t);
			oldestLogTimestampByDevice.put(device, t);
		}
	}

	public Map<LocalDate,List<Character>> getSrsSchedule() {
	    return srsQueue.getSrsSchedule();
	}

	public boolean reject(Character character) {
		return !(recordSheet.containsKey(character) || specialChars.contains(character));
	}


	public enum Progress { FAILED(-300), REVIEWING(200), TIMED_REVIEW(300), PASSED(400), UNKNOWN(-200);

	    // forceResetCode are the code used in the score field of the practice_log database. They should be outside of
        // the range of allowed scores. No real meaning to them.
		public final int forceResetCode;

		Progress(int forceResetCode){
			this.forceResetCode = forceResetCode;
		}

		private static Progress parse(Integer in, int advanceReviewing, boolean srsEnabled){
        	if(in == null){
				return Progress.UNKNOWN;
			} else if(in < -1 * advanceReviewing){
				return Progress.FAILED;
			} else if(in < 0){
				return Progress.REVIEWING;
            } else if(in < MAX_SCORE && srsEnabled){
                return Progress.TIMED_REVIEW;
            } else {
            	return Progress.PASSED;
			}

		}
	}
	
	ProgressTracker(Set<Character> allChars, int advanceIncorrect, int advanceReview, boolean useSRS, boolean useSRSAcrossSets, boolean skipSrsIfFirstCorrect, String setId){
		this.useSRS = useSRS;
		this.recordSheet = new LinkedHashMap<>();
		for(Character c: allChars){
			recordSheet.put(c, null);
		}
		this.advanceIncorrect = advanceIncorrect;
		this.advanceReview = advanceReview;
        this.skipSrsIfFirstCorrect = skipSrsIfFirstCorrect;
		this.setId = setId;
		this.useSRSAcrossSets = useSRSAcrossSets;

		srsQueue = new SRSQueue(setId);
	}

	private List<List<Character>> getSets(Set<Character> available){
		List<Character> failed = new ArrayList<>();
		List<Character> reviewing = new ArrayList<>();
		List<Character> timedReviewing = new ArrayList<>();
		List<Character> passed = new ArrayList<>();
        List<Character> unknown = new ArrayList<>();


        Map<Character, Progress> allScores = getAllScores();
		for(Map.Entry<Character, Progress> score: allScores.entrySet()){
			if(available != null && !available.contains(score.getKey())){
				continue;
			}

			if(score.getValue() == Progress.FAILED){
				failed.add(score.getKey());
			} else if(score.getValue() == Progress.REVIEWING){
				reviewing.add(score.getKey());
			} else if(score.getValue() == Progress.TIMED_REVIEW){
				timedReviewing.add(score.getKey());
			} else if(score.getValue() == Progress.UNKNOWN || score.getValue() == null){
				unknown.add(score.getKey());
			} else if(score.getValue() == Progress.PASSED){
				passed.add(score.getKey());
			} else {
				Log.e("nakama-progression", "Skipping unknown Progress: " + score.getValue());
			}
		}

		return Arrays.asList(failed, reviewing, timedReviewing, passed, unknown);
	}

	public enum StudyType { NEW_CHAR, REVIEW, SRS }
	/*========== TEMP HACK FOR TESTING ============ */
	public void setDateFactory(DateFactory d){
	    this.dateFactory = d;
    }
    /*========== END TEMP HACK FOR TESTING ============ */


    Pair<Character, StudyType> nextCharacter(Set<Character> rawAvailSet, boolean shuffling, CharacterProgressDataHelper.ProgressionSettings prog) {
		Log.i("nakama-progression", "-------------> Starting nexCharacter selection");

		boolean prevWasReview = isReview;

		Character prev = null;
		Set<Character> recentHistoryChars = new LinkedHashSet<>();
		Set<Character> availSet = new LinkedHashSet<>(rawAvailSet);
		for(int i = 0; i < prog.characterCooldown; i++){
			try {
				Character c = history.get(history.size() - 1 - i).chosenChar;
				availSet.remove(c);
				recentHistoryChars.add(c);
				if(prev == null){ prev = c; }
			} catch(ArrayIndexOutOfBoundsException e){
				// didn't have enough history
			}
		}

		if(useSRS) {
			try {
				LocalDate today = this.dateFactory.nowLocalDate();
				SRSQueue.SRSEntry soonestEntry = srsQueue.checkForEntry(recentHistoryChars, today);
				if (soonestEntry != null) {
					Log.i("nakama-progression", "Returning early from nextCharacter, found an scheduled SRS review.");
					Character n = soonestEntry.character;
					StudyRecord rec = new StudyRecord(n, prev, setId, StudyType.SRS, today.toString());
					history.add(rec);
					return Pair.create(rec.chosenChar, rec.type);
				}
			} catch(Throwable t){
				UncaughtExceptionLogger.backgroundLogError("Error during SRS nextCharacter", t);
			}
		}


		if(availSet.size() == 1){
			Log.i("nakama-progression", "Returning early from nextCharacter, only 1 character in set");
			return Pair.create(availSet.toArray(new Character[0])[0], StudyType.REVIEW);
		}

        List<List<Character>> unfilteredSets = getSets(rawAvailSet);
        List<Character> unfilteredFailed = unfilteredSets.get(0);
        List<Character> unfilteredReviewing = unfilteredSets.get(1);
        List<Character> unfilteredTimedReviewing = unfilteredSets.get(2);
        List<Character> unfilteredPassed = unfilteredSets.get(3);
        List<Character> unfilteredUnknown = unfilteredSets.get(4);


		List<List<Character>> sets = getSets(availSet);
		List<Character> failed = sets.get(0);
		List<Character> reviewing = sets.get(1);
		List<Character> timedReviewing = sets.get(2);
		List<Character> passed = sets.get(3);
		List<Character> unknown = sets.get(4);

		if(DEBUG_PROGRESS && BuildConfig.DEBUG) {
			Log.d("nakama-progression", "Character progression: reviewing sets");

			Log.d("nakama-progression", "Failed set is: " + Util.join(", ", failed));
			Log.d("nakama-progression", "Reviewing set is: " + Util.join(", ", reviewing));
			Log.d("nakama-progression", "TimedReviewing set is: " + Util.join(", ", timedReviewing));
			Log.d("nakama-progression", "Passed set is: " + Util.join(", ", passed));
			Log.d("nakama-progression", "Unknown set is: " + Util.join(", ", unknown));
		}

        Character n = null;
		isReview = false;
		String pool = "";

		// character not in cooldown that are failed get priority, in their order.
        if(failed.size() > 0){
            n = failed.get(0);
            isReview = true;
            pool = "failed";
        }

		// if we're not at reviewing or failed limits, alternate reviewing chars and new chars
		boolean spaceInReviewingPool = unfilteredReviewing.size() < prog.introReviewing;
        boolean spaceInFailedPool = unfilteredFailed.size() < prog.introIncorrect;
        boolean unknownCharsInSet = unknown.size() > 0;

		if(n == null && spaceInReviewingPool && spaceInFailedPool && unknownCharsInSet){
            if(!prevWasReview && reviewing.size() > 0) {
                n = reviewing.get(0);
                isReview = true;
                pool = "first-reviewing";
            }

            // Intro a new character! Congratulations!
            if(n == null && shuffling){
                n = unknown.get((int)(Math.random() * unknown.size()));
                pool = "new-char-shuffle";
            } else if(n == null){
                n = unknown.get(0);
                pool = "new-char";
            }
        }

        // do a reviewing char if no failed and no space for new chars.
        if(n == null && reviewing.size() > 0){
		    n = reviewing.get(0);
            isReview = true;
            pool = "reviewing-no-space";
        }

		// if we get here, there were no failed or reviewing characters available right now. I guess everything is in
		// timed review or passed? Go ahead and introduce a new character, even though it might take us over our pool limits.
		if(n == null && unknownCharsInSet){
			n = unknown.get(0);
			pool = "new-char-no-failed-or-reviewing";
		}

		// if we get here, there were no failed or reviewing characters, and we couldn't introduce a new char.
		// Logically, everything must be in timed review or passed.
		// These two sets won't progress on pass, so need to do them out of order.
		if(n == null && (timedReviewing.size() > 0 || passed.size() > 0)){
			List<Character> summed = new ArrayList<>(timedReviewing);
			summed.addAll(passed);
			n = summed.get((int)(Math.random() * summed.size()));
			isReview = true;
			pool = "random-review";
		}


        // if we get here, there were no failed or reviewing characters, and we couldn't introduce a new char,
		// and we can't do timed review. This should be impossible? Maybe a huge cooldown setting? What the hell, go to the unfiltered lists.
        if(n == null && unfilteredFailed.size() > 0){
		    n = unfilteredFailed.get(0);
            pool = "unfiltered-failed";
        }
        if(n == null && unfilteredReviewing.size() > 0){
            n = unfilteredReviewing.get(0);
            pool = "unfiltered-reviewing";
        }

        if(n == null){
            // I think this case can never happen? If nothing else was available, do completely random char.
            UncaughtExceptionLogger.backgroundLogError(
                    "Error: no char found. Set is: " + Util.join("", rawAvailSet) +
                            " and history is: " + Util.join("", history),
                        new RuntimeException());

            n = rawAvailSet.toArray(new Character[0])[(int)(rawAvailSet.size() * Math.random())];
            pool = "random-raw-avail";
        }

		if(BuildConfig.DEBUG) {
			Log.d("nakama-progression", "Picked: " + n + (isReview ? ", review" : ", fresh, current score: " + getScoreSheet().get(n)));
		}

		StudyRecord rec = new StudyRecord(n, prev, setId, isReview ? StudyType.REVIEW : StudyType.NEW_CHAR, pool);
		history.add(rec);
		return Pair.create(rec.chosenChar, rec.type);
    }

    public String debugHistory(){
        try {
            StringWriter sw = new StringWriter();
            JsonWriter jw = new JsonWriter(sw);

            jw.beginArray();
            for (StudyRecord s : history) {
                jw.beginObject();

                jw.name("char");
                jw.value(s.chosenChar.toString());

                jw.name("prev");
                jw.value(s.previousChar == null ? "none" : s.previousChar.toString());

                jw.name("set");
                jw.value(s.setId);

                jw.name("type");
                jw.value(s.type.toString());

                jw.name("pool");
                jw.value(s.pool);

                jw.endObject();
            }
            jw.endArray();

            return sw.toString();

        } catch(Throwable t){
            Log.e("nakama", "Error generating debug history json", t);
            return t.getMessage();
        }
    }

	public StudyType isReviewing(Character c){
		if(srsQueue.find(c, LocalDate.now())){
			return StudyType.SRS;
		}
		List<List<Character>> sets = getSets(null);
		List<Character> failed = sets.get(0);
		List<Character> reviewing = sets.get(1);

		boolean r = failed.contains(c) || reviewing.contains(c);
		if(BuildConfig.DEBUG) {
			Log.i("nakama-progression", "Failed set is: " + Util.join(", ", failed));
			Log.i("nakama-progression", "Reviewing set is: " + Util.join(", ", reviewing));
			Log.i("nakama-progression", "Character progression: is " + c + " reviewing? " + r);
		}
		return r ? StudyType.REVIEW : StudyType.NEW_CHAR;
	}


    public CharacterStudySet.SetProgress calculateProgress(){
        int known = 0, reviewing = 0, timedReviewing = 0, failed = 0, unknown = 0;
        Map<Character, Progress> scores = getAllScores();
        for(Map.Entry<Character, Progress> c: scores.entrySet()){
            if(c.getValue() == Progress.FAILED){
                failed++;
			} else if(c.getValue() == Progress.TIMED_REVIEW){
				timedReviewing++;
            } else if(c.getValue() == Progress.REVIEWING){
                reviewing++;
            } else if(c.getValue() == Progress.PASSED){
                known++;
            } else if(c.getValue() == Progress.UNKNOWN){
                unknown++;
            }
        }
        return new CharacterStudySet.SetProgress(known, reviewing, timedReviewing, failed, unknown, scores);
    }
	
	private List<Character> charactersMatchingScore(Set<Character> allowedChars, Integer... scores){

		List<Integer> scoresList = Arrays.asList(scores);
		List<Character> matching = new ArrayList<>();
        for(Map.Entry<Character, Integer> c: this.recordSheet.entrySet()){
        	Integer knownScore = c.getValue();
        	if(scoresList.contains(knownScore) && allowedChars.contains(c.getKey())){
				matching.add(c.getKey());
			}
		}
		return matching;
	}
	
	public boolean passedAllCharacters(Set<Character> allowedChars){
		List<Character> passed= charactersMatchingScore(allowedChars, 1);
		return passed.size() == allowedChars.size();
	}
	
	public void progressReset(Context ctx, String setName){
		if(!this.setId.equals(setName)) {
			return;
		}

        for (Character c : this.recordSheet.keySet()) {
            this.recordSheet.put(c, null);
            srsQueue.removeSRSQueue(c);
        }
	}

	/**
	 * A special one-time event when SRS was first introduced, to try to not freak people out.
	 */
	public void srsReset(String setId) {
		//Log.d("nakama-progress", "SRS Set reset!!!!!!!!!!!!!!!!!!! On " + setId);
		//Log.d("nakama-progress", "Prior to reset, schedule is: " + srsQueue.getSrsScheduleString());
		if(!(setId.equals(this.setId) || setId.equals("all"))){
			return;
		}

		List<Character> charsToReset = new ArrayList<>(srsQueue.size());
		Iterator<SRSQueue.SRSEntry> it = srsQueue.iterator();
		while(it.hasNext()){
			charsToReset.add(it.next().character);
		}

		for(Character c: charsToReset){
			overrideFullCompleted(c);
		}

		Log.d("nakama-progress", "After SRS reset, schedule is: " + srsQueue.getSrsScheduleString());
		//debugPrintAllScores();
	}

	public void overrideFullCompleted(Character c){
		if(!recordSheet.containsKey(c)){
			return;
		}

		recordSheet.put(c, MAX_SCORE);
		srsQueue.removeSRSQueue(c);
	}

	public SRSQueue.SRSEntry markSuccess(Character c, LocalDateTime time){
		boolean charInCurrentSet = recordSheet.containsKey(c);
		if(!charInCurrentSet){
			return null;
		}

		// if the char is in SRS queue, and the user practices it before it is scheduled, do not
		// adjust the scoresheet score; not marked 'passed' until the scheduled day has been won.
		SRSQueue.SRSEntry s = srsQueue.find(c);
		if(s != null && time.toLocalDate().isBefore(s.nextPractice)){
			return s;
		}

		// has the key, but is null, means char is in set, but has not yet been attempted
		boolean firstTime = recordSheet.get(c) == null;

		int score;

		// if you get the character right the first time you see it, skip the SRS queue.
		if(firstTime && skipSrsIfFirstCorrect){
		    score = MAX_SCORE;
        } else {
            score = recordSheet.get(c) == null ? -1 : recordSheet.get(c);
        }

		int newScore = Math.min(MAX_SCORE, score + 1);

        lastCharPrevScore = score;
        lastChar = c;

        lastPassed = true;
		recordSheet.put(c, newScore);

        SRSQueue.SRSEntry addedToSrs = srsQueue.addToSRSQueue(c, newScore, time, MAX_SCORE);
        return addedToSrs;
	}

	public SRSQueue.SRSEntry markFailure(Character c){
		if(!recordSheet.containsKey(c)){
			return null;
		}

		srsQueue.removeSRSQueue(c);
        lastCharPrevScore = recordSheet.get(c);
        lastChar = c;
        lastPassed = false;

		recordSheet.put(c, failScore());
		return null;
	}

	public Map<Character, Progress> getAllScores(){
		Map<Character, Progress> all = new LinkedHashMap<>(recordSheet.size());
		for(Map.Entry<Character, Integer> entry: recordSheet.entrySet()){
        	all.put(entry.getKey(), Progress.parse(entry.getValue(), advanceReview, useSRS));
		}
		return all;
	}

	public String toString(){
		return "[ProgressTracker: " + Util.join(", ", this.recordSheet.keySet()) + "]";
	}

	public Integer debugPeekCharacterScore(Character c){
		return this.recordSheet.get(c);
	}


	private int failScore(){
		return  -1 * (advanceIncorrect + advanceReview);
	}

	private int reviewScore(){
		return  -1 * (advanceReview);
	}

	private int timedReviewScore(){
		return  0;
	}

	private Integer unknownScore(){
		return null;
	}

	private static int knownScore(){
		return MAX_SCORE;
	}

	public void resetTo(Character character, Progress progress) {
		if(!recordSheet.containsKey(character)){
			return;
		}

		if(progress == Progress.TIMED_REVIEW){
			LocalDateTime time = DEBUG_SRS ? dateFactory.nowLocalDateTime().minusDays(2) : dateFactory.nowLocalDateTime();
			srsQueue.addToSRSQueue(character, 0, time, knownScore());
		} else {
			srsQueue.removeSRSQueue(character);
		}

		if(progress == Progress.FAILED) {
			recordSheet.put(character, failScore());
		} else if(progress == Progress.REVIEWING){
            recordSheet.put(character, reviewScore());
		} else if(progress == Progress.TIMED_REVIEW){
			recordSheet.put(character, timedReviewScore());
		} else if(progress == Progress.UNKNOWN) {
			recordSheet.put(character, unknownScore());
		} else if(progress == Progress.PASSED) {
			recordSheet.put(character, knownScore());
		}
	}

	public void overRideLast(){
        recordSheet.put(lastChar, lastCharPrevScore);
        if(lastPassed){
            markFailure(lastChar);
        } else {
            markSuccess(lastChar, dateFactory.nowLocalDateTime());
        }
    }

	public void debugAddDayToSRS() {
		srsQueue.debugAddDayToSRS();
	}

	public class ProgressState {
		public final String recordSheetJson, srsQueueJson;
		public final String oldestDateTime;

		public ProgressState(String recordSheetJson, String srsQueueJson, String oldestDateTime) {
			this.recordSheetJson = recordSheetJson;
			this.srsQueueJson = srsQueueJson;
			this.oldestDateTime = oldestDateTime;
		}
	}

	public ProgressState serializeOut(){
		if(oldestLogTimestampByDevice.size() == 0){
			return null;
		}

		try {

			// oldest log dates by set
			StringWriter oldestDates = new StringWriter();
			JsonWriter jd = new JsonWriter(oldestDates);
			jd.beginObject();
			for (Map.Entry<String, LocalDateTime> d : this.oldestLogTimestampByDevice.entrySet()) {
				jd.name(d.getKey());
				jd.value(d.getValue().toString());
			}
			jd.endObject();

			// record sheet
			StringWriter recordSheet = new StringWriter();
			JsonWriter j = new JsonWriter(recordSheet);
			j.beginObject();
			for (Map.Entry<Character, Integer> d : this.recordSheet.entrySet()) {
				j.name(d.getKey().toString());
				j.value(d.getValue());
			}
			j.endObject();


			j.close();

			return new ProgressState(recordSheet.toString(), srsQueue.serializeOut(), oldestDates.toString());
		} catch(IOException e){
			Log.d("nakama", "Error serializing out", e);
			return null;
		}
    }

    public void deserializeIn(String queueJson, String recordJson, String lastLogsByDevice) {
        try {
            JsonReader record = new JsonReader(new StringReader(recordJson));
            record.beginObject();
            while (record.hasNext()) {
                String name = record.nextName();
                Integer score = null;
                if (record.peek() == JsonToken.NULL) {
                    record.nextNull();
                } else {
                    score = record.nextInt();
                }
                recordSheet.put(name.charAt(0), score);
            }
            record.endObject();
            record.close();

            this.srsQueue = SRSQueue.deserializeIn(setId, queueJson);

            JsonReader jr = new JsonReader(new StringReader(lastLogsByDevice));
            this.oldestLogTimestampByDevice = new LinkedHashMap<>();
            jr.beginObject();
            while (jr.hasNext()) {
                oldestLogTimestampByDevice.put(jr.nextName(), LocalDateTime.parse(jr.nextString()));
            }
            jr.endObject();
            jr.close();
        } catch (IOException e) {
            Log.d("nakama", "Error deserializing in", e);
        }
    }

    interface DateFactory {
	    LocalDateTime nowLocalDateTime();
        LocalDate nowLocalDate();
    }
}
