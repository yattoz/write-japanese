package dmeeuwis.nakama.kanjidraw;

import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

public class Criticism {
    public static final int CORRECT_COLOUR = Color.GREEN;
    public static final int INCORRECT_COLOUR = Color.RED;

	public final List<String> critiques;
	public final List<PaintColourInstructions> knownPaintInstructions, drawnPaintInstructions;
	public boolean pass;
	
	public Criticism(){
		this.critiques = new ArrayList<>();
		this.knownPaintInstructions = new ArrayList<>();
        this.drawnPaintInstructions = new ArrayList<>();
		this.pass = true;
	}
	
	public void add(String critique, PaintColourInstructions knownPaint, PaintColourInstructions drawnPaint){
		this.critiques.add(critique);
        this.knownPaintInstructions.add(knownPaint);
        this.drawnPaintInstructions.add(drawnPaint);
		this.pass = false;
	}

    public interface PaintColourInstructions {
        void colour(int stroke, float t, Paint p, int defaultColor);
    }

    public final static PaintColourInstructions SKIP = new NoColours();

    public static class NoColours implements PaintColourInstructions {
        public void colour(int stroke, float t, Paint p, int defaultColor){
            // do nothing
        }
    }

    private static class StrokeColour implements PaintColourInstructions {
        private final int drawn, colour;

        public StrokeColour(int colour, int drawn){
            this.drawn = drawn;
            this.colour = colour;
        }

        public void colour(int stroke, float t, Paint p, int defaultColor){
            if(stroke == drawn){
                p.setColor(colour);
            } else {
                p.setColor(defaultColor);
            }
        }
    }

    public static class WrongStrokeColour extends StrokeColour {
        public WrongStrokeColour(int s){
            super(INCORRECT_COLOUR, s);
        }
    }

    public static class RightStrokeColour extends StrokeColour {
        public RightStrokeColour(int s){
            super(CORRECT_COLOUR, s);
        }
    }

    private static class OrderColours implements PaintColourInstructions {
        private final int drawn, shouldHaveBeen, colour;

        public OrderColours(int colour, int drawn, int shouldHaveBeen){
            this.colour = colour;
            this.drawn = drawn;
            this.shouldHaveBeen = shouldHaveBeen;
        }

        public void colour(int stroke, float t, Paint p, int defaultColor){
            if(stroke == drawn){
                p.setColor(colour);
            } else if (stroke == shouldHaveBeen){
                p.setColor(colour);
            } else {
                p.setColor(defaultColor);
            }
        }
    }

    public static class RightOrderColours extends OrderColours {
        public RightOrderColours(int drawn, int shouldHaveBeen){
            super(CORRECT_COLOUR, drawn, shouldHaveBeen);
        }
    }

    public static class WrongOrderColours extends OrderColours {
        public WrongOrderColours(int drawn, int shouldHaveBeen){
            super(INCORRECT_COLOUR, drawn, shouldHaveBeen);
        }
    }


    public static class LastColours implements PaintColourInstructions {
        private int colour, lastN, strokeCount;

        public LastColours(int colour, int lastN, int strokeCount){
            this.colour = colour;
            this.lastN = lastN;
            this.strokeCount = strokeCount;
        }

        public void colour(int stroke, float t, Paint p, int defaultColor){
            if(stroke >= (strokeCount - lastN)){
                p.setColor(colour);
            } else {
                p.setColor(defaultColor);
            }
        }
    }
}
