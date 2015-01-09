package dmeeuwis.nakama.data;

import java.io.IOException;

import android.util.Log;
import dmeeuwis.Kana;
import dmeeuwis.Kanji;
import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.nakama.LockChecker;

public class CharacterSets  {
	
	static public CharacterStudySet fromName(String name, KanjiFinder kf, LockChecker lockChecker){
		if(name.equals("hiragana")){ return hiragana(lockChecker); }
		else if(name.equals("katakana")){ return katakana(lockChecker); } 
		else if(name.equals("j1")){ return joyouG1(kf, lockChecker); } 
		else if(name.equals("j2")){ return joyouG2(kf, lockChecker); } 
		else if(name.equals("j3")){ return joyouG3(kf, lockChecker); } 
		else if(name.equals("j4")){ return joyouG4(kf, lockChecker); } 
		else if(name.equals("j5")){ return joyouG5(kf, lockChecker); } 
		else if(name.equals("j6")){ return joyouG6(kf, lockChecker); } 
		else { throw new RuntimeException("Unknown character set: " + name); }
	}
	
	
	public static CharacterStudySet hiragana(LockChecker lockChecker){
    	return new CharacterStudySet("Hiragana", "hiragana", CharacterStudySet.LockLevel.UNLOCKED, Kana.commonHiragana(), "", lockChecker){
			@Override public String label(){
   		 		return "hiragana";
	   		}

			@Override public String[] currentCharacterClues() {
				return new String[] { Kana.kana2Romaji(Character.toString(currentCharacter())) };
			}
		};
	}

	public static CharacterStudySet katakana(LockChecker lockChecker){
		return 
		 new CharacterStudySet("Katakana", "katakana", CharacterStudySet.LockLevel.LOCKED, Kana.commonKatakana(), "アイネホキタロマザピド", lockChecker){
				@Override public String label(){
					return "katakana";
				}

				@Override public String[] currentCharacterClues() {
					return new String[] { Kana.kana2Romaji(Character.toString(currentCharacter())) };
				}
			};
	}

	public static CharacterStudySet joyouG1(KanjiFinder kf, LockChecker lc){ return new KanjiCharacterStudySet("Kanji J1", "j1", Kanji.JOUYOU_G1, "", kf, CharacterStudySet.LockLevel.UNLOCKED, lc); };
	public static CharacterStudySet joyouG2(KanjiFinder kf, LockChecker lc){ return new KanjiCharacterStudySet("Kanji J2", "j2", Kanji.JOUYOU_G2, "内友行光図店星食記親", kf, CharacterStudySet.LockLevel.LOCKED, lc); }
	public static CharacterStudySet joyouG3(KanjiFinder kf, LockChecker lc){ return new KanjiCharacterStudySet("Kanji J3", "j3", Kanji.JOUYOU_G3, "申両世事泳指暗湯昭様", kf, CharacterStudySet.LockLevel.LOCKED, lc); }
	public static CharacterStudySet joyouG4(KanjiFinder kf, LockChecker lc){ return new KanjiCharacterStudySet("Kanji J4", "j4", Kanji.JOUYOU_G4, "令徒貨例害覚停副議給", kf, CharacterStudySet.LockLevel.LOCKED, lc); }
	public static CharacterStudySet joyouG5(KanjiFinder kf, LockChecker lc){ return new KanjiCharacterStudySet("Kanji J5", "j5", Kanji.JOUYOU_G5, "犯寄舎財税統像境飼謝", kf, CharacterStudySet.LockLevel.LOCKED, lc); }
	public static CharacterStudySet joyouG6(KanjiFinder kf, LockChecker lc){ return new KanjiCharacterStudySet("Kanji J6", "j6", Kanji.JOUYOU_G6, "至捨推針割疑層模訳欲", kf, CharacterStudySet.LockLevel.LOCKED, lc); }

	private static class KanjiCharacterStudySet extends CharacterStudySet {
		private final KanjiFinder kanjiFinder;
		
		public KanjiCharacterStudySet(String name, String path, String data, String freeData, KanjiFinder kanjiFinder, LockLevel locked, LockChecker lockChecker) {
			super(name, path, locked, data, freeData, lockChecker);
			this.kanjiFinder = kanjiFinder;
		}
		
		@Override public String label(){
			return "kanji";
		}

		@Override public String[] currentCharacterClues() {
			try {
				Kanji k = kanjiFinder.find(currentCharacter());
				Log.d("nakama", "currentCharacterClue:  Matched current character " + currentCharacter() + " to " + k);
				return k.meanings;
			} catch (IOException e) {
				throw new RuntimeException(e);
			} 
		}
	}
}
