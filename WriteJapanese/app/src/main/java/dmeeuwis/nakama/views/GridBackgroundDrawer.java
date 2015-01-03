package dmeeuwis.nakama.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

public final class GridBackgroundDrawer {
	public int width, height;
	private Paint gridPaint;
	private Path gridPath = new Path();
	
	public GridBackgroundDrawer(){
		this.gridPaint = new Paint();
		this.gridPaint.setColor(Color.LTGRAY);
		this.gridPaint.setStyle(Paint.Style.STROKE);
		this.gridPaint.setStrokeWidth(3);
		this.gridPaint.setPathEffect(new DashPathEffect(new float[] {20, 20}, 0));
	}

	final protected void measure (final int w, final int h){
		width = w;
		height = h;

		gridPath.reset();
		
		// y-axis: middle to top
		gridPath.moveTo(width/2, height/2);
		gridPath.lineTo(width/2, 0);
		
		// y-axis: middle to bottom
		gridPath.moveTo(width/2, height/2);
		gridPath.lineTo(width/2, height);
		
		// x-axis: middle to left
		gridPath.moveTo(width/2, height/2);
		gridPath.lineTo(0, height/2);
		
		// x-axis: middle to right
		gridPath.moveTo(width/2, height/2);
		gridPath.lineTo(width, height/2);
		
		Log.i("nakama", "Grid.measure: " + width + "," + height);
	}
	
	protected final void draw(Canvas canvas) {
		Log.i("nakama", "Grid.draw");
		canvas.drawPath(gridPath, gridPaint);
	}
}