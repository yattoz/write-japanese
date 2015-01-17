package dmeeuwis.nakama.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

public final class GridBackgroundDrawer {
	public int width, height;
    private final int gridPaddingTop, gridPaddingLeft;
	private Paint gridPaint;
	private Path gridPath = new Path();
	
    public GridBackgroundDrawer(int gridPaddingTop, int gridPaddingLeft) {
        init();
        this.gridPaddingTop = gridPaddingTop;
        this.gridPaddingLeft = gridPaddingLeft;

        Log.i("nakama", "GridBackgroundDrawer: using gridPaddings: top=" + gridPaddingTop + ", left=" + gridPaddingLeft);
    }

     private void init(){
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

        final float middleX = width / 2 + gridPaddingLeft;
        final float middleY = height / 2 + gridPaddingTop;

		// y-axis: middle to top
		gridPath.moveTo(middleX, middleY);
		gridPath.lineTo(middleX, 0);

		// y-axis: middle to bottom
		gridPath.moveTo(middleX, middleY);
		gridPath.lineTo(middleX, height);
		
		// x-axis: middle to left
		gridPath.moveTo(middleX, middleY);
		gridPath.lineTo(0, middleY);
		
		// x-axis: middle to right
		gridPath.moveTo(middleX, middleY);
		gridPath.lineTo(width, middleY);
	}
	
	protected final void draw(Canvas canvas) {
		Log.i("nakama", "Grid.draw");
		canvas.drawPath(gridPath, gridPaint);
	}
}