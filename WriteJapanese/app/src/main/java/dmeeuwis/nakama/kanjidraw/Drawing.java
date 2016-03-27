package dmeeuwis.nakama.kanjidraw;

import android.graphics.Rect;

import java.util.Iterator;
import java.util.List;

public interface Drawing extends Iterable<Stroke> {
    int strokeCount();
    Rect findBoundingBox();
    PointDrawing bufferEnds(int amount);
    List<ParameterizedEquation> toParameterizedEquations(float scale);
    Iterator<Stroke> iterator();
    PointDrawing toDrawing();
}
