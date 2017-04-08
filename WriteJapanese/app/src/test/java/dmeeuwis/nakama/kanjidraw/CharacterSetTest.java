package dmeeuwis.nakama.kanjidraw;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.Kanji;
import dmeeuwis.nakama.data.CharacterProgressDataHelper;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.ProgressTracker;
import dmeeuwis.util.Util;

import static junit.framework.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class CharacterSetTest {

    static class TestCharacterProgressDataHelper extends CharacterProgressDataHelper {
        public TestCharacterProgressDataHelper() {
            super(null, null);
        }

        @Override public ProgressionSettings getProgressionSettings(){
            return new ProgressionSettings(5, 10, 1, 2);
        }

        @Override public void recordPractice(String charset, String character, PointDrawing d, int score){
            // ignore
        }
    }

    @Test
    public void testAllCharactersSeenOnCorrectProgression(){
        String testChars = Kanji.JOUYOU_G3;
        CharacterStudySet c = new CharacterStudySet("Kana", "kana", "Some testing charset", "kana", CharacterStudySet.LockLevel.UNLOCKED,
                testChars, testChars, null, UUID.randomUUID(), true, new TestCharacterProgressDataHelper());
        c.nextCharacter();

        Set<Character> charsSeen = new HashSet<>();
        while(charsSeen.size() != testChars.length()) {
            c.markCurrent(null, true);
            c.nextCharacter();
            charsSeen.add(c.currentCharacter());
        }

        assertEquals("Progressed through all characters", testChars.length(), charsSeen.size());
    }

    @Test
    public void testMaxNumberSeenLimitedOnIncorrectProgression(){
        // test that no new characters are introduced when a user hits their set limits
        String testChars = Kanji.JOUYOU_G3;
        CharacterStudySet c = new CharacterStudySet("Kanji", "Kanji", "Some testing charset", "Kanji", CharacterStudySet.LockLevel.UNLOCKED,
                testChars, testChars, null, UUID.randomUUID(), true, new TestCharacterProgressDataHelper());
        c.nextCharacter();

        Set<Character> failed = new HashSet<>();
        while(failed.size() != CharacterProgressDataHelper.DEFAULT_INTRO_INCORRECT){
            c.markCurrent(null, false);
            failed.add(c.currentCharacter());
            System.out.println("Marking character " + c.currentCharacter() + " as failed");

            c.nextCharacter();
        }
        System.out.println("Failed.size is " + failed.size() + ", vs INTRO_INCORRECT " + CharacterProgressDataHelper.DEFAULT_INTRO_INCORRECT);
        System.out.println("Failed is: " + Util.join(", ", failed));

        for(int i = 0; i < 100; i++){
            c.nextCharacter();
            assertTrue("No new characters are introduced when at max INTRO_INCORRECT; failed was " +
                    Util.join(", ", failed) + " and new was " + c.currentCharacter() + " on loop iteration " + i,
                    failed.contains(c.currentCharacter()));
        }
    }

    @Test
    public void testMixOfCorrectIncorrectFinishesSet(){
        String testChars = Kanji.JOUYOU_G3;
        CharacterStudySet c = new CharacterStudySet("Kana", "kana", "Some testing charset", "kana", CharacterStudySet.LockLevel.UNLOCKED,
                testChars, testChars, null, UUID.randomUUID(), true, new TestCharacterProgressDataHelper());
        c.nextCharacter();

        Set<Character> charsSeen = new HashSet<>();
        int count = 0;
        while(charsSeen.size() != testChars.length()) {
            if(count < 20 && count % 2 == 0) {
                System.out.println("Marking incorrect");
                c.markCurrent(null, false);
            } else {
                System.out.println("Marking correct");
                c.markCurrent(null, true);
            }
            c.nextCharacter();
            charsSeen.add(c.currentCharacter());
            count++;
        }

        assertEquals("Progressed through all characters even after some incorrects", testChars.length(), charsSeen.size());
    }
}

