package dmeeuwis.nakama.kanjidraw;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dmeeuwis.Kana;
import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.nakama.data.AssetFinder;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.Point;
import dmeeuwis.nakama.data.Rect;
import dmeeuwis.util.Util;

class SimpleDrawingComparator implements Comparator {

	enum StrokeOrder { DISCOUNT, COUNT }

	enum StrokeCompareFailure { START_POINT_DIFFERENCE, END_POINT_DIFFERENCE, BACKWARDS }
	enum OverallFailure { EXTRA_STROKES, MISSING_STROKES, WRONG_STROKE_ORDER }

	private float FAIL_POINT_START_DISTANCE;
	private float FAIL_POINT_END_DISTANCE;
	private float CIRCLE_DETECTION_DISTANCE;
	static private final double STROKE_DIRECTION_LIMIT_RADIANS = Math.PI / 2;

	private static final boolean DEBUG = BuildConfig.DEBUG && true;

    static private final CharacterStudySet hiraganaSet = CharacterSets.hiragana(null, null);
    static private final CharacterStudySet katakanaSet = CharacterSets.katakana(null, null);

	char target;
	PointDrawing known;
	PointDrawing drawn;

	int drawingAreaMaxDim;

	final AssetFinder assetFinder;
	StrokeOrder strokeOrder;

	SimpleDrawingComparator(AssetFinder assetFinder, StrokeOrder order) {
		this.assetFinder = assetFinder;
		strokeOrder = order;
	}

	public Criticism compare(char target, PointDrawing challenger, CurveDrawing known) throws IOException {
		return compare(target, challenger, known, Recursion.ALLOW);
	}

	public Criticism compare(char target, PointDrawing challenger, CurveDrawing known, Recursion recursion) throws IOException {
        Log.d("nakama", "Initial input to drawing comparator is: " + challenger);

		this.target = target;

		this.drawn = challenger.cutOffEdges();// scaleToBox(nBounds);
        Log.d("nakama", "Trimmed input to drawing comparator is: " + this.drawn);

        Log.d("nakama", "\nFind drawn binding box....");
		Rect drawnBox = this.drawn.findBoundingBox();
        Log.d("nakama", "Rect drawnBox is: " + drawnBox);

        Log.d("nakama", "\nTrimming known binding box....");
        this.known = known.pointPointDrawing.cutOffEdges();

        Log.d("nakama", "\nFind trimmed known binding box....");
        Rect trimmedBox = this.known.findBoundingBox();
        Log.d("nakama", "Trimmed known box is: " + trimmedBox);

        Log.d("nakama", "\nScaling known binding box....");
		this.known = this.known.scaleToBox(drawnBox);

        Log.d("nakama", "\nFinding binding box for known-trimmed-scalled box....");
		Rect nBounds = this.known.findBoundingBox();
        Log.d("nakama", "Rect final knownBox is: " + nBounds);

		this.drawingAreaMaxDim = Math.max(nBounds.width(), nBounds.height());

		// only characters with indeterminate stroke orders?
		if(target == 'モ' || target == 'も' || target == 'ま'){
			Log.i("nakama", "Overriding stroke order to DISCOUNT");
			strokeOrder = StrokeOrder.DISCOUNT;
		}

        if(strokeOrder == StrokeOrder.DISCOUNT){
			this.FAIL_POINT_START_DISTANCE = (float) (drawingAreaMaxDim * 0.50);
			this.FAIL_POINT_END_DISTANCE = (float) (drawingAreaMaxDim * 0.50);
		} else {
			this.FAIL_POINT_START_DISTANCE = (float) (drawingAreaMaxDim * 0.42);
			this.FAIL_POINT_END_DISTANCE = (float) (drawingAreaMaxDim * 0.42);
		}
		this.CIRCLE_DETECTION_DISTANCE = (float) (drawingAreaMaxDim * 0.10);

		if(DEBUG) Log.d("nakama", "PathComparator.new: drawingAreaWidth: " + drawingAreaMaxDim);
		if(DEBUG) Log.d("nakama", "PathComparator.new: scaled drawn to " + this.drawn.findBoundingBox());
		if(DEBUG) Log.d("nakama", "PathComparator.new: circle detection distance " + this.CIRCLE_DETECTION_DISTANCE);
		if(DEBUG) Log.d("nakama", "PathComparator.new: known: " + known.findBoundingBox());

		return compare(recursion);
	}

