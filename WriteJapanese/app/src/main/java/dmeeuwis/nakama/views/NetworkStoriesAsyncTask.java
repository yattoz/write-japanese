package dmeeuwis.nakama.views;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NetworkStoriesAsyncTask extends AsyncTask<Character, String, List<String>> {

    private final AddString sa;
    private final Character c;
    private final UUID iid;

    public interface AddString {
        public void add(String s);
    }

    public NetworkStoriesAsyncTask(Character c, UUID iid, AddString sa){
        this.c = c;
        this.sa = sa;
        this.iid = iid;
    }

    @Override
    protected List<String> doInBackground(Character... params) {
        try {
            URL url = new URL("http://dmeeuwis.com/write-japanese/stories/" + c + "?iid=" + iid);
            Log.i("nakama", "Network: Starting network request for: " + url);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                urlConnection.setRequestMethod("GET");
                int statusCode = urlConnection.getResponseCode();
                Log.d("nakama", "Network: saw response " + statusCode + " for " + c);

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
                    return storyList;
                } else {
                    Map<String, List<String>> map = urlConnection.getHeaderFields();
                    Log.e("nakama", "Error response message: " + urlConnection.getResponseMessage());
                    Log.e("nakama", "Error response headers: " + Arrays.toString(map.entrySet().toArray()));
                    return Arrays.asList("Network error: " + statusCode);
                }

            } finally {
               urlConnection.disconnect();
            }

        } catch (Exception e) {
            Log.d("nakama", "Network: Error reading network stories for " + c, e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(final List<String> result) {
        if(result == null){
            sa.add("Network error");
            return;
        }

        for(String s: result){
            Log.i("nakama", "Adding network story: " + s);
            sa.add(s);
        }
    }
}
