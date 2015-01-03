package dmeeuwis.nakama.views;

import android.graphics.Point;

public class TimePoint extends Point {
	public final long time;
	
	public TimePoint(int x, int y, long time){
		super(x, y);
		this.time = time;
	}
}
