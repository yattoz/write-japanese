package dmeeuwis.nakama.kanjidraw;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import dmeeuwis.Kana;
import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.AssetFinder;
import dmeeuwis.nakama.kanjidraw.PathCalculator.Intersection;
import dmeeuwis.util.Util;

public class DrawingComparator {
	
	public enum StrokeCompareFailure { DISTANCE_TRAVELLED, START_POINT_DIFFERENCE, END_POINT_DIFFERENCE, START_DIRECTION_DIFFERENCE, END_DIRECTION_DIFFERENCE, BACKWARDS, TOO_MANY_SHARP_CURVES, TOO_FEW_SHARP_CURVES }
	public enum OverallFailure { EXTRA_STROKES, MISSING_STROKES, MISSING_INTERSECTION, WRONG_STROKE_ORDER }

	private final float FAIL_POINT_START_DISTANCE;
	private final float FAIL_POINT_END_DISTANCE;
	private final float CIRCLE_DETECTION_DISTANCE;
	static private final double STROKE_DIRECTION_LIMIT_RADIANS = Math.PI / 2;
	static private final int PERCENTAGE_DISTANCE_DIFF_LIMIT = 100;

	final char target;
	final PointDrawing known;
	final PointDrawing drawn;
	final int drawingAreaMaxDim;
	final AssetFinder assetFinder;
    final CharacterStudySet hiraganaSet, katakanaSet;

    static private final boolean debug = false;
	
	public DrawingComparator(char target, CurveDrawing known, PointDrawing challenger, AssetFinder assetFinder){
		this.target = target; 
		this.assetFinder = assetFinder;
        this.hiraganaSet = CharacterSets.hiragana(null);
        this.katakanaSet = CharacterSets.katakana(null);
		
		this.drawn = challenger.cutOffEdges();// scaleToBox(nBounds);
		Rect drawnBox = this.drawn.findBoundingBox();
		
		PointDrawing cutOffKnown = known.pointPointDrawing.cutOffEdges();
		this.known = cutOffKnown.scaleToBox(drawnBox);

		Rect nBounds = this.known.findBoundingBox();
		this.drawingAreaMaxDim = Math.max(nBounds.width(), nBounds.height());
		
		this.FAIL_POINT_START_DISTANCE = (float)(drawingAreaMaxDim * 0.40);
		this.FAIL_POINT_END_DISTANCE = (float)(drawingAreaMaxDim * 0.40);
		this.CIRCLE_DETECTION_DISTANCE = (float)(drawingAreaMaxDim * 0.10);
		
		if(debug) Log.d("nakama", "PathComparator.new: drawingAreaWidth: " + drawingAreaMaxDim);
		if(debug) Log.d("nakama", "PathComparator.new: scaled drawn to " + this.drawn.findBoundingBox());
		if(debug) Log.d("nakama", "PathComparator.new: circle detection distance " + this.CIRCLE_DETECTION_DISTANCE);
		if(debug) Log.d("nakama", "PathComparator.new: known: " + known.findBoundingBox());
		if(debug) Log.d("nakama", "PathComparator.new: known cut bounds: " + cutOffKnown.findBoundingBox());
		if(debug) Log.d("nakama", "PathComparator.new: known cut scaled bounds: " + this.known.findBoundingBox());
	}

	private static class StrokeCriticism {
		final public Integer cost;
		final public String message;

        public StrokeCriticism(String message){
            this.message = message;
            this.cost = 1;
        }

        public StrokeCriticism(String message, int cost) {
            this.message = message;
            this.cost = cost;
        }

	}
	
	
	public Criticism compare(){
		return compare(Recursion.ALLOW);
	}

