package dmeeuwis.nakama.kanjidraw;

import android.graphics.Point;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import dmeeuwis.nakama.kanjidraw.PathCalculator.Intersection;

public class PointDrawing implements Iterable<Stroke>, Drawing {

	private final List<Stroke> strokes;
	
	public static PointDrawing fromPoints(List<List<Point>> points){
		List<Stroke> strokes = new ArrayList<>(points.size());
		for(List<Point> p: points)
			strokes.add(new Stroke(p));
		return new PointDrawing(strokes);
	}


	public PointDrawing(List<Stroke> pointStrokes){
		this.strokes = Collections.unmodifiableList(pointStrokes);
	}

    public PointDrawing toDrawing(){
        return this;
    }
	
	public int strokeCount(){
		return this.strokes.size();
	}
	
	public Rect findBoundingBox(){
        Rect box = strokes.get(0).findBoundingBox();
        for(int i = 1; i < this.strokeCount(); i++){
            box.union(strokes.get(i).findBoundingBox());
        }
        return box;
	}
	
	public List<Intersection> findIntersections(){
		return PathCalculator.findIntersections(this.strokes);
	}
	
	public Stroke get(int stroke_i){
		return this.strokes.get(stroke_i);
	}
	
	public PointDrawing bufferEnds(int amount){
		List<Stroke> newList = new ArrayList<>(strokes.size());
		for(Stroke stroke: strokes){
			Stroke newStroke = stroke.bufferEnds(amount);
			newList.add(newStroke);
		}
		return new PointDrawing(newList);
	}

	public PointDrawing cutOffEdges(){
		Rect bounds = this.findBoundingBox();
		List<List<Point>> newCopy = new ArrayList<>(this.strokeCount());
		for(Stroke stroke: this.strokes){
			ArrayList<Point> newStroke = new ArrayList<>(stroke.pointSize());
			for(Point p: stroke.points){
				newStroke.add(new Point(p.x - bounds.left, p.y - bounds.top));
			}
			newCopy.add(newStroke);
		}
		return PointDrawing.fromPoints(newCopy);
	}
	
	public PointDrawing scaleToBox(Rect box){
		Rect bbSubject = this.findBoundingBox();

		float increaseXFactor = (float)(box.right - box.left) / bbSubject.right;
		float increaseYFactor = (float)(box.bottom - box.top) / bbSubject.bottom;
		
		List<List<Point>> scaled = new ArrayList<>(this.strokeCount());
		for(Stroke lp: this.strokes){
			List<Point> sl = new ArrayList<>(lp.pointSize());
			for(Point p: lp)
				sl.add(new Point((int)(p.x * increaseXFactor), 
						         (int)(p.y * increaseYFactor)));
			scaled.add(sl);
		}
		
		return PointDrawing.fromPoints(scaled);
	}

	@Override
	public Iterator<Stroke> iterator() {
		return this.strokes.iterator();
	}

    @Override
    public Iterator<ParameterizedEquation> parameterizedEquations(float scale) {
        return this.toParameterizedEquations(scale).iterator();
    }

    @Override
	public List<ParameterizedEquation> toParameterizedEquations(float scale){
		List<ParameterizedEquation> ret = new ArrayList<>(this.strokeCount());
		for(Stroke stroke: strokes)
			ret.add(stroke.toParameterizedEquation(scale, 0));
		return ret;
	}
}
