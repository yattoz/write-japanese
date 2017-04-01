package dmeeuwis.nakama.data;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.util.Util;

public class ProgressTracker {
    final Random random = new Random();

    public enum Progress { FAILED, REVIEWING, PASSED, UNKNOWN;
		private static Progress parse(Integer in, int advanceReviewing){
        	if(in == null){
				return Progress.UNKNOWN;
			} else if(in < -1 * advanceReviewing){
				return Progress.FAILED;
			} else if(in < 0){
				return Progress.REVIEWING;
			} else {
            	return Progress.PASSED;
			}
		}
	}
	
	private final Map<Character, Integer> recordSheet;
	private final int advanceIncorrect;
	private final int advanceReview;

	ProgressTracker(Map<Character, Integer> recordSheet, int advanceIncorrect, int advanceReview){
        this.recordSheet = recordSheet;
		this.advanceIncorrect = advanceIncorrect;
		this.advanceReview = advanceReview;
	}

	private List<Set<Character>> getSets(){
		Set<Character> failed = new LinkedHashSet<>();
		Set<Character> reviewing = new LinkedHashSet<>();
		Set<Character> passed = new LinkedHashSet<>();
		Set<Character> unknown = new LinkedHashSet<>();

		for(Map.Entry<Character, Progress> score: getAllScores().entrySet()){
			if(score.getValue() == Progress.FAILED){
				failed.add(score.getKey());
			} else if(score.getValue() == Progress.REVIEWING){
				reviewing.add(score.getKey());
			} else if(score.getValue() == Progress.UNKNOWN){
				unknown.add(score.getKey());
			} else if(score.getValue() == Progress.PASSED){
				passed.add(score.getKey());
			} else {
				Log.e("nakama-progression", "Skipping unknown Progress: " + score.getValue());
			}
		}

		return Arrays.asList(failed, reviewing, passed, unknown);
	}

    Pair<Character, Boolean> nextCharacter(Set<Character> rawAllChars, Character currentChar, Set<Character> rawAvailSet, boolean shuffling,
												  int introIncorrect, int introReviewing) {

		LinkedHashSet<Character> allChars = new LinkedHashSet<>(rawAllChars);
		LinkedHashSet<Character> availSet = new LinkedHashSet<>(rawAvailSet);


		if(availSet.size() == 1){
			Log.i("nakama-progression", "Returning early from nextCharacter, only 1 character in set");
			return Pair.create(availSet.toArray(new Character[0])[0], Boolean.TRUE);
		}

        double ran = random.nextDouble();

		List<Set<Character>> sets = getSets();
		Set<Character> failed = sets.get(0);
		Set<Character> reviewing = sets.get(1);
		Set<Character> passed = sets.get(2);
		Set<Character> unknown = sets.get(3);

		if(BuildConfig.DEBUG) {
			Log.i("nakama-progression", "Character progression: reviewing sets");
			Log.i("nakama-progression", "Failed set is: " + Util.join(", ", failed));
			Log.i("nakama-progression", "Reviewing set is: " + Util.join(", ", reviewing));
			Log.i("nakama-progression", "Passed set is: " + Util.join(", ", passed));
			Log.i("nakama-progression", "Unknown set is: " + Util.join(", ", unknown));
		}

        // probs array: failed, reviewing, unknown, passed
        float[] probs;

        // still learning new chars, but maxed out the reviewing and failed buckets so just review
        if(failed.size() >= introIncorrect || reviewing.size() > introReviewing) {
            Log.i("nakama-progress", "Failed or Review buckets maxed out, reviewing 50/50");
            probs = new float[]{0.5f, 0.5f, 0.0f, 0.0f};

        // still learning new chars, haven't seen all
        } else if(unknown.size() > 0) {
			Log.i("nakama-progress", "Still room in failed and review buckets, chance of new characters");
            probs = new float[]{0.35f, 0.30f, 0.35f, 0.0f};

        // have seen all characters, still learning
        } else if(unknown.size() == 0){
			Log.i("nakama-progress", "Have seen all characters, reviewing 40/40/0/20");
            probs = new float[] { 0.40f, 0.40f, 0.0f, 0.2f };

        // what situation is this?
        } else {
			Log.i("nakama-progress", "Unknown situation, reviewing 25/25/25/25.");
            probs = new float[] { 0.25f, 0.25f, 0.25f, 0.25f };
        }

        availSet.remove(currentChar);
		failed.remove(currentChar);
		reviewing.remove(currentChar);
		passed.remove(currentChar);
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
		return Pair.create(n, isReview);
    }

	private Character sortAndReturnFirst(Set<Character> allChars, Set<Character> unknown) {
		final List<Character> chars = new ArrayList<>(allChars);
		Log.d("nakama-progression", "Sorting allchars: " + Util.join(", ", chars)  + " to order unknown set " + Util.join(", ", unknown));

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

	public boolean isReviewing(Character c){
		List<Set<Character>> sets = getSets();
		Set<Character> failed = sets.get(0);
		Set<Character> reviewing = sets.get(1);

		boolean r = failed.contains(c) || reviewing.contains(c);
		if(BuildConfig.DEBUG) {
			Log.i("nakama-progression", "Failed set is: " + Util.join(", ", failed));
			Log.i("nakama-progression", "Reviewing set is: " + Util.join(", ", reviewing));
			Log.i("nakama-progression", "Character progression: is " + c + " reviewing? " + r);
		}
		return r;
	}


    public CharacterStudySet.SetProgress calculateProgress(){
        int known = 0, reviewing = 0, failed = 0, unknown = 0;
        for(Map.Entry<Character, Progress> c: getAllScores().entrySet()){
            if(c.getValue() == Progress.FAILED){
                failed++;
            } else if(c.getValue() == Progress.REVIEWING){
                reviewing++;
            } else if(c.getValue() == Progress.PASSED){
                known++;
            } else if(c.getValue() == Progress.UNKNOWN){
                unknown++;
            }
        }
        return new CharacterStudySet.SetProgress(known, reviewing, failed, unknown);
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
	
	public void progressReset(){
		for(Character c: this.recordSheet.keySet()){
			this.recordSheet.put(c, null);
		}
	}

	public void markSuccess(Character c){
		Log.i("nakama-progression", "Marking success on char " + c);
		if(!recordSheet.containsKey(c))
			throw new IllegalArgumentException("Character " + c + " is not in dataset. Recordsheet is " + Util.join(", ", recordSheet.keySet()));
		int score = recordSheet.get(c) == null ? 0 : recordSheet.get(c);
		recordSheet.put(c, Math.min(0, score + 1));
		Log.d("nakama-progression", "Correct: char " + c + " now has score " + recordSheet.get(c));
	}

	public void markFailure(Character c){
		Log.i("nakama-progression", "Marking failure on char " + c);
		if(!recordSheet.containsKey(c))
			throw new IllegalArgumentException("Character " + c + " is not in dataset. Recordsheet is " + Util.join(", ", recordSheet.keySet()));
		recordSheet.put(c, -1 * (advanceIncorrect + advanceReview));
		Log.d("nakama-progression", "Incorrect: char " + c + " now has score " + recordSheet.get(c));
	}

	public Map<Character, Progress> getAllScores(){
		Map<Character, Progress> all = new HashMap<>(recordSheet.size());
		for(Map.Entry<Character, Integer> entry: recordSheet.entrySet()){
        	all.put(entry.getKey(), Progress.parse(entry.getValue(), advanceReview));
		}
		return all;
	}

	public String toString(){
		return "[ProgressTracker: " + Util.join(", ", this.recordSheet.keySet()) + "]";
	}
}