	private enum Recursion { ALLOW, DISALLOW }
	private Criticism compare(Recursion allowRecursion){
		Criticism c = new Criticism();
		List<OverallFailure> overallFailures = new ArrayList<OverallFailure>();
		
		if(this.drawn.strokeCount() < this.known.strokeCount())
			overallFailures.add(OverallFailure.MISSING_STROKES);
		if(this.drawn.strokeCount() > this.known.strokeCount())
			overallFailures.add(OverallFailure.EXTRA_STROKES);
		
		StrokeCriticism[][] criticismMatrix = new StrokeCriticism[known.strokeCount()][drawn.strokeCount()];
		int[][] scoreMatrix = new int[known.strokeCount()][drawn.strokeCount()];


        boolean correctDiagonal = known.strokeCount() == drawn.strokeCount();
        if(correctDiagonal){ // possibly
           for(int i = 0; i < known.strokeCount(); i++) {
               StrokeCriticism r = compareStroke(i, i);
               criticismMatrix[i][i] = r;
               scoreMatrix[i][i] = r.cost;
               correctDiagonal = r.cost == 0 && correctDiagonal;
           }
        }

        if(correctDiagonal){
            if(debug) Log.d("nakama", "Correct diagonal detected! Going home early.");
            return new Criticism();
        }

		// calculate score and criticism matrix
		for(int known_i = 0; known_i < known.strokeCount(); known_i++){
			for(int drawn_i = 0; drawn_i < drawn.strokeCount(); drawn_i++){
                if(drawn_i == known_i){ continue; }         // calculated in previous block

				StrokeCriticism result = compareStroke(known_i, drawn_i);
				if(debug) Log.d("nakama", "Compared known " + known_i + " to drawn " + drawn_i + ": " + result.cost + "; " + result.message);
				criticismMatrix[known_i][drawn_i] = result;
				scoreMatrix[known_i][drawn_i] = result.cost;
			}
		}
		
	
		if(debug) Log.d("nakama", "Score Matrix\n======================" + printMatrix(scoreMatrix) + "====================");
	
		if(debug) Log.d("nakama", "Scanning for known intersects.");
		List<Intersection> knownIntersects = this.known.findIntersections();
		if(debug) Log.d("nakama", "This kanji should have " + knownIntersects.size() + " intersects:\n" + Util.join("\n", knownIntersects) + "\n");
		
		if(debug) Log.d("nakama", "Scanning for drawn intersects.");
		List<Intersection> drawnIntersects = this.drawn.findIntersections();
		if(debug) Log.d("nakama", "This drawn kanji has " + drawnIntersects.size() + " intersects:\n" + Util.join("\n", drawnIntersects + "\n"));

		int intersectDistanceLimit = (int)(drawingAreaMaxDim * 0.3);
		if(debug) Log.d("nakama", "Using max intersect distance of " + intersectDistanceLimit);
		outer: for(Intersection knownInt: knownIntersects){
			for(Intersection drawnInt: drawnIntersects){
				double distance = PathCalculator.distance(knownInt.intersectPoint, drawnInt.intersectPoint);
				if(drawnInt.strokesMatch(drawnInt) && distance <= intersectDistanceLimit){
					continue outer;
				} else {
					if(debug) Log.d("nakama", "Saw distance between intersects " + drawnInt + " and known int " + knownInt.intersectPoint + " as " + distance);
				}
			}
			c.add("Your " + Util.adjectify(knownInt.firstPathIndex, known.strokeCount()) + " and " + Util.adjectify(knownInt.secondPathIndex, known.strokeCount()) + " strokes should meet.");
		}

		if(overallFailures.contains(OverallFailure.MISSING_STROKES)){
			int missingStrokes = this.known.strokeCount() - this.drawn.strokeCount();
			if(missingStrokes == 1){
				c.add("You are missing a stroke.");
			} else {
				c.add("You are missing " + Util.nounify(missingStrokes) + " strokes.");
			}
		} else if(overallFailures.contains(OverallFailure.EXTRA_STROKES)) {
			int extraStrokes = this.drawn.strokeCount() - this.known.strokeCount();
			if(extraStrokes == 1){
				c.add("You drew an extra stroke.");
			} else {
				c.add("You drew " + Util.nounify(extraStrokes) + " extra strokes.");
			}
		}
		
		
		// find best set of strokes
		List<StrokeResult> bestStrokes = findBestPairings(scoreMatrix);
		best: for(StrokeResult s: bestStrokes){
			if(s.score == 0){ 
				if(!s.knownStrokeIndex.equals(s.drawnStrokeIndex)) {
					for(StrokeResult subS: bestStrokes){
						if(s.drawnStrokeIndex.equals(subS.knownStrokeIndex) && subS.score == 0){
							c.add("Your " + Util.adjectify(s.knownStrokeIndex, drawn.strokeCount()) + " and " + Util.adjectify(s.drawnStrokeIndex, drawn.strokeCount()) + " strokes are correct, except drawn in the wrong order.");
							break best;
						}
					}
				}
			} else {
				if(!(s.knownStrokeIndex == null || s.drawnStrokeIndex == null)){
					c.add(criticismMatrix[s.knownStrokeIndex][s.drawnStrokeIndex].message);
				}
			}
		}
		

		// special case for hiragana and katakana: find if the user drew katakana version instead of hiragana, and vice-versa
		if(allowRecursion == Recursion.ALLOW){
			if(!c.pass && Kana.isHiragana(target)){
				char katakanaVersion = Kana.hiragana2Katakana(String.valueOf(target)).charAt(0);
				DrawingComparator pc = new DrawingComparator(katakanaVersion, assetFinder.findGlyphForCharacter(katakanaSet, katakanaVersion), this.drawn, assetFinder);
				if(pc.compare(Recursion.DISALLOW).pass){
					Criticism specific = new Criticism();
					specific.add("You drew the katakana " + katakanaVersion + " instead of the hiragana " + target + ".");
					return specific;
				}
			} else if(!c.pass && Kana.isKatakana(target)){
				char hiraganaVersion = Kana.katakana2Hiragana(String.valueOf(target)).charAt(0);
				DrawingComparator pc = new DrawingComparator(hiraganaVersion, assetFinder.findGlyphForCharacter(hiraganaSet, hiraganaVersion), this.drawn, assetFinder);
				if(pc.compare(Recursion.DISALLOW).pass){
					Criticism specific = new Criticism();
					specific.add("You drew the hiragana " + hiraganaVersion + " instead of the katakana " + target + ".");
					return specific;
				}
			}
		}
		
		return c;
		
		// ToDo:
		// intersection point distance errors
	}
	
