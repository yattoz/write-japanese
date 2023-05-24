package dmeeuwis.kanjimaster.logic.data;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmeeuwis.Kana;
import dmeeuwis.Kanji;
import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.kanjimaster.logic.drawing.CurveDrawing;
import dmeeuwis.util.Util;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class SvgExistsTest {

    @Test
    public void svgPathExistsForAllKanji() throws Exception {
        String path = System.getProperty("user.dir") + "/src/main/assets/paths/";

        List<String> failures = new ArrayList<>();
        Map<String, String> sets = new HashMap<>();
        sets.put("hiragana", Kana.commonHiragana());
        sets.put("katakana", Kana.commonHiragana());
        sets.put("joyou_1", Kanji.JOUYOU_G1);
        sets.put("joyou_2", Kanji.JOUYOU_G2);
        sets.put("joyou_3", Kanji.JOUYOU_G3);
        sets.put("joyou_4", Kanji.JOUYOU_G4);
        sets.put("joyou_5", Kanji.JOUYOU_G5);
        sets.put("joyou_6", Kanji.JOUYOU_G6);
        sets.put("joyou_ss", Kanji.JOUYOU_SS);
        sets.put("jlpt_5", Kanji.JLPT_N5);
        sets.put("jlpt_4", Kanji.JLPT_N4);
        sets.put("jlpt_3", Kanji.JLPT_N3);
        sets.put("jlpt_2", Kanji.JLPT_N2);
        sets.put("jlpt_1", Kanji.JLPT_N1);

        for(Map.Entry<String, String> s: sets.entrySet()){
            for(Character c: s.getValue().toCharArray()){
                String id = Integer.toHexString((c).charValue());
                String filename = path + id + ".path";
                boolean success = new File(filename).exists();

                if(success) {
                    try (FileInputStream fin = new FileInputStream(filename)){
                        String[] lines = Util.slurp(fin).split("\n");
                        new CurveDrawing(lines);
                    } catch(Throwable t){
                        failures.add("Error making CurveDrawing out of .svg for character: " + c + " [" + id + "] in set " + s.getKey() + ": " + t.getMessage());
                    }
                } else {
                    String retest = path + "0" + Integer.toHexString((c).charValue()) + ".path";
                    boolean recheck = new File(retest).exists();

                    failures.add("Set " + s.getKey()  + " character " + c + " [" + Integer.toHexString(c.charValue()) + "] has no .path file; " + filename + ";  recheck shows " + recheck);
                }
            }
        }

        for(String s: failures){ System.out.println(s); }
        assertEquals("All characters have paths", 0, failures.size());
    }
}
