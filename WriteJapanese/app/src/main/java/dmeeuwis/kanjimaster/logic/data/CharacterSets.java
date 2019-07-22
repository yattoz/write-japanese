package dmeeuwis.kanjimaster.logic.data;

import java.util.UUID;

import dmeeuwis.kanjimaster.core.Kana;
import dmeeuwis.kanjimaster.core.Kanji;
import dmeeuwis.kanjimaster.ui.billing.LockChecker;
import dmeeuwis.kanjimaster.ui.sections.primary.IidAndroid;
import dmeeuwis.kanjimaster.ui.sections.primary.IidFactory;

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
	private static final String HS_DESCRIPTION = "The seventh level of 'regular use kanji' (常用漢字). Learned by Japanese schoolchildren during Junior High School.";

    private static final String JLPT5_DESCRIPTION = "The first and simplest level of required characters for the Japanese Language Proficiency Test (JLPT). Basic reading abilities.";
    private static final String JLPT4_DESCRIPTION = "The second level of required kanji for the Japanese Language Proficiency Test (JLPT). Familiar with daily happenings.";
    private static final String JLPT3_DESCRIPTION = "The third level of required kanji for the Japanese Language Proficiency Test (JLPT). Should be able to read newspaper headlines.";
    private static final String JLPT2_DESCRIPTION = "The fourth level of required kanji for the Japanese Language Proficiency Test (JLPT). Read realistic Japanese books.";
    private static final String JLPT1_DESCRIPTION = "The fifth level of required kanji for the Japanese Language Proficiency Test (JLPT). Covers all characters for adult level Japanese.";

	static public CharacterStudySet fromName(String name, LockChecker LockChecker){
        if(name == null) {
            return joyouG1(LockChecker);
        }

		if(name.equals("hiragana")){ return hiragana(LockChecker); }
		else if(name.equals("katakana")){ return katakana(LockChecker); }
		else if(name.equals("j1")){ return joyouG1(LockChecker); }
		else if(name.equals("j2")){ return joyouG2(LockChecker); }
		else if(name.equals("j3")){ return joyouG3(LockChecker); }
		else if(name.equals("j4")){ return joyouG4(LockChecker); }
		else if(name.equals("j5")){ return joyouG5(LockChecker); }
		else if(name.equals("j6")){ return joyouG6(LockChecker); }
		else if(name.equals("jhs")){ return joyouHS(LockChecker); }

		else if(name.equals("jlpt1")){ return jlptN1(LockChecker); }
		else if(name.equals("jlpt2")){ return jlptN2(LockChecker); }
		else if(name.equals("jlpt3")){ return jlptN3(LockChecker); }
		else if(name.equals("jlpt4")){ return jlptN4(LockChecker); }
		else if(name.equals("jlpt5")){ return jlptN5(LockChecker); }

		else {
			CharacterStudySet s = new CustomCharacterSetDataHelper().get(name);
			if(s != null){
				return s;
			}
			throw new RuntimeException("Unknown character set: " + name);
		}
	}

	static public CharacterStudySet createCustom(){
		return new CharacterStudySet("", "", "", UUID.randomUUID().toString(), CharacterStudySet.LockLevel.UNLOCKED, "", "", null, IidFactory.get(), false);
	}

	public static CharacterStudySet hiragana(LockChecker LockChecker){ return new CharacterStudySet("Hiragana", "Hiragana", HIRAGANA_DESC, "hiragana", CharacterStudySet.LockLevel.UNLOCKED, Kana.commonHiragana(), "", LockChecker, IidFactory.get(), true); }
	public static CharacterStudySet katakana(LockChecker LockChecker){ return new CharacterStudySet("Katakana", "Katakana", KATAKANA_DESC, "katakana", CharacterStudySet.LockLevel.LOCKED, Kana.commonKatakana(), "アイネホキタロマザピド", LockChecker, IidFactory.get(), true); }
	public static CharacterStudySet joyouG1(LockChecker lc){ return new CharacterStudySet("Joyou Kanji 1", "Kanji J1", G1_DESCRIPTION, "j1", CharacterStudySet.LockLevel.UNLOCKED, Kanji.JOUYOU_G1, "", lc, IidFactory.get(), true); }
	public static CharacterStudySet joyouG2(LockChecker lc){ return new CharacterStudySet("Joyou Kanji 2", "Kanji J2", G2_DESCRIPTION, "j2", CharacterStudySet.LockLevel.LOCKED, Kanji.JOUYOU_G2,   "内友行光図店星食記親", lc, IidFactory.get(), true); }
	public static CharacterStudySet joyouG3(LockChecker lc){ return new CharacterStudySet("Joyou Kanji 3", "Kanji J3", G3_DESCRIPTION, "j3", CharacterStudySet.LockLevel.LOCKED, Kanji.JOUYOU_G3,   "申両世事泳指暗湯昭様", lc, IidFactory.get(), true); }
	public static CharacterStudySet joyouG4(LockChecker lc){ return new CharacterStudySet("Joyou Kanji 4", "Kanji J4", G4_DESCRIPTION, "j4", CharacterStudySet.LockLevel.LOCKED, Kanji.JOUYOU_G4,   "令徒貨例害覚停副議給", lc, IidFactory.get(), true); }
	public static CharacterStudySet joyouG5(LockChecker lc){ return new CharacterStudySet("Joyou Kanji 5", "Kanji J5", G5_DESCRIPTION, "j5", CharacterStudySet.LockLevel.LOCKED, Kanji.JOUYOU_G5,   "犯寄舎財税統像境飼謝", lc, IidFactory.get(), true); }
	public static CharacterStudySet joyouG6(LockChecker lc){ return new CharacterStudySet("Joyou Kanji 6", "Kanji J6", G6_DESCRIPTION, "j6", CharacterStudySet.LockLevel.LOCKED, Kanji.JOUYOU_G6,   "至捨推針割疑層模訳欲", lc, IidFactory.get(), true); }
	public static CharacterStudySet joyouHS(LockChecker lc){ return new CharacterStudySet("Joyou Kanji JHS", "Kanji JHS", HS_DESCRIPTION, "jhs", CharacterStudySet.LockLevel.LOCKED, Kanji.JOUYOU_SS, "充妄企仰伐伏旬旨匠如", lc, IidFactory.get(), true); }

    public static CharacterStudySet jlptN5(LockChecker lc){ return new CharacterStudySet("JLPT N5", "JLPT N5", JLPT5_DESCRIPTION, "jlpt5", CharacterStudySet.LockLevel.UNLOCKED, Kanji.JLPT_N5, "", lc, IidFactory.get(), true); }
    public static CharacterStudySet jlptN4(LockChecker lc){ return new CharacterStudySet("JLPT N4", "JLPT N4", JLPT4_DESCRIPTION, "jlpt4", CharacterStudySet.LockLevel.LOCKED, Kanji.JLPT_N4, "兄公会同事自社者肉自", lc, IidFactory.get(), true); }
    public static CharacterStudySet jlptN3(LockChecker lc){ return new CharacterStudySet("JLPT N3", "JLPT N3", JLPT3_DESCRIPTION, "jlpt3", CharacterStudySet.LockLevel.LOCKED, Kanji.JLPT_N3, "未任引政議民連対部合", lc, IidFactory.get(), true); }
    public static CharacterStudySet jlptN2(LockChecker lc){ return new CharacterStudySet("JLPT N2", "JLPT N2", JLPT2_DESCRIPTION, "jlpt2", CharacterStudySet.LockLevel.LOCKED, Kanji.JLPT_N2, "了介仏党協総区領県設", lc, IidFactory.get(), true); }
    public static CharacterStudySet jlptN1(LockChecker lc){ return new CharacterStudySet("JLPT N1", "JLPT N1", JLPT1_DESCRIPTION, "jlpt1", CharacterStudySet.LockLevel.LOCKED, Kanji.JLPT_N1, "乃仙仮氏統保第結派案", lc, IidFactory.get(), true); }


    public static CharacterStudySet[] standardSets(LockChecker lc) {
        return new CharacterStudySet[]{
                hiragana(lc),
                katakana(lc),
                joyouG1(lc),
                joyouG2(lc),
                joyouG3(lc),
                joyouG4(lc),
                joyouG5(lc),
                joyouG6(lc),
				joyouHS(lc),

                jlptN5(lc),
                jlptN4(lc),
                jlptN3(lc),
                jlptN2(lc),
                jlptN1(lc)
        };
    }
}
