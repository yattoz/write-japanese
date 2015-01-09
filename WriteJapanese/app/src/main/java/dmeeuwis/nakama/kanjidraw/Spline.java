package dmeeuwis.nakama.kanjidraw;

import java.util.List;

public class Spline extends ParameterizedEquation {
	public final List<ParameterizedEquation> eqns;
	private final float[] shares;		// % of total arc length each eqn takes. Sums to 1.
	private final float[] lengths;		// arclength of each eqns.
	private final float totalLength;	// summed arclength of all eqns.
	
	public Spline(List<ParameterizedEquation> eqns){
		this.eqns = eqns;
		this.shares = new float[eqns.size()];
		this.lengths = new float[eqns.size()];
	
		float total = 0f;
		for(int i = 0; i < eqns.size(); i++){
			float arcLength = eqns.get(i).arclength();
			this.lengths[i] = arcLength;
    		total += arcLength;
		}
		this.totalLength = total;

		for(int i = 0; i < eqns.size(); i++){
			shares[i] = this.lengths[i] / this.totalLength;
		}
	}

	@Override
	public final float x(float t) {
		// find correct eqn
		float pos = 0;
		int share_i = this.eqns.size()-1;
		float relative_t = t;
		for(int i = 0; i < this.shares.length; i++){
			if(pos + this.shares[i] >= t){
				share_i = i;
				break;
			}
			pos += this.shares[i];
			relative_t -= this.shares[i];
		}
	
		float scaled_relative_t = (float) (relative_t * (1.0 / this.shares[share_i]));
		return this.eqns.get(share_i).x(scaled_relative_t);
	}

	@Override
	public final float y(float t) {
		// find correct eqn
		float pos = 0;
		int share_i = this.eqns.size()-1;
		float relative_t = t;
		for(int i = 0; i < this.shares.length; i++){
			if(pos + this.shares[i] >= t){
				share_i = i;
				break;
			}
			pos += this.shares[i];
			relative_t -= this.shares[i];
		}
		
		float scaled_relative_t = (float) (relative_t * (1.0 / this.shares[share_i]));
		return this.eqns.get(share_i).y(scaled_relative_t);
	}

	@Override
	public float arclength() {
		return this.totalLength;
	}
}