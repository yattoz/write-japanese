package dmeeuwis.kanjimaster.logic.data;

import java.io.IOException;
import java.io.InputStream;
import dmeeuwis.kanjimaster.logic.drawing.CurveDrawing;
import dmeeuwis.kanjimaster.logic.core.util.Util;

public class AssetFinder {

    public static String findPathFile(Character character){
        return "paths/" + Integer.toHexString(character.charValue()) + ".path";
    }

    public interface InputStreamGenerator {
        InputStream fromPath(String path) throws IOException;
    }

	private final InputStreamGenerator is;
	
	public AssetFinder(InputStreamGenerator is){
		this.is = is;
	}

	public CurveDrawing findGlyphForCharacter(char c) throws IOException {
		return new CurveDrawing(findSvgForCharacter(c));
	}

    public String[] findSvgForCharacter(char c) throws IOException {
        String path = findPathFile(c);
        InputStream is = this.is.fromPath(path);
        try {
            return Util.slurp(is).split("\n");
        } finally {
            is.close();
        }
	}
}
