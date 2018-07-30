package dmeeuwis.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;

public class UtilTest {

    @Test
    public void testStringToCharList(){
        assertEquals(Arrays.asList('a', 'b', 'c'), Util.stringToCharList("abc"));
        assertEquals(new ArrayList<Character>(), Util.stringToCharList(""));
        assertEquals(Arrays.asList('!'), Util.stringToCharList("!"));
    }
}
