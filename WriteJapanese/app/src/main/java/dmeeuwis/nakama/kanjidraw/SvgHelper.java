package dmeeuwis.nakama.kanjidraw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.graphics.Path;
import android.util.Log;

public class SvgHelper {
	
	public float scale;
	private float penXAbsolute = 0, penYAbsolute = 0;
	
    private final static List<Character> SHORTHAND_OPS = Arrays.asList(new Character[]{ 'S', 's', 'C', 'c'});
	
	public SvgHelper(){
		this.scale = 1.0f;
	}

  	public List<ParameterizedEquation> readSvgEquations(String[] in){
  		if(in == null)
  			return new ArrayList<>(0);
  		
		penXAbsolute = 0;
		penYAbsolute = 0;

		List<ParameterizedEquation> ret = new ArrayList<ParameterizedEquation>();
		for(String i: in){
			AbsoluteCubicBezierProcesser collector = new AbsoluteCubicBezierProcesser();
			processSvgEquation(i, collector);
			ret.add(new Spline(collector.eqns));
		}
		return ret;
	}

  	
  	private static interface Processor {
    	public void process(char c, float[] coords);
  	}

    private static class AbsoluteCubicBezierProcesser implements Processor {
    	final public List<ParameterizedEquation> eqns;
    	
    	public AbsoluteCubicBezierProcesser(){
    		eqns = new ArrayList<ParameterizedEquation>();
    	}
    	
    	public void process(char c, float[] coords){
    		if(c != 'M' && c != 'm')
	    		eqns.add(new CubicBezier(coords));
    	}
    }
    
