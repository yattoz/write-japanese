package dmeeuwis.nakama.kanjidraw;

import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import dmeeuwis.util.Util;

public class Stroke implements Iterable<Point> {

	final protected List<Point> points;
	
	final Point startPoint, endPoint;
	
	public Stroke(List<Point> points){
		this.points = Collections.unmodifiableList(points);
		this.startPoint = this.points.get(0);
		this.endPoint = this.points.get(this.points.size() - 1);
	}

	/**
	 * Returns the arc length of the entire stroke, calculated from line segment addition.
	 **/
	public int arcLength(){
		return PathCalculator.calculateArcLength(points);
	}
	
	/**
	 * Returns the length of a straight line drawn from the first point, to the last point.
	 */
	public double distanceFromStartToEndPoints(){
		return PathCalculator.distance(startPoint, endPoint);
	}

	/**
	 * Returns the direction of the line at the start of the path, in human understandable english.
	 */
	public String startDirectionEnglish(){
		return Util.radiansToEnglish(startDirection());
	}

	/**
	 * Returns the direction of the line at the end of the path, in human understandable english.
	 */
	public String endDirectionEnglish(){
		return Util.radiansToEnglish(endDirection());
	}


	/**
	 * Returns the direction of the line at the start of the path, in radians.
	 * @return A float between -PI and PI.
	 */
	public double startDirection(){
		Point p0 = points.get(0);
		Point p1 =  points.get(1);
		double dir = PathCalculator.angle(p0, p1);
		
		return dir;
	}

    public int maxY(){
        int y = 0;
        for(Point p: points){
           if(p.y > y){
               y = p.y;
           }
        }
        return y;
    }
	
	public double endDirection(){
		final int s = points.size();
		double angle = PathCalculator.angle(points.get(s-2), points.get(s-1));
		//Log.i("nakama", "End angle from points " + points.get(s-2) + " and " + points.get(s-1) + " found to be: " + angle);
		return angle;
	}
	
	public double normalizeRadians(double in){
		if(in > Math.PI * 2){
			return in - Math.PI * 2;
		} else if(in < 0){
			return in + Math.PI * 2;
		} else {
			return in;
		}
	}

	/**
	 * Returns the direction of the line at the end of the path, in radians. 
	 * @return A float between -PI and PI.
	 */
	public double complexEndDirection(){
		final int s = points.size();
		if(points.size() == 2){
//			Log.i("nakama", "endDirection: only 2 points, using them!");
//			Log.i("nakama", "endDirection: first point " + points.get(s-2) + ", second point " + points.get(s-1) + ": angle " + PathCalculator.angle(points.get(s-2),  points.get(s-1)));
			return PathCalculator.angle(points.get(s-2), points.get(s-1));
		} else {
		
			double totalDistance = 0;
			for(int i = 1; i < points.size(); i++){
				Point p0 = points.get(i - 1);
				Point p1 = points.get(i);
				double distance = PathCalculator.distance(p0, p1);
				totalDistance += distance;
			}
			
			double averageDistance = totalDistance / (s - 1);
			double lastDistance = PathCalculator.distance(points.get(s-2), points.get(s-1));
			
			if(lastDistance < averageDistance / 2){
//				Log.i("nakama", "endDirection: Last line segment truncated (distance " + lastDistance + ", average was " + averageDistance + "), using third last instead.");
				return PathCalculator.angle(points.get(s-3), points.get(s-1));
			} else {
//				Log.i("nakama", "endDirection: using last 2 points as normal.");
				return PathCalculator.angle(points.get(s-2), points.get(s-1));
			}
		}
	}
	
	public Rect findBoundingBox(){
		return PathCalculator.findBoundingBox(this.points);
	}
	
	public Stroke bufferEnds(int amount){
		if(points.size() < 2)
			throw new IllegalArgumentException("Cannot buffer a path with size < 2");
		
		List<Point> buffered = new ArrayList<Point>(points.size() + 2);
		
		buffered.add(0, null);
		for(int i = 1; i < points.size(); i++){
			buffered.add(i, points.get(i-1));
		}
		buffered.add(points.size(), null);

		{
			// extend the path a little in front of the first point according to slope of line of first 2 points
			Point p0 = points.get(0);
			Point p1 = points.get(1);

			Point extend = PathCalculator.extendSegment(p1, p0, amount);
			buffered.set(0, extend);
			// Log.d("nakama", "Buffered start " + p0 + ", " + p1 + " by " + amount + " with new first element  " + extend);
		}

		{
			// extend the path a little out of the last point according to slope of line of last 2 points
			final int last = points.size() - 1;
			final int nlast = points.size() - 2;
			Point plast = points.get(last);
			Point pnlast = points.get(nlast);

			Point extend = PathCalculator.extendSegment(pnlast, plast, amount);
			buffered.set(points.size(), extend);
			// Log.d("nakama", "Buffered end " + pnlast + ", " + plast + " by " + amount + " with new element " + extend);
		}
		
		return new Stroke(buffered);
	}
	
	public Stroke scale(float scale){
		List<Point> newLine = new ArrayList<Point>(points.size());
		for(Point origPoint: points){
			newLine.add(new Point((int)(origPoint.x*scale), (int)(origPoint.y*scale)));
		}
		return new Stroke(newLine);
	}
	
	public List<Pair<Point, Integer>> createDebugDots(){
		List<Pair<Point, Integer>> dots = new ArrayList<Pair<Point, Integer>>(2);
		
		// start and end points
		dots.add(Pair.create(points.get(0), Color.CYAN));
		dots.add(Pair.create(points.get(points.size() - 1), Color.CYAN));
		
		return dots;
	}
	
	public ParameterizedEquation toParameterizedEquation(float scaler, float padding){
		float xOffset = padding;
		float yOffset = padding;
    	List<ParameterizedEquation> ret = new ArrayList<ParameterizedEquation>(points.size() - 1);
    	Point start = points.get(0);
    	for(int i = 1; i < points.size(); i++){
    		Point end = points.get(i);
    		ret.add(new LinearEquation((start.x*scaler) + xOffset, (start.y*scaler) + yOffset, 
    									(end.x*scaler) + xOffset, (end.y*scaler) + yOffset));
    		start = end;
    	}
    	
    	return new Spline(ret);
	}

	@Override
	public Iterator<Point> iterator() {
		return points.iterator();
	}
	
	public int pointSize(){
		return points.size();
	}
}
