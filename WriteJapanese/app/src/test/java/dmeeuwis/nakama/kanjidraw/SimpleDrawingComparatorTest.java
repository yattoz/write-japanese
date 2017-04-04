package dmeeuwis.nakama.kanjidraw;

import android.util.Log;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.robolectric.shadows.ShadowLog;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import dmeeuwis.nakama.data.AssetFinder;
import dmeeuwis.nakama.data.Point;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class SimpleDrawingComparatorTest {

    @BeforeClass
    public static void setup(){
        ShadowLog.stream = System.out;
    }

    public static class TestInputStreamGenerator implements AssetFinder.InputStreamGenerator {
        @Override
        public InputStream fromPath(String path) throws IOException {
            String userDir = System.getProperty("user.dir");
            String correctedPath;
            if(userDir.endsWith("app")){
                correctedPath = userDir;             // Linux
            } else {
                correctedPath = userDir + "/app";    // OS X
            }
            return new FileInputStream(correctedPath + "/src/main/assets/" + path);
        }
    }

    @Test
    public void testMisorderedStrokesOK(){
        PowerMockito.mockStatic(Log.class);
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

    public static List<Point> toStroke(int ... values){
        List<Point> l = new ArrayList<>();
        for(int i = 0; i < values.length; i+= 2){
           l.add(new Point(values[i], values[i+1]));
        }
        return l;
    }

    @Test
    public void katakanaNiFailError() throws IOException {
        PowerMockito.mockStatic(Log.class);
        AssetFinder as = new AssetFinder(new TestInputStreamGenerator());

        SimpleDrawingComparator s = new SimpleDrawingComparator(as, SimpleDrawingComparator.StrokeOrder.DISCOUNT);
        List<List<Point>> list = new ArrayList<>();
        list.add(toStroke(213, 16, 1074, 24));
        list.add(toStroke(0, 775, 1160, 742));

        String[] in = as.findSvgForCharacter('ニ');
        CurveDrawing known = new CurveDrawing(in);
        Criticism c = s.compare('ニ', PointDrawing.fromPrefilteredPoints(1176, 739, list), known);

        System.out.println(c.toString());
        assertEquals("Simple katakana ni passed", true, c.pass);
    }


    private static void findMatch(double[][] m){
        List<StrokeResult> res = SimpleDrawingComparator.findBestPairings(m);
        for (StrokeResult r : res) {
            assertEquals("All strokes found a match", 0, r.score);
        }
    }
}