    private void processSvgEquation(String in, Processor processer){
    	float[] previousCoords = {};
    	Character previousOp = null;
    	
    	for(int i = 0; i < in.length(); i++){
    		char c = in.charAt(i);
    		
    		if(c == 'M'){
    			SvgReadData move = readSvgMove(in, i);
    			i += move.string.length() - 1;
    			
    			float coords[] = new float[] { move.coords[0], move.coords[1] };
    			processer.process(c, coords);
    			
    			previousOp = 'M';
    			previousCoords = coords;
    			
    			penXAbsolute = move.coords[0];
    			penYAbsolute = move.coords[1];
    			
    		} else if(c == 'c'){
    			SvgReadData curve = readAndScaleSvgCurve(in, i);
    			//System.out.println("SVG-c: " + curve.toString());
    			i += curve.string.length() - 1;

    			float coords[] = new float[] { 
					penXAbsolute, penYAbsolute,
					curve.coords[0] + penXAbsolute, curve.coords[1] + penYAbsolute, 
					curve.coords[2] + penXAbsolute, curve.coords[3] + penYAbsolute,
					curve.coords[4] + penXAbsolute, curve.coords[5] + penYAbsolute
    			};
    			processer.process(c, coords);
    			
    			penXAbsolute += curve.coords[4];
    			penYAbsolute += curve.coords[5];
    			
    			previousOp = 'c';
    			previousCoords = coords;
    			
    		} else if(c == 'C'){
    			SvgReadData curve = readAndScaleSvgCurve(in, i);
    			//System.out.println("SVG-C: " + curve.toString());
    			i += curve.string.length() - 1;

    			float[] coords = new float[] { 
    						penXAbsolute, penYAbsolute,
    						curve.coords[0], curve.coords[1], 
    						curve.coords[2], curve.coords[3],
    						curve.coords[4], curve.coords[5]
    			};
    			processer.process(c, coords);
    					
    			previousOp = 'C';
    			previousCoords = coords;
    			
    			penXAbsolute = curve.coords[4];
    			penYAbsolute = curve.coords[5];
    			
    		/*
    		 * Draws a cubic Bézier curve from the current point to (x,y). The first control point is assumed to be the reflection 
    		 * of the second control point on the previous command relative to the current point. (If there is no previous command 
    		 * or if the previous command was not an C, c, S or s, assume the first control point is coincident with the current 
    		 * point.) (x2,y2) is the second control point (i.e., the control point at the end of the curve). S (uppercase) 
    		 * indicates that absolute coordinates will follow.
    		 */
    		} else if(c == 'S'){
    			SvgReadData curve = readSvgShortCurve(in, i);
    			//System.out.println("SVG-" + c + ": " + curve.toString());
    			i += curve.string.length() - 1;

    			// calculate first 'shorthand' control point
    			float cp1_x, cp1_y;
//    			if(!SHORTHAND_OPS.contains(previousOp)){
    				cp1_x = curve.coords[0];
    				cp1_y = curve.coords[1];
//    			} else {
    				//System.out.println("Constructing shorthand from previous CP3 coords: " + previousCoords[previousCoords.length - 4] + ", " + previousCoords[previousCoords.length - 3]);
//	    			cp1_x = 2 * curve.coords[0] - previousCoords[previousCoords.length - 4];
//	    			cp1_y = 2 * curve.coords[1] - previousCoords[previousCoords.length - 3];
 //   			}
    						
    			float[] coords = new float[] { 
    						penXAbsolute, penYAbsolute,
    						cp1_x, cp1_y,
    						curve.coords[0], curve.coords[1], 
    						curve.coords[2], curve.coords[3]
    			};
    			processer.process(c, coords);
    			
    			previousOp = 'S';
    			previousCoords = coords;

    			penXAbsolute = curve.coords[2];
    			penYAbsolute = curve.coords[3];

    		/*
    		 * Draws a cubic Bézier curve from the current point to (x,y). The first control point is assumed to be the reflection 
    		 * of the second control point on the previous command relative to the current point. (If there is no previous command 
    		 * or if the previous command was not an C, c, S or s, assume the first control point is coincident with the current 
    		 * point.) (x2,y2) is the second control point (i.e., the control point at the end of the curve). s (lowercase) indicates 
    		 * that relative coordinates will follow. 
    		 */
    		} else if(c == 's'){
    			SvgReadData curve = readSvgShortCurve(in, i);
    			//System.out.println("SVG-" + c + ": " + curve.toString());
    			i += curve.string.length() - 1;

    			float cp1_x, cp1_y;
    			if(!SHORTHAND_OPS.contains(previousOp) || previousCoords.length < 5){
    				//System.out.println("Cons: degenerate s-curve.");
    				cp1_x = (curve.coords[0] + penXAbsolute);
    				cp1_y = (curve.coords[1] + penYAbsolute);
    			} else {
    				float prev_cp3_x = previousCoords[previousCoords.length - 4];
    				float prev_cp3_y = previousCoords[previousCoords.length - 3];
    				//System.out.println("Cons: s-curve CP1: CP1 = 2*(" + penXAbsolute + ", " + penYAbsolute + ") - (" + prev_cp3_x + ", " + prev_cp3_y + ")");
    				//System.out.println("Previous coords: " + Util.join(", ", previousCoords));
	    			cp1_x = 2*penXAbsolute - prev_cp3_x;
	    			cp1_y = 2*penYAbsolute - prev_cp3_y;
    			}
    		
    			float[] coords = new float[] { 
    						penXAbsolute, penYAbsolute,
    						cp1_x, cp1_y,
    						curve.coords[0] + penXAbsolute, curve.coords[1] + penYAbsolute, 
    						curve.coords[2] + penXAbsolute, curve.coords[3] + penYAbsolute
    			};
    			processer.process(c, coords);
    			
    			previousOp = 's';
    			previousCoords = coords;

    			penXAbsolute += curve.coords[2];
    			penYAbsolute += curve.coords[3];
    			
    		} else {
    			Log.e("nakama", "Skipping past unknown SVG symbol in path: " + c + " in string " + in);
    		}
    	}
    }

