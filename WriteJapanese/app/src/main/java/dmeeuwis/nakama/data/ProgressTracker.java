package dmeeuwis.nakama.data;

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
			} else if(in <= -2){
				return Progress.FAILED;
			} else if(in <= 0){
				return Progress.REVIEWING;
			} else {
            	return Progress.PASSED;
			}
		}
	}
	
	final private LinkedHashMap<Character, Integer> recordSheet;

	public ProgressTracker(String characters){
		this.recordSheet = new LinkedHashMap<>(characters.length());
		for(int i = 0; i < characters.length(); i++){
			this.recordSheet.put(characters.charAt(i), null);
		}
	}

	public ProgressTracker(Collection<Character> characters){
		this.recordSheet = new LinkedHashMap<>(characters.size());
		for(Character c: characters){
			this.recordSheet.put(c, null);
		}
	}
	
	private List<Character> charactersMatchingScore(Set<Character> allowedChars, Integer... scores){
		List<Integer> scoresList = Arrays.asList(scores);
		List<Character> matching = new ArrayList<>();
        for(Map.Entry<Character, Integer> c: this.recordSheet.entrySet()){
        	Integer knownScore = c.getValue() == null ? 0 : c.getValue();
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
		if(score == null) score = 0;
        for(Map.Entry<Character, Integer> c: this.recordSheet.entrySet()){
        	Integer knownScore = c.getValue() == null ? 0 : c.getValue();
        	if(score.equals(knownScore) && allowedChars.contains(c.getKey())){
				return c.getKey();
			}
		}
		return null;
	}

	public Character randomMistakenNext(Set<Character> allowedChars){
		List<Character> matching = charactersMatchingScore(allowedChars, -2);
		return matching.size() == 0 ? null : matching.get((int)(Math.random() * matching.size()));
	}

	
	public Character randomReviewingNext(Set<Character> allowedChars){
		List<Character> matching = charactersMatchingScore(allowedChars, -1, 0);
		return matching.size() == 0 ? null : matching.get((int)(Math.random() * matching.size()));
	}
	
	public Character randomCorrectNext(Set<Character> allowedChars){
		List<Character> matching = charactersMatchingScore(allowedChars, 1);
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

	public void updateFromString(String savedString){
        if(savedString == null) return;

		String[] lines = savedString.split("\n");
		for(String l: lines){
			String[] parts = l.split("=");
			if(parts.length < 2){
			} else if(parts[1].equals("!")){
				this.recordSheet.put(parts[0].charAt(0), null);
			} else {
				this.recordSheet.put(parts[0].charAt(0), Math.max(-2, Math.min(2, Integer.parseInt(parts[1]))));
			}
		}
	}

	public String saveToString(){
		StringBuilder sb = new StringBuilder();
		for(Map.Entry<Character, Integer> entry: recordSheet.entrySet()){
			sb.append(entry.getKey().toString());        	
			sb.append("=");
			sb.append(entry.getValue() == null ? "!" : entry.getValue().toString());
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public String toString(){
		return "[ProgressTracker: " + Util.join(", ", this.recordSheet.keySet()) + "]";
	}
}