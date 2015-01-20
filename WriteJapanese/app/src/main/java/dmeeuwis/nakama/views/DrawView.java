package dmeeuwis.nakama.views;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

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

    private final static double DIRECTION_LIMIT = Math.PI / 8;

	static final private float MIN_GRADING_POINT_DISTANCE_DP = 25;
	static final private float MIN_DRAW_POINT_DISTANCE_DP = 5;
	static final private float PAINT_THICKNESS_DP = 4;

	private float PAINT_THICKNESS_PX;
	private float MIN_GRADING_POINT_DISTANCE_PX;
	private float MIN_DRAW_POINT_DISTANCE_PX;
	
	// user input data stored here
	protected List<List<Point>> linesToDraw = new ArrayList<>();
	protected List<List<Point>> linesToGrade = new ArrayList<>();
    protected List<List<Point>> linesToFade = new ArrayList<>();

	List<Point> currentDrawLine = new ArrayList<>(200);
	List<Point> currentGradeLine = new ArrayList<>(100);

	protected Paint fingerPaint = new Paint();
	protected Paint fadePaint = new Paint();
	
	protected OnStrokeListener onStrokeListener = null;
	protected OnClearListener onClearListener = null;
	
	protected List<OnTouchListener> extraListeners = new LinkedList<>();

	protected Bitmap drawBitmap;
	protected Canvas drawCanvas;

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

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DrawView, defStyle, 0);
        this.gridPaddingLeft = a.getDimensionPixelSize(R.styleable.DrawView_gridPaddingLeft, 0);
        this.gridPaddingTop = a.getDimensionPixelSize(R.styleable.DrawView_gridPaddingTop, 0);
        a.recycle();

        Resources r = getContext().getResources();
        MIN_GRADING_POINT_DISTANCE_PX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MIN_GRADING_POINT_DISTANCE_DP, r.getDisplayMetrics());
        MIN_DRAW_POINT_DISTANCE_PX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MIN_DRAW_POINT_DISTANCE_DP, r.getDisplayMetrics());
        PAINT_THICKNESS_PX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, PAINT_THICKNESS_DP, r.getDisplayMetrics());

        Log.i("nakama", "MIN_GRADING_POINT_DISTANCE_PX: " + MIN_GRADING_POINT_DISTANCE_PX);
        Log.i("nakama", "MIN_DRAW_POINT_DISTANCE_PX: " + MIN_DRAW_POINT_DISTANCE_PX);
        Log.i("nakama", "PAINT_THICKNESS_PX: " + PAINT_THICKNESS_PX);

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
        this.fingerPaint.setColor(Color.BLACK);

        this.grid = new GridBackgroundDrawer(0, 0);
	}

    public DrawView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

	public DrawView(Context context) {
		this(context, null, 0);
	}

    public void setGridPadding(int gridPaddingTop, int gridPaddingLeft){
        this.gridPaddingTop = gridPaddingTop;
        this.gridPaddingLeft = gridPaddingLeft;
        this.grid = new GridBackgroundDrawer(gridPaddingTop, gridPaddingLeft);
        this.invalidate();
    }
	
	public void clear(){
		this.linesToDraw = new ArrayList<>();
		this.linesToGrade = new ArrayList<>();
		
		currentDrawLine = new ArrayList<>(200);
		currentGradeLine = new ArrayList<>(100);
		
		redraw();
		
		if(this.onClearListener != null)
			this.onClearListener.onClear();
	}
	
	@Override
	public void setBackgroundColor(int c){
		this.backgroundColor = c;
	}

	public PointDrawing getDrawing(){
		return PointDrawing.fromPoints(drawnPaths());
	}
	
	/**
	 * Returns the number of on-screen strokes the user has drawn.
	 */
	public int getStrokeCount(){
		return this.linesToGrade.size();
	}
	
	/**
	 * List of list of points. Each list of points is at least 2 elements long. Points depend on
	 * current screen size, and are not scaled.
	 */
	private List<List<Point>> drawnPaths(){
		List<List<Point>> linesToGradeRef = this.linesToGrade;
		List<List<Point>> drawn = new ArrayList<>(linesToGradeRef.size());
		for(List<Point> line: linesToGradeRef){
			if(line.size() >= 2)
				drawn.add(line);
		}
		return drawn;
	}
	
	public void undo(){
		List<List<Point>> linesToDrawRef = this.linesToDraw;
		List<List<Point>> linesToGradeRef = this.linesToGrade;

		if(linesToDrawRef.size() == 0 || linesToGradeRef.size() == 0){
			return;
		}

        this.linesToFade.add(linesToDrawRef.get(linesToDrawRef.size() - 1));
		this.linesToDraw = Util.popCopy(linesToDrawRef);

		this.linesToGrade = Util.popCopy(linesToGradeRef);
		linesToGradeRef = this.linesToGrade;

		redraw();

		startFadeTimer();

		if(linesToGradeRef.size() == 0 && this.onClearListener != null){
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
	private void moveAction(MotionEvent me, List<Point> drawPoints, List<Point> gradePoints){
		Point lastDraw = drawPoints.get(drawPoints.size()-1);
		Point lastGrade = gradePoints.get(gradePoints.size()-1);
        Double lastDirection = null;
        if(drawPoints.size() > 1) {
            lastDirection = PathCalculator.angle(drawPoints.get(drawPoints.size() - 2), drawPoints.get(drawPoints.size() - 1));
        }

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

            Log.d("nakama", "\nDistance between points: for index " + h + " ; " + distance + "; points: " + lastDraw.x + ", " + lastDraw.y  + "  " + hx + ", " + hy);

			if(distanceInclude){
                Log.d("nakama", "Draw point include from distance! " + hx + "," + hy + "; distance was " + distance + " vs limit " + MIN_DRAW_POINT_DISTANCE_PX);
                double direction = PathCalculator.angle(lastDraw.x, lastDraw.y, hx, hy);
                boolean directionInclude = lastDirection == null || Math.abs(lastDirection - direction) >= DIRECTION_LIMIT;

				Point latest = new Point(hx, hy);
				drawCanvas.drawLine(lastDraw.x, lastDraw.y, hx, hy, fingerPaint);
				dirtyBox.union(lastDraw.x, lastDraw.y);
				dirtyBox.union(hx, hy);

                final double gradeDistance = PathCalculator.distance(lastGrade.x, lastGrade.y, hx, hy);
                final boolean gradeDistanceInclude = gradeDistance >= MIN_GRADING_POINT_DISTANCE_PX;

                Log.d("nakama", String.format("Looking at grade point: gradeDistance: %.2f gradeDistanceInclude: %b", gradeDistance, gradeDistanceInclude));
				if(directionInclude || gradeDistanceInclude){
                    if(directionInclude)
                        Log.d("nakama", "Grade point include via DIRECTION! " + hx + ", " + hy + " : " + direction + " vs min direction " + DIRECTION_LIMIT);
                    if(gradeDistanceInclude)
                        Log.d("nakama", "Grade point include via DISTANCE! " + hx + ", " + hy + " : " + gradeDistance + " vs min distance " + MIN_GRADING_POINT_DISTANCE_PX);
					gradePoints.add(latest);
					lastGrade = latest;
                    Log.d("nakama", "Line is now: " + Util.join(", ", gradePoints));
				}
				
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
		List<List<Point>> linesToGradeRef = this.linesToGrade;

		List<Point> currentGradeLineRef = currentGradeLine;
		List<Point> currentDrawLineRef = currentDrawLine;

		if(actionCode == MotionEvent.ACTION_DOWN){
            Log.i("nakama", "DrawView: ===============================================");
            Log.i("nakama", "DrawView: onTouch for stroke " + this.linesToDraw.size());

			Point p = new Point(x, y);
			currentDrawLineRef.add(p);
			currentGradeLineRef.add(p);
			
		} else if(actionCode == MotionEvent.ACTION_MOVE && currentDrawLineRef.size() > 0){
			moveAction(me, currentDrawLineRef, currentGradeLineRef);
			
		} else if(actionCode == MotionEvent.ACTION_UP && currentDrawLineRef.size() > 0){
            moveAction(me, currentDrawLineRef, currentGradeLineRef);

			// throw away single dots
			if(currentDrawLineRef.size() != 1){
                Log.d("nakama", "====> Finished a stroke! Points are: " + Util.join(", ", currentGradeLineRef));

                linesToGradeRef.add(currentGradeLineRef);
                linesToDrawRef.add(currentDrawLineRef);

                if (this.onStrokeListener != null) {
                    this.onStrokeListener.onStroke(linesToGradeRef.get(linesToGradeRef.size() - 1));
                }

                this.invalidate();
            }

            // reset current for next stroke
            this.currentDrawLine = new ArrayList<>(200);
            this.currentGradeLine = new ArrayList<>(100);
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
	}
	
	private final void redraw(){
        if(drawBitmap == null){ return; }

		drawBitmap.eraseColor(backgroundColor);

		grid.measure(getWidth(), getHeight());
		grid.draw(drawCanvas);
		
        for(List<Point> line: this.linesToDraw){
            for(int pi = 1; pi < line.size(); pi++){
                Point p0 = line.get(pi-1);
                Point p1 = line.get(pi);
                drawCanvas.drawLine(p0.x, p0.y, p1.x, p1.y, this.fingerPaint);
            }
        }
	}
	
	private final void initGrid(int decidedWidth, int decidedHeight){
		if(decidedWidth == 0 || decidedHeight == 0) return;

		boolean remakeBitmaps = drawBitmap == null || decidedWidth != drawBitmap.getWidth() || decidedHeight != drawBitmap.getHeight();
		if(remakeBitmaps){
			Log.i("nakama", "DrawView initGrid bitmap recreate");
			if(drawBitmap != null) drawBitmap.recycle();
			drawBitmap = Bitmap.createBitmap(decidedWidth, decidedHeight, Bitmap.Config.ARGB_4444);     // 6.6MB
			drawCanvas = new Canvas(drawBitmap);
		}

		redraw();
	}

	@Override protected void onDraw(Canvas canvas) {
		if(drawBitmap == null){
			initGrid(getWidth(), getHeight());
		}

		canvas.drawBitmap(drawBitmap, 0, 0, null);
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
