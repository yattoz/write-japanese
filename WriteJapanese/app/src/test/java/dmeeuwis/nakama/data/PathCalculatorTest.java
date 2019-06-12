package dmeeuwis.nakama.data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import dmeeuwis.nakama.kanjidraw.PathCalculator;
import dmeeuwis.nakama.kanjidraw.Stroke;

import static org.junit.Assert.assertEquals;

public class PathCalculatorTest {

    @Test
    public void testLineIntersection(){
        Stroke s1 = new Stroke(new Point(-1, 0), new Point(1, 0));
        Stroke s2 = new Stroke(new Point( 0, 1), new Point(0, -1));
        List<Point> intersects = new ArrayList<>(PathCalculator.intersections(s1, s2));

        assertEquals("Found simple intersect", 1, intersects.size());
        assertEquals("Found simple intersect", new Point(0, 0), intersects.get(0));
    }

    @Test
    public void testSelfIntersection(){
        Stroke s1 = new Stroke(new Point(-1, 0), new Point(1, 0), new Point(1, 1),
                new Point(0, 1), new Point(0, -1));
        List<Point> intersects = new ArrayList<>(PathCalculator.intersections(s1, s1));

        assertEquals("Found simple intersect", 1, intersects.size());
        assertEquals("Found simple intersect", new Point(0, 0), intersects.get(0));
    }
}
