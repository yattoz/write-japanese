package dmeeuwis.nakama.data;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.Period;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.util.Util;

public class ProgressTracker {

	final static public int MAX_SCORE = 4;

    final Random random = new Random();
	private final boolean useSRS;

	public Map<Character,Integer> getScoreSheet() {
		return this.recordSheet;
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
	
	private final Map<Character, Integer> recordSheet;
	private final Map<Character, Integer> othersRecordSheet;
	private final PriorityQueue<SRSEntry> srsQueue;
	public final boolean useSRSAcrossSets;

	private final int advanceIncorrect;
	private final int advanceReview;

	public static class Result {
		public final int score;
		public final SRSEntry srs;

		public Result(int score, SRSEntry srs) {
			this.score = score;
			this.srs = srs;
		}
	}

	public static class SRSEntry {
		public final Character character;
		public final LocalDate nextPractice;

		private SRSEntry(Character character, LocalDate nextPractice) {
			this.character = character;
			this.nextPractice = nextPractice;
		}
	}

	private final Period[] SRSTable = new Period[] {
		Period.ofDays(1),
		Period.ofDays(3),
		Period.ofDays(7),
		Period.ofDays(14),
        Period.ofDays(30)
	};

	private SRSEntry addToSRSQueue(Character character, int score, LocalDateTime timestamp){
		if(score < 0){
			return null;
		}

		// remove any existing entries
		removeSRSQueue(character);

		// schedule next
		Period delay = SRSTable[score];
		LocalDate nextDate = timestamp.plus(delay).toLocalDate();
		SRSEntry entry = new SRSEntry(character, nextDate);

		srsQueue.add(entry);

		return entry;
	}

	private boolean findInSRSQueue(Character c, LocalDate forTime){
		Iterator<SRSEntry> it = srsQueue.iterator();
		while(it.hasNext()){
			SRSEntry e = it.next();
			if(e.character.equals(c) && (e.nextPractice.isBefore(forTime) || e.nextPractice.isEqual(forTime))){
				return true;
			}
		}
		return false;
	}

	private void removeSRSQueue(Character c){
		Iterator<SRSEntry> it = srsQueue.iterator();
		while(it.hasNext()){
			SRSEntry e = it.next();
			if(e.character.equals(c)){
				srsQueue.remove(e);
				break;
			}
		}
	}

	private static class SRSEntryComparator implements Comparator<SRSEntry> {
		@Override
		public int compare(SRSEntry o1, SRSEntry o2) {
			int compareDate = o1.nextPractice.compareTo(o2.nextPractice);
			if(compareDate != 0){
				return compareDate;
			}

			// if scheduled dates are equal, ensure a consistent ordering
			return o1.character.compareTo(o2.character);
		}

		@Override
		public boolean equals(Object obj) {
			return obj != null && obj.getClass().equals(this.getClass());
		}
	}

	ProgressTracker(Set<Character> allChars, int advanceIncorrect, int advanceReview, boolean useSRS, boolean useSRSAcrossSets){
		this.useSRS = useSRS;
		this.recordSheet = new LinkedHashMap<>();
		this.othersRecordSheet = new LinkedHashMap<>();
		for(Character c: allChars){
			recordSheet.put(c, null);
		}
		this.advanceIncorrect = advanceIncorrect;
		this.advanceReview = advanceReview;
		this.useSRSAcrossSets = useSRSAcrossSets;

		srsQueue = new PriorityQueue<>(20, new SRSEntryComparator());
	}

	private List<Set<Character>> getSets(){
		Set<Character> failed = new LinkedHashSet<>();
		Set<Character> reviewing = new LinkedHashSet<>();
		Set<Character> timedReviewing = new LinkedHashSet<>();
		Set<Character> passed = new LinkedHashSet<>();
		Set<Character> unknown = new LinkedHashSet<>();


        Map<Character, Progress> allScores = getAllScores();
		for(Map.Entry<Character, Progress> score: allScores.entrySet()){
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

		LinkedHashSet<Character> allChars = new LinkedHashSet<>(rawAllChars);
		LinkedHashSet<Character> availSet = new LinkedHashSet<>(rawAvailSet);

		if(useSRS) {
			SRSEntry soonestEntry = srsQueue.peek();
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

		List<Set<Character>> sets = getSets();
		Set<Character> failed = sets.get(0);
		Set<Character> reviewing = sets.get(1);
		Set<Character> timedReview = sets.get(2);
		Set<Character> passed = sets.get(3);
		Set<Character> unknown = sets.get(4);

		if(BuildConfig.DEBUG) {
			Log.i("nakama-progression", "Character progression: reviewing sets");
			Log.i("nakama-progression", "Failed set is: " + Util.join(", ", failed));
			Log.i("nakama-progression", "Reviewing set is: " + Util.join(", ", reviewing));
			Log.i("nakama-progression", "Timed Reviewing set is: " + Util.join(", ", timedReview));
			Log.i("nakama-progression", "Passed set is: " + Util.join(", ", passed));
			Log.i("nakama-progression", "Unknown set is: " + Util.join(", ", unknown));
		}

        // probs array: failed, reviewing, unknown, passed
        float[] probs;

        // still learning new chars, but maxed out the reviewing and failed buckets so just review
        if(failed.size() >= introIncorrect || reviewing.size() > introReviewing) {
            Log.i("nakama-progress", "Failed or Review buckets maxed out, reviewing 50/50");
            probs = new float[]{0.5f, 0.5f, 0.0f, 0.0f, 0.0f};

        // still learning new chars, haven't seen standardSets
        } else if(unknown.size() > 0) {
			Log.i("nakama-progress", "Still room in failed and review buckets, chance of new characters");
            probs = new float[]{0.35f, 0.30f, 0.0f, 0.35f, 0.0f};

        // have seen standardSets characters, still learning
        } else if(unknown.size() == 0){
			Log.i("nakama-progress", "Have seen standardSets characters, reviewing 40/40/0/20");
            probs = new float[] { 0.30f, 0.30f, 0.2f, 0.0f, 0.2f };

        // what situation is this?
        } else {
			Log.i("nakama-progress", "Unknown situation, reviewing 25/25/25/25.");
            probs = new float[] { 0.25f, 0.25f, 0.0f, 0.25f, 0.25f };
        }

        availSet.remove(currentChar);
		failed.remove(currentChar);
		reviewing.remove(currentChar);
		passed.remove(currentChar);
		timedReview.remove(currentChar);
		unknown.remove(currentChar);

		Set<Character> chosenOnes = new HashSet<>();

        if(ran <= probs[0] && failed.size() > 0){
			chosenOnes.addAll(failed);
        } else if(ran <= (probs[0] + probs[1]) && (failed.size() > 0 || reviewing.size() > 0)){
			chosenOnes.addAll(failed);
			chosenOnes.addAll(reviewing);
		} else if(ran <= (probs[0] + probs[1] + probs[2]) && unknown.size() > 0){
			if(shuffling){
				chosenOnes.addAll(unknown);
			} else {
				chosenOnes.add(sortAndReturnFirst(allChars, unknown));
			}
        } else {
			chosenOnes.addAll(failed);
			chosenOnes.addAll(reviewing);
			chosenOnes.addAll(timedReview);
			chosenOnes.addAll(unknown);
			chosenOnes.addAll(passed);
		}

		final Character[] next = chosenOnes.toArray(new Character[0]);
		final Character n = next[(int)(ran * next.length)];
		final boolean isReview = failed.contains(n) || reviewing.contains(n) || passed.contains(n);

		if(BuildConfig.DEBUG) {
			Log.i("nakama-progression", "Potential set is: " + Util.join(", ", chosenOnes));
			Log.i("nakama-progression", "Picked: " + n + (isReview ? ", review" : ", fresh"));
		}
		return Pair.create(n, isReview ? StudyType.REVIEW : StudyType.NEW_CHAR);
    }

	private Character sortAndReturnFirst(Set<Character> allChars, Set<Character> unknown) {
		final List<Character> chars = new ArrayList<>(allChars);
		//Log.d("nakama-progression", "Sorting allchars: " + Util.join(", ", chars)  + " to order unknown set " + Util.join(", ", unknown));

		final HashMap<Character, Integer> indexed = new HashMap<>(chars.size());
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
				if(i1 == null) { return i2.compareTo(null); }
				return i1.compareTo(i2);
			}
		});

		return toSort.get(0);
	}

