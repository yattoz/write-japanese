package dmeeuwis.nakama.kanjidraw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import dmeeuwis.nakama.data.Point;
import dmeeuwis.nakama.data.Rect;
import dmeeuwis.util.Util;

public abstract class ParameterizedEquation {
	public abstract float x(float t);
	public abstract float y(float t);
	public abstract float arclength();

	private static final float CURVE_THRESHOLD = (float) (Math.PI / 12);
	private static final float INCREMENT = 0.05f;

    public static float[] findHeavyTurns(ParameterizedEquation eqn){
        final List<Float> tPoints = new ArrayList<>();
        final List<Double> tWeights = new ArrayList<>();

        double dir;
        double prevDir = PathCalculator.angle(eqn.x(0), eqn.y(0), eqn.x(INCREMENT), eqn.y(INCREMENT));

        // scan finely through and identify sharp turns
        for(float t = INCREMENT; t < 1.0f - INCREMENT; t += INCREMENT){
            dir = PathCalculator.angle(eqn.x(t), eqn.y(t), eqn.x(t+INCREMENT), eqn.y(t+INCREMENT));
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
        return heavyTs;
    }

    public ParameterizedEquation scale(final float scale){
        return new ParameterizedEquation() {
            @Override public float x(float t) { return ParameterizedEquation.this.x(t) * scale; }
            @Override public float y(float t) { return ParameterizedEquation.this.y(t) * scale; }
            @Override public float arclength() { return ParameterizedEquation.this.arclength() * scale; }
        };
    }

    public Rect findBoundingBox(){
        Rect box = new Rect((int)x(0), (int)y(0), (int)x(0), (int)y(0));
        for(float t = INCREMENT; t <= 0.9999; t += INCREMENT){
            box.union((int)x(t), (int)y(t));
        }
        return box;
    }

	public List<Point> toPoints(){
        List<Point> points = new ArrayList<>();
        float[] heavyTs = findHeavyTurns(this);
        for(int i = 0; i < heavyTs.length; i++){
            float t = heavyTs[i];
            points.add(new Point( (int)x(t), (int)y(t)));
        }

		// first and last point always included
		points.add(0, new Point( (int)x(0.0f), (int)y(0.0f)));
		points.add(   new Point( (int)x(0.9999f), (int)y(0.9999f))); // TODO: fix error in param eqns where xy(1.0) was going way off from xy(0.999)
		
		//Log.d("nakama", "Discretized ParameterizedEquation into " + points.size() + " points: " + Util.join(", ", points));
		return points;
	}
}