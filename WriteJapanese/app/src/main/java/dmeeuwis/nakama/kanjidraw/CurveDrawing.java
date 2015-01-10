package dmeeuwis.nakama.kanjidraw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.graphics.Rect;

public class CurveDrawing implements Drawing {

	public final List<ParameterizedEquation> strokes;
	public final PointDrawing pointPointDrawing;
	private final SvgHelper svg = new SvgHelper();
	
	public CurveDrawing(String[] in){
		this.strokes = Collections.unmodifiableList(svg.readSvgEquations(in));
		this.pointPointDrawing = this.toDrawing();
	}
	
	public int strokeCount(){
		return this.strokes.size();
	}
	
	public Rect findBoundingBox(){
		return this.pointPointDrawing.findBoundingBox();
	}
	
	public PointDrawing bufferEnds(int amount){
		return this.pointPointDrawing.bufferEnds(amount);
	}

    @Override
    public Iterator<ParameterizedEquation> parameterizedEquations(float scale, float padding) {
        return this.strokes.iterator();
    }

    @Override
	public Iterator<Stroke> iterator() {
		throw new RuntimeException("Not supported.");
	}
	
	public PointDrawing toDrawing(){
		List<Stroke> asStrokes = new ArrayList<Stroke>(strokes.size());
		for(ParameterizedEquation eqn: this.strokes){
			Stroke s = new Stroke(eqn.toPoints());
			asStrokes.add(s);
		}
		return new PointDrawing(asStrokes);
	}
}
