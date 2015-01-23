package dmeeuwis.nakama.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.kanjidraw.Drawing;
import dmeeuwis.nakama.kanjidraw.ParameterizedEquation;
import dmeeuwis.nakama.views.MeasureUtil.ScaleAndOffsets;
import dmeeuwis.nakama.views.MeasureUtil.WidthAndHeight;

public class AnimatedCurveView extends View implements Animatable {
    public static enum DrawTime { ANIMATED, STATIC }
    public static enum DrawStatus { DRAWING, FINISHED }

	static final private float FRAME_RATE_PER_SEC = 60;
    static final private float FRAMES_PER_STROKE = 60;
	static final private float T_INCREMENTS = 1 / FRAMES_PER_STROKE;

	final Paint paint = new Paint();
    final Paint debugPaint = new Paint();
	final ScaleAndOffsets scaleAndOffsets = new ScaleAndOffsets();
	List<Path> pathsToDraw = new ArrayList<>();
	List<ParameterizedEquation> eqns = new LinkedList<>();

	int eqn_i = 0;
	int allowedStrokes = 1;
	float time = -1;
	boolean autoIncrement = true;
    int paddingTop = 0, paddingLeft = 0;

	Drawing drawing = null;
	RectF unscaledBoundingBox = null;

    Timer animateTimer = null;
	Runnable onAnimationFinishCallback = null;
	
	Paint bufferPaint = new Paint();

	DrawTime drawTime = DrawTime.ANIMATED;


