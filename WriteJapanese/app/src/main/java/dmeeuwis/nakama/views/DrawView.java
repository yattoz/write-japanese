package dmeeuwis.nakama.views;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.kanjidraw.PointDrawing;
import dmeeuwis.nakama.kanjidraw.PathCalculator;
import dmeeuwis.nakama.views.MeasureUtil.WidthAndHeight;
import dmeeuwis.util.Util;

/**
 * A drawing pad for characters, supporting undo.
 */
public class DrawView extends View implements OnTouchListener {

	public final static int BACKGROUND_COLOR = 0xFFece5b4;

	static final private float MIN_DRAW_POINT_DISTANCE_DP = 0.0f;
	static final private float PAINT_THICKNESS_DP = 4;

	private float PAINT_THICKNESS_PX;
	private float MIN_DRAW_POINT_DISTANCE_PX;
	
	// user input data stored here
	protected List<List<Point>> linesToDraw = new ArrayList<>();
    protected List<List<Point>> linesToFade = new ArrayList<>();

	List<Point> currentDrawLine = new ArrayList<>(200);

	protected Paint fingerPaint = new Paint();
	protected Paint fadePaint = new Paint();

	protected OnStrokeListener onStrokeListener = null;
	protected OnClearListener onClearListener = null;
	
	protected List<OnTouchListener> extraListeners = new LinkedList<>();

    protected Integer gridPaddingLeft = 0, gridPaddingTop = 0;

	protected Timer fadeTimer = null;
	protected int fadeAlpha = 0;
	protected Integer backgroundColor = Color.WHITE;

	protected GridBackgroundDrawer grid;
	
	public interface OnStrokeListener {
		public void onStroke(List<Point> stroke);
	}
	
	public interface OnClearListener {
		public void onClear();
	}

	private class FadeTimerTask extends TimerTask {
		@Override public void run() {
			if(fadeAlpha >= 0){
				fadeAlpha -= 25;
				postInvalidate();
			} else {
				fadeTimer.cancel();
				fadeTimer = null;
                linesToFade.clear();
			}
		}
	}
	
