package dmeeuwis.nakama.kanjidraw;

import java.util.Iterator;
import java.util.List;

import dmeeuwis.nakama.data.Rect;

public interface Drawing extends Iterable<Stroke> {
    int strokeCount();
    Rect findBoundingBox();
    Rect findBounds();
    PointDrawing bufferEnds(int amount);
    List<ParameterizedEquation> toParameterizedEquations(float scale);
    Iterator<Stroke> iterator();
    PointDrawing toDrawing();
}
