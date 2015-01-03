
package dmeeuwis.nakama.views;

import static java.lang.Math.abs;
import dmeeuwis.nakama.library.Constants;
import android.graphics.RectF;
import android.util.Log;
import android.view.View.MeasureSpec;

public class MeasureUtil {
	static final private float PADDING_PERCENTAGE = 0.10f;

	public static int squareMeasure(int widthMeasureSpec, int heightMeasureSpec){
		int wMode = MeasureSpec.getMode(widthMeasureSpec);
		int hMode = MeasureSpec.getMode(heightMeasureSpec);
		
		int requestedWidth = MeasureSpec.getSize(widthMeasureSpec);
		int requestedHeight = MeasureSpec.getSize(heightMeasureSpec);
		
		Integer decidedWidth = null;
		Integer decidedHeight = null;
		
		int maxWidth = Integer.MAX_VALUE;
		int maxHeight = Integer.MAX_VALUE;
		
		if(wMode == MeasureSpec.EXACTLY){
			// Log.d("nakama", "onMeasure has EXACTLY width mode: " + requestedWidth);
			decidedWidth = requestedWidth;
		} else if(wMode == MeasureSpec.AT_MOST){
			maxWidth = MeasureSpec.getSize(widthMeasureSpec);
			// Log.d("nakama", "onMeasure has AT_MOST width mode: " + maxWidth);
		}
		
		if(hMode == MeasureSpec.EXACTLY){
			// Log.d("nakama", "onMeasure has EXACTLY height mode: " + requestedWidth);
			decidedHeight = requestedHeight;
		} else {
			maxHeight = MeasureSpec.getSize(heightMeasureSpec);
			// Log.d("nakama", "onMeasure has AT_MOST height mode: " + maxHeight);
		}
		
		// if not decided (anything, or at most modes) for both
		if(decidedHeight == null && decidedWidth == null){
			decidedWidth = Math.min(maxWidth, Constants.KANJI_SVG_WIDTH);
			decidedHeight = Math.min(maxHeight, Constants.KANJI_SVG_WIDTH);
			// Log.d("nakama", "Neither width nor height set, using " + decidedWidth + "x" + decidedHeight);
		} else if(decidedWidth != null && decidedHeight == null){
			decidedHeight = Math.min(maxHeight, decidedWidth);
			// Log.d("nakama", "Width set, basing height on width, using " + decidedWidth + "x" + decidedHeight);
		} else if(decidedHeight != null && decidedWidth == null){
			decidedWidth = Math.min(maxWidth, decidedHeight);
			// Log.d("nakama", "Height set, basing width on height, using " + decidedWidth + "x" + decidedHeight);
		} // else  both decidedHeight and Width are non-null, so nothing to do.
			
		if(decidedHeight == null || decidedWidth == null)
			throw new NullPointerException("Null decidedHeight or width; should have been impossible.");

		// force a square drawing area, no matter what
		decidedHeight = decidedWidth = Math.min(decidedWidth, decidedHeight);
		// Log.d("nakama", "Decided on width: " + decidedWidth);

       return decidedHeight;
	}

	public static class WidthAndHeight {
		final public int width;
		final public int height;
		
		public WidthAndHeight(int w, int h){
			this.width = w;
			this.height = h;
		}
	}
	
	public static WidthAndHeight fillMeasure(int widthMeasureSpec, int heightMeasureSpec){
	    int desiredWidth, desiredHeight;
	    
	    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
	    switch(widthMode){
	        case MeasureSpec.UNSPECIFIED:
	        	desiredWidth = 500;	// never used
	        case MeasureSpec.EXACTLY: 
	        case MeasureSpec.AT_MOST:
	        default:
	        	desiredWidth = MeasureSpec.getSize(widthMeasureSpec);
	    }

	    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
	    if(heightMode == MeasureSpec.UNSPECIFIED){
	       	desiredHeight = 500;	// never used
	    } else if(heightMode == MeasureSpec.EXACTLY){
	       	desiredHeight = MeasureSpec.getSize(heightMeasureSpec);
	    } else if(heightMode == MeasureSpec.AT_MOST){
	       	desiredHeight = MeasureSpec.getSize(heightMeasureSpec);
	    } else {
	       	desiredHeight = MeasureSpec.getSize(heightMeasureSpec);
	    }
	    
	    return new WidthAndHeight(desiredWidth, desiredHeight);
	}

	public static class ScaleAndOffsets {
		public float scale = 0f, xOffset = 0f, yOffset = 0f;
		public boolean initialized = false;

		public void calculate(RectF unscaledBoundingBox, int width, int height){
			// don't want character to go exactly edge to edge, so add some padding.
			float xEdgeBuffer = width * PADDING_PERCENTAGE;
			float yEdgeBuffer = height * PADDING_PERCENTAGE;
			
			if(unscaledBoundingBox == null){
				xOffset = 0;
				yOffset = 0;
				scale = 1;
			} else {
				float unscaledHeight = abs(unscaledBoundingBox.bottom - unscaledBoundingBox.top);
				float unscaledWidth =  abs(unscaledBoundingBox.right - unscaledBoundingBox.left);
			
				float xscale = (width - 2*xEdgeBuffer) / Math.max(unscaledWidth, unscaledHeight);
				float yscale = (height - 2*yEdgeBuffer) / Math.max(unscaledHeight, unscaledWidth);
				scale = Math.min(xscale, yscale);
				
				float scaledHeight = unscaledHeight * scale;
				float scaledWidth = unscaledWidth * scale;
			
				Log.i("nakama", "Calculating scale: width: " + width + "; height " + height);
				Log.i("nakama", "Calculating scale: unscaledWidth: " + unscaledWidth + "; unscaledHeight: " + unscaledHeight);
				Log.i("nakama", "Calculating scale: scale: " + scale + "; scaledWidth: " + scaledWidth + "; scaledHeight: " + scaledHeight);
				Log.i("nakama", "Calculating scale: unscaledBoundingBox.left: " + unscaledBoundingBox.left + "; scaled to " + unscaledBoundingBox.left * scale);
				Log.i("nakama", "Calculating scale: unscaledBoundingBox.top: " + unscaledBoundingBox.top + "; scaled to " + unscaledBoundingBox.top * scale);
				
				xOffset = (width - scaledWidth) / 2 - unscaledBoundingBox.left * scale;
				yOffset = (height - scaledHeight) / 2 - unscaledBoundingBox.top * scale;
				Log.i("nakama", "Calculating scale: xOffset: " + xOffset + "; yOffset: " + yOffset);
			}
			Log.i("nakama", "Calculated ScaleAndOffsets to " + this);
			initialized = true;
		}
		
		@Override public String toString(){
			return "Scale: " + this.scale + ", xOffset: " + xOffset + "; yOffset: " + this.yOffset;
		}
	}
}
