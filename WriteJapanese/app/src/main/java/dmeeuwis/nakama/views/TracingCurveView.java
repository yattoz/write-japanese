package dmeeuwis.nakama.views;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import dmeeuwis.nakama.kanjidraw.Drawing;
import dmeeuwis.nakama.kanjidraw.Glyph;
import dmeeuwis.nakama.views.AnimatedCurveView.DrawTime;
import dmeeuwis.nakama.views.DrawView.OnStrokeListener;
import dmeeuwis.nakama.views.MeasureUtil.WidthAndHeight;

public class TracingCurveView extends FrameLayout implements Animatable {
	private enum AnimationState { RUNNING, STOPPED }

	AnimationState animState = AnimationState.STOPPED;
	AnimatedCurveView animatedCurve;
	DrawView kanjiPad;
	Glyph glyph;
	
	Integer currentTracingTargetStrokeCount = null;
	
	OnTraceCompleteListener onTraceListener;
	OnStrokeListener onStrokeListener;

	public TracingCurveView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	public TracingCurveView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public TracingCurveView(Context context) {
		super(context);
		init();
	}
	
	private void init(){
		Context context = this.getContext();
		
		this.animatedCurve = new AnimatedCurveView(context);
		this.animatedCurve.setCurveColor(Color.LTGRAY);
		this.animatedCurve.setAutoIncrement(false);
		this.animatedCurve.setBackgroundColor(DrawView.BACKGROUND_COLOR);

		this.kanjiPad = new DrawView(context);
		this.kanjiPad.setBackgroundColor(0x00FFFFFF);
		this.kanjiPad.addOnTouchListener(new OnTouchListener() {
			@Override public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_UP){
					int allowed = animatedCurve.incrementCurveStroke();
					Log.d("tracing", "Saw touch up event, incremented animated curve by 1 to " + allowed);
				}
				return false;
			}
		});
		
		this.kanjiPad.setOnStrokeListener(new OnStrokeListener() {
			@Override public void onStroke(List<Point> stroke) {
				if(TracingCurveView.this.onStrokeListener != null){
					TracingCurveView.this.onStrokeListener.onStroke(stroke);
				}
				if(TracingCurveView.this.currentTracingTargetStrokeCount != null && 
					TracingCurveView.this.currentTracingTargetStrokeCount == TracingCurveView.this.kanjiPad.getStrokeCount()){
					
						Drawing drawn = TracingCurveView.this.kanjiPad.getDrawing();
					
						if(onTraceListener != null)
							onTraceListener.onComplete(drawn);
						
						TracingCurveView.this.postDelayed(new Runnable(){
							@Override public void run() {
								if(animState == AnimationState.RUNNING){
									TracingCurveView.this.kanjiPad.clear();
									TracingCurveView.this.startAnimation(0);
								}
							}
						}, 
						1000);
				}
			}
		});
		
		this.addView(this.animatedCurve, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		this.addView(this.kanjiPad, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	}
	
	public static interface OnTraceCompleteListener {
		public void onComplete(Drawing drawing);
	}
	
	public void setOnTraceCompleteListener(OnTraceCompleteListener listener){
		this.onTraceListener = listener;
	}
	
	public void setOnStrokeListener(OnStrokeListener listener){
		this.onStrokeListener = listener;
	}
	
	public void setGlyph(Glyph glyph){
		Log.d("TracingCurveView", "Setting strokes to array length " + glyph.strokeCount());
		this.glyph = glyph;
		this.animatedCurve.setDrawing(glyph, DrawTime.ANIMATED);
		this.currentTracingTargetStrokeCount = glyph.strokeCount();
	}
	
	public void startAnimation(int delay){
		Log.d("TracingCurveView", "Starting stroke animation");
		this.animatedCurve.startAnimation(delay);
	}
	
	public void startAnimation(){
		Log.d("TracingCurveView", "Starting stroke animation");
		this.animState = AnimationState.RUNNING;
		this.animatedCurve.startAnimation(1000);
	}
	
	public void clear(){
		Log.i("nakama", "TracingCurveView.clear");
		this.animatedCurve.clear();
		this.kanjiPad.clear();
	}
	
	public void stopAnimation(){
		this.animState = AnimationState.STOPPED;
		this.animatedCurve.stopAnimation();
	}
	
	public void undo(){
		this.kanjiPad.undo();
	}
	
	public int drawnStrokeCount(){
		return this.kanjiPad.getStrokeCount();
	}
	
	@Override protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
	    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	    WidthAndHeight wh = MeasureUtil.fillMeasure(widthMeasureSpec, heightMeasureSpec);
	    setMeasuredDimension(wh.width, wh.height);
	    
		this.animatedCurve.measure(widthMeasureSpec, heightMeasureSpec);
		this.kanjiPad.measure(widthMeasureSpec, heightMeasureSpec);
	}
}