    public AnimatedCurveView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);

        this.setBackgroundColor(Color.WHITE);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AnimatedCurveView, defStyle, 0);
        this.paddingLeft = a.getDimensionPixelSize(R.styleable.AnimatedCurveView_gridPaddingLeft, 0);
        this.paddingTop = a.getDimensionPixelSize(R.styleable.AnimatedCurveView_gridPaddingTop, 0);
        a.recycle();
        Log.i("nakama", "AnimatedCurveView: grid settings are: " + this.paddingLeft + ", " + this.paddingTop);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Join.ROUND);
        paint.setStrokeCap(Cap.ROUND);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeWidth(6);
        paint.setColor(Color.BLACK);

        debugPaint.setStyle(Paint.Style.STROKE);
        debugPaint.setColor(Color.RED);
        debugPaint.setStrokeWidth(5);

        bufferPaint.setStyle(Style.STROKE);
        bufferPaint.setStrokeWidth(2);
        bufferPaint.setColor(Color.LTGRAY);
    }

	public AnimatedCurveView(Context context, AttributeSet as){
		this(context, as, 0);
	}
	
	public AnimatedCurveView(Context context){
		this(context, null);
	}

	public void setCurveColor(int color){
		this.paint.setColor(color);
	}

	/**
	 * Clears current registered strokes.
	 */
	public void clear(){
        Log.i("nakama", "CLEAR!");
        pathsToDraw = new ArrayList<>();
        time = 0;
        eqn_i = 0;
        time = -1;
        scaleAndOffsets.initialized = false; // will force recalculation of scale and offsets.
        stopAnimation();
        invalidate();
	}

    public void setCurvePaddingPixels(int paddingTop, int paddingLeft){
        this.paddingTop = paddingTop;
        this.paddingLeft = paddingLeft;
        this.invalidate();
        Log.d("nakama", "AnimatedCurveView: setting padding (grid) to top=" + this.paddingTop + ", left=" + this.paddingLeft);
    }

	/**
	 * This controls whether the animation will automatically go through all strokes. If set to true,
	 * all strokes will be played back. If false, one stroke will be played on startAnimation, and one
	 * more after each call to incrementCurveStroke().
	 */
	public void setAutoIncrement(boolean val){
        Log.i("nakama", "AnimatedCurveView: setAutoIncrement=" + val);
        this.autoIncrement = val;
	}
	
	/**
	 * When not in autoincrement mode, this will increase the number of strokes being animated by one.
	 */
	public int incrementCurveStroke(){
        Log.i("nakama", "AnimatedCurveView: incrementCurveStroke");
        return (this.allowedStrokes++);
	}

    /**
     * Clears current strokes, and registers a new set from point lists.
     */
    public void setDrawing(final Drawing drawing, final DrawTime drawTimeParam){
        if(drawing == null){ throw new IllegalArgumentException("Cannot accept null Drawing"); }
        clear();

        this.unscaledBoundingBox = new RectF(drawing.findBoundingBox());
        this.eqns = drawing.toParameterizedEquations(1);

        this.drawTime = drawTimeParam;
        this.drawing = drawing;

        this.allowedStrokes = 1;

        invalidate();
    }

	@Override
	public boolean onTouchEvent(MotionEvent event){
		super.onTouchEvent(event);
		if(drawTime == DrawTime.STATIC)
			return false;
		
		stopAnimation();
		clear();
		startAnimation(0);
		
		return true;
	}

	DrawStatus threadDrawStatus = DrawStatus.FINISHED;
    /*
     * Version of drawIncrement() that can be called from other-than-UI-thread.
     */
	private void drawIncrementSafe(){
		Runnable setWork = new Runnable() {
			@Override public void run() {
				threadDrawStatus = drawIncrement();
			}
		};
		if(Looper.getMainLooper() == Looper.myLooper()){
			setWork.run();
		} else{
			post(setWork);
		}
	}
	
	private DrawStatus drawIncrement(){
		List<Path> pathsToDrawRef = this.pathsToDraw;
		List<ParameterizedEquation> eqnsRef = this.eqns;
		if(time <= 1 && eqn_i < eqnsRef.size()){
	    	Path path = null;

	    	// initialize new path
	    	if(time <= 0){
	    		time = 0;
	    		path = new Path();

                float x = this.paddingLeft + scaleAndOffsets.scale * eqnsRef.get(eqn_i).x(time) + scaleAndOffsets.xOffset;
                float y = this.paddingTop + scaleAndOffsets.scale * eqnsRef.get(eqn_i).y(time) + scaleAndOffsets.yOffset;
	    		
		    	pathsToDrawRef.add(eqn_i, path);
		    	path.moveTo(x, y);
		    	
	    	// or continue continue drawing current path.
	    	} else {
                float x = this.paddingLeft + scaleAndOffsets.scale * eqnsRef.get(eqn_i).x(time) + scaleAndOffsets.xOffset;
                float y = this.paddingTop + scaleAndOffsets.scale * eqnsRef.get(eqn_i).y(time) + scaleAndOffsets.yOffset;

		    	if(eqn_i < pathsToDrawRef.size()){
		   			path = pathsToDrawRef.get(eqn_i);
		   		}
		    	if(path != null)
		    		path.lineTo(x, y);
	    	}
	    	
	    	time += T_INCREMENTS;
	    	
		} else {
			if(eqn_i >= eqnsRef.size()-1){
	    		if(onAnimationFinishCallback != null)
	    			onAnimationFinishCallback.run();
	    		return DrawStatus.FINISHED;
			}
			
			if((autoIncrement) || ((eqn_i+1) < allowedStrokes)){
				eqn_i++;
				time = -1;
				pathsToDrawRef.add(eqn_i, new Path());
			}
		}
		
		return DrawStatus.DRAWING;
	}
	
	/**
	 * Starts animating current strokes.
	 * @param delayFirst How long in ms to delay before starting animation.
	 */
	public void startAnimation(int delayFirst){
		stopAnimation();
		clear();
		
		threadDrawStatus = DrawStatus.DRAWING;
        TimerTask task = new TimerTask() {
			@Override
			public void run() {
				drawIncrementSafe();
				if(threadDrawStatus == DrawStatus.FINISHED){
					this.cancel();
				}
				postInvalidate();
			}
		};

        this.animateTimer = new Timer();
		animateTimer.scheduleAtFixedRate(task, delayFirst, (long)(1000.0 / FRAME_RATE_PER_SEC));
	}

	/**
	 * Stops current animation exactly where it is.
	 */
	public void stopAnimation(){
        if(this.animateTimer != null){
			this.animateTimer.cancel();
			this.animateTimer = null;
		}
	}
	
	@Override protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
	    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	    WidthAndHeight wh = MeasureUtil.fillMeasure(widthMeasureSpec, heightMeasureSpec);
	    setMeasuredDimension(wh.width, wh.height);
	}
	
	@Override
	protected void onDraw(Canvas canvas){
        // TODO: move this block out of onDraw. Maybe onMeasure? Figure out inits so this can't happen here
		if(!this.scaleAndOffsets.initialized){
          if(this.animateTimer != null){
				Log.i("nakama", "AnimatedCurveView: rescaled, resetting animateTimer.");
				stopAnimation();
				this.time = 0;
				this.eqn_i = 0;
				startAnimation(0);
			}

			time = 0;
			scaleAndOffsets.calculate(unscaledBoundingBox, getWidth() - this.paddingLeft, getHeight() - this.paddingTop);

            Log.i("nakama", String.format("unscaled: %.2f %.2f %.2f %.2f; scaled-offsets: x=%.2f y=%.2f",
                    unscaledBoundingBox.top, unscaledBoundingBox.right, unscaledBoundingBox.bottom, unscaledBoundingBox.left,
                    scaleAndOffsets.xOffset, scaleAndOffsets.yOffset));

			if(drawTime == DrawTime.STATIC){
				Log.i("nakama", "Pre-drawing STATIC AnimatedCurveView in onDraw");
				while(drawIncrement() != DrawStatus.FINISHED){ /* loop */ }
			}
			
		}
		
		// draw the paths
		for(Path eachPath: pathsToDraw){
	    	canvas.drawPath(eachPath, paint);
		}

/*        if(BuildConfig.DEBUG){
            if(unscaledBoundingBox != null){
                RectF scaledBox = new RectF(
                        unscaledBoundingBox.left * scaleAndOffsets.scale + scaleAndOffsets.xOffset,
                        unscaledBoundingBox.top * scaleAndOffsets.scale + scaleAndOffsets.yOffset,
                        unscaledBoundingBox.right * scaleAndOffsets.scale + scaleAndOffsets.xOffset,
                        unscaledBoundingBox.bottom * scaleAndOffsets.scale + scaleAndOffsets.yOffset
                );
                debugPaint.setColor(Color.CYAN);
                canvas.drawRect(scaledBox, debugPaint);
            }
        }
*/
	}
}