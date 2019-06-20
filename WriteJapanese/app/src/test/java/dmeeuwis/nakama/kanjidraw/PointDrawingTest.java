package dmeeuwis.nakama.kanjidraw;


import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

import java.util.Arrays;

import dmeeuwis.kanjimaster.logic.drawing.PointDrawing;
import dmeeuwis.kanjimaster.logic.data.Rect;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class PointDrawingTest {

    @Test public void boundingBoxTest(){
        PowerMockito.mockStatic(Log.class);

        PointDrawing p = PointDrawing.fromPrefilteredPoints(109, 109, Arrays.asList(
                SimpleDrawingComparatorTest.toStroke(19, 32, 62, 34),
                SimpleDrawingComparatorTest.toStroke(7,  72, 79, 74)));
        Rect r = p.findBoundingBox();
        System.out.println("Generated bounding box: " + r);

        assertEquals("Calculated bound box left", 7, r.left);
        assertEquals("Calculated bound box top", 32, r.top);
        assertEquals("Calculated bound box right", 79, r.right);
        assertEquals("Calculated bound box bottom", 74, r.bottom);
    }

    @Test public void cuttoffEdgesTest(){
        PowerMockito.mockStatic(Log.class);

        PointDrawing p = PointDrawing.fromPrefilteredPoints(109, 109, Arrays.asList(
                SimpleDrawingComparatorTest.toStroke(19, 34, 62, 34),
                SimpleDrawingComparatorTest.toStroke(7,  74, 79, 74)));
        PointDrawing p2 = p.cutOffEdges();
        Rect bounds = p2.findBoundingBox();
        System.out.println("Edges cutt to " + bounds);
        assertEquals("cutoffEdges moves to left edge", 0, bounds.left);
        assertEquals("cutoffEdges moves to top edge", 0, bounds.top);
        assertTrue("cuttofEdges don't go negative", bounds.right >= 0);
        assertTrue("cuttofEdges don't go negative", bounds.bottom >= 0);
    }
}
