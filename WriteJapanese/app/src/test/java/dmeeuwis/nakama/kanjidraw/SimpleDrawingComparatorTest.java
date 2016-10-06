package dmeeuwis.nakama.kanjidraw;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SimpleDrawingComparatorTest {

    @Test
    public void testMisorderedStrokesOK(){
        findMatch(new int[][]{
                new int[]{1, 0, 2},
                new int[]{0, 1, 2},
                new int[]{1, 1, 0}});

        findMatch(new int[][]{
                new int[]{0, 1, 2},
                new int[]{1, 2, 0},
                new int[]{1, 0, 1}});

        findMatch(new int[][]{
                new int[]{0, 1, 0},
                new int[]{0, 0, 0},
                new int[]{0, 0, 1}});
    }


    private static void findMatch(int[][] m){
        List<StrokeResult> res = SimpleDrawingComparator.findBestPairings(m);
        for (StrokeResult r : res) {
            assertEquals("All strokes found a match", 0, r.score);
        }
    }
}
