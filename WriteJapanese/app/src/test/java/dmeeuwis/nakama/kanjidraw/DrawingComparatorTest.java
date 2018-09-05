package dmeeuwis.nakama.kanjidraw;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.nakama.data.Point;
import dmeeuwis.util.Util;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class DrawingComparatorTest {

    @Test
    public void testAboveNone(){
        Stroke s1 = fromPoints(1, 3, 3, 3);
        Stroke s2 = fromPoints(1, 3, 3, 3);

        PointDrawing p = new PointDrawing(5, 5, Arrays.asList(s1, s2));
        boolean[][] above = DrawingComparator.calculateAboveMatrix(p);

        assertArrayEquals(new boolean[]{ false, false }, above[0]);
        assertArrayEquals(new boolean[]{ false, false }, above[1]);
    }

    @Test
    public void testAboveOneMatch(){
        Stroke s1 = fromPoints(1, 30, 30, 35);
        Stroke s2 = fromPoints(1, 53, 30, 59);

        PointDrawing p = new PointDrawing(70, 70, Arrays.asList(s1, s2));
        boolean[][] above = DrawingComparator.calculateAboveMatrix(p);

        assertArrayEquals(new boolean[]{ false, true  }, above[0]);
        assertArrayEquals(new boolean[]{ false, false }, above[1]);
    }

    @Ignore
    @Test
    public void testJustMisOneMatch(){
        // should not mark as above because the y's are really really close, test the 'extra' gap
        Stroke s1 = fromPoints(1, 29, 30, 34);
        Stroke s2 = fromPoints(1, 30, 30, 35);

        PointDrawing p = new PointDrawing(60, 60, Arrays.asList(s1, s2));
        boolean[][] above = DrawingComparator.calculateAboveMatrix(p);

        assertArrayEquals(new boolean[]{ false, false }, above[0]);
        assertArrayEquals(new boolean[]{ false, false }, above[1]);
    }

    @Test
    public void testOneMatch(){
        // should not mark as above because the y's are really really close, test the 'extra' gap
        Stroke s1 = fromPoints(1, 29, 30, 34);
        Stroke s2 = fromPoints(1, 38, 30, 44);

        PointDrawing p = new PointDrawing(60, 60, Arrays.asList(s1, s2));
        boolean[][] above = DrawingComparator.calculateAboveMatrix(p);

        assertArrayEquals(new boolean[]{ false, true }, above[0]);
        assertArrayEquals(new boolean[]{ false, false }, above[1]);
    }


    private static Stroke fromPoints(int ... points){
        List<Point> l = new ArrayList<>();
        for(int i = 0; i < points.length; i += 2){
            Point p = new Point(points[0], points[1]);
            l.add(p);
        }
        return new Stroke(l);
    }

    @Test
    public void testFindBestPairings(){
        {
            List<StrokeResult> r = DrawingComparator.findBestPairings(new int[][]{ {1, 1}, {1, 1}});
            assertEquals("Always assigns pairings", new StrokeResult(0, 0, 1), r.get(0));
            assertEquals("Always assigns pairings", new StrokeResult(1, 1, 1), r.get(1));
        }

        {
            int[][] test = new int[][]{ {3, 3}, {4, 4}};
            System.out.println("Testing find best pairing!");
            System.out.println(Util.printMatrix(test));
            List<StrokeResult> r = DrawingComparator.findBestPairings(test);
            assertEquals("Assigns best pairings", new StrokeResult(0, 0, 3), r.get(0));
            assertEquals("Assigns best pairings", new StrokeResult(1, 1, 4), r.get(1));
        }

    }
}
