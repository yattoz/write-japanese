package dmeeuwis.nakama.views;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

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
            URL url = new URL("http://dmeeuwis.com/write-japanese/stories/" + c + "?iid=" + installId);
            Log.i("nakama", "Saving story to: " + url);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");

                OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
                wr.write(story);
                wr.flush();

                int statusCode = urlConnection.getResponseCode();
                Log.i("nakama", "Save story for " + c + " received " + statusCode + " response.");
            } finally {
                urlConnection.disconnect();
            }

        } catch (Exception e) {
            Log.d("nakama", e.getLocalizedMessage());
        }
        return null;
    }
}
