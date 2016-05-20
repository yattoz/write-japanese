package dmeeuwis.nakama.kanjidraw;

import android.graphics.Point;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dmeeuwis.kanjimaster.BuildConfig;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class DrawingComparatorTest {

    @Test
    public void testAboveNone(){
        Stroke s1 = fromPoints(1, 3, 3, 3);
        Stroke s2 = fromPoints(1, 3, 3, 3);

        PointDrawing p = new PointDrawing(Arrays.asList(s1, s2));
        boolean[][] above = DrawingComparator.calculateAboveMatrix(p);

        assertArrayEquals(new boolean[]{ false, false }, above[0]);
        assertArrayEquals(new boolean[]{ false, false }, above[1]);
    }

    @Test
    public void testAboveOneMatch(){
        Stroke s1 = fromPoints(1, 30, 30, 35);
        Stroke s2 = fromPoints(1, 53, 30, 59);

        PointDrawing p = new PointDrawing(Arrays.asList(s1, s2));
        boolean[][] above = DrawingComparator.calculateAboveMatrix(p);

        assertArrayEquals(new boolean[]{ false, true  }, above[0]);
        assertArrayEquals(new boolean[]{ false, false }, above[1]);
    }

    @Test
    public void testJustMisOneMatch(){
        // should not mark as above because the y's are really really close, test the 'extra' gap
        Stroke s1 = fromPoints(1, 29, 30, 34);
        Stroke s2 = fromPoints(1, 30, 30, 35);

        PointDrawing p = new PointDrawing(Arrays.asList(s1, s2));
        boolean[][] above = DrawingComparator.calculateAboveMatrix(p);

        assertArrayEquals(new boolean[]{ false, false }, above[0]);
        assertArrayEquals(new boolean[]{ false, false }, above[1]);
    }

    @Test
    public void testOneMatch(){
        // should not mark as above because the y's are really really close, test the 'extra' gap
        Stroke s1 = fromPoints(1, 29, 30, 34);
        Stroke s2 = fromPoints(1, 38, 30, 44);

        PointDrawing p = new PointDrawing(Arrays.asList(s1, s2));
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
}

