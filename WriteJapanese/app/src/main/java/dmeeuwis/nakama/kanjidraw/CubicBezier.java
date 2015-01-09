package dmeeuwis.nakama.kanjidraw;

public class CubicBezier extends ParameterizedEquation {
	public final float x0, y0, cx1, cy1, cx2, cy2, x3, y3;

	public CubicBezier(float[] coords){
		if(coords.length != 8)
			throw new IllegalArgumentException("CubicBezier requires 8 coords; was given " + coords.length + " in constructor.");
		
		this.x0 = coords[0];
		this.y0 = coords[1];
		this.cx1 = coords[2];
		this.cy1 = coords[3];
		this.cx2 = coords[4];
		this.cy2 = coords[5];
		this.x3 = coords[6];
		this.y3 = coords[7];
		
		// System.out.println("Constructed Cubic Bezier: (" + x0 + ", " + y0 + ") CP1:(" + cx1 + ", " + cy1 +") CP2:(" + cx2 + ", " + cy2 + ") (" + x3 + ", " + y3 + ")");
	}
	
	public CubicBezier(float x0, float y0, float cx1, float cy1, float cx2, float cy2, float x3, float y3){
		this.x0 = x0;
		this.cx1 = cx1;
		this.cx2 = cx2;
		this.x3 = x3;
		this.y0 = y0;
		this.cy1 = cy1;
		this.cy2 = cy2;
		this.y3 = y3;
		
		// System.out.println("Constructed Cubic Bezier: (" + x0 + ", " + y0 + ") CP1:(" + cx1 + ", " + cy1 +") CP2:(" + cx2 + ", " + cy2 + ") (" + x3 + ", " + y3 + ")");
	}

	
	@Override
	public final float x(float t){
		return (1 - t)*(1 - t)*(1 - t)*x0 + 3*(1 - t)*(1 - t)*t*this.cx1 + 3*(1-t)*t*t*cx2 + t*t*t*x3;
	}
	
	@Override
	public final float y(float t){
		return (1 - t)*(1 - t)*(1 - t)*y0 + 3*(1 - t)*(1 - t)*t*this.cy1 + 3*(1-t)*t*t*cy2 + t*t*t*y3;
	}
	
	@Override
	public float arclength(){
		final int ARC_LENGTH_SEGMENTS = 10;
		float lx1 = x(0);
		float ly1 = y(0);
		float lx0, ly0;
		
		float length = 0;
		
		for(float t = (float) (1.0 / ARC_LENGTH_SEGMENTS); t <= 1; t += 1.0 / ARC_LENGTH_SEGMENTS){
			lx0 = lx1; 
			ly0 = ly1;
			lx1 = x(t);
			ly1 = y(t);
			
			double distance = Math.sqrt( (lx1 - lx0)*(lx1 - lx0) + (ly1 - ly0)*(ly1 - ly0) );
			length += distance;
		}
		
		return length;
	}
	
}