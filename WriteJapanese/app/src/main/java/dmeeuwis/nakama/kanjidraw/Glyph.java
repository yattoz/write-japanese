package dmeeuwis.nakama.kanjidraw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.graphics.Rect;

public class Glyph implements Iterable<ParameterizedEquation> {

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
	public Iterator<ParameterizedEquation> iterator() {
		return this.strokes.iterator();
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
