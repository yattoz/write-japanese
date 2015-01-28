package dmeeuwis.nakama.kanjidraw;

import android.graphics.Point;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import dmeeuwis.nakama.kanjidraw.PathCalculator.Intersection;

public class PointDrawing implements Iterable<Stroke>, Drawing {
    static final private float MIN_POINT_DISTANCE_DP = 25;
    static final private float MIN_POINT_DISTANCE_FOR_DIRECTION_DP = 10;
    private final static double DIRECTION_LIMIT = Math.PI / 8;

	private final List<Stroke> strokes;

	public static PointDrawing fromPrefilteredPoints(List<List<Point>> points){
		List<Stroke> strokes = new ArrayList<>(points.size());
		for(List<Point> p: points)
			strokes.add(new Stroke(p));
		return new PointDrawing(strokes);
	}

    public static PointDrawing fromDetailedPoints(List<List<Point>> points, Context context){
        Resources r = context.getResources();
        float MIN_POINT_DISTANCE_PX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MIN_POINT_DISTANCE_DP, r.getDisplayMetrics());
        float MIN_POINT_DISTANCE_FOR_DIRECTION_PX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MIN_POINT_DISTANCE_FOR_DIRECTION_DP, r.getDisplayMetrics());

        List<Stroke> gradeLines = new ArrayList<>(points.size());
        for(List<Point> line: points){
            Point lastGrade = line.get(0);
            List<Point> gradeLine = new ArrayList<>();
            gradeLine.add(line.get(0));
            double lastDirection = PathCalculator.angle(line.get(0).x, line.get(0).y, line.get(1).x, line.get(1).y);
            for(int i = 1; i < line.size(); i++) {
                Point p0 = line.get(i - 1);
                Point p1 = line.get(i);

                final double gradeDistance = PathCalculator.distance(lastGrade.x, lastGrade.y, p1.x, p1.y);
                final double direction = PathCalculator.angle(p0.x, p0.y, p1.x, p1.y);
                final boolean directionInclude = gradeDistance >= MIN_POINT_DISTANCE_FOR_DIRECTION_PX && Math.abs(lastDirection - direction) >= DIRECTION_LIMIT;
                final boolean gradeDistanceInclude = gradeDistance >= MIN_POINT_DISTANCE_PX;
                final boolean lastPointBonusDistanceInclude = (i == (line.size()-1)) && gradeDistance >= (MIN_POINT_DISTANCE_PX / 2);

                if (!lastGrade.equals(p1) && (directionInclude || gradeDistanceInclude || lastPointBonusDistanceInclude)) {
                    gradeLine.add(p1);
                    lastGrade = p1;
                }
            }
            gradeLines.add(new Stroke(gradeLine));
        }
        return new PointDrawing(gradeLines);
    }

	public PointDrawing(List<Stroke> pointStrokes){
		this.strokes = Collections.unmodifiableList(pointStrokes);
	}

    @Override
    public PointDrawing toDrawing(){
        return this;
    }

    @Override
	public int strokeCount(){
		return this.strokes.size();
	}

    @Override
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

    @Override
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
		return PointDrawing.fromPrefilteredPoints(newCopy);
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
		
		return PointDrawing.fromPrefilteredPoints(scaled);
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
