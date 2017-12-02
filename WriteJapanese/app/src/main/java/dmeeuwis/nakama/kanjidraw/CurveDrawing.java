package dmeeuwis.nakama.kanjidraw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import dmeeuwis.nakama.data.Rect;
import dmeeuwis.nakama.data.UncaughtExceptionLogger;
import dmeeuwis.util.Util;

public class CurveDrawing implements Drawing {

	private final int KANJI_FIXED_WIDTH = 109;
	private final int KANJI_FIXED_HEIGH = 109;

	public final List<ParameterizedEquation> strokes;
	public final PointDrawing pointPointDrawing;
	private final SvgHelper svg = new SvgHelper();
	
	public CurveDrawing(String[] in){
        if(in == null) { throw new IllegalArgumentException("Cannot construct CurveDrawing from null String[]."); }
		try {
			this.strokes = Collections.unmodifiableList(svg.readSvgEquations(in));
			this.pointPointDrawing = this.toDrawing();
		} catch(Throwable t){
			throw new RuntimeException("Caught error parsing curve svg: " + Util.join(", ", in), t);
		}
	}
	
	public int strokeCount(){
		return this.strokes.size();
	}

	@Override
	public Rect findBounds(){
		Rect box = new Rect(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
		for(ParameterizedEquation s: strokes){
			Rect r = s.findBoundingBox();
			box.left = Math.min(r.left, box.left);
			box.right = Math.max(r.right, box.right);
			box.top = Math.min(r.top, box.top);
			box.bottom = Math.max(r.bottom, box.bottom);
		}

		return box;
	}

	public Rect findBoundingBox(){
        Rect box = new Rect();
        for(ParameterizedEquation eqn: this.strokes){
            box.union(eqn.findBoundingBox());
        }
        return box;
	}
	
	public PointDrawing bufferEnds(int amount){
		return this.pointPointDrawing.bufferEnds(amount);
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
		return new PointDrawing(KANJI_FIXED_WIDTH, KANJI_FIXED_HEIGH, asStrokes);
	}

    @Override
    public List<ParameterizedEquation> toParameterizedEquations(float scale){
        List<ParameterizedEquation> ret = new ArrayList<>(this.strokeCount());
        for(ParameterizedEquation stroke: strokes)
            ret.add(stroke.scale(scale));
        return ret;
    }
}
