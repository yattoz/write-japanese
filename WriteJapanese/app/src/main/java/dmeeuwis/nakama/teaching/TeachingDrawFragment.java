package dmeeuwis.nakama.teaching;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.AssetFinder;
import dmeeuwis.nakama.kanjidraw.Criticism;
import dmeeuwis.nakama.kanjidraw.CurveDrawing;
import dmeeuwis.nakama.kanjidraw.DrawingComparator;
import dmeeuwis.nakama.kanjidraw.PointDrawing;
import dmeeuwis.nakama.views.TracingCurveView;
import dmeeuwis.nakama.views.TracingCurveView.OnTraceCompleteListener;

public class TeachingDrawFragment extends Fragment implements OnTraceCompleteListener {
    final static String initialAdvice = "Trace the character";

    final static String[] goodAdvice = {
            "Good! Trace again, for practice.",
            "Good! Trace again, for practice.",
            "Good! Now trace from memory.",
            "Good! Once more!",
            "Good! Keep practicing, or move on to the 'story' tab."
    };

    final static String[] badAdvice = {
            "Not quite! Try again."
    };


    // data state
    String character;
    String[] currentCharacterSvg;
    CurveDrawing curveDrawing;
    int teachingLevel = 0;
    TeachingActivity parent;

    Animation fadeIn;
    Animation fadeOut;

    CardView messageCard;
    TextView message;
    TracingCurveView tracingView;


    public void updateCharacter(TeachingActivity parent) {
        this.parent = parent;
        this.character = parent.getCharacter();
        this.currentCharacterSvg = parent.getCurrentCharacterSvg();
        this.curveDrawing = new CurveDrawing(currentCharacterSvg);
        this.teachingLevel = 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("nakama", "TeachingDrawFragment lifecycle: onCreateView");
        View view = inflater.inflate(R.layout.fragment_draw, container, false);

        this.fadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_edge_card_in);
        this.fadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_edge_card_out);

        return view;
    }

    public void clear(){
        this.tracingView.clear();
    }

    public void startAnimation(int delay) {
        Log.i("nakama", "TeachingDrawFragment lifecycle: startAnimation");
        if(tracingView != null) {
            Log.e("nakama", "TeachingDrawFragment lifecycle: startAnimation success.");
            tracingView.startAnimation(delay);
        } else {
            Log.e("nakama", "TeachingDrawFragment lifecycle error: skipping tracing view start animation due to null view");
        }
    }

    @Override
    public void onComplete(PointDrawing pointDrawing){
		 DrawingComparator comp = new DrawingComparator(character.charAt(0), curveDrawing, pointDrawing, new AssetFinder(parent.getAssets()));
		 Criticism c = comp.compare();

		 if(c.pass){
			teachingLevel = Math.max(0, Math.min(goodAdvice.length-1, teachingLevel+1));
			changeCardMessage(goodAdvice[teachingLevel]);
		 } else {
			teachingLevel = Math.max(0, Math.min(teachingLevel-1, goodAdvice.length-1));
			changeCardMessage(badAdvice[0]);
		 }
         Log.i("nakama", "TeachingDrawFragment onComplete; teachingLevel becomes " + teachingLevel);

		 tracingView.clear();

		 if(teachingLevel >= 2){
			 tracingView.stopAnimation();
		 } else {
			 tracingView.startAnimation(500);
		 }
	 }

    void changeCardMessage(final String newMessage){
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) { }
            @Override public void onAnimationRepeat(Animation animation) { }

            @Override
            public void onAnimationEnd(Animation animation) {
                try {
                    fadeOut.setAnimationListener(null);
                    message.setText(newMessage);
                    messageCard.startAnimation(fadeIn);
                } catch(NullPointerException e){
                    Log.i("nakama", "TeachingDrawFragment.changeCardMessage: A view element was nulled before end of animation", e);
                }
            }
        });

        try {
            messageCard.startAnimation(this.fadeOut);
        } catch(NullPointerException e){
            Log.i("nakama", "TeachingDrawFragment.changeCardMessage: A view element was nulled while changing card message.");
        }
    }

	public boolean undo(){
		if(tracingView.drawnStrokeCount() > 0){
			tracingView.undo();
			return true;
		}
		return false;
	}
	
	@Override
	public void onPause(){
		tracingView.stopAnimation();
        tracingView.clear();
		super.onPause();
	}

    @Override
    public void onAttach(Activity activity) {
        Log.i("nakama", "TeachingDrawFragment lifecycle: onAttach");
        super.onAttach(activity);
    }

	@Override
	public void onResume(){
        Log.i("nakama", "TeachingDrawFragment lifecycle: onResume; getView=" + getView());
        updateCharacter((TeachingActivity) this.getActivity());
        this.teachingLevel = 0;

        tracingView = (TracingCurveView)getView().findViewById(R.id.tracingPad);
        tracingView.setOnTraceCompleteListener(this);
        tracingView.setCurveDrawing(curveDrawing);

        message = (TextView) getView().findViewById(R.id.tipMessage);
        message.setText(initialAdvice);

        messageCard = (CardView)getView().findViewById(R.id.messageCard);
        startAnimation(300);

		super.onResume();
	}
}