	private int[] findExtraStrokes(int knownCount, int drawnCount, List<StrokeResult> best){
        int extra = drawnCount - knownCount;
        if(extra <= 0){
            // only try to colour if we're sure we can identify the extra strokes
            return null;
        }

        // refactor this to streams when possible
        List<Integer> drewWell = new ArrayList<>(knownCount);
        for(StrokeResult r: best){
            if(r.score == 0){
                drewWell.add(r.drawnStrokeIndex);
            }
        }

        // only colour extra strokes if every stoke you drew, you drew well
        if(drewWell.size() == knownCount){
            return Util.missingInts(drawnCount, drewWell);
        } else {
            return null;
        }
    }


	private int[] findMissingStrokes(int knownCount, int drawnCount, List<StrokeResult> best){
		int missing = knownCount - drawnCount;
		if(missing <= 0){
			// only try to colour if we're sure we can identify the missing strokes
			return null;
		}

        // refactor this to streams when possible
		List<Integer> drewWell = new ArrayList<>(drawnCount);
		for(StrokeResult r: best){
			if(r.score == 0){
				drewWell.add(r.knownStrokeIndex);
			}
		}

		// only colour missing strokes if every stoke you drew, you drew well
		if(drewWell.size() == drawnCount){
			return Util.missingInts(knownCount, drewWell);
		} else {
			return null;
		}
	}
	
	
	private enum Recursion { ALLOW, DISALLOW }
	private Criticism compare(Recursion allowRecursion) throws IOException {
		Criticism c = new Criticism();
		List<OverallFailure> overallFailures = new ArrayList<OverallFailure>();

		if (this.drawn.strokeCount() < this.known.strokeCount())
			overallFailures.add(OverallFailure.MISSING_STROKES);
		if (this.drawn.strokeCount() > this.known.strokeCount())
			overallFailures.add(OverallFailure.EXTRA_STROKES);

		StrokeCriticism[][] criticismMatrix = new StrokeCriticism[known.strokeCount()][drawn.strokeCount()];
		double[][] scoreMatrix = new double[known.strokeCount()][drawn.strokeCount()];
		c.setScoreMatrix(scoreMatrix);


		boolean correctDiagonal = known.strokeCount() == drawn.strokeCount();
		if (correctDiagonal) { // possibly
			for (int i = 0; i < known.strokeCount(); i++) {
				StrokeCriticism r = compareStroke(i, i);
				if(r == null){
					throw new RuntimeException("Invalid null StrokeCriticism comparing " + i + " and " + i);
				}
				criticismMatrix[i][i] = r;
				scoreMatrix[i][i] = r.cost;
				correctDiagonal = r.cost == 0 && correctDiagonal;
			}
		}

		if (correctDiagonal) {
			Log.d("nakama", "Correct diagonal detected! Early CORRECT response..");
			return new Criticism();
		}

		// calculate score and criticism matrix
		for (int known_i = 0; known_i < known.strokeCount(); known_i++) {
			for (int drawn_i = 0; drawn_i < drawn.strokeCount(); drawn_i++) {
				if (known_i == drawn_i) {
					continue; // calculated in above diagonal block
				}
				StrokeCriticism result = compareStroke(known_i, drawn_i);
				if (DEBUG)
					Log.d("nakama", "Compared known " + known_i + " to drawn " + drawn_i + ": " + result.cost + "; " + result.message);
				criticismMatrix[known_i][drawn_i] = result;
				scoreMatrix[known_i][drawn_i] = result.cost;
			}
		}

		Log.d("nakama", "Score Matrix (y-axis=known, x-axis=drawn)\n======================" + Util.printMatrix(scoreMatrix) + "====================");

		// find best set of strokes
		List<StrokeResult> bestStrokes = findBestPairings(scoreMatrix);
		{
			Set<Integer> rearrangedDrawnStrokes = new HashSet<>(bestStrokes.size());
			List<StrokeResult> misorderedStrokes = new ArrayList<>(bestStrokes.size());

			for (StrokeResult s : bestStrokes) {
				//Log.d("nakama", "Best chosen: " + s + ": " + criticismMatrix[s.knownStrokeIndex][s.drawnStrokeIndex].message);

				if (s.score == 0) {
					if (!s.knownStrokeIndex.equals(s.drawnStrokeIndex)) {
						for (StrokeResult subS : bestStrokes) {
							boolean addWrongOrderCriticism =
									s.drawnStrokeIndex.equals(subS.knownStrokeIndex) &&
											subS.score == 0 &&
											!rearrangedDrawnStrokes.contains(s.drawnStrokeIndex) &&
											!rearrangedDrawnStrokes.contains(subS.drawnStrokeIndex);

							if (addWrongOrderCriticism) {
								misorderedStrokes.add(s);
								rearrangedDrawnStrokes.add(s.drawnStrokeIndex);
								break;
							}
						}
					}
				} else {
					if (!(s.knownStrokeIndex == null || s.drawnStrokeIndex == null)) {
						c.add(criticismMatrix[s.knownStrokeIndex][s.drawnStrokeIndex].message,
								new Criticism.RightStrokeColour(s.knownStrokeIndex),
								new Criticism.WrongStrokeColour(s.drawnStrokeIndex));
					}
				}
			}

			if(strokeOrder != StrokeOrder.DISCOUNT) {
                if (misorderedStrokes.size() > 2) {
                    String message = misorderedStrokes.size() == known.strokeCount() ?
                            "Your strokes seem correct, but are drawn in the wrong order." :
                            "Several strokes are drawn correctly, but in the wrong order.";
                    c.add(message,
                            Criticism.SKIP,
                            Criticism.SKIP);
                } else {
                    for (StrokeResult s : misorderedStrokes) {
                        c.add("Your " + Util.adjectify(s.knownStrokeIndex, drawn.strokeCount()) + " and " + Util.adjectify(s.drawnStrokeIndex, drawn.strokeCount()) + " strokes are correct, except drawn in the wrong order.",
                                Criticism.correctColours(s.knownStrokeIndex, s.drawnStrokeIndex),
                                Criticism.incorrectColours(s.knownStrokeIndex, s.drawnStrokeIndex));
                    }
                }
			}
		}


		if (overallFailures.contains(OverallFailure.MISSING_STROKES)) {
			int missingStrokes = this.known.strokeCount() - this.drawn.strokeCount();
			String message = missingStrokes == 1 ?
					"You are missing a stroke." :
					"You are missing " + Util.nounify(missingStrokes) + " strokes.";

			Criticism.PaintColourInstructions colours;
			int[] missedStrokes = findMissingStrokes(known.strokeCount(), drawn.strokeCount(), bestStrokes);
			if (missedStrokes == null) {
				colours = Criticism.SKIP;
			} else {
				colours = Criticism.correctColours(missedStrokes);
			}
			c.add(message, colours, Criticism.SKIP);

		} else if (overallFailures.contains(OverallFailure.EXTRA_STROKES)) {
			int extraStrokes = this.drawn.strokeCount() - this.known.strokeCount();
			String message = extraStrokes == 1 ?
					"You drew an extra stroke." :
					"You drew " + Util.nounify(extraStrokes) + " extra strokes.";

			Criticism.PaintColourInstructions colours;
			int[] nonKnownStrokes = findExtraStrokes(known.strokeCount(), drawn.strokeCount(), bestStrokes);
			if (nonKnownStrokes == null) {
				colours = Criticism.SKIP;
			} else {
				colours = Criticism.incorrectColours(nonKnownStrokes);
			}

			c.add(message, Criticism.SKIP, colours);
		}


		// special case for hiragana and katakana: find if the user drew katakana version instead of hiragana, and vice-versa
		if (allowRecursion == Recursion.ALLOW) {
			if (!c.pass && Kana.isHiragana(target) && target != 'も') {
				Log.d("nakama", "Doing additional comparison between hiragana and katanama");
				char katakanaVersion = Kana.hiragana2Katakana(String.valueOf(target)).charAt(0);
				SimpleDrawingComparator pc = new SimpleDrawingComparator(assetFinder, strokeOrder);
				if (pc.compare(katakanaVersion, this.drawn, assetFinder.findGlyphForCharacter(katakanaSet, katakanaVersion), Recursion.DISALLOW).pass) {
					Criticism specific = new Criticism();
					specific.add("You drew the katakana " + katakanaVersion + " instead of the hiragana " + target + ".", Criticism.SKIP, Criticism.SKIP);
					return specific;
				}
			} else if (!c.pass && Kana.isKatakana(target) && target != 'モ') {
				Log.d("nakama", "Doing additional comparison between hiragana and katanama");
				char hiraganaVersion = Kana.katakana2Hiragana(String.valueOf(target)).charAt(0);
				SimpleDrawingComparator pc = new SimpleDrawingComparator(assetFinder, strokeOrder);
				if (pc.compare(hiraganaVersion, this.drawn, assetFinder.findGlyphForCharacter(hiraganaSet, hiraganaVersion), Recursion.DISALLOW).pass) {
					Criticism specific = new Criticism();
					specific.add("You drew the hiragana " + hiraganaVersion + " instead of the katakana " + target + ".", Criticism.SKIP, Criticism.SKIP);
					return specific;
				}
			}
		}

		return c;
	}

