package dmeeuwis.nakama.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

public class SingleBarChart extends View {
	
	static private final int DEFAULT_WIDTH = 300;
	static private final int DEFAULT_HEIGHT = 100;
	static private final int PADDING = 5;
	
	private BarChartEntry[] entries;
	private Rect[] entryRects;
	private Rect[] entryBorderRects;
	private Paint[] entryPaints;
	private Paint[] entryBorderPaints;
	
	private Paint textPaint;
	
	public SingleBarChart(Context context) {
		super(context);
		init();
	}
	
	public SingleBarChart(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SingleBarChart(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	private void init(){
		textPaint = new Paint();
		Resources r = getResources();
		textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, r.getDisplayMetrics()));
		textPaint.setAntiAlias(true);
		textPaint.setColor(Color.WHITE);
	}


	public static class BarChartEntry {
		public final int percent;
		public final int borderColor, innerColor;
		public final String label;
		
		int drawLabelX, drawLabelY;
		boolean drawLabel;
		
		public BarChartEntry(int percent, int borderColor, int innerColor, String label){
			this.percent = percent;
			this.innerColor = innerColor;
			this.borderColor = borderColor;
			this.label = percent + "% " + label;
			
			drawLabel = false;
			drawLabelX = drawLabelY = 0;
		}
	}
	
	public void setPercents(BarChartEntry ... entries){
		this.entries = entries;
		
		this.entryRects = new Rect[entries.length];
		this.entryPaints = new Paint[entries.length];
		
		this.entryBorderRects = new Rect[entries.length];
		this.entryBorderPaints = new Paint[entries.length];
	
		for(int i = 0; i < this.entryPaints.length; i++){
			this.entryRects[i] = new Rect();
			this.entryBorderRects[i] = new Rect();
			
			this.entryPaints[i] = new Paint();
			this.entryPaints[i].setColor(this.entries[i].innerColor);
			this.entryPaints[i].setStyle(Style.FILL);
			
			this.entryBorderPaints[i] = new Paint();
			this.entryBorderPaints[i].setStyle(Style.FILL);
			this.entryBorderPaints[i].setColor(this.entries[i].borderColor);
		}

		this.requestLayout();
		this.invalidate();
	}

	
	Rect textBounds = new Rect(0, 0, 0, 0);
	@Override protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
		int wmode = MeasureSpec.getMode(widthMeasureSpec);
		int hmode = MeasureSpec.getMode(heightMeasureSpec);
		
		int wsize = MeasureSpec.getSize(widthMeasureSpec);
		int hsize = MeasureSpec.getSize(heightMeasureSpec);
		
		int decidedWidth, decidedHeight;
		
		if(wmode == MeasureSpec.EXACTLY){
			decidedWidth = wsize;
		} else {
			decidedWidth = Math.min(DEFAULT_WIDTH, wsize);
		}

		if(hmode == MeasureSpec.EXACTLY){ 
			decidedHeight = hsize;
		} else {
			decidedHeight = Math.min(DEFAULT_HEIGHT, hsize);
		}

		setMeasuredDimension(decidedWidth, decidedHeight);

		float total = 0;
		for(BarChartEntry e: entries)
			total += e.percent;

		int startX = 0;
		for(int i = 0; i < entries.length; i++){
			int width = (int)((entries[i].percent / total) * wsize);
			entryBorderRects[i].left = startX;
			entryBorderRects[i].right = startX + width;
			entryBorderRects[i].top = 0;
			entryBorderRects[i].bottom = decidedHeight;
			
			entryRects[i].left = startX + PADDING;
			entryRects[i].right = startX + width - PADDING;
			entryRects[i].top = PADDING;
			entryRects[i].bottom = decidedHeight - PADDING;
		
			textPaint.getTextBounds(entries[i].label, 0, entries[i].label.length(), textBounds);
			if(textBounds.width() <= width && textBounds.height() <= decidedHeight){
				entries[i].drawLabel = true;
				entries[i].drawLabelX = startX + (int)((width - textBounds.width()) / 2);
				entries[i].drawLabelY = (decidedHeight / 2) + (textBounds.height() / 2);
			} else {
				entries[i].drawLabel = false;
			}

			startX += width;
			//Log.d("nakama", "Bar " + i + " is border " + entryBorderRects[i] + " with inner " + entryRects[i] + "; " + entries[i].percent + " % => width of bar: " + width);
		}
	}
	
	@Override protected void onDraw(Canvas canvas){
		for(int i = 0; i < entries.length; i++){
			if(entryBorderRects[i].left != entryBorderRects[i].right){
				canvas.drawRect(entryBorderRects[i], entryBorderPaints[i]);
				canvas.drawRect(entryRects[i], entryPaints[i]);
				if(entries[i].drawLabel){
					canvas.drawText(entries[i].label, entries[i].drawLabelX, entries[i].drawLabelY, textPaint);
				}
			}
		}
		
	}
}