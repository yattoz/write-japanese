package dmeeuwis.nakama.kanjidraw;

import org.junit.Test;

import java.util.UUID;

import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;

/**
 * Created by dmeeuwis on 01/04/17.
 */

public class CharacterSetTest {

    @Test
    public void testAllCharactersSeenOnCorrectProgression(){
        CharacterStudySet c = CharacterSets.joyouG1(null, UUID.randomUUID());
        c.markCurrent();
    }

    @Test
    public void testMaxNumberSeenLimitedOnIncorrectProgression(){
        CharacterStudySet c = CharacterSets.joyouG1(null, UUID.randomUUID());
    }

    @Test
    public void testMixOfCorrectIncorrectFinishesSet(){

    }
}

