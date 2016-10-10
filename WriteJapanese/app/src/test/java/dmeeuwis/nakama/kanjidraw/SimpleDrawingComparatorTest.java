package dmeeuwis.nakama.kanjidraw;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SimpleDrawingComparatorTest {

    @Test
    public void testMisorderedStrokesOK(){
        findMatch(new double[][]{
                new double[]{1, 0, 2},
                new double[]{0, 1, 2},
                new double[]{1, 1, 0}});

        findMatch(new double[][]{
                new double[]{0, 1, 2},
                new double[]{1, 2, 0},
                new double[]{1, 0, 1}});

        // taken from drawing hiragana mo with bad stroke order
        findMatch(new double[][]{
                new double[]{0, 1, 0},
                new double[]{0, 0, 0},
                new double[]{0, 0, 1}});
    }


    private static void findMatch(double[][] m){
        List<StrokeResult> res = SimpleDrawingComparator.findBestPairings(m);
        for (StrokeResult r : res) {
            assertEquals("All strokes found a match", 0, r.score);
        }
    }
}
