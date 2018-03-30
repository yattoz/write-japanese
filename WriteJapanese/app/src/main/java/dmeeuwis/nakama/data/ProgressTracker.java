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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

	final Random random = new Random();
	private final boolean useSRS;

    // these 2 track last grading to support override functionality
    private Integer lastCharPrevScore = null;
    private boolean lastPassed = false;

    private Character lastChar = null;
    public final String setId;
    public final boolean useSRSAcrossSets;
    private final int advanceIncorrect;
    private final int advanceReview;

	public LinkedHashMap<String, LocalDateTime> oldestLogTimestampByDevice = new LinkedHashMap<>();

	private SRSQueue srsQueue;
    private Map<Character, Integer> recordSheet;


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
		return !recordSheet.containsKey(character);
	}


	public enum Progress { FAILED(-300), REVIEWING(200), TIMED_REVIEW(300), PASSED(400), UNKNOWN(-200);
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
	
	ProgressTracker(Set<Character> allChars, int advanceIncorrect, int advanceReview, boolean useSRS, boolean useSRSAcrossSets, String setId){
		this.useSRS = useSRS;
		this.recordSheet = new LinkedHashMap<>();
		for(Character c: allChars){
			recordSheet.put(c, null);
		}
		this.advanceIncorrect = advanceIncorrect;
		this.advanceReview = advanceReview;
		this.setId = setId;
		this.useSRSAcrossSets = useSRSAcrossSets;

		if(useSRSAcrossSets){
			srsQueue = SRSQueue.GLOBAL;
		} else {
			srsQueue = new SRSQueue(setId);
		}
	}

	private List<Set<Character>> getSets(Set<Character> available){
		Set<Character> failed = new LinkedHashSet<>();
		Set<Character> reviewing = new LinkedHashSet<>();
		Set<Character> timedReviewing = new LinkedHashSet<>();
		Set<Character> passed = new LinkedHashSet<>();
		Set<Character> unknown = new LinkedHashSet<>();


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

    Pair<Character, StudyType> nextCharacter(Set<Character> rawAllChars, Character currentChar, Set<Character> rawAvailSet, boolean shuffling,
												  int introIncorrect, int introReviewing) {
		Log.i("nakama-progression", "-------------> Starting nexCharacter selection");

		LinkedHashSet<Character> availSet = new LinkedHashSet<>(rawAvailSet);

		if(useSRS) {
			SRSQueue.SRSEntry soonestEntry = srsQueue.peek();
			LocalDate today = LocalDate.now();
			if (soonestEntry != null && (soonestEntry.nextPractice.isBefore(today) || soonestEntry.nextPractice.isEqual(today))) {
				Log.i("nakama-progression", "Returning early from nextCharacter, found an scheduled SRS review.");
				return Pair.create(srsQueue.poll().character, StudyType.SRS);
			}
		}

		if(availSet.size() == 1){
			Log.i("nakama-progression", "Returning early from nextCharacter, only 1 character in set");
			return Pair.create(availSet.toArray(new Character[0])[0], StudyType.REVIEW);
		}

        double ran = random.nextDouble();

		List<Set<Character>> sets = getSets(availSet);
		Set<Character> failed = sets.get(0);
		Set<Character> reviewing = sets.get(1);
		Set<Character> passed = sets.get(3);
		Set<Character> unknown = sets.get(4);

		if(DEBUG_PROGRESS && BuildConfig.DEBUG) {
			Log.d("nakama-progression", "Character progression: reviewing sets");

			Log.d("nakama-progression", "Failed set is: " + Util.join(", ", failed));
			Log.d("nakama-progression", "Reviewing set is: " + Util.join(", ", reviewing));
			Log.d("nakama-progression", "Passed set is: " + Util.join(", ", passed));
			Log.d("nakama-progression", "Unknown set is: " + Util.join(", ", unknown));
		}

        // probs array: failed, reviewing, unknown, passed
        float[] probs;

        // still learning new chars, but maxed out the reviewing and failed buckets so just review
        if(failed.size() >= introIncorrect || reviewing.size() > introReviewing) {
            Log.d("nakama-progress", "Failed or Review buckets maxed out, reviewing 50/50");
            probs = new float[]{0.5f, 0.5f, 0.0f, 0.0f};

        // still learning new chars, haven't seen standardSets
        } else if(unknown.size() > 0) {
			Log.d("nakama-progress", "Still room in failed and review buckets, chance of new characters");
            probs = new float[]{0.35f, 0.30f, 0.35f, 0.0f};

        // have seen standardSets characters, still learning
        } else if(unknown.size() == 0){
			Log.d("nakama-progress", "Have seen standardSets characters, reviewing 40/40/0/20");
            probs = new float[] { 0.40f, 0.30f, 0.0f, 0.2f };

        // what situation is this?
        } else {
			Log.d("nakama-progress", "Unknown situation, reviewing 25/25/25/25.");
            probs = new float[] { 0.25f, 0.25f, 0.25f, 0.25f };
        }

        availSet.remove(currentChar);
		failed.remove(currentChar);
		reviewing.remove(currentChar);
		passed.remove(currentChar);
		unknown.remove(currentChar);

		Set<Character> chosenOnes = new LinkedHashSet<>();

        if(ran <= probs[0] && failed.size() > 0){
			chosenOnes.addAll(failed);
        } else if(ran <= (probs[0] + probs[1]) && (failed.size() > 0 || reviewing.size() > 0)){
			chosenOnes.addAll(failed);
			chosenOnes.addAll(reviewing);
		} else if(ran <= (probs[0] + probs[1] + probs[2]) && unknown.size() > 0){
			if(shuffling){
				chosenOnes.addAll(unknown);
			} else {
				chosenOnes.add(sortAndReturnFirst(rawAvailSet, unknown));
			}
        } else {
			chosenOnes.addAll(failed);
			chosenOnes.addAll(reviewing);
			chosenOnes.addAll(unknown);
			chosenOnes.addAll(passed);
		}

        if(chosenOnes.size() == 0){
            chosenOnes.addAll(rawAvailSet);

			if(chosenOnes.size() == 0){
				chosenOnes.addAll(rawAllChars);
			}
        }

		final Character[] next = chosenOnes.toArray(new Character[0]);
		final Character n = next[(int)(ran * next.length)];
		final boolean isReview = failed.contains(n) || reviewing.contains(n) || passed.contains(n);

		if(BuildConfig.DEBUG) {
			Log.d("nakama-progression", "Potential set is: " + Util.join(", ", chosenOnes));
			Log.d("nakama-progression", "Picked: " + n + (isReview ? ", review" : ", fresh, current score: " + getScoreSheet().get(n)));
		}
		return Pair.create(n, isReview ? StudyType.REVIEW : StudyType.NEW_CHAR);
    }

	private Character sortAndReturnFirst(Set<Character> allChars, Set<Character> unknown) {
		final List<Character> chars = new ArrayList<>(allChars);
		//Log.d("nakama-progression", "Sorting allchars: " + Util.join(", ", chars)  + " to order unknown set " + Util.join(", ", unknown));

		final LinkedHashMap<Character, Integer> indexed = new LinkedHashMap<>(chars.size());
		for(int i = 0; i < chars.size(); i++){
			indexed.put(chars.get(i), i);
		}

		List<Character> toSort = new ArrayList<>(unknown);
		Collections.sort(toSort, new Comparator<Character>() {
			@Override
			public int compare(Character o1, Character o2) {
				Integer i1 = indexed.get(o1);
				Integer i2 = indexed.get(o2);
				if(i1 == null && i2 == null) { return 0; }
				if(i1 == null) { return -1; }
				if(i2 == null) { return 1; }

				return i1.compareTo(i2);
			}
		});

		return toSort.get(0);
	}

	public StudyType isReviewing(Character c){
		if(srsQueue.find(c, LocalDate.now())){
			return StudyType.SRS;
		}
		List<Set<Character>> sets = getSets(null);
		Set<Character> failed = sets.get(0);
		Set<Character> reviewing = sets.get(1);

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
        for(Map.Entry<Character, Progress> c: getAllScores().entrySet()){
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
        return new CharacterStudySet.SetProgress(known, reviewing, timedReviewing, failed, unknown);
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
		for(SRSQueue.SRSEntry o: srsQueue){
			charsToReset.add(o.character);
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

		int score = recordSheet.get(c) == null ? -1 : recordSheet.get(c);
		int newScore = Math.min(MAX_SCORE, score + 1);

        lastCharPrevScore = score;
        lastChar = c;

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


	public void debugPrintAllScores(){
		Map<Character, Progress> all = new LinkedHashMap<>(recordSheet.size());
		for(Map.Entry<Character, Integer> entry: recordSheet.entrySet()){
			Log.d("nakama-progress", "In set " + setId + " char " + entry.getKey() + " has score " + entry.getValue());
		}
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
			LocalDateTime time = DEBUG_SRS ? LocalDateTime.now().minusDays(2) : LocalDateTime.now();
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
            markSuccess(lastChar, LocalDateTime.now());
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

    public void deserializeIn(String queueJson, String recordJson, String lastLogsByDevice){
		try {
			JsonReader record = new JsonReader(new StringReader(recordJson));
			record.beginObject();
			while (record.hasNext()) {
				String name = record.nextName();
				Integer score = null;
				if(record.peek() == JsonToken.NULL){
					record.nextNull();
				} else {
					score = record.nextInt();
				}
				recordSheet.put(name.charAt(0), score);
			}
			record.endObject();
			record.close();


			this.srsQueue.deserializeIn(setId, queueJson);

			JsonReader jr = new JsonReader(new StringReader(lastLogsByDevice));
			this.oldestLogTimestampByDevice = new LinkedHashMap<>();
			jr.beginObject();
			while(jr.hasNext()){
				oldestLogTimestampByDevice.put(jr.nextName(), LocalDateTime.parse(jr.nextString()));
			}
			jr.endObject();
			jr.close();
		} catch(IOException e){
			Log.d("nakama", "Error deserializing in", e);
		}
    }
}
