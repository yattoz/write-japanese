package dmeeuwis.kanjimaster.logic.drawing;

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dmeeuwis.kanjimaster.logic.data.Point;
import dmeeuwis.kanjimaster.logic.data.Rect;

public class PathCalculator {


	public final static double reverseDirection(double direction){
		double n = Math.PI + direction;
		if(n >= 2*Math.PI){
			n -= 2*Math.PI;
		} 
		return n;
	}

	public final static int calculateArcLength(List<Point> points){
		final int size = points.size();
		
		if(size <= 1)
			return 0;

		double circum = 0;
		for(int i = 0; i < size - 1; i++){
			Point p1 = points.get(i);
			Point p2 = points.get(i+1);
			
			int width  = Math.abs(p2.x - p1.x);
			int height = Math.abs(p2.y - p1.y);
			
			double arclength = Math.sqrt(width*width + height*height);
			circum += arclength;
		}

		return (int)circum;
	}
	
	public final static double distance(float startx, float starty, float endx, float endy){
		return Math.sqrt( (endx - startx)*(endx - startx) + (endy - starty)*(endy - starty) );
	}
	
	public final static double distance(Point start, Point end){
		return Math.sqrt( (end.x - start.x)*(end.x - start.x) + (end.y - start.y)*(end.y - start.y) );
	}

	public final static double angle(Point a, Point b){
		return Math.atan2(-1 * b.y - -1 * a.y, b.x - a.x);
	}
	
	public final static double angle(double ax, double ay, double bx, double by){
		return Math.atan2(-1 * by - -1 * ay, bx - ax);
	}
	
	public final static Point extendSegment(Point p1, Point p2, int amount){
		double angle = angle(p1, p2);
		double hyp = amount;
		double xExt = Math.cos(angle) * hyp;
		double slope = (p2.y - p1.y) / ((double)p2.x - p1.x);
		double yExt = slope * xExt;

		// Log.d("nakama", "Extending segment " + p1 + " - " + p2 + " by amount " + amount + " with by adding: " + xExt + ", " + yExt);
		
		return new Point((int)xExt + p2.x, (int)yExt + p2.y);
	}

	public final static Rect findBoundingBox(List<Point> path){
		if(path == null || path.size() < 2)
			throw new IllegalArgumentException("Require at least 2 points to construct a bounding box.");
		
		Integer left = null, right = null, top = null, bottom = null;
		for(Point p: path){
			if(p == null)
				throw new NullPointerException("Null Point passed into findBoundingBox");
			
			if(left == null || p.x < left)
				left = p.x;
			if(right == null || p.x > right)
				right = p.x;
			if(top == null || p.y < top)
				top = p.y;
			if(bottom == null || p.y > bottom)
				bottom = p.y;
		}
		
		if(left == null || top == null || right == null || bottom == null)
			throw new NullPointerException(); // should be impossible since we must have >=2 to enter method body.
		
		//Log.d("nakama", "Constructed bounding box: " + Util.join(", ", left.toString(), top.toString(), right.toString(), bottom.toString())
		//			+ " from points: " + Util.join(path, ", "));
		return new Rect(left, top, right, bottom);
	}
	
	public final static List<Intersection> findIntersections(List<Stroke> paths){
		
		// construct bounding boxes for standardSets
		Rect[] boundingBoxes = new Rect[paths.size()];
		for(int i = 0; i < paths.size(); i++){
			boundingBoxes[i] = paths.get(i).findBoundingBox();
		}
		
		Set<Pair<Integer, Integer>> possibleIntersects = new HashSet<Pair<Integer, Integer>>();
		
		// only do detailed intersects for lines whose bounding boxes meet. n^2 for number of paths.
		for(int i = 0; i < paths.size(); i++){
			for(int j = i; j < paths.size(); j++){
				if(i == j) continue;
				
				if(Rect.intersects(boundingBoxes[i], boundingBoxes[j])){
					possibleIntersects.add(Pair.create(i, j));
				}
			}
		}
		
		List<Intersection> intersections = new LinkedList<Intersection>();
		// do expensive intersect finding for likely matches. n * m^2 (maybe) for n number of paths, m # of points in each path!
		for(Pair<Integer, Integer> possible: possibleIntersects){
			Stroke path1 = paths.get(possible.first);
			Stroke path2 = paths.get(possible.second);
			
			Set<Point> intersects = intersections(path1, path2);
			for(Point p: intersects)
				intersections.add(new Intersection(possible.first,possible.second, p));
		}
		
		return intersections;
	}

	private static final double TURN_THRESHOLD_RADIANS = Math.PI / 3;
	/** 
	 * Finds sharp turn points. Currently turns are between sequential points, but would be better to 
	 * do based on distance.
	 */
	static final List<Point> findSharpCurves(Stroke path){
        ParameterizedEquation p = path.toParameterizedEquation(1, 0);
        float[] turns = ParameterizedEquation.findHeavyTurns(p);
        List<Point> points = new ArrayList<>(turns.length);
        for(float t: turns) {
            points.add(new Point((int)p.x(t), (int)p.y(t)));
        }
        return points;
/*		List<Point> turnPoints = new ArrayList<>();
		Double prevDir = null;
		Point p0, p1;
		for(int i = 0; i < path.pointSize()-1; i++){
			p0 = path.points.get(i);
			p1 = path.points.get(i+1);
			double dir = angle(p0,  p1);
			if(prevDir != null && Math.abs(dir - prevDir) >= TURN_THRESHOLD_RADIANS){
                Log.d("nakama", String.format("Saw distance between points: %d, %d: %s and %s: %.2f >= %.2f", i, i+1, p0.toString(), p1.toString(), Math.abs(dir-prevDir), TURN_THRESHOLD_RADIANS));
				turnPoints.add(p0);
			}
			prevDir = dir;
		}
		return turnPoints;
*/
	}