	static List<StrokeResult> findBestPairings(double[][] matrix){
        HungarianAlgorithm al = new HungarianAlgorithm(matrix);
        int[] matches = al.execute();
		Log.i("nakama", "Assignment results: " + Arrays.toString(matches));
        List<StrokeResult> l = new ArrayList<>(matches.length);
        for(int i = 0; i < matrix.length; i++){
			if(matches[i] < 0){
				// add failing stroke result?
			} else {
				l.add(new StrokeResult(i, matches[i], (int) matrix[i][matches[i]]));
			}
        }
        return l;
	}

	/**
	 * Compares one stroke to another, generating a list of criticisms.
	 */
	private StrokeCriticism compareStroke(int baseIndex, int challengerIndex){
		if(DEBUG) Log.d("nakama", "\n================================================================");
		if(DEBUG) Log.d("nakama", "Comparing base stroke " + baseIndex + " to challenger stroke " + challengerIndex);
		List<StrokeCompareFailure> failures = new LinkedList<StrokeCompareFailure>();

		Stroke bpath = this.known.get(baseIndex);
		Stroke cpath = this.drawn.get(challengerIndex);
		if(DEBUG) Log.d("nakama", String.format("%30s:  Base: %6d Drawn: %6d", "Number of points", bpath.pointSize(), cpath.pointSize()));

        if(DEBUG) Log.d("nakama", String.format("Base points: " + Util.join(", ", bpath.points)));
        if(DEBUG) Log.d("nakama", String.format("Drawn points: " + Util.join(", ", cpath.points)));

		Point bstart = bpath.startPoint;
		Point bend = bpath.endPoint;
		Point cstart = cpath.startPoint;
		Point cend = cpath.endPoint;
		
		if(DEBUG) Log.d("nakama", String.format("%30s:  Base[0]: %s Drawn[0]: %s", "Start points", bstart, cstart));
		if(DEBUG) Log.d("nakama", String.format("%30s:  Base[-1]: %s Drawn[-1]: %s", "End points", bend, cend));
		
		final double bStartRadians = bpath.startDirection();
		final double cStartRadians = cpath.startDirection();

		final double bEndRadians = bpath.endDirection();
		final double cEndRadians = cpath.endDirection();

		if(DEBUG) Log.d("nakama", "Base stroke " + baseIndex + " has points: " + Util.join(", ", bpath.points));

		{
			double startDistance = PathCalculator.distance(bstart, cstart);
			if(startDistance > FAIL_POINT_START_DISTANCE){
				failures.add(StrokeCompareFailure.START_POINT_DIFFERENCE);
			}
			if(DEBUG) Log.d("nakama", String.format("%30s:  %6.2f from points known %6s, drawn %6s. Fail distance is %6.2f.", "Start Point Difference", startDistance, bstart, cstart, FAIL_POINT_START_DISTANCE));
		}
		
		// end points should be close to each other.
		{
			double endDistance = PathCalculator.distance(bend, cend);
			if(endDistance > FAIL_POINT_END_DISTANCE){
				failures.add(StrokeCompareFailure.END_POINT_DIFFERENCE);
			}
			if(DEBUG) Log.d("nakama", String.format("%30s:  %6.2f from points known %6s, drawn %6s. Fail distance is %6.2f.", "End Point Difference", endDistance, bend, cend, FAIL_POINT_END_DISTANCE));
		}

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
		if(DEBUG) Log.d("nakama", "========================== end of " + baseIndex + " vs " + challengerIndex);

		if(failures.size() == 0) {
			return new StrokeCriticism(null, 0);
			
		} else if(failures.size() == 1) {
			StrokeCompareFailure f = failures.get(0);
		
			switch (f) {
			case START_POINT_DIFFERENCE:
				return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke's starting point is off.");
			case END_POINT_DIFFERENCE:
				return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke's ending point is off.");
			case BACKWARDS:
				return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke is backwards.");

			default:
				throw new RuntimeException("Error: unhandled StrokeCompareFailure");
			}
			
		} else if(failures.size() == 2){
			return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke is not correct.", failures.size());
		}  else {
			return new StrokeCriticism("Your " + Util.adjectify(challengerIndex, drawn.strokeCount()) + " stroke is not correct.", failures.size());
		}
	}
}