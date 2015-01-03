package dmeeuwis.masterlibrary;

import java.util.List;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
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
	
	AlphaAnimation fadeIn = new AlphaAnimation(0.0f , 1.0f ) ; 
	AlphaAnimation fadeOut = new AlphaAnimation( 1.0f , 0.0f ) ; 

	@Override public void onAttach(Activity activity){
		TeachingActivity parent = (TeachingActivity)getActivity();
		this.character = parent.getCharacter();
		this.currentCharacterSvg = parent.getCurrentCharacterSvg();
		Log.d("nakama", "TeachingInfoFragment: parent is " + parent + "; got parent's kanji " + this.character + "; char svg: " + this.currentCharacterSvg);
		
		super.onAttach(activity);	
	}
	
	 @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		 View view = inflater.inflate(R.layout.fragment_draw, container, false);
		 
	     this.tracingView = (TracingCurveView)view.findViewById(R.id.tracingPad);
	     
	     this.fadeIn.setDuration(500);
	     this.fadeIn.setFillAfter(true);
	     this.fadeOut.setDuration(500);
	     this.fadeOut.setFillAfter(true);

	     this.glyph = new Glyph(currentCharacterSvg);
	     
	     this.message = (TextView)view.findViewById(R.id.tipMessage);
	     
	     this.tracingView.setOnTraceCompleteListener(this);
	     this.tracingView.setOnStrokeListener(this);
	     
		 return view;
	 }
	 
	 @Override public void onStart() {
	     this.tracingView.setGlyph(glyph);
	     super.onStart();
	 }
	 
	 public void onComplete(Drawing drawing){
         Log.d("nakama", "TeachingDrawFragment: onComplete");
		 PathComparator comp = new PathComparator(character.charAt(0), glyph, drawing, new AssetFinder(this.getActivity().getAssets()));
		 Criticism c = comp.compare();
		 if(c.pass){
			teachingLevel = Math.max(0, Math.min(goodAdvice.length-1, teachingLevel+1));
			message.setText(goodAdvice[teachingLevel]);
		 } else {
			teachingLevel = Math.max(0, Math.min(teachingLevel-1, goodAdvice.length-1));
			message.setText(badAdvice[0]);
		 }
		
		 this.tracingView.clear();
		 this.message.startAnimation(this.fadeIn);
		 this.waitingForStroke = true;
		 
		 if(teachingLevel >= 2){
             Log.d("nakama", "TeachingDrawFragment: onComplete, stopping animation for teachingLevel " + teachingLevel);
			 this.tracingView.stopAnimation();
		 } else {
             Log.d("nakama", "TeachingDrawFragment: onComplete, restarting animation for teachingLevel " + teachingLevel);
			 this.tracingView.startAnimation();
		 }
	 }

	@Override
	public void onStroke(List<Point> stroke) {
		if(waitingForStroke){
		 this.message.startAnimation(this.fadeOut);
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