	private static class StrokeResult {
		public final Integer knownStrokeIndex;
		public final Integer drawnStrokeIndex;
		public final int score;
		
		public StrokeResult(Integer known, Integer drawn, int score){ 
			this.knownStrokeIndex = known;
			this.drawnStrokeIndex = drawn;
			this.score = score;
		}
	}
	
	
	static List<StrokeResult> findBestPairings(int[][] matrix){
		Set<Integer> finishedRows = new TreeSet<>();
		Set<Integer> finishedCols = new TreeSet<>();
		List<StrokeResult> pairs = new ArrayList<>(Math.max(matrix.length, matrix[0].length));
		
		// first go down the diagonal and accept any 0s
		for(int i = 0; i < matrix.length && i < matrix[i].length; i++){
			if(matrix[i][i] == 0){
				finishedRows.add(i);
				finishedCols.add(i);
				
				pairs.add(new StrokeResult(i, i, 0));
			}
		}
		
		// now go row by row, column by column and assign the first match
		for(int i = 0; i < matrix.length; i++){
			if(finishedRows.contains(i)){
				continue;
			}

			int selected = -1;
			int min = Integer.MAX_VALUE;
			
			for(int j = 0; j < matrix[i].length; j++){
				if(finishedCols.contains(j)){
					continue;
				}
			
				if(min >= matrix[i][j]){
					selected = j;
					min = matrix[i][j];
				}
			}
			
			if(selected >= 0){
				finishedRows.add(i);
				finishedCols.add(selected);
				pairs.add(new StrokeResult(i,  selected, matrix[i][selected]));
			}
		}
		
		for(int i = 0; i < matrix.length; i++){
			if(!finishedRows.contains(i)){
				pairs.add(new StrokeResult(i, null, 1));
			}
		}
		
		return pairs;
	}
	
