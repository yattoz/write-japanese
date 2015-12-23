package dmeeuwis.nakama.data;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dmeeuwis.util.Util;

public class ProgressTracker {
	
	public enum Progress { FAILED, REVIEWING, PASSED, UNKNOWN;
		public static Progress parse(Integer in){
        	if(in == null){
				return Progress.UNKNOWN;
			} else if(in <= -200){
				return Progress.FAILED;
			} else if(in <= 0){
				return Progress.REVIEWING;
			} else {
            	return Progress.PASSED;
			}
		}
	}
	
	final private Map<Character, Integer> recordSheet;

	public ProgressTracker(Collection<Character> characters){
		this.recordSheet = new LinkedHashMap<>(characters.size());
		for(Character c: characters){
			this.recordSheet.put(c, null);
		}
	}

    public ProgressTracker(Map<Character, Integer> recordSheet){
        this.recordSheet = recordSheet;
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
	

	public List<Character> charactersNotYetSeen(Set<Character> allowedChars){
		List<Character> matching = new ArrayList<>();
        for(Map.Entry<Character, Integer> c: this.recordSheet.entrySet()){
        	if(c.getValue() == null && allowedChars.contains(c.getKey())){
				matching.add(c.getKey());
			}
		}
		return matching;
	}

	private Character firstCharacterMatching(Integer score, Set<Character> allowedChars){
        for(Map.Entry<Character, Integer> c: this.recordSheet.entrySet()){
        	Integer knownScore = c.getValue();
        	if(score == knownScore || (score != null && score.equals(knownScore) && allowedChars.contains(c.getKey()))){
				return c.getKey();
			}
		}
		return null;
	}

	public Character randomMistakenNext(Set<Character> allowedChars){
		List<Character> matching = charactersMatchingScore(allowedChars, -2);
		Log.i("nakama-progression", "Characters in mistaken: " + Util.join(", ", matching));
		return matching.size() == 0 ? null : matching.get((int)(Math.random() * matching.size()));
	}

	
	public Character randomReviewingNext(Set<Character> allowedChars){
		List<Character> matching = charactersMatchingScore(allowedChars, -1, 0);
		Log.i("nakama-progression", "Characters in review: " + Util.join(", ", matching));
		return matching.size() == 0 ? null : matching.get((int)(Math.random() * matching.size()));
	}
	
	public Character randomCorrectNext(Set<Character> allowedChars){
		List<Character> matching = charactersMatchingScore(allowedChars, 1);
		Log.i("nakama-progression", "Characters in correct: " + Util.join(", ", matching));
		return matching.size() == 0 ? null : matching.get((int)(Math.random() * matching.size()));
	}
	
	public Character standardNext(Set<Character> allowedChars){
		
		// first iterate through the set, one by one
        Character c = firstCharacterMatching(null, allowedChars);
		if(c != null){
			return c;
		}
	
		// after user has seen (and has some rating) for all chars,  then switch to random.
        return shuffleNext(allowedChars);
	}

    public Character randomNext(Set<Character> allowedChars){
        List<Character> matching = charactersMatchingScore(allowedChars, -2, -1, 0, 1);
        if(matching.size() > 0){
            return matching.get((int)(Math.random() * matching.size()));
        }
        throw new RuntimeException("Error: could not find a character to progress to.");
    }

	public Character shuffleNext(Set<Character> allowedChars){
		List<Character> matching = charactersNotYetSeen(allowedChars);
		if(matching.size() > 0){
			return matching.get((int)(Math.random() * matching.size()));
		}

		matching = charactersMatchingScore(allowedChars, -1, -2, 0, 1);
		if(matching.size() > 0){
			return matching.get((int)(Math.random() * matching.size()));
		}

		throw new RuntimeException("Error: could not find a character to progress to.");
	}
	
	public boolean passedAllCharacters(Set<Character> allowedChars){
		List<Character> passed= charactersMatchingScore(allowedChars, 2);
		return passed.size() == allowedChars.size();
	}
	
	public void progressReset(){
		for(Character c: this.recordSheet.keySet()){
			this.recordSheet.put(c, null);
		}
	}

	public void markSuccess(Character c){
		if(!recordSheet.containsKey(c))
			throw new IllegalArgumentException("Character " + c + " is not in dataset. Recordsheet is " + Util.join(", ", recordSheet.keySet()));
		int score = recordSheet.get(c) == null ? 0 : recordSheet.get(c);
		recordSheet.put(c, Math.min(2, score + 1));
	}

	public void markFailure(Character c){
		if(!recordSheet.containsKey(c))
			throw new IllegalArgumentException("Character " + c + " is not in dataset. Recordsheet is " + Util.join(", ", recordSheet.keySet()));
		int score = recordSheet.get(c) == null ? 0 : recordSheet.get(c);
		recordSheet.put(c, Math.max(-2, score - 2));
	}

	public Map<Character, Progress> getAllScores(){
		Map<Character, Progress> all = new HashMap<>(recordSheet.size());
		for(Map.Entry<Character, Integer> entry: recordSheet.entrySet()){
        	all.put(entry.getKey(), Progress.parse(entry.getValue()));
		}
		return all;
	}

	public String toString(){
		return "[ProgressTracker: " + Util.join(", ", this.recordSheet.keySet()) + "]";
	}
}