package dmeeuwis.nakama.kanjidraw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.graphics.Point;
import android.util.Log;
import dmeeuwis.nakama.kanjidraw.PathCalculator;
import dmeeuwis.util.Util;

public abstract class ParameterizedEquation {
	public abstract float x(float t);
	public abstract float y(float t);
	public abstract float arclength();

	private final float CURVE_THRESHOLD = (float) (Math.PI / 12);
	private final float INCREMENT = 0.05f;
	
	public List<Point> toPoints(){
		final List<Float> tPoints = new ArrayList<Float>();
		final List<Double> tWeights = new ArrayList<Double>();
		
		double dir;
		double prevDir = PathCalculator.angle(x(0), y(0), x(INCREMENT), y(INCREMENT));
		
		// scan finely through and identify sharp turns
		for(float t = INCREMENT; t < 1.0f - INCREMENT; t += INCREMENT){
			dir = PathCalculator.angle(x(t), y(t), x(t+INCREMENT), y(t+INCREMENT));
			double diff = Math.abs(dir - prevDir);
			if(diff > CURVE_THRESHOLD){
				tWeights.add(diff);
				tPoints.add(t);	// track diff values, take top n changes 
			}
			prevDir = dir;
		}

		final Integer[] indexes = Util.makeIndexArray(tPoints.size());
		Arrays.sort(indexes, new Comparator<Integer>(){
			@Override public int compare(Integer lhs, Integer rhs) {
				return Double.compare(tWeights.get(lhs), tWeights.get(rhs));
			}
		});
		
		// take the (at most) 10 heaviest
		int pointsCount = Math.min(10, indexes.length);
		float[] heavyTs = new float[pointsCount];
		for(int i = 0; i < pointsCount; i++){
			heavyTs[i] = tPoints.get(i);
		}
		Arrays.sort(heavyTs);
		
		List<Point> points = new ArrayList<>();
		for(int i = 0; i < heavyTs.length; i++){
			float t = heavyTs[i];
			points.add(new Point( (int)x(t), (int)y(t)));
		}

        for(float t = 0; t <= 1.01f; t += INCREMENT){
            Log.d("nakama", String.format("Printing f(t): x(%.2f) = %.2f; y(%.2f) = %.2f", t, x(t), t, y(t)));
        }

		// first and last point always included
		points.add(0, new Point( (int)x(0.0f), (int)y(0.0f)));
		points.add(   new Point( (int)x(0.9999f), (int)y(0.9999f))); // TODO: fix error in param eqns where xy(1.0) was going way off from xy(0.999)
		
		Log.d("nakama", "Discretized ParameterizedEquation into " + points.size() + " points: " + Util.join(", ", points));
		return points;
	}
}