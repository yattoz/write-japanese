package dmeeuwis.kanjimaster.logic.data;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dmeeuwis.kanjimaster.BuildConfig;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class ProgressTrackerTest {

    private final static Character[] CHARS = new Character[] {
            'a',
            'b',
            'c'
    };
    private final static List<Character> CHARS_LIST = Arrays.asList(CHARS);
    private final static Set<Character> CHARS_SET = new LinkedHashSet<>(CHARS_LIST);

    private final static Character[] CHARS_2 = new Character[] {
            'x',
            'y',
            'z'
    };
    private final static List<Character> CHARS_LIST_2 = Arrays.asList(CHARS_2);
    private final static Set<Character> CHARS_SET_2 = new LinkedHashSet<>(CHARS_LIST_2);


    private final static Character[] CHARS_4 = new Character[] {
            'a',
            'b',
            'c',
            'd',
            'e',
    };
    private final static List<Character> CHARS_LIST_4 = Arrays.asList(CHARS_4);
    private final static Set<Character> CHARS_SET_4 = new LinkedHashSet<>(CHARS_LIST_4);

    @Test
    public void testEmptyProgressTracker(){
        ProgressTracker p = new ProgressTracker(
                CHARS_SET, 2, 2, true, false, true, "test-set");
        assertEquals("Empty tracker has no SRS dates", 0, p.getSrsSchedule().size());

        Map<Character, Integer> scoreSheet = p.getScoreSheet();
        assertEquals("Progress tracker knows about valid chars", 3, scoreSheet.size());
        assertEquals("Progress tracker starts with null integers", null, scoreSheet.get('a'));
        assertEquals("Progress tracker starts with null integers", null, scoreSheet.get('b'));
        assertEquals("Progress tracker starts with null integers", null, scoreSheet.get('c'));
    }

    @Test
    public void testSingleChar(){
        CharacterStudySet s = CharacterSets.jlptN1(null, RuntimeEnvironment.application);
        ProgressTracker p = new ProgressTracker(
                CHARS_SET, 2, 2, true, false, false,"test-set");
        s.load(p);
        SRSQueue.registerSetsForGlobalSRS(Arrays.asList(s));

        p.markSuccess('a', LocalDateTime.of(2017, 1, 1, 12,1));

        assertEquals("Single correct goes to score 0", Integer.valueOf(0), p.debugPeekCharacterScore('a'));

        {
            Map<LocalDate, List<Character>> srs = p.getSrsSchedule();
            assertEquals("First correct goes to SRS", 1, srs.size());
            assertEquals("First SRS scheduled for next day", LocalDate.of(2017, 1, 2), getSingleDate(srs.keySet()));
        }

        p.markSuccess('a', LocalDateTime.of(2017, 1, 2, 14,1));
        {
            Map<LocalDate, List<Character>> srs = p.getSrsSchedule();
            assertEquals("Second correct advances SRS", 1, srs.size());
            assertEquals("Score should be at 1 to advance 3 days", Integer.valueOf(1), p.debugPeekCharacterScore('a'));
            assertEquals("First SRS scheduled for 3 days after", LocalDate.of(2017, 1, 5), getSingleDate(srs.keySet()));
        }

        p.markSuccess('a', LocalDateTime.of(2017, 1, 5, 15,10));
        {
            Map<LocalDate, List<Character>> srs = p.getSrsSchedule();
            assertEquals("Second correct advances SRS", 1, srs.size());
            assertEquals("Score should be at 2 to advance 7 days", Integer.valueOf(2), p.debugPeekCharacterScore('a'));
            assertEquals("First SRS scheduled for 7 days after", LocalDate.of(2017, 1, 12), getSingleDate(srs.keySet()));
        }

        p.markSuccess('a', LocalDateTime.of(2017, 1, 14, 15,10));
        {
            Map<LocalDate, List<Character>> srs = p.getSrsSchedule();
            assertEquals("Second correct advances SRS", 1, srs.size());
            assertEquals("Score should be at 3 to advance 14 days", Integer.valueOf(3), p.debugPeekCharacterScore('a'));
            assertEquals("First SRS scheduled for 14 days after", LocalDate.of(2017, 1, 28), getSingleDate(srs.keySet()));
        }

        p.markSuccess('a', LocalDateTime.of(2017, 2, 1, 15,10));
        {
            Map<LocalDate, List<Character>> srs = p.getSrsSchedule();
            assertEquals("Second correct advances SRS", 1, srs.size());
            assertEquals("Score should be at 4 to advance 30 days", Integer.valueOf(4), p.debugPeekCharacterScore('a'));
            assertEquals("Last SRS scheduled for 30 days after", LocalDate.of(2017, 3, 3), getSingleDate(srs.keySet()));
        }

        p.markSuccess('a', LocalDateTime.of(2017, 3, 4, 15,10));
        {
            Map<LocalDate, List<Character>> srs = p.getSrsSchedule();
            assertEquals("Score should be at 5 to advance 30 days", Integer.valueOf(5), p.debugPeekCharacterScore('a'));
            assertEquals("After completing char, out of SRS", 0, srs.size());
        }
    }

    @Test
    public void testGlobalSet() {

        ProgressTracker p1 = new ProgressTracker(
                CHARS_SET, 2, 2, true, true, false, "test-1");
        CharacterStudySet s1 = new CharacterStudySet("test1", "test1", "test", "test1", CharacterStudySet.LockLevel.UNLOCKED, "a", "xz", null, UUID.randomUUID(), true, RuntimeEnvironment.application);
        s1.load(p1);

        ProgressTracker p2 = new ProgressTracker(
                CHARS_SET_2, 2, 2, true, true, false, "test-2");
        CharacterStudySet s2 = new CharacterStudySet("test2", "test2", "test", "test2", CharacterStudySet.LockLevel.UNLOCKED, "xz", "xz", null, UUID.randomUUID(), true, RuntimeEnvironment.application);
        s2.load(p2);

        SRSQueue.registerSetsForGlobalSRS(Arrays.asList(s1, s2));

        p1.markSuccess('a', LocalDateTime.of(2017, 1, 1, 2, 1));
        p1.markSuccess('a', LocalDateTime.of(2017, 1, 2, 12, 1));

        // following 2 should have no effect
        p1.markSuccess('z', LocalDateTime.of(2017, 1, 2, 12, 1));
        p1.markSuccess('z', LocalDateTime.of(2017, 1, 3, 12, 1));

        p2.markSuccess('x', LocalDateTime.of(2017, 1, 5, 9, 1));
        p2.markSuccess('x', LocalDateTime.of(2017, 1, 6, 12, 1));

        Map<LocalDate, List<Character>> srs1 = p1.getSrsSchedule();
        Map<LocalDate, List<Character>> srs2 = p2.getSrsSchedule();

        assertEquals("2 sets with useSRSAcrossSets on give same SRS queue", srs1, srs2);
        assertEquals("Invalid practice chars on a set don't make it into SRS",
                Arrays.asList(LocalDate.of(2017, 1, 5),
                              LocalDate.of(2017, 1, 9)),
                new ArrayList<>(srs1.keySet()));
    }

    private static LocalDate getSingleDate(Set<LocalDate> set){
        if(set.size() > 1 || set.size() == 0){
            throw new IllegalArgumentException("Invalid set size for getSingleDate: " + set.size());
        }

        return set.toArray(new LocalDate[0])[0];
    }

    @Test
    public void testCompletedSRSSet(){

        CharacterStudySet s = CharacterSets.jlptN1(null, RuntimeEnvironment.application);
        ProgressTracker p1 = new ProgressTracker(
                CHARS_SET_4, 2, 2, true, false, false, "test-1");
        s.load(p1);
        SRSQueue.registerSetsForGlobalSRS(Arrays.asList(s));

        CharacterProgressDataHelper.ProgressionSettings p = new CharacterProgressDataHelper.ProgressionSettings(2, 2, 1, 1, 5, true);

        {
            ProgressTracker.StudyRecord c = p1.nextCharacter(CHARS_SET_4, false, p);
            assertEquals("Practice starts with first character in set", Character.valueOf('a'), c.chosenChar);
            SRSQueue.SRSEntry o = p1.markSuccess('a', LocalDateTime.now());
            assertEquals("Getting char right on first attempt gets it into SRS for tomorrow", LocalDate.now().plusDays(1), o.nextPractice);
        }

        {
            ProgressTracker.StudyRecord c = p1.nextCharacter(CHARS_SET_4, false, p);
            assertEquals("Practice continues to second in set", Character.valueOf('b'), c.chosenChar);
            p1.markSuccess('b', LocalDateTime.now());
        }


        {
            ProgressTracker.StudyRecord c = p1.nextCharacter(CHARS_SET_4, false, p);
            assertEquals("Practice continues to third in set", Character.valueOf('c'), c.chosenChar);
            p1.markSuccess('c', LocalDateTime.now());
        }

        {
            ProgressTracker.StudyRecord c = p1.nextCharacter(CHARS_SET_4, false, p);
            assertEquals("Practice continues to third in set", Character.valueOf('d'), c.chosenChar);
            p1.markSuccess('d', LocalDateTime.now());
        }

        {
            ProgressTracker.StudyRecord c = p1.nextCharacter(CHARS_SET_4, false, p);
            assertEquals("Practice continues to third in set", Character.valueOf('e'), c.chosenChar);
            p1.markSuccess('e', LocalDateTime.now());
        }

        Map<Character, Integer> count = new HashMap<>();
        for(int i = 0; i < 200; i++){
            ProgressTracker.StudyRecord c = p1.nextCharacter(CHARS_SET_4, false, p);
            count.put(c.chosenChar, count.get(c.chosenChar) == null ? 1 : count.get(c.chosenChar) + 1);
            p1.markSuccess(c.chosenChar, LocalDateTime.now());
        }

        for(Map.Entry<Character, Integer> e: count.entrySet()){
            System.out.println(e.getKey() + ": " + e.getValue());
        }

        for(Map.Entry<Character, Integer> e: count.entrySet()){
            assertTrue("Every char has been repeated a reasonable number of times after getting all chars into SRS: char " + e.getKey() + " was repeated " + e.getValue(),
            e.getValue() >= 200 / count.size() / 2.0);
        }

        List<Character> all = new ArrayList<>();
        for(List<Character> values: p1.getSrsSchedule().values()){
            all.addAll(values);
        }
        assertEquals("After lots of repetitive practice, SRS chars aren't cleared unless they hit their SRS day.", CHARS_4.length, all.size());
    }



}