	public StudyType isReviewing(Character c){
		if(findInSRSQueue(c, LocalDate.now())){
			return StudyType.SRS;
		}
		List<Set<Character>> sets = getSets();
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
		for(Character c: this.recordSheet.keySet()){
			this.recordSheet.put(c, null);
			removeSRSQueue(c);
		}
	}

	public void srsReset(String setId) {
		List<Character> charsToReset = new ArrayList<>(srsQueue.size());
		for(SRSEntry o: srsQueue){
			charsToReset.add(o.character);
		}

		for(Character c: charsToReset){
			overrideFullCompleted(c);
		}
	}

	public void overrideFullCompleted(Character c){
		boolean charInCurrentSet = recordSheet.containsKey(c);
		Map<Character, Integer> scoreSheetToUse;
		if(charInCurrentSet){
			scoreSheetToUse = recordSheet;
		} else {
			scoreSheetToUse = othersRecordSheet;
		}
		int score = MAX_SCORE;
		scoreSheetToUse.put(c, score);

		removeSRSQueue(c);
	}

	public Result markSuccess(Character c, LocalDateTime time){
		boolean charInCurrentSet = recordSheet.containsKey(c);
		Map<Character, Integer> scoreSheetToUse;
		if(charInCurrentSet){
			scoreSheetToUse = recordSheet;
		} else {
			scoreSheetToUse = othersRecordSheet;
		}
		int score = scoreSheetToUse.get(c) == null ? 0 : scoreSheetToUse.get(c);
		int newScore = Math.min(MAX_SCORE, score + 1);
		scoreSheetToUse.put(c, Math.min(0, newScore));

		if(charInCurrentSet || useSRSAcrossSets) {
			SRSEntry addedToSrs = addToSRSQueue(c, newScore, time);
			return new Result(newScore, addedToSrs);
		}

		return new Result(newScore, null);
	}

	public Result markFailure(Character c){
		removeSRSQueue(c);

		boolean charInCurrentSet = recordSheet.containsKey(c);
		Map<Character, Integer> scoreSheetToUse;
		if(charInCurrentSet){
			scoreSheetToUse = recordSheet;
		} else {
			scoreSheetToUse = othersRecordSheet;
		}

		scoreSheetToUse.put(c, failScore());
		return new Result(failScore(), null);
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

    public Map<LocalDate, List<Character>> getSrsSchedule() {
        Map<LocalDate, List<Character>> out = new LinkedHashMap<>();
        SRSEntry[] entries = srsQueue.toArray(new SRSEntry[0]);
        Arrays.sort(entries, new SRSEntryComparator());
        for(SRSEntry s: entries){
            List<Character> list = out.get(s.nextPractice);
            if(list == null){
                list = new ArrayList<>();
                out.put(s.nextPractice, list);
            }
            list.add(s.character);
        }
        return out;
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

	private int knownScore(){
		return  MAX_SCORE;
	}

	public void resetTo(Character character, Progress progress) {
		if(!recordSheet.containsKey(character)){
			return;
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

}
