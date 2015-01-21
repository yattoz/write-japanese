package dmeeuwis.nakama.kanjidraw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

public class CurveDrawing implements Drawing {

	public final List<ParameterizedEquation> strokes;
	public final PointDrawing pointPointDrawing;
	private final SvgHelper svg = new SvgHelper();
	
	public CurveDrawing(String[] in){
        if(in == null) { throw new IllegalArgumentException("Cannot construct CurveDrawing from null String[]."); }
		this.strokes = Collections.unmodifiableList(svg.readSvgEquations(in));
		this.pointPointDrawing = this.toDrawing();
	}
	
	public int strokeCount(){
		return this.strokes.size();
	}
	
	public Rect findBoundingBox(){
        Rect box = new Rect();
        for(ParameterizedEquation eqn: this.strokes){
            box.union(eqn.findBoundingBox());
        }
        Log.i("nakama", "For curve drawing, calculated bounding box as " + box);
        return box;
	}
	
	public PointDrawing bufferEnds(int amount){
		return this.pointPointDrawing.bufferEnds(amount);
	}

    @Override
    public Iterator<ParameterizedEquation> parameterizedEquations(float scale) {
        return this.strokes.iterator();
    }

    @Override
	public Iterator<Stroke> iterator() {
		throw new RuntimeException("Not supported.");
	}
	
	public PointDrawing toDrawing(){
		List<Stroke> asStrokes = new ArrayList<>(strokes.size());
		for(ParameterizedEquation eqn: this.strokes){
			Stroke s = new Stroke(eqn.toPoints());
			asStrokes.add(s);
		}
		return new PointDrawing(asStrokes);
	}

    @Override
    public List<ParameterizedEquation> toParameterizedEquations(float scale){
        List<ParameterizedEquation> ret = new ArrayList<>(this.strokeCount());
        for(ParameterizedEquation stroke: strokes)
            ret.add(stroke.scale(scale));
        return ret;
    }
}
