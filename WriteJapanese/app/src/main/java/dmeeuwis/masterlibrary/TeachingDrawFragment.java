package dmeeuwis.masterlibrary;

import java.util.List;

import android.app.Activity;
import android.graphics.Point;
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
import dmeeuwis.nakama.kanjidraw.Drawing;
import dmeeuwis.nakama.kanjidraw.Glyph;
import dmeeuwis.nakama.kanjidraw.PathComparator;
import dmeeuwis.nakama.views.DrawView.OnStrokeListener;
import dmeeuwis.nakama.views.TracingCurveView;
import dmeeuwis.nakama.views.TracingCurveView.OnTraceCompleteListener;

public class TeachingDrawFragment extends Fragment implements OnTraceCompleteListener, OnStrokeListener {

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
	

	boolean waitingForStroke = false;
	
	String character;
	String[] currentCharacterSvg;
	Glyph glyph;
	TracingCurveView tracingView;
	TextView message;
	int teachingLevel = 0;

    CardView messageCard;

	Animation fadeIn;
	Animation fadeOut;

	@Override public void onAttach(Activity activity){
		TeachingActivity parent = (TeachingActivity)getActivity();
		this.character = parent.getCharacter();
		this.currentCharacterSvg = parent.getCurrentCharacterSvg();

		super.onAttach(activity);	
	}
	
	 @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		 View view = inflater.inflate(R.layout.fragment_draw, container, false);
		 
	     this.tracingView = (TracingCurveView)view.findViewById(R.id.tracingPad);

         this.fadeIn = AnimationUtils.loadAnimation(this.getActivity(), R.anim.slide_in_up);
         this.fadeOut = AnimationUtils.loadAnimation(this.getActivity(), R.anim.slide_out_down);

	     this.glyph = new Glyph(currentCharacterSvg);
	     
	     this.message = (TextView)view.findViewById(R.id.tipMessage);
         this.messageCard = (CardView)view.findViewById(R.id.messageCard);
         this.message.setText(initialAdvice);

	     this.tracingView.setOnTraceCompleteListener(this);
	     this.tracingView.setOnStrokeListener(this);
	     
		 return view;
	 }
	 
	 @Override public void onStart() {
	     this.tracingView.setGlyph(glyph);
	     super.onStart();
	 }
	 
	 public void onComplete(Drawing drawing){
		 PathComparator comp = new PathComparator(character.charAt(0), glyph, drawing, new AssetFinder(this.getActivity().getAssets()));
		 Criticism c = comp.compare();

		 if(c.pass){
			teachingLevel = Math.max(0, Math.min(goodAdvice.length-1, teachingLevel+1));
			changeCardMessage(goodAdvice[teachingLevel]);
		 } else {
			teachingLevel = Math.max(0, Math.min(teachingLevel-1, goodAdvice.length-1));
			changeCardMessage(badAdvice[0]);
		 }
		
		 this.tracingView.clear();
		 this.waitingForStroke = true;
		 
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

	@Override
	public void onStroke(List<Point> stroke) {
		if(waitingForStroke){
		 waitingForStroke = false;
		}
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
		super.onPause();
	}
	
	@Override
	public void onStop(){
		tracingView.clear();
		super.onStop();
	}
	
	@Override
	public void onResume(){
		tracingView.startAnimation(300);
		super.onStop();
	}
}
