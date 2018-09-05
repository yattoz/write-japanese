package dmeeuwis.nakama.data;

import android.util.Pair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.threeten.bp.LocalDate;
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
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ProgressTrackerN5Test {

    private List<Character> chars = Util.toCharList(Kanji.JLPT_N5);
    private Set<Character> charSet = new LinkedHashSet<>(chars);

    private ProgressTracker simpleTestTracker(int advIncorrect, int advReview, boolean skipSrsIfFirstCorrect){
        ProgressTracker p = new ProgressTracker(
                charSet, advIncorrect, advReview, true, false, skipSrsIfFirstCorrect, "jlpt5");
        p.clearGlobalState();
        return p;
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

        s.markCurrent(s.currentCharacter(), null, false);
        assertEquals("Marks as failed", ProgressTracker.Progress.FAILED, s.getRecordSheet().get('一'));

        s.overRideLast();
        assertEquals("After rest, marks as passed", ProgressTracker.Progress.PASSED, s.getRecordSheet().get('一'));
    }

    @Test
    public void testProgressionCustomization(){
        ProgressTracker tracker = simpleTestTracker(10, 5, true);

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

        Set<Character> first = new HashSet<>();
        first.add(c0);
        first.add(c1);
        first.add(c2);
        first.add(c3);
        first.add(c4);

        ProgressTracker t = simpleTestTracker(1, 1, true);
        Character last;

        // all are in incorrect, preventing new characters
        for(int i = 0; i < 100; i++) {
            // note character cooldown of 3 here
            CharacterProgressDataHelper.ProgressionSettings p = new CharacterProgressDataHelper.ProgressionSettings(
                    5, 5, 1, 1, 4, true);
            ProgressTracker.StudyRecord r = t.nextCharacter(charSet, false, p);
            last = r.chosenChar;
            System.out.println("Next char reported as: " + last + ", " + r.type);
            t.markFailure(r.chosenChar);
            printValidScores(t);
            assertTrue(i + ": With introIncorrect at 5, only 5 chars are ever shown: " + r.chosenChar + " should be in " + first,
                    first.contains(r.chosenChar));
        }

        {
            Map<Character, Integer> scores = t.getScoreSheet();
            assertEquals("Character " + c0 + " had failed score", Integer.valueOf(-2), scores.get(c0));
            assertEquals("Character " + c1 + " had failed score", Integer.valueOf(-2), scores.get(c1));
            assertEquals("Character " + c2 + " had failed score", Integer.valueOf(-2), scores.get(c2));
            assertEquals("Character " + c3 + " had failed score", Integer.valueOf(-2), scores.get(c3));
            assertEquals("Character " + c4 + " had failed score", Integer.valueOf(-2), scores.get(c4));
        }

        t.markSuccess(c0, LocalDateTime.now());
        t.markSuccess(c1, LocalDateTime.now());
        t.markSuccess(c2, LocalDateTime.now());
        t.markSuccess(c3, LocalDateTime.now());
        t.markSuccess(c4, LocalDateTime.now());

        assertEquals("Character " + c0 + " had failed score", Integer.valueOf(-1), t.getScoreSheet().get(c0));
        assertEquals("Character " + c1 + " had failed score", Integer.valueOf(-1), t.getScoreSheet().get(c1));
        assertEquals("Character " + c2 + " had failed score", Integer.valueOf(-1), t.getScoreSheet().get(c2));
        assertEquals("Character " + c3 + " had failed score", Integer.valueOf(-1), t.getScoreSheet().get(c3));
        assertEquals("Character " + c4 + " had failed score", Integer.valueOf(-1), t.getScoreSheet().get(c4));

        // all seen are in reviewing, still preventing new characters
        System.out.println("=====================> Stage 2 in-review");

        CharacterProgressDataHelper.ProgressionSettings p = new CharacterProgressDataHelper.ProgressionSettings(1, 1, 5, 5, 0, false);
        for(int i = 0; i < 100; i++) {
            ProgressTracker.StudyRecord r = t.nextCharacter(charSet, false, p);
            last = r.chosenChar;
            System.out.println("Next char reported as: " + last + ", " + r.type);
            printValidScores(t);
            assertTrue("With introReviewing at 3, and 3 in review state, only 3 chars are ever shown: " + r, first.contains(r.chosenChar));
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
                        20, 100, 1, 2, 5, true);

        s.nextCharacter(prog);

        int i = 0;
        for(Pair<String, Boolean> p: bugList){
            s.overRideLast();
            System.out.println("Replaying " + p);
            s.markCurrent(s.currentCharacter(),null, p.second);
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
            s.markCurrent(s.currentCharacter(),null, true);
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
    public void iterateOverAllCharsSkipSRSOnFirstDISABLED() {
        iterateOverAllTestImpl(false, ProgressTracker.Progress.TIMED_REVIEW);
    }

    @Test
    public void iterateOverAllCharsSkipSRSOnFirstENABLED() {
        iterateOverAllTestImpl(true, ProgressTracker.Progress.PASSED);
    }


    private void iterateOverAllTestImpl(boolean skipSRSOnFirstCorrect, ProgressTracker.Progress expected){
        ProgressTracker t = simpleTestTracker(2, 1, skipSRSOnFirstCorrect);
        CharacterStudySet s = CharacterSets.jlptN5(null, RuntimeEnvironment.application);
        s.load(t);

        s.nextCharacter();
        for(int i = 0; i < chars.size(); i++){
            assertEquals("Characters go in order (" + i + ")", chars.get(i), s.currentCharacter());
            s.markCurrent(s.currentCharacter(),null, true);
            s.nextCharacter();
        }

        for(Map.Entry<Character, ProgressTracker.Progress> e: s.getRecordSheet().entrySet()){
           assertEquals("All chars are in " + expected, expected, e.getValue());
        }
    }

    @Test
    public void showReviewAfterHISTORYEntries(){
        int characterCooldown = 4;
        ProgressTracker t = simpleTestTracker(2, 1, true);
        CharacterStudySet s = CharacterSets.jlptN5(null, RuntimeEnvironment.application);
        s.load(t);

        s.nextCharacter();
        for(int i = 0; i < chars.size(); i++) {
            if (!s.currentCharacter().equals('書')){
                s.markCurrent(s.currentCharacter(), null, true);
                s.nextCharacter();
            }  else {
                s.markCurrent(s.currentCharacter(), null, false);
                s.nextCharacter();
                break;
            }
        }

        for(int i = 0; i < characterCooldown - 1; i++){
            assertNotSame("Bad char doesn't repeat for IGNORE_HISTORY turns", '書', s.currentCharacter());
            s.markCurrent(s.currentCharacter(), null, true);
            s.nextCharacter();
        }

        s.markCurrent(s.currentCharacter(), null, true);
        s.nextCharacter();

        assertEquals("Immediately after IGNORE_HISTORY expires, bad char comes back", '書', (char)s.currentCharacter());

    }


    @Test
    public void srsWithShuffle() {
        ProgressTracker t = new ProgressTracker(
                charSet, 2, 1, true, false, false, "jlpt5");
        t.clearGlobalState();
        CharacterStudySet s = CharacterSets.jlptN5(null, RuntimeEnvironment.application);
        s.setShuffle(true);
        s.load(t);

        s.nextCharacter();
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < chars.size(); i++){
            sb.append(s.currentCharacter());
            s.markCurrent(s.currentCharacter(), null, true);
            s.nextCharacter();
        }

        System.out.println("Saw shuffled order as: " + sb.toString());
        assertNotSame("If shuffle enabled, chars are NOT in order: " + s.toString(), Kanji.JLPT_N5, s.toString());

        for(Map.Entry<Character, ProgressTracker.Progress> e: s.getRecordSheet().entrySet()){
            assertEquals("All chars are in TIMED_REVIEW", ProgressTracker.Progress.TIMED_REVIEW, e.getValue());
        }
    }

    private ProgressTracker.DateFactory makeDateFactory(final int year, final int month, final int day){
        return new ProgressTracker.DateFactory() {
            @Override
            public LocalDateTime nowLocalDateTime() {
                return LocalDateTime.of(year, month, day, 12, 0);
            }

            @Override
            public LocalDate nowLocalDate() {
                return LocalDate.of(year, month, day);
            }
        };
    }

    @Test
    public void testGettingIntoPassedStateAfterSRS() {
        CharacterStudySet s = CharacterSets.jlptN5(null, RuntimeEnvironment.application);
        ProgressTracker t = new ProgressTracker(
                charSet, 2, 1, true, false, false,"jlpt5");
        t.clearGlobalState();
        s.load(t);
        SRSQueue.registerSetsForGlobalSRS(Arrays.asList(s));

        t.markSuccess('書', LocalDateTime.of(2000, 1, 1, 12, 30));
        assertEquals("Char goes into SRS", ProgressTracker.Progress.TIMED_REVIEW, t.getAllScores().get('書'));

        // note: character cooldown disabled completely for this test.
        CharacterProgressDataHelper.ProgressionSettings p = new CharacterProgressDataHelper.ProgressionSettings(1, 1, 2, 1, 0, true);

        {       // 1 day later
            ProgressTracker.DateFactory df = makeDateFactory(2000, 1, 2);
            t.setDateFactory(df);
            ProgressTracker.StudyRecord c = t.nextCharacter(charSet, false, p);
            t.markSuccess('書', df.nowLocalDateTime());
            assertEquals("Char shows up on next date", '書', (char) c.chosenChar);
            assertEquals("Char still in SRS", ProgressTracker.Progress.TIMED_REVIEW, t.getAllScores().get('書'));
        }


        {       // 3 days later
            ProgressTracker.DateFactory df = makeDateFactory(2000, 1, 5);
            t.setDateFactory(df);
            ProgressTracker.StudyRecord c = t.nextCharacter(charSet, false, p);
            t.markSuccess('書', df.nowLocalDateTime());
            assertEquals("Char shows up on next date", '書', (char) c.chosenChar);
            assertEquals("Char still in SRS", ProgressTracker.Progress.TIMED_REVIEW, t.getAllScores().get('書'));
        }

        {       // 7 days later
            ProgressTracker.DateFactory df = makeDateFactory(2000, 1, 12);
            t.setDateFactory(df);
            ProgressTracker.StudyRecord c = t.nextCharacter(charSet, false, p);
            t.markSuccess('書', df.nowLocalDateTime());
            assertEquals("Char shows up on next date", '書', (char) c.chosenChar);
            assertEquals("Char still in SRS", ProgressTracker.Progress.TIMED_REVIEW, t.getAllScores().get('書'));
        }

        {       // 14 days later
            ProgressTracker.DateFactory df = makeDateFactory(2000, 1, 26);
            t.setDateFactory(df);
            ProgressTracker.StudyRecord c = t.nextCharacter(charSet, false, p);
            t.markSuccess('書', df.nowLocalDateTime());
            assertEquals("Char shows up on next date", '書', (char) c.chosenChar);
            assertEquals("Char still in SRS", ProgressTracker.Progress.TIMED_REVIEW, t.getAllScores().get('書'));
        }

        {       // 30 days later
            ProgressTracker.DateFactory df = makeDateFactory(2000, 2, 25);
            t.setDateFactory(df);
            ProgressTracker.StudyRecord c = t.nextCharacter(charSet, false, p);
            t.markSuccess('書', df.nowLocalDateTime());
            assertEquals("Char shows up on next date", '書', (char) c.chosenChar);
            assertEquals("Char still in now PASSED", ProgressTracker.Progress.PASSED, t.getAllScores().get('書'));
        }
    }

    @Test
    public void testCountsOnStudyProgressVersusCharsetGoals(){

    }
}