	/**
	 * Compares one stroke to another, generating a list of criticisms.
	 */
	private StrokeCriticism compareStroke(int baseIndex, int challengerIndex){
		if(debug) Log.d("nakama", "\n================================================================");
		if(debug) Log.d("nakama", "Comparing base stroke " + baseIndex + " to challenger stroke " + challengerIndex);
		List<StrokeCompareFailure> failures = new LinkedList<StrokeCompareFailure>();



		Stroke bpath = this.known.get(baseIndex);
		Stroke cpath = this.drawn.get(challengerIndex);
		if(debug) Log.d("nakama", String.format("%30s:  Base: %6d Drawn: %6d", "Number of points", bpath.pointSize(), cpath.pointSize()));

        if(debug) Log.d("nakama", String.format("Base points: " + Util.join(", ", bpath.points)));
        if(debug) Log.d("nakama", String.format("Drawn points: " + Util.join(", ", cpath.points)));

		Point bstart = bpath.startPoint;
		Point bend = bpath.endPoint;
		Point cstart = cpath.startPoint;
		Point cend = cpath.endPoint;
		
		if(debug) Log.d("nakama", String.format("%30s:  Base[0]: %s Drawn[0]: %s", "Start points", bstart, cstart));
		if(debug) Log.d("nakama", String.format("%30s:  Base[-1]: %s Drawn[-1]: %s", "End points", bend, cend));
		
		/* Arc length is seeming to be not so accurate, due to all the minor fluctuation in a user's stroke throwing it off.
		 * Lets rely more on distance traveled (distance from start point to end point ignoring curvature completely).

  		int blength = bpath.arcLength();
		int clength = cpath.arcLength();
		float arcLengthDiff = 1 - Math.abs((float)Math.min(blength, clength) / Math.max(blength, clength));
		boolean arcLengthDifference = arcLengthDiff > arcLengthFailPercentage(blength);
		// arc lengths should be about equal.
		Log.d("nakama", String.format("%30s:  %6d %6d", "Arc Length", blength, clength));
		if(arcLengthDifference){
			if(blength > clength)
				criticisms.add(Criticism.distanceFail("Your " + Util.adjectify(challengerIndex) + " stroke is too short.", // [Percent diff was: " + arcLengthDiff + "; base length: " + blength + ", your length: " + clength + "]",
					bend, cend, FAIL_ARC_LENGTH_PERCENTAGE));
			else 
				criticisms.add(Criticism.distanceFail("Your " + Util.adjectify(challengerIndex) + " stroke is too long.", // [Percent diff was: " + arcLengthDiff + "; base length: " + blength + ", your length: " + clength + "]",
				bend, cend, FAIL_ARC_LENGTH_PERCENTAGE));
		}
		 */
		
		double bDistanceTravelled = bpath.distanceFromStartToEndPoints();
		double cDistanceTravelled = cpath.distanceFromStartToEndPoints();
		int percentDiff = (int)(Math.abs(bDistanceTravelled - cDistanceTravelled) / ((bDistanceTravelled + cDistanceTravelled) / 2) * 100);
		if(debug) Log.d("nakama", String.format("%30s:  %6.2f %6.2f. Percentage diff: %d, limit is %d", "Distance Travelled", bDistanceTravelled, cDistanceTravelled, percentDiff, PERCENTAGE_DISTANCE_DIFF_LIMIT));
		if(percentDiff > PERCENTAGE_DISTANCE_DIFF_LIMIT)
			failures.add(StrokeCompareFailure.DISTANCE_TRAVELLED);
		
	
		final double bStartRadians = bpath.startDirection();
		final double cStartRadians = cpath.startDirection();
		
		final double bEndRadians = bpath.endDirection();
		final double cEndRadians = cpath.endDirection();
		
		if(debug) Log.d("nakama", "Base stroke " + baseIndex + " has points: " + Util.join(", ", bpath.points));
		
		double challengerStartDiff = Math.min((2 * Math.PI) - Math.abs(cStartRadians - cStartRadians), Math.abs(cStartRadians - cStartRadians));
		double baseStartDiff = Math.min((2 * Math.PI) - Math.abs(bStartRadians - bStartRadians), Math.abs(cStartRadians - cStartRadians));

		final boolean smallDistance = bDistanceTravelled < CIRCLE_DETECTION_DISTANCE && cDistanceTravelled < CIRCLE_DETECTION_DISTANCE;
		final boolean baseSameStartEndDirection = baseStartDiff < STROKE_DIRECTION_LIMIT_RADIANS;
		final boolean challengerSameStartEndDirection = challengerStartDiff < STROKE_DIRECTION_LIMIT_RADIANS;
		if(smallDistance && baseSameStartEndDirection && challengerSameStartEndDirection){
			if(debug) Log.d("nakama", "SPECIAL CASE: CIRCLE detected, ignoring stroke directions.");
			return new StrokeCriticism(null, 0);
		}
				
		
		{
			double startDistance = PathCalculator.distance(bstart, cstart);
			if(startDistance > FAIL_POINT_START_DISTANCE){
				failures.add(StrokeCompareFailure.START_POINT_DIFFERENCE);
			}
			if(debug) Log.d("nakama", String.format("%30s:  %6.2f from points known %6s, drawn %6s. Fail distance is %6.2f.", "Start Point Difference", startDistance, bstart, cstart, FAIL_POINT_START_DISTANCE));
		}
		
		// end points should be close to each other.
		{
			double endDistance = PathCalculator.distance(bend, cend);
			if(endDistance > FAIL_POINT_END_DISTANCE){
				failures.add(StrokeCompareFailure.END_POINT_DIFFERENCE);
			}
			if(debug) Log.d("nakama", String.format("%30s:  %6.2f from points known %6s, drawn %6s. Fail distance is %6.2f.", "End Point Difference", endDistance, bend, cend, FAIL_POINT_END_DISTANCE));
		}

		// TODO: curvature differences: concave vs convex lines might have same length, but be wrong.
		// but maybe beginning and end direction deal with this pretty well?
	
		// for the 2 direction comparisons below, first compare the numerical difference. If its big enough, also compare string 
		// version. String compare is also needed because string version is what is shown to users. If we don't compare strings, 
		// users could see 'was down, should be down'. If we don't compare numbers, users could be off by 0.00001 radians, but if
		// they are right on the border between two direction-descriptions, still get a fail. So, both are needed.
		
		// direction at beginning of stroke
		String bStartDirection = Util.radiansToEnglish(bStartRadians);
		String cStartDirection = Util.radiansToEnglish(cStartRadians);
		double radianStartDiff = Math.min((2 * Math.PI) - Math.abs(bStartRadians - cStartRadians), Math.abs(bStartRadians - cStartRadians));
		if(debug) Log.d("nakama", String.format("%30s:  %6.2f (%s) %6.2f (%s). Radian difference is %6.2f Limit %f.", "Start angle", bStartRadians, bStartDirection, cStartRadians, cStartDirection, radianStartDiff, STROKE_DIRECTION_LIMIT_RADIANS));
		if(radianStartDiff > STROKE_DIRECTION_LIMIT_RADIANS && !cStartDirection.equals(bStartDirection)){
			failures.add(StrokeCompareFailure.START_DIRECTION_DIFFERENCE);
		}
	
		// direction at end of stroke
		String bEndDirection = Util.radiansToEnglish(bEndRadians);
		String cEndDirection = Util.radiansToEnglish(cEndRadians);
		double radianEndDiff = Math.min((2 * Math.PI) - Math.abs(bEndRadians - cEndRadians), Math.abs(bEndRadians - cEndRadians));
		if(debug) Log.d("nakama", String.format("%30s:  %6.2f (%s) %6.2f (%s). Radian difference is %6.2f. Limit %f.", "End angle", bEndRadians, bEndDirection, cEndRadians, cEndDirection, radianEndDiff, STROKE_DIRECTION_LIMIT_RADIANS));
		if(radianEndDiff > STROKE_DIRECTION_LIMIT_RADIANS && !bEndDirection.equals(cEndDirection)){
			failures.add(StrokeCompareFailure.END_DIRECTION_DIFFERENCE);
		}
		
		// hard curves
/*		List<Point> baseCurvePoints = PathCalculator.findSharpCurves(bpath);
		Log.d("nakama", "Scanned base for sharp curves, found " + baseCurvePoints.size());
		List<Point> drawnCurvePoints = PathCalculator.findSharpCurves(cpath);
		Log.d("nakama", "Scanned drawn for sharp curves, found " + drawnCurvePoints.size());
		if(drawnCurvePoints.size() > baseCurvePoints.size()){
			failures.add(StrokeCompareFailure.TOO_MANY_SHARP_CURVES);
		} else if(drawnCurvePoints.size() < baseCurvePoints.size()){
			failures.add(StrokeCompareFailure.TOO_FEW_SHARP_CURVES);
		} else {
			// point by point comparison
		}
*/
		
		// basic data points collected above; now go through basic criticisms data, and try to combine into more constructive ones.
		// ===================================================================================================================
		
		// detect if the stroke is good, but in the wrong direction. 
		boolean startsCloserToEnd = PathCalculator.distance(bstart, cend) < FAIL_POINT_END_DISTANCE;
		boolean endsCloserToStart = PathCalculator.distance(bend, cstart) < FAIL_POINT_START_DISTANCE;
		
		//TODO: this needs checking:
		boolean startsReversed = Math.abs(bStartRadians - PathCalculator.reverseDirection(cEndRadians)) < STROKE_DIRECTION_LIMIT_RADIANS;
		boolean endsReversed = Math.abs(bEndRadians - PathCalculator.reverseDirection(cStartRadians)) < STROKE_DIRECTION_LIMIT_RADIANS;
		if(startsCloserToEnd && endsCloserToStart && startsReversed && endsReversed){
			failures.clear();
			failures.add(StrokeCompareFailure.BACKWARDS);
				// + "startsAtEnd: " + startsCloserToEnd + "; endsAtStart: " + endsCloserToStart + "; startsReversed: " + startsReversed + "; endReversed: " + endsReversed + "; " +
				//	"base dirs: " + bStartRadians + "; " + bEndRadians + "; challenger dirs: " + cStartRadians + ", " + cEndRadians +
				//	"; base dirs: " + bStartDirection + ", " + bEndDirection + "; challenger dirs: " + cStartDirection + ", " + cEndDirection));
		}
		if(debug) Log.d("nakama", "========================== end of " + baseIndex + " vs " + challengerIndex);

		
		if(failures.size() == 0) {
			return new StrokeCriticism(null, 0);
			
		} else if(failures.size() == 1) {
			StrokeCompareFailure f = failures.get(0);
		
			switch (f) {
			case START_DIRECTION_DIFFERENCE:
				return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke starts pointing " + cStartDirection + ", but should point " + bStartDirection + "."
                + (BuildConfig.DEBUG ? String.format(" [points %.2f, but should be %.2f]", cStartRadians, bStartRadians) : ""));
			case END_DIRECTION_DIFFERENCE:
				return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke ends pointing " + cEndDirection + ", but should point " + bEndDirection + "."
                        + (BuildConfig.DEBUG ? String.format(" [points %.2f, but should be %.2f]", cEndRadians, bEndRadians) : ""));
			case START_POINT_DIFFERENCE:
				return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke's starting point is off.");
			case END_POINT_DIFFERENCE:
				return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke's ending point is off.");
			case DISTANCE_TRAVELLED:
				if(cDistanceTravelled < bDistanceTravelled) {
					return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke is too short." +
                            (BuildConfig.DEBUG ? " (base: " + bDistanceTravelled + ", challenge: " + cDistanceTravelled + ")" : ""));
				} else {
					return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke is too long." +
                        (BuildConfig.DEBUG ? " (base: " + bDistanceTravelled + ", challenge: " + cDistanceTravelled + ")" : ""));
				}
