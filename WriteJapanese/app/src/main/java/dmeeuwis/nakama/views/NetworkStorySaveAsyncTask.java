package dmeeuwis.nakama.views;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;

import dmeeuwis.nakama.data.UncaughtExceptionLogger;

public class NetworkStorySaveAsyncTask extends AsyncTask<Character, String, Void> {

    private final Character c;
    private final String story;
    private final UUID installId;

    public NetworkStorySaveAsyncTask(Character c, String story, UUID installId){
        this.c = c;
        this.story = story;
        this.installId = installId;
    }

    @Override
    protected Void doInBackground(Character... params) {
        try {
            URL url = new URL("https://dmeeuwis.com/write-japanese/stories/" + URLEncoder.encode(c.toString(), "UTF-8") + "?iid=" + installId);
            Log.i("nakama", "Saving story to: " + url);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");

                OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
                wr.write(story);
                wr.flush();

                int statusCode = urlConnection.getResponseCode();
                Log.e("nakama", "Save story for " + c + " received " + statusCode + " response: " + urlConnection.getResponseMessage());
            } finally {
                urlConnection.disconnect();
            }

        } catch (Exception e) {
            UncaughtExceptionLogger.backgroundLogError("Caught exception in background story save", e, null);
        }
        return null;
    }
}
