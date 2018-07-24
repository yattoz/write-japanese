package dmeeuwis.nakama.data;

import android.util.Pair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.threeten.bp.LocalDateTime;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dmeeuwis.Kanji;
import dmeeuwis.util.Util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ProgressTrackerN5Test {

    private List<Character> chars = Util.toCharList(Kanji.JLPT_N5);
    private Set<Character> charSet = new LinkedHashSet<>(chars);

    private ProgressTracker simpleTestTracker(int advIncorrect, int advReview){
        return new ProgressTracker(charSet, advIncorrect, advReview, true, false, "jlpt5");
    }

    @Test
    public void testNullBeforeCallingNext() {
        CharacterStudySet s = CharacterSets.jlptN5(null, RuntimeEnvironment.application);
        s.load(RuntimeEnvironment.application, CharacterStudySet.LoadProgress.NO_LOAD_SET_PROGRESS);
        assertEquals("Null until .nextCharacter is called", null, s.currentCharacter());
    }

    @Test
    public void testResetLast() {
        CharacterStudySet s = CharacterSets.jlptN5(null, RuntimeEnvironment.application);
        s.load(RuntimeEnvironment.application, CharacterStudySet.LoadProgress.NO_LOAD_SET_PROGRESS);
        s.nextCharacter();

        s.markCurrent(null, false);
        assertEquals("Marks as failed", ProgressTracker.Progress.FAILED, s.getRecordSheet().get('一'));

        s.overRideLast();
        assertEquals("After rest, marks as passed", ProgressTracker.Progress.TIMED_REVIEW, s.getRecordSheet().get('一'));
    }

    @Test
    public void testProgressionCustomization(){
        ProgressTracker tracker = simpleTestTracker(10, 5);

        tracker.markFailure('一');
        assertEquals("After failure, set to failed", ProgressTracker.Progress.FAILED, tracker.getAllScores().get('一'));
        assertEquals("After failure, set to worst score", -15, (int)tracker.getScoreSheet().get('一'));

        tracker.markSuccess('一', LocalDateTime.now());
        assertEquals("After one success, a bit better", -14, (int)tracker.getScoreSheet().get('一'));

        // 9 more
        tracker.markSuccess('一', LocalDateTime.now());
        tracker.markSuccess('一', LocalDateTime.now());
        tracker.markSuccess('一', LocalDateTime.now());
        tracker.markSuccess('一', LocalDateTime.now());
        tracker.markSuccess('一', LocalDateTime.now());
        tracker.markSuccess('一', LocalDateTime.now());
        tracker.markSuccess('一', LocalDateTime.now());
        tracker.markSuccess('一', LocalDateTime.now());
        tracker.markSuccess('一', LocalDateTime.now());

        assertEquals("Made it into reviewing", -5, (int)tracker.getScoreSheet().get('一'));
        assertEquals("Made it into reviewing", ProgressTracker.Progress.REVIEWING, tracker.getAllScores().get('一'));

        tracker.markSuccess('一', LocalDateTime.now());
        tracker.markSuccess('一', LocalDateTime.now());
        tracker.markSuccess('一', LocalDateTime.now());
        tracker.markSuccess('一', LocalDateTime.now());
        tracker.markSuccess('一', LocalDateTime.now());

        assertEquals("Made it into timed reviewing", 0, (int)tracker.getScoreSheet().get('一'));
        assertEquals("Made it into timed reviewing", ProgressTracker.Progress.TIMED_REVIEW, tracker.getAllScores().get('一'));
    }

    private void printValidScores(ProgressTracker t){
        System.out.println("=============================");
        for(Map.Entry<Character, Integer> r: t.getScoreSheet().entrySet()){
           if(r.getValue() != null){
               System.out.println(r.getKey() + " => " + r.getValue());
           }
        }
    }

    @Test
    public void testVisibleCharLimits(){
        Character c0 = Kanji.JLPT_N5.charAt(0);
        Character c1 = Kanji.JLPT_N5.charAt(1);
        Character c2 = Kanji.JLPT_N5.charAt(2);
        Character c3 = Kanji.JLPT_N5.charAt(3);
        Character c4 = Kanji.JLPT_N5.charAt(4);

        Character c5 = Kanji.JLPT_N5.charAt(5);
        Character c6 = Kanji.JLPT_N5.charAt(6);

        Set<Character> firstThree = new HashSet<>();
        firstThree.add(c0);
        firstThree.add(c1);
        firstThree.add(c2);
        firstThree.add(c3);
        firstThree.add(c4);

        firstThree.add(c5);     // bug?

        ProgressTracker t = simpleTestTracker(1, 1);
        Character last = null;

        // all are in incorrect, preventing new characters
        for(int i = 0; i < 100; i++) {
            Pair<Character, ProgressTracker.StudyType> r = t.nextCharacter(charSet, last, charSet, false, 5, 5);
            last = r.first;
            System.out.println("Next char reported as: " + last + ", " + r.second);
            t.markFailure(r.first);
            printValidScores(t);
            assertTrue(i + ": With introIncorrect at 3, only 3 chars are ever shown: " + r.first + " should be in " + firstThree,
                    firstThree.contains(r.first));
        }

        {
            Map<Character, Integer> scores = t.getScoreSheet();
            assertEquals(-2, (int) scores.get(c0));
            assertEquals(-2, (int) scores.get(c1));
            assertEquals(-2, (int) scores.get(c2));
            assertEquals(-2, (int) scores.get(c3));
            assertEquals(-2, (int) scores.get(c4));
            assertEquals(-2, (int) scores.get(c5));
        }

        t.markSuccess(c0, LocalDateTime.now());
        t.markSuccess(c1, LocalDateTime.now());
        t.markSuccess(c2, LocalDateTime.now());
        t.markSuccess(c3, LocalDateTime.now());
        t.markSuccess(c4, LocalDateTime.now());
        t.markSuccess(c5, LocalDateTime.now());

        assertEquals(-1, (int)t.getScoreSheet().get(c0));
        assertEquals(-1, (int)t.getScoreSheet().get(c1));
        assertEquals(-1, (int)t.getScoreSheet().get(c2));
        assertEquals(-1, (int)t.getScoreSheet().get(c3));
        assertEquals(-1, (int)t.getScoreSheet().get(c4));
        assertEquals(-1, (int)t.getScoreSheet().get(c5));

        // all seen are in reviewing, still preventing new characters
        System.out.println("=====================> Stage 2 in-review");


        firstThree.add(c6);     // off-by-one bug!!


        for(int i = 0; i < 100; i++) {
            Pair<Character, ProgressTracker.StudyType> r = t.nextCharacter(charSet, last, charSet, false, 5, 5);
            last = r.first;
            System.out.println("Next char reported as: " + last + ", " + r.second);
            printValidScores(t);
            assertTrue("With introReviewing at 3, and 3 in review state, only 3 chars are ever shown: " + r, firstThree.contains(r.first));
        }
    }

    @Test
    public void srsWithShuffle() {

    }

}
