
package dmeeuwis.nakama.views;

import android.graphics.RectF;
import android.util.Log;
import android.view.View.MeasureSpec;

public class MeasureUtil {
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
	        	desiredWidth = 500;
                break;
	        case MeasureSpec.EXACTLY: 
	        case MeasureSpec.AT_MOST:
	        default:
	        	desiredWidth = MeasureSpec.getSize(widthMeasureSpec);
	    }

	    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
	    if(heightMode == MeasureSpec.UNSPECIFIED){
	       	desiredHeight = 500;
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
			if(unscaledBoundingBox == null){
				xOffset = 0;
				yOffset = 0;
				scale = 1;
			} else {
                final double usableWidth = width * 0.85;
                final double usableHeight = height * 0.85;

				this.scale = (float) Math.min(usableWidth / unscaledBoundingBox.width(), usableHeight / unscaledBoundingBox.height());
				
				final float scaledHeight = unscaledBoundingBox.height() * scale;
				final float scaledWidth = unscaledBoundingBox.width() * scale;

				this.xOffset = (width - scaledWidth) / 2 - unscaledBoundingBox.left * scale;
				this.yOffset = (height - scaledHeight) / 2 - unscaledBoundingBox.top * scale;
			}
			initialized = true;
		}
		
		@Override public String toString(){
			return "Scale: " + this.scale + ", xOffset: " + xOffset + "; yOffset: " + this.yOffset;
		}
	}
}
