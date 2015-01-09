package dmeeuwis.nakama.kanjidraw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import dmeeuwis.nakama.kanjidraw.PathCalculator.Intersection;

public class Drawing implements Iterable<Stroke>, IGlyph {

	private final List<Stroke> strokes;
	
	private Rect boundingBoxCache = null;
	
	public static Drawing fromPoints(List<List<Point>> points){
		List<Stroke> strokes = new ArrayList<Stroke>(points.size());
		for(List<Point> p: points)
			strokes.add(new Stroke(p));
		return new Drawing(strokes);
	}

	public Drawing(List<Stroke> pointStrokes){
		this.strokes = Collections.unmodifiableList(pointStrokes);
	}

    public Drawing toDrawing(){
        return this;
    }
	
	public int strokeCount(){
		return this.strokes.size();
	}
	
	public Rect findBoundingBox(){
		if(this.boundingBoxCache == null){
			Integer left = null, right = null, top = null, bottom = null;
			for(Stroke stroke: this.strokes){
				Rect strokeBox = stroke.findBoundingBox();
				if(left == null || strokeBox.left < left)
					left = strokeBox.left;
				if(right == null || strokeBox.right > right)
					right = strokeBox.right;
				if(top == null || strokeBox.top < top)
					top = strokeBox.top;
				if(bottom == null || strokeBox.bottom > bottom)
					bottom = strokeBox.bottom;
			}
	
			if(left == null || top == null || right == null || bottom == null)
				throw new NullPointerException("Drawing: could not determine bounding box");
			
			boundingBoxCache = new Rect(left, top, right, bottom);
		}
		return this.boundingBoxCache;
	}
	
	public List<Intersection> findIntersections(){
		return PathCalculator.findIntersections(this.strokes);
	}
	
	public Stroke get(int stroke_i){
		return this.strokes.get(stroke_i);
	}
	
	public Drawing bufferEnds(int amount){
		List<Stroke> newList = new ArrayList<Stroke>(strokes.size());
		for(Stroke stroke: strokes){
			Stroke newStroke = stroke.bufferEnds(amount);
			newList.add(newStroke);
		}
		return new Drawing(newList);
	}

    public Drawing scale(float scale){
		List<Stroke> scaledStrokes = new ArrayList<Stroke>(this.strokes.size());
		for(int i = 0; i < scaledStrokes.size(); i++){
			scaledStrokes.add(this.strokes.get(i).scale(scale));
		}
		return new Drawing(scaledStrokes);
	}
	
	public Drawing cutOffEdges(){
		Rect bounds = this.findBoundingBox();
		List<List<Point>> newCopy = new ArrayList<List<Point>>(this.strokeCount());
		for(Stroke stroke: this.strokes){
			ArrayList<Point> newStroke = new ArrayList<Point>(stroke.pointSize());
			for(Point p: stroke.points){
				newStroke.add(new Point(p.x - bounds.left, p.y - bounds.top));
			}
			newCopy.add(newStroke);
		}
		return Drawing.fromPoints(newCopy);
	}
	
	public Drawing scaleToBox(Rect box){
		Log.i("nakama", "Scaling drawing to box " + box);
		Rect bbSubject = this.findBoundingBox();

		int desiredXSize = box.right - box.left;
		int desiredYSize = box.bottom - box.top;

		int currentXSize = bbSubject.right;
		int currentYSize = bbSubject.bottom;
		
		float increaseXFactor = (float)desiredXSize / currentXSize;
		float increaseYFactor = (float)desiredYSize / currentYSize;
		
		List<List<Point>> scaled = new ArrayList<List<Point>>(this.strokeCount());
		for(Stroke lp: this.strokes){
			List<Point> sl = new ArrayList<Point>(lp.pointSize());
			for(Point p: lp)
				sl.add(new Point((int)(p.x * increaseXFactor), 
						         (int)(p.y * increaseYFactor)));
			scaled.add(sl);
		}
		
		return Drawing.fromPoints(scaled);
	}

	@Override
	public Iterator<Stroke> iterator() {
		return this.strokes.iterator();
	}

    @Override
    public Iterator<ParameterizedEquation> parameterizedEquations(float scale, float padding) {
        return this.toParameterizedEquations(scale, padding).iterator();
    }

	public List<ParameterizedEquation> toParameterizedEquations(float scale, float padding){
		List<ParameterizedEquation> ret = new ArrayList<ParameterizedEquation>(this.strokeCount());
		for(Stroke stroke: strokes)
			ret.add(stroke.toParameterizedEquation(scale, padding));
		return ret;
	}
}
