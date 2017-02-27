package dmeeuwis.nakama.data;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import dmeeuwis.nakama.LockChecker;
import dmeeuwis.nakama.kanjidraw.CurveDrawing;
import dmeeuwis.util.Util;

public class AssetFinder {

    public static String findPathFile(Character character, LockChecker lc, UUID iid){
        return findPathFile(null, character, lc, iid);
    }

    public static String findPathFile(CharacterStudySet curSet, Character character, LockChecker lc, UUID iid){
        String path = null;
        if(curSet != null && curSet.systemSet) {
            path = curSet.pathPrefix;
        } else {
            for(CharacterStudySet c: CharacterSets.all(lc, iid)){
                if(c.allCharactersSet.contains(Character.valueOf(character))){
                    path = c.pathPrefix;
                    break;
                }
            }
            if(path == null){
                path = "";      // failure!
            }
        }
        String fullPath = path + "/" + Integer.toHexString(character.charValue()) + ".path";
        Log.i("nakama", "Found .path file for " + character + " as " + fullPath);
        return fullPath;
    }

    public interface InputStreamGenerator {
        InputStream fromPath(String path) throws IOException;
    }

	private final InputStreamGenerator is;
	
	public AssetFinder(InputStreamGenerator is){
		this.is = is;
	}

	public CurveDrawing findGlyphForCharacter(CharacterStudySet charset, char c) throws IOException {
		return new CurveDrawing(findSvgForCharacter(charset, c));
	}

    public String[] findSvgForCharacter(CharacterStudySet charset, char c) throws IOException {
        String path = findPathFile(charset, c, null, null);
        InputStream is = this.is.fromPath(path);
        try {
            return Util.slurp(is).split("\n");
        } finally {
            is.close();
        }
	}
}
