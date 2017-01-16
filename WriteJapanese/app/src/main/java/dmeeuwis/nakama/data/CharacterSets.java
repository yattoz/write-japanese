package dmeeuwis.nakama.data;

import java.util.UUID;

import dmeeuwis.Kana;
import dmeeuwis.Kanji;
import dmeeuwis.nakama.LockChecker;

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

	static public CharacterStudySet fromName(String name, LockChecker LockChecker, UUID iid){
		if(name.equals("hiragana")){ return hiragana(LockChecker, iid); }
		else if(name.equals("katakana")){ return katakana(LockChecker, iid); }
		else if(name.equals("j1")){ return joyouG1(LockChecker, iid); }
		else if(name.equals("j2")){ return joyouG2(LockChecker, iid); }
		else if(name.equals("j3")){ return joyouG3(LockChecker, iid); }
		else if(name.equals("j4")){ return joyouG4(LockChecker, iid); }
		else if(name.equals("j5")){ return joyouG5(LockChecker, iid); }
		else if(name.equals("j6")){ return joyouG6(LockChecker, iid); }
		else { throw new RuntimeException("Unknown character set: " + name); }
	}

	static public CharacterStudySet createCustom(UUID iid){
		return new CharacterStudySet("Custom Set", "Custom", "Your custom character set", UUID.randomUUID().toString(), CharacterStudySet.LockLevel.UNLOCKED, "", "", null, iid, false);
	}

	public static CharacterStudySet hiragana(LockChecker LockChecker, UUID iid){ return new CharacterStudySet("Hiragana", "Hiragana", HIRAGANA_DESC, "hiragana", CharacterStudySet.LockLevel.UNLOCKED, Kana.commonHiragana(), "", LockChecker, iid, true); }
	public static CharacterStudySet katakana(LockChecker LockChecker, UUID iid){ return new CharacterStudySet("Katakana", "Katakana", KATAKANA_DESC, "katakana", CharacterStudySet.LockLevel.LOCKED, Kana.commonKatakana(), "アイネホキタロマザピド", LockChecker, iid, true); }
	public static CharacterStudySet joyouG1(LockChecker lc, UUID iid){ return new CharacterStudySet("Joyou Kanji 1", "Kanji J1", G1_DESCRIPTION, "j1", CharacterStudySet.LockLevel.UNLOCKED, Kanji.JOUYOU_G1, "", lc, iid, true); }
	public static CharacterStudySet joyouG2(LockChecker lc, UUID iid){ return new CharacterStudySet("Joyou Kanji 2", "Kanji J2", G2_DESCRIPTION, "j2", CharacterStudySet.LockLevel.LOCKED, Kanji.JOUYOU_G2, "内友行光図店星食記親", lc, iid, true); }
	public static CharacterStudySet joyouG3(LockChecker lc, UUID iid){ return new CharacterStudySet("Joyou Kanji 3", "Kanji J3", G3_DESCRIPTION, "j3", CharacterStudySet.LockLevel.LOCKED, Kanji.JOUYOU_G3, "申両世事泳指暗湯昭様", lc, iid, true); }
	public static CharacterStudySet joyouG4(LockChecker lc, UUID iid){ return new CharacterStudySet("Joyou Kanji 4", "Kanji J4", G4_DESCRIPTION, "j4", CharacterStudySet.LockLevel.LOCKED, Kanji.JOUYOU_G4, "令徒貨例害覚停副議給", lc, iid, true); }
	public static CharacterStudySet joyouG5(LockChecker lc, UUID iid){ return new CharacterStudySet("Joyou Kanji 5", "Kanji J5", G5_DESCRIPTION, "j5", CharacterStudySet.LockLevel.LOCKED, Kanji.JOUYOU_G5, "犯寄舎財税統像境飼謝", lc, iid, true); }
	public static CharacterStudySet joyouG6(LockChecker lc, UUID iid){ return new CharacterStudySet("Joyou Kanji 6", "Kanji J6", G6_DESCRIPTION, "j6", CharacterStudySet.LockLevel.LOCKED, Kanji.JOUYOU_G6, "至捨推針割疑層模訳欲", lc, iid, true); }


    public static CharacterStudySet[] all(LockChecker lc, UUID iid) {
        return new CharacterStudySet[]{
                hiragana(lc, iid),
                katakana(lc, iid),
                joyouG1(lc, iid),
                joyouG2(lc, iid),
                joyouG3(lc, iid),
                joyouG4(lc, iid),
                joyouG5(lc, iid),
                joyouG6(lc, iid)
        };
    }
}
