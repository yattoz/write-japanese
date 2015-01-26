package dmeeuwis.nakama.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

public final class GridBackgroundDrawer {
    private final int gridPaddingTop, gridPaddingLeft;
	private final Paint gridPaint = new Paint();
	private final Path gridPath = new Path();

    public int width, height;

    public GridBackgroundDrawer(int gridPaddingTop, int gridPaddingLeft) {
        init();
        this.gridPaddingTop = gridPaddingTop;
        this.gridPaddingLeft = gridPaddingLeft;
    }

     private void init(){
		this.gridPaint.setColor(Color.LTGRAY);
		this.gridPaint.setStyle(Paint.Style.STROKE);
		this.gridPaint.setStrokeWidth(3);
		this.gridPaint.setPathEffect(new DashPathEffect(new float[] {20, 20}, 0));
	}

	final protected void measure (final int w, final int h){
		width = w;
		height = h;

		gridPath.reset();

        final float middleX = (width - gridPaddingLeft) / 2 + gridPaddingLeft;
        final float middleY = (height - gridPaddingTop) / 2 + gridPaddingTop;

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
		canvas.drawPath(gridPath, gridPaint);
	}
}