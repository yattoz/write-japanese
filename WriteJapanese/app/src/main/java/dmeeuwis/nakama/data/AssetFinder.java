package dmeeuwis.nakama.data;

import java.io.IOException;
import java.io.InputStream;

import dmeeuwis.nakama.kanjidraw.CurveDrawing;
import dmeeuwis.util.Util;

public class AssetFinder {

    public interface InputStreamGenerator {
        InputStream fromPath(String path) throws IOException;
    }

	private final InputStreamGenerator is;
	
	public AssetFinder(InputStreamGenerator is){
		this.is = is;
	}

    public CurveDrawing findGlyphForCharacter(String pathPrefix, char c) throws IOException {
        return new CurveDrawing(findSvgForCharacter(pathPrefix, c));
    }

	public CurveDrawing findGlyphForCharacter(CharacterStudySet charset, char c) throws IOException {
		return new CurveDrawing(findSvgForCharacter(charset, c));
	}

    public String[] findSvgForCharacter(CharacterStudySet charset, char c) throws IOException {
        return findSvgForCharacter(charset.pathPrefix, c);
    }

	public String[] findSvgForCharacter(String pathPrefix, char c) throws IOException {
        int unicodeValue = c;
        String path = pathPrefix + "/" + Integer.toHexString(unicodeValue) + ".path";
        InputStream is = this.is.fromPath(path);
        try {
            return Util.slurp(is).split("\n");
        } finally {
            is.close();
        }
	}
	
}
