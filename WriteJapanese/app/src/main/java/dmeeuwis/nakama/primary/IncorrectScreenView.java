package dmeeuwis.nakama.primary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;
import dmeeuwis.nakama.kanjidraw.Drawing;
import dmeeuwis.nakama.kanjidraw.Glyph;
import dmeeuwis.nakama.views.MeasureUtil;

public class IncorrectScreenView extends View {
	
	private Paint circlePaint;
	private int squareArea = 0;

	public IncorrectScreenView(Context context) {
		super(context);
		init();
	}
	
	public IncorrectScreenView(Context context, AttributeSet set) {
		super(context, set);
		init();
	}
	
	/**
	 * @param correct  
	 * @param incorrect  
	 * @param character  
	 */
	public void setInformation(Glyph correct, Drawing incorrect, char character){
		// throw new RuntimeException("Need implementation.");
	}
	
	private void init(){
		this.setBackgroundColor(0xFFa40000);
		this.setDrawingCacheEnabled(true);
		
		circlePaint = new Paint();
		circlePaint.setAntiAlias(true);
		circlePaint.setColor(0xffffffff);
		circlePaint.setStyle(Style.STROKE);
		circlePaint.setStrokeWidth(40);
	}
	
	@Override protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
		squareArea = MeasureUtil.squareMeasure(widthMeasureSpec, heightMeasureSpec);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	@Override protected void onDraw(Canvas canvas) {
		int xOffset = (this.getWidth() - squareArea) / 2;
		int yOffset = (this.getHeight() - squareArea) / 2;
		canvas.drawLine(
				xOffset + squareArea /5, yOffset + squareArea/5, 
				xOffset + (4*squareArea/5), yOffset + (4*squareArea/5), 
				circlePaint);
		
		canvas.drawLine(
				xOffset + (4*squareArea/5), yOffset + (squareArea/5), 
				xOffset + (squareArea/5), yOffset + (4*squareArea/5), 
				circlePaint);
	}
}
