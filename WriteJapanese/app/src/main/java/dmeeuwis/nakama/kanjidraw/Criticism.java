package dmeeuwis.nakama.kanjidraw;

import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

import dmeeuwis.util.Util;

public class Criticism {
    public static final int CORRECT_COLOUR = 0xFF0EA10E;
    public static final int INCORRECT_COLOUR = 0xFF8B000B;

    public final static PaintColourInstructions SKIP = new NoColours();
    public final static ArrayList<PaintColourInstructions> SKIP_LIST = new ArrayList<>();

    private double[][] matrix;


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

    public void setScoreMatrix(double[][] matrix){
        this.matrix = matrix;
    }

    public void add(Criticism c){
        for(String m: c.critiques){
            critiques.add(m);
        }

        for(PaintColourInstructions p: c.knownPaintInstructions){
            knownPaintInstructions.add(p);
        }

        for(PaintColourInstructions p: c.drawnPaintInstructions){
            drawnPaintInstructions.add(p);
        }
        this.pass = false;
    }

    public interface PaintColourInstructions {
        void colour(int stroke, float t, Paint p, int defaultColor);
    }

    public static class NoColours implements PaintColourInstructions {
        public void colour(int stroke, float t, Paint p, int defaultColor){
            p.setColor(defaultColor);
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

    public static PaintColourInstructions incorrectColours(int...strokes){
        return new ColourStrokes(INCORRECT_COLOUR, strokes);
    }

    public static PaintColourInstructions correctColours(int...strokes){
        return new ColourStrokes(CORRECT_COLOUR, strokes);
    }

    private static class ColourStrokes implements PaintColourInstructions {
        private int[] strokes;
        private int colour;

        public ColourStrokes(int colour, int ... stroke){
            this.colour = colour;
            this.strokes = stroke;
        }

        public void colour(int stroke, float t, Paint p, int defaultColor){
            boolean contains = false;
            for(int s: strokes) {
                if (stroke == s) {
                    contains = true;
                }
            }

            if(contains){
                p.setColor(colour);
            } else {
                p.setColor(defaultColor);
            }
        }
    }

    public String toString(){
        return "Critique passed: " + pass + "\n" +
                Util.join(critiques) +
                (matrix == null ? "\n no score matrix" : "\n" + Util.printMatrix(matrix));
    }
}
