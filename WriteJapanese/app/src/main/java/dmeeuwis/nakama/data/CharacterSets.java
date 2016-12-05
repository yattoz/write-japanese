package dmeeuwis.nakama.data;

import java.io.IOException;
import java.util.UUID;

import dmeeuwis.Kana;
import dmeeuwis.Kanji;
import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.nakama.LockChecker;
import dmeeuwis.util.Util;

public class CharacterSets  {

    private static final String HIRAGANA_DESC = "Hiragana is the most basic and essential script in Japan. It is the primary phonetic alphabet.\n" +
            "Japanese schoolchildren learn their hiragana by Grade 1, at around 5 years of age.";
    private static final String KATAKANA_DESC = "The second Japanese phonetic alphabet, Katakana is used for foreign words imported into Japanese, or emphasis. " +
                                        "Schoolchildren learn katakana by Grade 1, at around 5 years of age.";
    private static final String G1_DESCRIPTION = "The first level of 'regular use kanji' (常用漢字). Learned by Japanese schoolchildren in their first year of school, at around 5 years of age.";
    private static final String G2_DESCRIPTION = "The second level of 'regular use kanji' (常用漢字). Learned by Japanese schoolchildren in their second year of school, at around 6 years of age.";
    private static final String G3_DESCRIPTION = "The third level of 'regular use kanji' (常用漢字). Learned by Japanese schoolchildren in their third year of school, at around 7 years of age.";
    private static final String G4_DESCRIPTION = "The fourth level of 'regular use kanji' (常用漢字). Learned by Japanese schoolchildren in their fourth year of school, at around 8 years of age.";
    private static final String G5_DESCRIPTION = "The fifth level of 'regular use kanji' (常用漢字). Learned by Japanese schoolchildren in their fifth year of school, at around 9 years of age.";
    private static final String G6_DESCRIPTION = "The sixth level of 'regular use kanji' (常用漢字). Learned by Japanese schoolchildren in their sixth year of school, at around 10 years of age.";

	static public CharacterStudySet fromName(String name, KanjiFinder kf, LockChecker LockChecker, UUID iid){
		if(name.equals("hiragana")){ return hiragana(LockChecker, iid); }
		else if(name.equals("katakana")){ return katakana(LockChecker, iid); }
		else if(name.equals("j1")){ return joyouG1(kf, LockChecker, iid); }
		else if(name.equals("j2")){ return joyouG2(kf, LockChecker, iid); }
		else if(name.equals("j3")){ return joyouG3(kf, LockChecker, iid); }
		else if(name.equals("j4")){ return joyouG4(kf, LockChecker, iid); }
		else if(name.equals("j5")){ return joyouG5(kf, LockChecker, iid); }
		else if(name.equals("j6")){ return joyouG6(kf, LockChecker, iid); }
		else { throw new RuntimeException("Unknown character set: " + name); }
	}
	
	
	public static CharacterStudySet hiragana(LockChecker LockChecker, UUID iid){
    	return new CharacterStudySet("Hiragana", "Hiragana", HIRAGANA_DESC, "hiragana", CharacterStudySet.LockLevel.UNLOCKED, Kana.commonHiragana(), "", LockChecker, iid){
			@Override public String label(){
   		 		return "hiragana";
	   		}

			@Override public String[] currentCharacterClues() {
				return new String[] { Kana.kana2Romaji(Character.toString(currentCharacter())) };
			}
		};
	}

	public static CharacterStudySet katakana(LockChecker LockChecker, UUID iid){
		return 
		 new CharacterStudySet("Katakana", "Katakana", KATAKANA_DESC, "katakana", CharacterStudySet.LockLevel.LOCKED, Kana.commonKatakana(), "アイネホキタロマザピド", LockChecker, iid){
				@Override public String label(){
					return "katakana";
				}

				@Override public String[] currentCharacterClues() {
					return new String[] { Kana.kana2Romaji(Character.toString(currentCharacter())) };
				}
			};
	}

