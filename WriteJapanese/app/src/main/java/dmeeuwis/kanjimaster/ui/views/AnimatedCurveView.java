package dmeeuwis.kanjimaster.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.data.Rect;
import dmeeuwis.kanjimaster.logic.drawing.Criticism;
import dmeeuwis.kanjimaster.logic.drawing.Drawing;
import dmeeuwis.kanjimaster.logic.drawing.ParameterizedEquation;
import dmeeuwis.kanjimaster.logic.drawing.Stroke;
import dmeeuwis.kanjimaster.ui.views.MeasureUtil.ScaleAndOffsets;
import dmeeuwis.kanjimaster.ui.views.MeasureUtil.WidthAndHeight;

public class AnimatedCurveView extends View implements Animatable {
    private List<Criticism.PaintColourInstructions> knownPaintInstructions;

    public enum DrawTime { ANIMATED, STATIC }
    public enum DrawStatus { DRAWING, FINISHED }
    public enum PlayStatus { PLAYING, STOPPED }

    static final private float FRAMES_PER_STROKE = 30;
	static final private float T_INCREMENTS = 1 / FRAMES_PER_STROKE;

    static final private int PAINT_THICKNESS_DP = 3;

	final Paint paint = new Paint();
    final Paint debugPaint = new Paint();
	final ScaleAndOffsets scaleAndOffsets = new ScaleAndOffsets();
	List<Path> pathsToDraw = new ArrayList<>();
	List<ParameterizedEquation> eqns = new LinkedList<>();
    List<Double> strokeLengthMultipliers = new LinkedList<>();

	int eqn_i = 0;
	int allowedStrokes = 1;
	float time = -1;
	boolean autoIncrement = true;
    int paddingTop = 0, paddingRight = 0, paddingBottom = 0, paddingLeft = 0;

	Drawing drawing = null;
	Rect unscaledBoundingBox = null;

    Handler animateHandler;
    Runnable animationRunnable;

	Runnable onAnimationFinishCallback = null;
	
	Paint bufferPaint = new Paint();

	DrawTime drawTime = DrawTime.ANIMATED;
    PlayStatus playingState = PlayStatus.STOPPED;


