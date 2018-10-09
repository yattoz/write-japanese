package dmeeuwis.nakama.data;

import org.junit.*;

public class SRSQueueUnitTest {
    @Test public void testSRSGettingStuckBetweenSets(){
        // char in one set A with very early January repeat


        // char is in another set B with later date


       // global SRS used first date

       // but when setting next, next is set on B.

       // So then, when getting next char, is taken from unchanged date on set A again

    }
}
