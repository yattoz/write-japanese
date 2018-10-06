package dmeeuwis.nakama.kanjidraw;

import android.content.Context;
import android.content.res.Resources;
import android.util.JsonWriter;
import android.util.Log;
import android.util.TypedValue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import dmeeuwis.nakama.data.Point;
import dmeeuwis.nakama.data.Rect;
import dmeeuwis.nakama.kanjidraw.PathCalculator.Intersection;
import dmeeuwis.util.Util;

public class PointDrawing implements Iterable<Stroke>, Drawing {
	static final private float MIN_POINT_DISTANCE_DP = 25;
	static final private float MIN_POINT_DISTANCE_FOR_DIRECTION_DP = 10;
	private final static double DIRECTION_LIMIT = Math.PI / 8;

	private final List<Stroke> strokes;
	private final int scaleX, scaleY;

	public static PointDrawing fromPrefilteredPoints(int scaleX, int scaleY, List<List<Point>> points){
		List<Stroke> strokes = new ArrayList<>(points.size());
		for(List<Point> p: points)
			strokes.add(new Stroke(p));
		return new PointDrawing(scaleX, scaleY, strokes);
	}

	public static PointDrawing fromDetailedPoints(int scaleX, int scaleY, List<List<Point>> points, Context context){
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

			if(gradeLine.size() == 1){
				gradeLine.add(line.get(line.size()-1));
			}
			gradeLines.add(new Stroke(gradeLine));
		}

		return new PointDrawing(scaleX, scaleY, gradeLines);
	}

	public PointDrawing(int scaleX, int scaleY, List<Stroke> pointStrokes){
		this.strokes = Collections.unmodifiableList(pointStrokes);
		this.scaleX = scaleX;
		this.scaleY = scaleY;
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
	public Rect findBounds(){
		Rect box = new Rect(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
		Log.i("nakama", "Bounding box for stroke 0 " + box);
		for(Stroke s: strokes){
			Rect r = s.findBoundingBox();
			box.left = Math.min(r.left, box.left);
			box.right = Math.max(r.right, box.right);
			box.top = Math.min(r.top, box.top);
			box.bottom = Math.max(r.bottom, box.bottom);
		}
		Log.i("nakama", "Created unioned bounding box: " + box);

		return box;
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
		return new PointDrawing(scaleX, scaleY, newList);
	}

	public PointDrawing cutOffEdges(){
		Rect bounds = this.findBoundingBox();
		System.out.println("Bounding box for " + this + " found as " + bounds);
		List<List<Point>> newCopy = new ArrayList<>(this.strokeCount());
		for(Stroke stroke: this.strokes){
			ArrayList<Point> newStroke = new ArrayList<>(stroke.pointSize());
			for(Point p: stroke.points){
				newStroke.add(new Point(p.x - bounds.left, p.y - bounds.top));
			}
			newCopy.add(newStroke);
		}
		PointDrawing newP = PointDrawing.fromPrefilteredPoints(scaleX, scaleY, newCopy);
		return newP;
	}

	public PointDrawing scaleToBox(Rect box){
		Rect bbSubject = this.findBoundingBox();

		float increaseXFactor = (float)(box.right - box.left) / bbSubject.right;
		float increaseYFactor = (float)(box.bottom - box.top) / bbSubject.bottom;

		List<List<Point>> scaled = new ArrayList<>(this.strokeCount());
		for(Stroke lp: this.strokes){
			List<Point> sl = new ArrayList<>(lp.pointSize());

			for(Point p: lp){
				sl.add(new Point((int)(p.x * increaseXFactor),
						(int)(p.y * increaseYFactor)));
			}

			scaled.add(sl);
		}
		
		return PointDrawing.fromPrefilteredPoints(scaleX, scaleY, scaled);
	}

	public PointDrawing scaleToBoxBetter(Rect box){
		Rect bbSubject = this.findBoundingBox();
		Log.i("nakama",  "This drawing has box: " + bbSubject);
		Log.i("nakama",  "Will scale to box: " + box);

		float increaseXFactor = (float)(box.right - box.left) / bbSubject.right;
		float increaseYFactor = (float)(box.bottom - box.top) / bbSubject.bottom;
		float scaleFactor = Math.min(increaseXFactor, increaseYFactor);

		Log.i("nakama",  "Will scale by " + scaleFactor);

		List<List<Point>> scaled = new ArrayList<>(this.strokeCount());
		for(Stroke lp: this.strokes){
			List<Point> sl = new ArrayList<>(lp.pointSize());
			for(Point p: lp) {
				Point np = new Point((int) (p.x * scaleFactor), (int) (p.y * scaleFactor));
				sl.add(np);
				Log.i("nakama", p + " -> " + np);
			}
			scaled.add(sl);
		}

		return PointDrawing.fromPrefilteredPoints(scaleX, scaleY, scaled);
	}


	@Override
	public Iterator<Stroke> iterator() {
		return this.strokes.iterator();
	}

	@Override
	public List<ParameterizedEquation> toParameterizedEquations(float scale){
		List<ParameterizedEquation> ret = new ArrayList<>(this.strokeCount());
		for(Stroke stroke: strokes)
			ret.add(stroke.toParameterizedEquation(scale, 0));
		return ret;
	}

	public String serialize() {
		try {
			Writer sw = new StringWriter();
			JsonWriter jw = new JsonWriter(sw);

			serialize(jw);

			jw.close();
			return sw.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public void serialize(JsonWriter jw) throws IOException {
        jw.beginObject();
        jw.name("scaleX");
        jw.value(scaleX);
        jw.name("scaleY");
        jw.value(scaleY);
        jw.name("drawing");
        jw.beginArray();
        for (Stroke s : strokes) {
            jw.beginArray();
            for (Point p : s.points) {
                jw.beginArray();
                jw.value(p.x);
                jw.value(p.y);
                jw.endArray();
            }
            jw.endArray();
        }
        jw.endArray();
        jw.endObject();
	}

	public String toString(){
		StringBuilder s = new StringBuilder();
		s.append("PointDrawing: scaleX " + scaleX + "; scaleY: " + scaleY);
		for(Stroke stroke: this.strokes){
			s.append("\n\t");
			s.append(Util.join(", ", stroke.points));
		}
		return s.toString();
	}
}
