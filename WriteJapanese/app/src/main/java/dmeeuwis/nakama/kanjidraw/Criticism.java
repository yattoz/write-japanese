package dmeeuwis.nakama.kanjidraw;

import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

public class Criticism {
	public final List<String> critiques;
	public final List<PaintColourInstructions> visualAids;
	public boolean pass;
	
	public Criticism(){
		this.critiques = new ArrayList<>();
		this.visualAids = new ArrayList<>();
		this.pass = true;
	}
	
	public void add(String critique, PaintColourInstructions p){
		this.critiques.add(critique);
		this.pass = false;
	}

	public static class PaintColourInstructions {
		public final static PaintColourInstructions SKIP = new PaintColourInstructions(int targetStroke){
			
			@Override public void colour(float t, Paint p, int defaultColor){ /* do nothing */ }
		};

		final private float start;
		final private float end;
		final private int colour;

		public PaintColourInstructions(int targetStroke) {
			this.start = this.end = 0.0f;
			this.colour = 0x000000;
			this.targetStroke = targetStroke;
		}

		public PaintColourInstructions(float start, float end, int colour) {
			this.start = start;
			this.end = end;
			this.colour = colour;
		}

		public void colour(float t, Paint p, int defaultColor, int currentStroke) {
			if(targetStroke == currentStroke) {
				if (t >= start && t <= end) {
					p.setColor(colour);
				} else {
					p.setColor(defaultColor);
				}
			}
		}
	}
}
