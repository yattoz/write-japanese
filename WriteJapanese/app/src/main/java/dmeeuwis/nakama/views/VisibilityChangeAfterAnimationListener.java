package dmeeuwis.nakama.views;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

public class VisibilityChangeAfterAnimationListener implements AnimationListener {
	
	final public View targetView;
	final public int targetVisibility;
	
	public VisibilityChangeAfterAnimationListener(View target, int visibility){
		this.targetView = target;
		this.targetVisibility = visibility;
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		targetView.setVisibility(targetVisibility);
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// nothing
	}

	@Override
	public void onAnimationStart(Animation animation) {
		if(targetView.getVisibility() == View.INVISIBLE && targetVisibility == View.VISIBLE)
			targetView.setVisibility(View.VISIBLE);
	}
}
