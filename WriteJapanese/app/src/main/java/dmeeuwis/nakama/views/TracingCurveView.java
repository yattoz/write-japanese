package dmeeuwis.nakama.views;

import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.kanjidraw.PointDrawing;
import dmeeuwis.nakama.kanjidraw.CurveDrawing;
import dmeeuwis.nakama.views.AnimatedCurveView.DrawTime;
import dmeeuwis.nakama.views.DrawView.OnStrokeListener;
import dmeeuwis.nakama.views.MeasureUtil.WidthAndHeight;

public class TracingCurveView extends FrameLayout implements Animatable {
	private enum AnimationState { RUNNING, STOPPED }

	AnimationState animState = AnimationState.STOPPED;
	AnimatedCurveView animatedCurve;
	DrawView kanjiPad;
	CurveDrawing curveDrawing;
	
	Integer currentTracingTargetStrokeCount = null;
    Integer gridPaddingLeft = 0, gridPaddingTop = 0;
	
	OnTraceCompleteListener onTraceListener;
	OnStrokeListener onStrokeListener;

	public TracingCurveView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DrawView, defStyle, 0);
        this.gridPaddingTop = a.getDimensionPixelSize(R.styleable.TracingCurveView_gridPaddingTop, 0);
        this.gridPaddingLeft = a.getDimensionPixelSize(R.styleable.TracingCurveView_gridPaddingLeft, 0);
        a.recycle();

        this.animatedCurve = new AnimatedCurveView(context);
        this.animatedCurve.setCurveColor(Color.LTGRAY);
        this.animatedCurve.setAutoIncrement(false);
        this.animatedCurve.setCurvePaddingPixels(gridPaddingTop, gridPaddingLeft);
        this.animatedCurve.setBackgroundColor(DrawView.BACKGROUND_COLOR);

        this.kanjiPad = new DrawView(context);
        this.kanjiPad.setBackgroundColor(0x00FFFFFF);
        this.kanjiPad.setGridPadding(gridPaddingTop, gridPaddingLeft);
        this.kanjiPad.addOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    animatedCurve.incrementCurveStroke();
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

                    PointDrawing drawn = TracingCurveView.this.kanjiPad.getDrawing();

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

	public TracingCurveView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TracingCurveView(Context context) {
		this(context, null, 0);
	}

	public static interface OnTraceCompleteListener {
		public void onComplete(PointDrawing pointDrawing);
	}
	
	public void setOnTraceCompleteListener(OnTraceCompleteListener listener){
		this.onTraceListener = listener;
	}
	
	public void setOnStrokeListener(OnStrokeListener listener){
		this.onStrokeListener = listener;
	}
	
	public void setCurveDrawing(CurveDrawing curveDrawing){
		this.curveDrawing = curveDrawing;
		this.animatedCurve.setDrawing(curveDrawing, DrawTime.ANIMATED);
		this.currentTracingTargetStrokeCount = curveDrawing.strokeCount();
	}
	
	public void startAnimation(int delay){
		this.animatedCurve.startAnimation(delay);
	}

	public void clear(){
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
