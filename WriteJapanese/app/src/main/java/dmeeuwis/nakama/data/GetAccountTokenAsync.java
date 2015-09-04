package dmeeuwis.nakama.data;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.IOException;

public class GetAccountTokenAsync extends AsyncTask<Void, Void, String> {
    final Activity mActivity;
    final String mEmail;
    final RunWithAuthcode post;

    // android id
    //final static String SCOPE = "audience:server:client_id:120575778353-114alu4i9r3qm9fehap16ks172pshmne.apps.googleusercontent.com";

    // service id
    final static String SCOPE = "audience:server:client_id:120575778353-iv0lut9pspdt19qluq5n14g9tq2k17ch.apps.googleusercontent.com";
    public final int AUTH_REQUEST_ACTIVITY_CODE = 0x7832;

    public interface RunWithAuthcode {
        void exec(String authcode);
    }

    public GetAccountTokenAsync(Activity activity, String name, RunWithAuthcode post) {
        this.mActivity = activity;
        this.mEmail = name;
        this.post = post;
    }

    /**
     * Executes the asynchronous job. This runs when you call execute()
     * on the AsyncTask instance.
     */
    @Override
    protected String doInBackground(Void... params) {
        Log.i("nakama-auth", "GetAccountTokenAsync: doInBackground getting token for: " + mActivity + ", " + mEmail + ", " + SCOPE);
        try {
            String token = GoogleAuthUtil.getToken(mActivity, mEmail, SCOPE);
            Log.i("nakama-auth", "Found auth token as: " + token);
            return token;
        } catch (UserRecoverableAuthException userAuthEx) {
            // Start the user recoverable action using the intent returned by getIntent()
            mActivity.startActivityForResult( userAuthEx.getIntent(), AUTH_REQUEST_ACTIVITY_CODE);
            return null;
        } catch (Exception e) {
            Log.e("nakama-auth", "GetAccountTokenAsync: caught exception fetching token", e);
            // The fetchToken() method handles Google-specific exceptions,
            // so this indicates something went wrong at a higher level.
            // TIP: Check for network connectivity before starting the AsyncTask.
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        this.post.exec(result);
    }
}
