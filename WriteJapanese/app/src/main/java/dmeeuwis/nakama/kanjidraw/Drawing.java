package dmeeuwis.nakama.kanjidraw;

import android.graphics.Rect;

import java.util.Iterator;
import java.util.List;

public interface Drawing extends Iterable<Stroke> {
    public int strokeCount();
    public Rect findBoundingBox();
    public PointDrawing bufferEnds(int amount);
    public Iterator<ParameterizedEquation> parameterizedEquations(float scale);
    public List<ParameterizedEquation> toParameterizedEquations(float scale);
    public Iterator<Stroke> iterator();
    public PointDrawing toDrawing();
}
