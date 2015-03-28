package dmeeuwis.nakama.views;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NetworkStoriesAsyncTask extends AsyncTask<Character, String, List<String>> {

    private final AddString sa;
    private final Character c;

    public interface AddString {
        public void add(String s);
    }

    public NetworkStoriesAsyncTask(Character c, AddString sa){
        this.c = c;
        this.sa = sa;
    }

    @Override
    protected List<String> doInBackground(Character... params) {
        try {
            URL url = new URL("http://dmeeuwis.com/write-japanese/stories/" + c);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                urlConnection.setRequestMethod("GET");
                int statusCode = urlConnection.getResponseCode();

                    /* 200 represents HTTP OK */
                if (statusCode == 200) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        response.append(line);
                    }

                    JSONArray jar = new JSONArray(response.toString());
                    List<String> storyList = new ArrayList<>(jar.length());
                    for (int i = 0; i < jar.length(); i++) {
                        storyList.add(jar.getString(i));
                    }
                } else {
                    // indicate in UI, could not connect to network
                }

            } finally {
               urlConnection.disconnect();
            }

        } catch (Exception e) {
            Log.d("nakama", e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(final List<String> result) {
        for(String s: result){
            sa.add(s);
        }
    }
}
