package dmeeuwis.nakama.teaching;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

    // ui state
	TracingCurveView tracingView;
	TextView message;
    CardView messageCard;

	Animation fadeIn;
	Animation fadeOut;


    public void updateCharacter(TeachingActivity parent){
        this.parent = parent;
        this.character = parent.getCharacter();
        this.currentCharacterSvg = parent.getCurrentCharacterSvg();
        this.curveDrawing = new CurveDrawing(currentCharacterSvg);
        this.teachingLevel = 0;
    }
	
	 @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		 View view = inflater.inflate(R.layout.fragment_draw, container, false);
		 
	     this.tracingView = (TracingCurveView)view.findViewById(R.id.tracingPad);
         this.tracingView.setOnTraceCompleteListener(this);

         this.fadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_edge_card_in);
         this.fadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_edge_card_out);

         this.messageCard = (CardView)view.findViewById(R.id.messageCard);
         this.message = (TextView)view.findViewById(R.id.tipMessage);

		 return view;
	 }
	 
	 @Override public void onStart() {
	     this.tracingView.setCurveDrawing(curveDrawing);
         this.message.setText(initialAdvice);
         this.teachingLevel = 0;
	     super.onStart();
	 }
	 
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
		
		 this.tracingView.clear();

		 if(teachingLevel >= 2){
			 this.tracingView.stopAnimation();
		 } else {
			 this.tracingView.startAnimation(500);
		 }
	 }

    void changeCardMessage(final String newMessage){
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {  }
            @Override public void onAnimationRepeat(Animation animation) {  }

            @Override
            public void onAnimationEnd(Animation animation) {
                fadeOut.setAnimationListener(null);
                message.setText(newMessage);
                messageCard.startAnimation(fadeIn);
            }
        });
        this.messageCard.startAnimation(this.fadeOut);
    }

	public boolean undo(){
		if(this.tracingView.drawnStrokeCount() > 0){
			this.tracingView.undo();
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
	public void onResume(){
		tracingView.startAnimation(300);
		super.onResume();
	}
}
