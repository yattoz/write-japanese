package dmeeuwis.nakama.data;

import android.util.Pair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

    private Map<Character, Integer> filterToValidScores(ProgressTracker t){
        Map<Character, Integer> m =  new LinkedHashMap(t.getScoreSheet());
        Set<Character> toDelete = new HashSet<>();
        for(Map.Entry<Character, Integer> r: m.entrySet()){
            if(r.getValue() == null){
                toDelete.add(r.getKey());
            }
        }

        for(Character d: toDelete){
            m.remove(d);
        }

        return m;
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


    public final List<Pair<String, Boolean>> bugList = Arrays.asList(
            Pair.create("一", true),
            Pair.create("七", true),
            Pair.create("万", true),
            Pair.create("三", true),
            Pair.create("上", true),
            Pair.create("下", true),
            Pair.create("中", true),
            Pair.create("九", true),
            Pair.create("二", true),
            Pair.create("五", true),
            Pair.create("人", true),
            Pair.create("今", true),
            Pair.create("休", true),
            Pair.create("何", true),
            Pair.create("先", true),
            Pair.create("入", true),
            Pair.create("八", true),
            Pair.create("六", true),
            Pair.create("円", true),
            Pair.create("入", true),
            Pair.create("出", true),
            Pair.create("前", false),
            Pair.create("北", false),
            Pair.create("前", true),
            Pair.create("北", false),
            Pair.create("十", true),
            Pair.create("前", true),
            Pair.create("北", true),
            Pair.create("千", true),
            Pair.create("北", true),
            Pair.create("昼", true),
            Pair.create("半", true),
            Pair.create("北", true),
            Pair.create("南", true),
            Pair.create("友", true),
            Pair.create("右", false),
            Pair.create("名", true),
            Pair.create("右", true),
            Pair.create("四", true),
            Pair.create("国", true),
            Pair.create("土", true),
            Pair.create("外", true),
            Pair.create("右", true),
            Pair.create("大", true),
            Pair.create("天", true),
            Pair.create("女", true),
            Pair.create("子", true),
            Pair.create("学", true),
            Pair.create("小", true),
            Pair.create("山", true),
            Pair.create("川", true),
            Pair.create("左", false),
            Pair.create("年", true),
            Pair.create("左", true),
            Pair.create("後", false),
            Pair.create("日", true),
            Pair.create("後", true),
            Pair.create("時", false),
            Pair.create("左", true),
            Pair.create("時", true),
            Pair.create("時", false),
            Pair.create("書", false),
            Pair.create("書", true)
/*          Pair.create("後", false),
            Pair.create("時", true),
            Pair.create("後", true),
            Pair.create("書", true),
            Pair.create("後", true),
            Pair.create("書", true),
            Pair.create("月", true),
            Pair.create("木", true),
            Pair.create("書", true),
            Pair.create("本", true),
            Pair.create("来", true),
            Pair.create("書", true),
            Pair.create("東", true),
            Pair.create("書", true),
            Pair.create("校", true),
            Pair.create("母", true),
            Pair.create("書", true),
            Pair.create("毎", true),
            Pair.create("毎", true),
            Pair.create("気", true),
            Pair.create("母", true),
            Pair.create("水", true),
            Pair.create("火", true),
            Pair.create("書", true),
            Pair.create("毎", true),
            Pair.create("書", true),
            Pair.create("母", true),
            Pair.create("母", true),
            Pair.create("父", true),
            Pair.create("書", true),
            Pair.create("生", true),
            Pair.create("書", true),
            Pair.create("男", true),
            Pair.create("毎", true),
            Pair.create("男", true),
            Pair.create("白", true)
*/
    );

    @Test
    public void testProgression(){
        CharacterStudySet s = CharacterSets.jlptN5(null, RuntimeEnvironment.application);
        s.load(RuntimeEnvironment.application, CharacterStudySet.LoadProgress.NO_LOAD_SET_PROGRESS);

        CharacterProgressDataHelper.ProgressionSettings prog =
                new CharacterProgressDataHelper.ProgressionSettings(
                        20, 100, 1, 2);

        s.nextCharacter(prog);

        int i = 0;
        for(Pair<String, Boolean> p: bugList){
            s.overrideCurrent(p.first.charAt(0));
            System.out.println("Replaying " + p);
            s.markCurrent(null, p.second);
            i++;
        }

        Map<Character, Integer> scores = filterToValidScores(s.getProgressTracker());
        Set<Character> seen = scores.keySet();

        Set<Character> unseen = new HashSet<>(charSet);
        unseen.removeAll(seen);

        System.out.println("Charset has seen " + scores.size() + " characters");
        System.out.println("Charset has total " + s.getScoreSheet().size() + " characters");
        System.out.println("Unseen chars: " + unseen);

        final int REPEATS = 200;

        List<Character> nexts = new ArrayList<>();
        for(int j = 0; j < REPEATS; j++){
            s.nextCharacter(prog);
            System.out.println("While looking at nexts, saw: " + s.currentCharacter());
            nexts.add(s.currentCharacter());
            s.markCurrent(null, true);
        }

        System.out.println("Nexts are: " + Util.join(nexts));

        Map<Character, Integer> counts = new HashMap<>();
        for(Character c: nexts){
            if(!counts.containsKey(c)){
                counts.put(c, 0);
            }

            counts.put(c, counts.get(c) + 1);
        }

        printValidScores(s.getProgressTracker());

        System.out.println("Over " + REPEATS + " reviews, saw distinct: " + counts.size());
        for(Map.Entry<Character, Integer> c: counts.entrySet()){
            System.out.println(c.getKey() + " => " + c.getValue());
        }

        System.out.println("That's it.");
    }

    @Test
    public void srsWithShuffle() {

    }

    @Test
    public void testGettingIntoPassedStateAfterSRS() {

    }

    @Test
    public void testCountsOnStudyProgressVersusCharsetGoals(){

    }
}