	public static CharacterStudySet joyouG1(KanjiFinder kf, LockChecker lc, UUID iid){ return new KanjiCharacterStudySet("Joyou Kanji 1", "Kanji J1", G1_DESCRIPTION, "j1", Kanji.JOUYOU_G1, "", kf, CharacterStudySet.LockLevel.UNLOCKED, lc, iid); };
	public static CharacterStudySet joyouG2(KanjiFinder kf, LockChecker lc, UUID iid){ return new KanjiCharacterStudySet("Joyou Kanji 2", "Kanji J2", G2_DESCRIPTION, "j2", Kanji.JOUYOU_G2, "内友行光図店星食記親", kf, CharacterStudySet.LockLevel.LOCKED, lc, iid); }
	public static CharacterStudySet joyouG3(KanjiFinder kf, LockChecker lc, UUID iid){ return new KanjiCharacterStudySet("Joyou Kanji 3", "Kanji J3", G3_DESCRIPTION, "j3", Kanji.JOUYOU_G3, "申両世事泳指暗湯昭様", kf, CharacterStudySet.LockLevel.LOCKED, lc, iid); }
	public static CharacterStudySet joyouG4(KanjiFinder kf, LockChecker lc, UUID iid){ return new KanjiCharacterStudySet("Joyou Kanji 4", "Kanji J4", G4_DESCRIPTION, "j4", Kanji.JOUYOU_G4, "令徒貨例害覚停副議給", kf, CharacterStudySet.LockLevel.LOCKED, lc, iid); }
	public static CharacterStudySet joyouG5(KanjiFinder kf, LockChecker lc, UUID iid){ return new KanjiCharacterStudySet("Joyou Kanji 5", "Kanji J5", G5_DESCRIPTION, "j5", Kanji.JOUYOU_G5, "犯寄舎財税統像境飼謝", kf, CharacterStudySet.LockLevel.LOCKED, lc, iid); }
	public static CharacterStudySet joyouG6(KanjiFinder kf, LockChecker lc, UUID iid){ return new KanjiCharacterStudySet("Joyou Kanji 6", "Kanji J6", G6_DESCRIPTION, "j6", Kanji.JOUYOU_G6, "至捨推針割疑層模訳欲", kf, CharacterStudySet.LockLevel.LOCKED, lc, iid); }

	private static class KanjiCharacterStudySet extends CharacterStudySet {
		private final KanjiFinder kanjiFinder;
		
		public KanjiCharacterStudySet(String name, String shortName, String desc, String path, String data, String freeData, KanjiFinder kanjiFinder, LockLevel locked, LockChecker LockChecker, UUID iid) {
			super(name, shortName, desc, path, locked, data, freeData, LockChecker, iid);
			this.kanjiFinder = kanjiFinder;
		}

		@Override
		public String currentCharacterCluesText(int currentCharacterClueIndex) {
			readings: if(Kana.isKanji(currentCharacter()) && Settings.getClueType(LockChecker.getParentActivity()) == Settings.ClueType.VOCAB) {
				return currentCharacterClueIndex == 0 ?
					"Draw the kanji with reading" :
					"which can also be read as";
			}
			return super.currentCharacterCluesText(currentCharacterClueIndex);
		}

		@Override public String label(){
			return "kanji";
		}

		@Override public String[] currentCharacterClues() {
			try {
				// clue from readings
				readings: if(Kana.isKanji(currentCharacter()) && Settings.getClueType(LockChecker.getParentActivity()) == Settings.ClueType.VOCAB) {
					Kanji k;
					try {
						k = kanjiFinder.find(currentCharacter());
					} catch (IOException e) {
						break readings;
					}
					String[] readings = Util.concat(k.onyomi, k.kunyomi);
					return readings;
				}


				Kanji k = kanjiFinder.find(currentCharacter());
				return k.meanings;
			} catch (IOException e) {
				throw new RuntimeException(e);
			} 
		}
	}
}
