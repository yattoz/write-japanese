package dmeeuwis.nakama.kanjidraw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.graphics.Rect;

public class Glyph implements IGlyph {

	public final List<ParameterizedEquation> strokes;
	public final Drawing pointDrawing;
	private final SvgHelper svg = new SvgHelper();
	
	public Glyph(String[] in){
		this.strokes = Collections.unmodifiableList(svg.readSvgEquations(in));
		this.pointDrawing = this.toDrawing();
	}
	
	public int strokeCount(){
		return this.strokes.size();
	}
	
	public Rect findBoundingBox(){
		return this.pointDrawing.findBoundingBox();
	}
	
	public Drawing bufferEnds(int amount){
		return this.pointDrawing.bufferEnds(amount);
	}

    @Override
    public Iterator<ParameterizedEquation> parameterizedEquations(float scale, float padding) {
        return this.strokes.iterator();
    }

    @Override
	public Iterator<Stroke> iterator() {
		throw new RuntimeException("Not supported.");
	}
	
	public Drawing toDrawing(){
		List<Stroke> asStrokes = new ArrayList<Stroke>(strokes.size());
		for(ParameterizedEquation eqn: this.strokes){
			Stroke s = new Stroke(eqn.toPoints());
			asStrokes.add(s);
		}
		return new Drawing(asStrokes);
	}
}
