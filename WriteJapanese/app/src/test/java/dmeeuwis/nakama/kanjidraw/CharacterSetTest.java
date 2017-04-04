package dmeeuwis.nakama.kanjidraw;

import android.content.Context;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import dmeeuwis.Kanji;
import dmeeuwis.nakama.data.CharacterProgressDataHelper;
import dmeeuwis.nakama.data.CharacterStudySet;

import static junit.framework.Assert.assertTrue;

public class CharacterSetTest {

    static class TestCharacterProgressDataHelper extends CharacterProgressDataHelper {
        public TestCharacterProgressDataHelper() {
            super(null, null);
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

        Set<Character> charsSeen = new HashSet<>();
        while(charsSeen.size() != testChars.length()) {
            c.markCurrent(null, true);
            c.nextCharacter();
            charsSeen.add(c.currentCharacter());
        }

        assertTrue("Progressed through all characters", true);
    }

    @Test
    public void testMaxNumberSeenLimitedOnIncorrectProgression(){
    }

    @Test
    public void testMixOfCorrectIncorrectFinishesSet(){

    }
}

