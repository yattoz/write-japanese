package dmeeuwis.nakama.data;


import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import dmeeuwis.Kanji;
import dmeeuwis.indexer.KanjiFinder;

import static junit.framework.Assert.assertEquals;

public class MeaningsExistTest {
    @Test
    public void testMeaningsExistsForAllKanji() throws Exception {
        List<String> failures = new ArrayList<>();
        String[] sets = new String[]{
                Kanji.JOUYOU_G1,
                Kanji.JOUYOU_G2,
                Kanji.JOUYOU_G3,
                Kanji.JOUYOU_G4,
                Kanji.JOUYOU_G5,
                Kanji.JOUYOU_G6,
                Kanji.JOUYOU_SS,
                Kanji.JLPT_N5,
                Kanji.JLPT_N4,
                Kanji.JLPT_N3,
                Kanji.JLPT_N2,
                Kanji.JLPT_N1,
        };

        final String kanjiDictPath = System.getProperty("user.dir") + "/src/main/assets/kanjidic.utf8.awb";
        final String kanjiIndexPath = System.getProperty("user.dir") + "/src/main/assets/kanjidic.index.awb";

        FileInputStream findex = new FileInputStream(kanjiIndexPath);
        FileInputStream fdata = new FileInputStream(kanjiDictPath);

        KanjiFinder kf = new KanjiFinder(findex, new File(kanjiIndexPath).length(), fdata.getChannel(), 0);


        for(String s: sets){
            for(Character c: s.toCharArray()){

                Kanji k = kf.find(c);
                if(k == null){
                    failures.add("No Kanji object found for " + c + " in set " + s);
                }

                if(k.meanings == null || k.meanings.length == 0){
                    failures.add("No meanings found on kanji object found for " + c + " in set " + s);
                }
            }
        }

        for(String s: failures){ System.out.println(s); }
        assertEquals("All characters have meanings", 0, failures.size());
    }
}