    private SvgReadData readSvgMove(String in, int start){
    	int index = start + 1;

    	String xs = readSvgNumber(in, index);
    	index += xs.length();
    	
    	String conn = readSvgConnector(in, index);
    	index += conn.length();
    	
    	String ys = readSvgNumber(in, index);
    	index += ys.length();
    	
    	float[] coords = { Float.parseFloat(xs) * this.scale, Float.parseFloat(ys) * this.scale };
    	return new SvgReadData(in.substring(start, index), in.charAt(start), coords);
    }

    private SvgReadData readSvgShortCurve(String in, int start){
    	int index = start + 1;
    	String[] coords = new String[4];
    	
    	for(int i = 0; i < 2; i++){
	    	String x = readSvgNumber(in, index);
	    	index += x.length();
	    	
	    	String conn = readSvgConnector(in, index);
	    	index += conn.length();
	    	
	    	String y = readSvgNumber(in, index);
	    	index += y.length();
	    	
	    	coords[i*2]   = x;
	    	coords[i*2+1] = y;
	    	
	    	if(i != 1){
		    	String conn2 = readSvgConnector(in, index);
		    	index += conn2.length();
	    	}
    	}
    	
    	float[] pcoords = { 
    			Float.parseFloat(coords[0]) * this.scale, Float.parseFloat(coords[1]) * this.scale,
    			Float.parseFloat(coords[2]) * this.scale, Float.parseFloat(coords[3]) * this.scale
    	};
    	return new SvgReadData(in.substring(start, index), in.charAt(start), pcoords);
    }
    
    
    private SvgReadData readAndScaleSvgCurve(String in, int start){
    	int index = start + 1;
    	String[] coords = new String[6];
    	
    	for(int i = 0; i < 3; i++){
	    	String x = readSvgNumber(in, index);
	    	index += x.length();
	    	
	    	String conn = readSvgConnector(in, index);
	    	index += conn.length();
	    	
	    	String y = readSvgNumber(in, index);
	    	index += y.length();
	    	
	    	coords[i*2]   = x;
	    	coords[i*2+1] = y;
	    	
	    	if(i != 2){
		    	String conn2 = readSvgConnector(in, index);
		    	index += conn2.length();
	    	}
    	}
    	
    	float[] pcoords = { 
    			Float.parseFloat(coords[0]) * this.scale, Float.parseFloat(coords[1]) * this.scale,
    			Float.parseFloat(coords[2]) * this.scale, Float.parseFloat(coords[3]) * this.scale,
    			Float.parseFloat(coords[4]) * this.scale, Float.parseFloat(coords[5]) * this.scale
    	};
    	return new SvgReadData(in.substring(start, index), in.charAt(start), pcoords);
    }

    
    private static String readSvgNumber(String in, int start){
    	int end = start;
    	for(; end < in.length(); end++){
    		boolean negNumberIndicator = (end == start && in.charAt(start) == '-');
    		if(!negNumberIndicator && !in.substring(end, end+1).matches("[0123456789.]")){
    			break;
    		}
    	}
    	return in.substring(start, end);
    }
    
    private static String readSvgConnector(String in, int start){
    	int end = start;
    	for(; end < in.length(); end++){
    		if(!in.substring(end, end+1).matches("[ ,]")){
    			break;
    		}
    	}
    	return in.substring(start, end);
    }

    
    private static class SvgReadData {
    	final String string;
    	final char command;
    	final float[] coords;
    	
    	public SvgReadData(String string, char command, float[] coords){
    		this.string = string;
    		this.command = command;
    		this.coords = coords;
    	}
    	
    	@Override
		public String toString(){
    		StringBuilder sb = new StringBuilder();
    		sb.append(this.command);
    		sb.append(": ");
    		for(int i = 0; i < this.coords.length; i++){
    			sb.append(this.coords[i]);
    			if(i != this.coords.length-1) sb.append(", ");
    		}
    		return sb.toString();
    	}
    }
}
