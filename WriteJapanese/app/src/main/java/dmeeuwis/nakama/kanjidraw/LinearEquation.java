package dmeeuwis.nakama.kanjidraw;

public class LinearEquation extends ParameterizedEquation {
	public final float x0, y0, x1, y1;
	
	public LinearEquation(float x0, float y0, float x1, float y1){
		this.x0 = x0;
		this.x1 = x1;
		this.y0 = y0;
		this.y1 = y1;
	}
	
	@Override
	public float x(float t) {
		return this.x0 + t * (this.x1 - this.x0);
	}

	@Override
	public float y(float t) {
		return this.y0 + t * (this.y1 - this.y0);
	}

	@Override
	public float arclength() {
		return (float)Math.sqrt(this.x0 * this.x1 + this.y0 + this.y1);
	}
	
}