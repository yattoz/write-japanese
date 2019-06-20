package dmeeuwis.kanjimaster.logic.data;

import android.content.Context;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import dmeeuwis.kanjimaster.BuildConfig;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class PracticeLogSyncTest {

    public PracticeLogSyncTest(){

    }

    private static class TestDependencies extends PracticeLogSync.ExternalDependencies {
        private final String file;
        public TestDependencies(Context ctx, String jsonFile){
            super(ctx);
            this.file = jsonFile;
        }
        @Override
        public InputStream sendPost(String jsonPost) throws Exception {
            return new StringInputStream(readTestData(file));
        }
    }

    @Test
    public void testEmptySync() throws IOException {
        PracticeLogSync sync = new PracticeLogSync(
                new TestDependencies(RuntimeEnvironment.application, "empty_sync.json"),
                RuntimeEnvironment.application);
        sync.sync();
    }

    @Test
    public void testEmptySyncBeforeCharsetEdits() throws IOException {
        PracticeLogSync sync = new PracticeLogSync(
                new TestDependencies(RuntimeEnvironment.application, "empty_sync_no_charset_edits.json"),
                RuntimeEnvironment.application);
        sync.sync();
    }

    @Test
    public void testMultipleCharsetGoalBug() throws IOException {
        PracticeLogSync sync = new PracticeLogSync(
                new TestDependencies(RuntimeEnvironment.application, "multiple_charset_goals_sync.json"),
                RuntimeEnvironment.application);
        sync.sync();
    }

    // test practice logs sync

    // test charset goals edit sync

    // test charset edits sync


    public static String readTestData(String filename) throws Exception {
        java.net.URL url = PracticeLogSyncTest.class.getResource(filename);
        if(url == null){ throw new RuntimeException("Cannot get test resource data"); }
        File f = new File(url.toURI());
        if(!f.exists()){ throw new RuntimeException("Can't find test data: " + f); };
        String json = new java.util.Scanner(f, "UTF-8").useDelimiter("\\Z").next();
        System.out.println("Read test input json as: " + json);
        return json;
    }
}