//			case TOO_FEW_SHARP_CURVES:
//			case TOO_MANY_SHARP_CURVES:
//				return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke has " + drawnCurvePoints.size() + " sharp curve" + (drawnCurvePoints.size() == 1 ? "" : "s") + ", but should have " + baseCurvePoints.size() + ".");
			case BACKWARDS:
				return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke is backwards.");
			default: 
				throw new RuntimeException("Error: unhandled StrokeCompareFailure");
			}
			
		} else if(failures.size() == 2){

			if(failures.contains(StrokeCompareFailure.DISTANCE_TRAVELLED)){
				String distanceMessage = cDistanceTravelled > bDistanceTravelled ? "too long" : "too short";
				if(failures.contains(StrokeCompareFailure.START_DIRECTION_DIFFERENCE)){
					return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke is " + distanceMessage + ", and starts pointing " + cStartDirection +" instead of " + bStartDirection, 2);
				} else if(failures.contains(StrokeCompareFailure.END_DIRECTION_DIFFERENCE) && failures.contains(StrokeCompareFailure.DISTANCE_TRAVELLED)){
					return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke is " + distanceMessage + ", and ends pointing " + cEndDirection +" instead of " + bEndDirection, 2);
				} else if(failures.contains(StrokeCompareFailure.END_POINT_DIFFERENCE)){
					if(bDistanceTravelled > cDistanceTravelled){
						return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke is too short.", 2);
					} else {
						return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke is too long.", 2);
					}
				}
			}

			return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke is not correct.", failures.size());
		}  else {
			return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke is not correct.", failures.size());
		}
	}

	public static String printMatrix(int[][] matrix){
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		for(int i = 0; i < matrix.length; i++){
			for(int j = 0; j < matrix[i].length; j++){
				sb.append(Integer.toString(matrix[i][j]));
				if(j != matrix[i].length - 1){
					sb.append(" ");
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