    public AnimatedCurveView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);

        this.setBackgroundColor(Color.WHITE);
        this.animateHandler = new Handler();
        this.animationRunnable = new AnimationRunnable();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AnimatedCurveView, defStyle, 0);
        this.paddingLeft = a.getDimensionPixelSize(R.styleable.AnimatedCurveView_gridPaddingLeft, 0);
        this.paddingTop = a.getDimensionPixelSize(R.styleable.AnimatedCurveView_gridPaddingTop, 0);
        this.paddingRight = a.getDimensionPixelSize(R.styleable.AnimatedCurveView_gridPaddingRight, 0);
        this.paddingBottom = a.getDimensionPixelSize(R.styleable.AnimatedCurveView_gridPaddingBottom, 0);
        a.recycle();

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int paintThicknessPx = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, PAINT_THICKNESS_DP, displayMetrics);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Join.ROUND);
        paint.setStrokeCap(Cap.ROUND);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeWidth(paintThicknessPx);
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
        //Log.i("nakama", "AnimatedCurveView clear");
        pathsToDraw = new ArrayList<>();
        eqn_i = 0;
        time = -1;
        allowedStrokes = 1;
        stopAnimationInternal();
        invalidate();
	}

    public void setCurvePaddingPixels(int paddingTop, int paddingRight, int paddingBottom, int paddingLeft){
        this.paddingTop = paddingTop;
        this.paddingLeft = paddingLeft;
        this.paddingBottom = paddingBottom;
        this.paddingRight = paddingRight;
        this.invalidate();
        //Log.d("nakama-scale", "AnimatedCurveView setCurvePaddingPixels: " + this.paddingTop + ", " + this.paddingRight + ", " + this.paddingBottom + ", " + this.paddingRight);
    }

	/**
	 * This controls whether the animation will automatically go through standardSets strokes. If set to true,
	 * standardSets strokes will be played back. If false, one stroke will be played on startAnimation, and one
	 * more after each call to incrementCurveStroke().
	 */
	public void setAutoIncrement(boolean val){
        this.autoIncrement = val;
	}

	/**
	 * When not in autoincrement mode, this will increase the number of strokes being animated by one.
	 */
	public int incrementCurveStroke(){
        //Log.i("nakama", "AnimatedCurveView: incrementCurveStroke to " + allowedStrokes + " + 1; playstate " + this.playingState);
        this.allowedStrokes++;

        if(playingState == PlayStatus.PLAYING){
            //Log.i("nakama", "AnimatedCurveView: incrementCurveStroke; playstate is PLAYING, so will resumeAnimation()");
            resumeAnimation(0);
        }

        return this.allowedStrokes;
	}

    /**
     * Clears current strokes, and registers a new set from point lists.
     */
    public void setDrawing(final Drawing drawing, final DrawTime drawTimeParam, List<Criticism.PaintColourInstructions> knownPaintInstructions){
        if(drawing == null){ throw new IllegalArgumentException("Cannot accept null Drawing"); }

        this.knownPaintInstructions = knownPaintInstructions;
        clear();

        if(drawTimeParam == DrawTime.STATIC){
            // force block in onDraw that draw static version
            this.scaleAndOffsets.initialized = false;
        }

        this.unscaledBoundingBox = new Rect(drawing.findBoundingBox());
        this.eqns = drawing.toParameterizedEquations(1);

        Iterator<Stroke> o = drawing.toDrawing().iterator();
        while(o.hasNext()){
            int arcLength = o.next().arcLength();
            this.strokeLengthMultipliers.add(
                    Math.max(1,
                        Math.min(0.25,
                                 108.0 / arcLength)));
        }
        this.scaleAndOffsets.initialized = false;

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

		clear();
		startAnimationInternal();
		return true;
	}

	DrawStatus threadDrawStatus = DrawStatus.FINISHED;
	private DrawStatus drawIncrement(){
        //Log.i("nakama", "AnimatedCurveView: drawIncrement " + time);
		List<Path> pathsToDrawRef = this.pathsToDraw;
		List<ParameterizedEquation> eqnsRef = this.eqns;
		if(time <= 0.99f && eqn_i < eqnsRef.size()){
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
	    	
	    	time += T_INCREMENTS * strokeLengthMultipliers.get(eqn_i);
	    	
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
			}  else {
                return DrawStatus.FINISHED;
            }
		}
		return DrawStatus.DRAWING;
	}
	
	/**
	 * Starts animating current strokes.
	 * @param delayFirst How long in ms to delay before starting animation.
	 */
	public void startAnimation(int delayFirst){
        //Log.i("nakama", "AnimatedCurveView startAnimation " + delayFirst);
        stopAnimationInternal();
		clear();
        playingState = PlayStatus.PLAYING;
	    resumeAnimation(delayFirst);
	}

    private void startAnimationInternal(){
        //Log.i("nakama", "AnimatedCurveView startAnimationInternal ");
        clear();
        resumeAnimation(0);
    }

    private class AnimationRunnable implements Runnable {
        @Override public void run() {
            threadDrawStatus = drawIncrement();
            if(threadDrawStatus == DrawStatus.DRAWING) {
                animateHandler.postDelayed(this, 16);
            } else {
                //Log.i("nakama", "DrawStatus was " + threadDrawStatus + ", CANCELLING timer");
                animateHandler.removeCallbacksAndMessages(null);
            }
            postInvalidate();
        }
    }

    private void resumeAnimation(int delayFirst){
        //Log.i("nakama", "AnimatedCurveView resumeAnimation " + delayFirst);
        threadDrawStatus = DrawStatus.DRAWING;
        this.animateHandler.postDelayed(animationRunnable, delayFirst);
    }

    /**
     * Stops and resets animation.
     */
	public void stopAnimation(){
        this.playingState = PlayStatus.STOPPED;
        stopAnimationInternal();
	}

    /**
     * Stops current animation exactly where it is.
     **/
    public void stopAnimationInternal(){
        //Log.i("nakama", "AnimatedCurveView: stopAnimationInternal.");
        animateHandler.removeCallbacksAndMessages(null);
    }

	@Override protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
	    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	    WidthAndHeight wh = MeasureUtil.fillMeasure(widthMeasureSpec, heightMeasureSpec);

        //if(wh.width != getWidth() || wh.height != getHeight())
            this.scaleAndOffsets.initialized = false;

	    setMeasuredDimension(wh.width, wh.height);

	}
	
	@Override
	protected void onDraw(Canvas canvas){
        // TODO: move this block out of onDraw. Maybe onMeasure? Figure out inits so this can't happen here
		if(!this.scaleAndOffsets.initialized){
            //Log.d("nakama-scale", "AnimatedCurveView measuring: paddings: " + this.paddingTop + ", " + this.paddingRight + ", " + this.paddingBottom + ", " + this.paddingRight);
            scaleAndOffsets.calculate(unscaledBoundingBox, getWidth() - this.paddingLeft - this.paddingRight, getHeight() - this.paddingTop - this.paddingBottom);

            //Log.i("nakama", "AnimatedCurveView: rescaled, resetting animateTimer. threadDrawStatus: " + threadDrawStatus);
            clear();
            if(playingState == PlayStatus.PLAYING){
                startAnimationInternal();
            }

            if(drawTime == DrawTime.STATIC){
                //Log.i("nakama", "Pre-drawing STATIC AnimatedCurveView in onDraw");
                while(drawIncrement() == DrawStatus.DRAWING){ /* loop */ }
            }
        }

		// draw the paths
		for(int i = 0; i < pathsToDraw.size(); i++){
            Path p = pathsToDraw.get(i);
            for(Criticism.PaintColourInstructions colour: knownPaintInstructions){
                colour.colour(i, 0, paint, Color.BLACK);
            }
	    	canvas.drawPath(p, paint);
		}
	}
}