	public DrawView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DrawView, defStyle, 0);
        this.gridPaddingLeft = a.getDimensionPixelSize(R.styleable.DrawView_gridPaddingLeft, 0);
        this.gridPaddingTop = a.getDimensionPixelSize(R.styleable.DrawView_gridPaddingTop, 0);
        a.recycle();

        Resources r = getContext().getResources();
        MIN_DRAW_POINT_DISTANCE_PX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MIN_DRAW_POINT_DISTANCE_DP, displayMetrics);
        PAINT_THICKNESS_PX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, PAINT_THICKNESS_DP, displayMetrics);

        this.setOnTouchListener(this);

        this.fingerPaint = new Paint();
        this.fingerPaint.setStyle(Paint.Style.STROKE);
        this.fingerPaint.setStrokeCap(Cap.ROUND);
        this.fingerPaint.setAntiAlias(true);
        this.fingerPaint.setDither(true);
        this.fingerPaint.setStrokeWidth(PAINT_THICKNESS_PX);
        this.fingerPaint.setColor(Color.BLACK);

        this.fadePaint = new Paint();
        this.fadePaint.setStyle(Paint.Style.STROKE);
        this.fadePaint.setStrokeCap(Cap.ROUND);
        this.fadePaint.setAntiAlias(true);
        this.fadePaint.setDither(true);
        this.fadePaint.setStrokeWidth(PAINT_THICKNESS_PX);
        this.fadePaint.setColor(Color.BLACK);

        WindowManager wm = (WindowManager)context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE); // the results will be higher than using the activity context object or the getWindowManager() shortcut
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        Log.i("nakama", String.format("DrawView: setting up grid. Screen res: %dx%d gridLeft: %d, gridRight: %d", screenWidth, screenHeight, gridPaddingLeft, gridPaddingTop));
        this.grid = new GridBackgroundDrawer(this.gridPaddingTop, this.gridPaddingLeft);
	}

    public DrawView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

	public DrawView(Context context) {
		this(context, null, 0);
	}

    public void setGridPadding(int gridPaddingTop, int gridPaddingLeft){
        Log.i("nakama", "DrawView: setGridPadding: setting gridPaddingTop=" + gridPaddingTop + ", gridPaddingLeft=" + gridPaddingLeft);
        this.gridPaddingTop = gridPaddingTop;
        this.gridPaddingLeft = gridPaddingLeft;
        this.grid = new GridBackgroundDrawer(gridPaddingTop, gridPaddingLeft);
        this.invalidate();
    }
	
	public void clear(){
		this.linesToDraw = new ArrayList<>();
		currentDrawLine = new ArrayList<>(200);
		this.invalidate();
		
		if(this.onClearListener != null)
			this.onClearListener.onClear();
	}
	
	@Override
	public void setBackgroundColor(int c){
		this.backgroundColor = c;
	}

	public PointDrawing getDrawing(){
		return PointDrawing.fromDetailedPoints(this.linesToDraw, this.getContext());
	}
	
	/**
	 * Returns the number of on-screen strokes the user has drawn.
	 */
	public int getStrokeCount(){
		return this.linesToDraw.size();
	}
	
	public void undo(){
		List<List<Point>> linesToDrawRef = this.linesToDraw;

		if(linesToDrawRef.size() == 0){
			return;
		}

        this.linesToFade.add(linesToDrawRef.get(linesToDrawRef.size() - 1));
		this.linesToDraw = Util.popCopy(linesToDrawRef);

        this.invalidate();
		startFadeTimer();

		if(linesToDrawRef.size() == 0 && this.onClearListener != null){
			this.onClearListener.onClear();
		}
	}
	
	private void startFadeTimer(){
		fadeAlpha = 255;
		if(fadeTimer == null){
			fadeTimer = new Timer();
			fadeTimer.schedule(new FadeTimerTask(), 0, 20);
		}
	}
	
	Rect dirtyBox = new Rect();
	private void moveAction(MotionEvent me, List<Point> drawPoints){
		Point lastDraw = drawPoints.get(drawPoints.size()-1);

		dirtyBox.set((int)me.getX(), (int)me.getY(), (int)me.getX(), (int)me.getY());
		
		final int history = me.getHistorySize();
		for(int h = 0; h <= history; h++){
			int hx, hy;
			if(h == history){
                hx = (int)me.getX();
                hy = (int)me.getY();
			} else {
				hx = (int)me.getHistoricalX(0, h);
				hy = (int)me.getHistoricalY(0, h);
			}

			double distance = PathCalculator.distance(lastDraw.x, lastDraw.y, hx, hy);
            boolean distanceInclude = distance >= MIN_DRAW_POINT_DISTANCE_PX;

			if(distanceInclude){
				Point latest = new Point(hx, hy);
				dirtyBox.union(lastDraw.x, lastDraw.y);
				dirtyBox.union(hx, hy);

				drawPoints.add(latest);
				lastDraw = latest;
			}
		}
		dirtyBox.left -= PAINT_THICKNESS_PX;
		dirtyBox.right += PAINT_THICKNESS_PX;
		dirtyBox.top -= PAINT_THICKNESS_PX;
		dirtyBox.bottom += PAINT_THICKNESS_PX;
		this.invalidate(dirtyBox);
	}
	
	
	@Override public boolean onTouch(View v, MotionEvent me) {
		final int actionCode = me.getAction();
		final int x = (int)me.getX();
		final int y = (int)me.getY();
		List<List<Point>> linesToDrawRef = this.linesToDraw;

		List<Point> currentDrawLineRef = currentDrawLine;

		if(actionCode == MotionEvent.ACTION_DOWN){
			Point p = new Point(x, y);
			currentDrawLineRef.add(p);

		} else if(actionCode == MotionEvent.ACTION_MOVE && currentDrawLineRef.size() > 0){
			moveAction(me, currentDrawLineRef);
			
		} else if(actionCode == MotionEvent.ACTION_UP && currentDrawLineRef.size() > 0){
            moveAction(me, currentDrawLineRef);

			// throw away single dots
			if(currentDrawLineRef.size() != 1){
                linesToDrawRef.add(currentDrawLineRef);

                if (this.onStrokeListener != null) {
                    this.onStrokeListener.onStroke(linesToDrawRef.get(linesToDrawRef.size() - 1));
                }

                this.invalidate();
            }

            // reset current for next stroke
            this.currentDrawLine = new ArrayList<>(200);
		}
		
		if(actionCode == MotionEvent.ACTION_UP){
			v.performClick();
		}
		
		for(OnTouchListener l: this.extraListeners)
			l.onTouch(v, me);
		
		return true;
	}

    public void addOnTouchListener(OnTouchListener t){
        this.extraListeners.add(t);
    }
	
	@Override protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
	    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	    WidthAndHeight wh = MeasureUtil.fillMeasure(widthMeasureSpec, heightMeasureSpec);
	    setMeasuredDimension(wh.width, wh.height);
        grid.measure(wh.width, wh.height);
	}
	
	@Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(backgroundColor);

        grid.measure(getWidth(), getHeight());
        grid.draw(canvas);

        for(int pi = 1; pi < currentDrawLine.size(); pi++){
            Point p0 = currentDrawLine.get(pi-1);
            Point p1 = currentDrawLine.get(pi);
            canvas.drawLine(p0.x, p0.y, p1.x, p1.y, this.fingerPaint);
        }

        for(List<Point> line: this.linesToDraw){
            for(int pi = 1; pi < line.size(); pi++){
                Point p0 = line.get(pi-1);
                Point p1 = line.get(pi);
                canvas.drawLine(p0.x, p0.y, p1.x, p1.y, this.fingerPaint);
            }
        }
		if(fadeAlpha > 0){
			fadePaint.setAlpha(fadeAlpha);
            for(List<Point> l: this.linesToFade){
                for(int i = 0; i < l.size() - 1; i++){
                    Point p0 = l.get(i);
                    Point p1 = l.get(i+1);
                    canvas.drawLine(p0.x, p0.y, p1.x, p1.y, fadePaint);
                }
            }
		}
	}

	public void setOnStrokeListener(OnStrokeListener listener){
		this.onStrokeListener = listener;
	}
	
	public void setOnClearListener(OnClearListener listener){
		this.onClearListener = listener;
	}
	
	public void stopAnimation(){
		if(fadeTimer != null){
			try {
				fadeTimer.cancel();
				fadeTimer = null;
			} catch(Throwable t){
				Log.e("nakama", "Error when stopping DrawView animations", t);
			}
		}
	}
}
