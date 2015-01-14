package dmeeuwis.nakama.data;

import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.util.Log;

import dmeeuwis.nakama.kanjidraw.CurveDrawing;
import dmeeuwis.util.Util;

public class AssetFinder {

	private final AssetManager am;
	
	public AssetFinder(AssetManager am){
		this.am = am;
	}

    public CurveDrawing findGlyphForCharacter(String pathPrefix, char c){
        return new CurveDrawing(findSvgForCharacter(pathPrefix, c));
    }

	public CurveDrawing findGlyphForCharacter(CharacterStudySet charset, char c){
		return new CurveDrawing(findSvgForCharacter(charset, c));
	}

    public String[] findSvgForCharacter(CharacterStudySet charset, char c){
        return findSvgForCharacter(charset.pathPrefix, c);
    }

	public String[] findSvgForCharacter(String pathPrefix, char c){
        int unicodeValue = c;
        String path = pathPrefix + "/" + Integer.toHexString(unicodeValue) + ".path";
        
        try {
	        InputStream is = this.am.open(path);
	        try {
				return Util.slurp(is).split("\n");
	        } finally {
	        	is.close();
	        }
		} catch (IOException e) {
			Log.e("KanaMaster", "Error loading path: " + path + " for character " + c + " (" + unicodeValue + ")");
			return null;
		}
	}
	
}