    static int countSharpCurvesParamEqn(Stroke path){
        ParameterizedEquation p = path.toParameterizedEquation(1, 0);
        float[] turns = ParameterizedEquation.findHeavyTurns(p);
        return turns.length;
    }

	static final public Set<Point> intersections(Stroke path1, Stroke path2){
		// Log.d("nakama", "Detailed intersection check for:\n\t" + Util.join("; ", path1) + "\n\t" + Util.join("; ", path2));
		
		Set<Point> intersections = new HashSet<Point>();
		for(int i = 0; i < path1.points.size() - 1; i++){
			for(int j = 0; j < path2.points.size() - 1; j++){
				Point p1 = path1.points.get(i);
				Point p2 = path1.points.get(i+1);
				Point p3 = path2.points.get(j);
				Point p4 = path2.points.get(j+1);
				Point intersect = segmentIntersection(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y);
				if(intersect != null){
					//Log.d("nakama", "\t++++++++ Found an intersection at " + intersect);
					//Log.d("nakama", "\t++++++++ Stroke 1 is " + Util.join(", ", path1.points));
					//Log.d("nakama", "\t++++++++ Stroke 2 is " + Util.join(", ", path2.points));
					intersections.add(intersect);
				}
			}
		}
		return intersections;
	}
	
	/**
	* Computes the intersection between two (infinite) lines. The calculated point is approximate, 
	* since integers are used. If you need a more precise result, use doubles
	* everywhere. 
	* (c) 2007 Alexander Hristov. Use Freely (LGPL license). http://www.ahristov.com
	*
	* @param x1 Point 1 of Line 1
	* @param y1 Point 1 of Line 1
	* @param x2 Point 2 of Line 1
	* @param y2 Point 2 of Line 1
	* @param x3 Point 1 of Line 2
	* @param y3 Point 1 of Line 2
	* @param x4 Point 2 of Line 2
	* @param y4 Point 2 of Line 2
	* @return Point where the segments intersect, or null if they don't
	*/
	static final public Point intersection(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
		int d = (x1-x2)*(y3-y4) - (y1-y2)*(x3-x4);
		if (d == 0) return null;
		
		int xi = ((x3-x4)*(x1*y2-y1*x2)-(x1-x2)*(x3*y4-y3*x4))/d;
		int yi = ((y3-y4)*(x1*y2-y1*x2)-(y1-y2)*(x3*y4-y3*x4))/d;
		
		return new Point(xi,yi);
  }	
	
	static final public Point segmentIntersection(int x1, int y1, int x2, int y2, 
	         int x3, int y3, int x4, int y4) {

		Point intersect = intersection(x1, y1, x2, y2, x3, y3, x4, y4);
		if(intersect == null)
			return null;
		
		if(!(inLineSegmentBounds(intersect, x1, y1, x2, y2) && inLineSegmentBounds(intersect, x3, y3, x4, y4))){
			return null;
		}

		// ignore intersects that are one line's end meeting the other line's beginning.
		if(	intersect.x == x1 && intersect.y == y1 ||
			intersect.x == x2 && intersect.y == y2 ||
            intersect.x == x3 && intersect.y == y3 ||
			intersect.x == x4 && intersect.y == y4){
			return null;
		}


		return intersect;
	}
	
	/*
	 * For a point p that Must be on the line defined by points (x1,y1) and (x2,y2), returns true
	 * if p is within the line segment (x1,y1),(x2,y2).
	 */
	static final public boolean inLineSegmentBounds(Point p, int x1, int y1, int x2, int y2){
		boolean inXBounds = ((x1 <= p.x && x2 >= p.x) || (x2 <= p.x && x1 >= p.x));
		boolean inYBounds = ((y1 <= p.y && y2 >= p.y) || (y2 <= p.y && y1 >= p.y));
		// Log.d("nakama", "\t\tTesting " + p + " for line segment " + x1 + "," + y1 + " - " + x1 + "," + y2 + ": in-x: " + inXBounds + "; in-y: " + inYBounds);
		
		return inXBounds && inYBounds;
	}
	  
	final public static class Intersection {
		final public int firstPathIndex;
		final public int secondPathIndex;
		final public Point intersectPoint;
		
		Intersection(int p1i, int p2i, Point intersect){
			this.firstPathIndex = p1i;
			this.secondPathIndex = p2i;
			this.intersectPoint = intersect;
		}
		
		public boolean strokesMatch(Intersection b){
			return this.firstPathIndex == b.firstPathIndex && this.secondPathIndex == b.secondPathIndex;
		}
		
		@Override public String toString(){
			return String.format(Locale.ENGLISH, "Stroke %d met %d at point %s.", 
					this.firstPathIndex, this.secondPathIndex, 
					this.intersectPoint.x + "," + this.intersectPoint.y);
		}
	}
}
