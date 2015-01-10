package dmeeuwis.nakama.kanjidraw;

import android.graphics.Rect;

import java.util.Iterator;

public interface Drawing extends Iterable<Stroke> {
    public int strokeCount();
    public Rect findBoundingBox();
    public PointDrawing bufferEnds(int amount);
    public Iterator<ParameterizedEquation> parameterizedEquations(float scale, float padding);
    public Iterator<Stroke> iterator();
    public PointDrawing toDrawing();
}
