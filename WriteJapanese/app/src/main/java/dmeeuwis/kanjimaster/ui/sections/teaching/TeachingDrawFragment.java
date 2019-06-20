package dmeeuwis.kanjimaster.ui.sections.teaching;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.data.AndroidInputStreamGenerator;
import dmeeuwis.kanjimaster.logic.data.AssetFinder;
import dmeeuwis.kanjimaster.logic.data.UncaughtExceptionLogger;
import dmeeuwis.kanjimaster.logic.drawing.Comparator;
import dmeeuwis.kanjimaster.logic.drawing.ComparisonFactory;
import dmeeuwis.kanjimaster.logic.drawing.Criticism;
import dmeeuwis.kanjimaster.logic.drawing.CurveDrawing;
import dmeeuwis.kanjimaster.logic.drawing.PointDrawing;
import dmeeuwis.kanjimaster.ui.views.TracingCurveView;
import dmeeuwis.kanjimaster.ui.views.TracingCurveView.OnTraceCompleteListener;
import dmeeuwis.util.Util;

public class TeachingDrawFragment extends Fragment implements OnTraceCompleteListener {
    final static String initialAdvice = "Trace the character";

    final static String[] goodAdvice = {
            "Good! Trace again, for practice.",
            "Good! Trace again, for practice.",
            "Good! Now trace from memory.",
            "Good! Once more!",
            "Good! Keep practicing, or write a story to remember the kanji with."
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
    View messageCardColour;
    TextView message;
    TracingCurveView tracingView;


    public void updateCharacter(TeachingActivity parent) {
        this.parent = parent;
        this.character = parent.getCharacter();
        try {
            this.currentCharacterSvg = parent.getCurrentCharacterSvg();
            this.curveDrawing = new CurveDrawing(currentCharacterSvg);
            this.teachingLevel = 0;
        } catch(Throwable t){
            throw new RuntimeException("Error parsing out svg for character: " + character + " " + Integer.toHexString(character.charAt(0)), t);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("nakama", "TeachingDrawFragment lifecycle: onCreateView");
        View view = inflater.inflate(R.layout.fragment_draw, container, false);

        this.fadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_edge_card_in);
        this.fadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_edge_card_out);

        return view;
    }

    public void clear() {
        this.tracingView.clear();
    }

    public void startAnimation(int delay) {
        Log.i("nakama", "TeachingDrawFragment lifecycle: startAnimation");
        if (tracingView != null) {
            tracingView.startAnimation(delay);
        }
    }

    @Override
    public void onComplete(PointDrawing pointDrawing) {
        AssetFinder.InputStreamGenerator is = new AndroidInputStreamGenerator(parent.getAssets());
        Comparator comp = ComparisonFactory.getUsersComparator(getActivity().getApplicationContext(),
                new AssetFinder(is));
        Criticism c;
        try {
            c = comp.compare(character.charAt(0), pointDrawing, curveDrawing);
        } catch (IOException e) {
            Toast.makeText(getActivity(), "Error accessing comparison data for " + character.charAt(0), Toast.LENGTH_LONG).show();
            UncaughtExceptionLogger.backgroundLogError("IOError from comparator", e, getContext());
            return;
        }

        if (c.pass) {
            teachingLevel = Math.max(0, Math.min(goodAdvice.length - 1, teachingLevel + 1));
            changeCardMessage(goodAdvice[teachingLevel], getResources().getColor(R.color.DarkGreen));
        } else {
            teachingLevel = Math.max(0, Math.min(teachingLevel - 1, goodAdvice.length - 1));
            changeCardMessage(Util.join(" ", c.critiques), getResources().getColor(R.color.DarkRed));
        }
        Log.i("nakama", "TeachingDrawFragment onComplete; teachingLevel becomes " + teachingLevel);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tracingView.clear();

                if (teachingLevel >= 2) {
                    tracingView.stopAnimation();
                } else {
                    tracingView.startAnimation(500);
                }
            }
        }, 500);
    }

    void changeCardMessage(final String newMessage, final int color) {
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                try {
                    fadeOut.setAnimationListener(null);
                    message.setText(newMessage);
                    messageCardColour.setBackgroundColor(color);
                    messageCard.startAnimation(fadeIn);
                } catch (NullPointerException e) {
                    Log.i("nakama", "TeachingDrawFragment.changeCardMessage: A view element was nulled before end of animation", e);
                }
            }
        });

        try {
            messageCard.startAnimation(this.fadeOut);
        } catch (NullPointerException e) {
            Log.i("nakama", "TeachingDrawFragment.changeCardMessage: A view element was nulled while changing card essage.");
        }
    }

    public boolean undo() {
        if (tracingView != null && tracingView.drawnStrokeCount() > 0) {
            tracingView.undo();
            return true;
        }
        return false;
    }

    @Override
    public void onPause() {
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
    public void onResume() {
        Log.i("nakama", "TeachingDrawFragment lifecycle: onResume; getView=" + getView());
        updateCharacter((TeachingActivity) this.getActivity());
        this.teachingLevel = 0;

        tracingView = (TracingCurveView) getView().findViewById(R.id.tracingPad);
        tracingView.setOnTraceCompleteListener(this);
        tracingView.setCurveDrawing(curveDrawing);

        message = (TextView) getView().findViewById(R.id.tipMessage);
        message.setText(initialAdvice);

        messageCard = (CardView) getView().findViewById(R.id.messageCard);
        messageCardColour = getView().findViewById(R.id.messageCard_colour);
        startAnimation(300);

        super.onResume();
    }
}
