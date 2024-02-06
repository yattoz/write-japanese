package dmeeuwis.kanjimaster.ui.views;

import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONException;
import org.json.JSONObject;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.data.UncaughtExceptionLogger;

public class ReportBugDialog extends DialogFragment implements DialogInterface.OnClickListener{

    public final static String DEBUG_DATA_BUNDLE_KEY = "debugData";

    public ReportBugDialog(){
        // empty constructor
    }

    CheckBox useDebugDataCheckbox;
    EditText userMessage;
    String debugData;

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle b = getArguments();
        debugData = b.getString(DEBUG_DATA_BUNDLE_KEY, "");
        final FrameLayout frameView = new FrameLayout(getActivity());
        Dialog d = new AlertDialog.Builder(getActivity())
                .setTitle("Report a Bug")
                .setView(frameView)
                .setPositiveButton("Send", this)
                .setNegativeButton("Cancel", null).create();

        LayoutInflater inflater = d.getLayoutInflater();
        View v = inflater.inflate(R.layout.fragment_report_bug, frameView);

        useDebugDataCheckbox = v.findViewById(R.id.debug_dialog_check);
        userMessage = v.findViewById(R.id.debug_user_message);

        return d;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        if(which == BUTTON_POSITIVE){
            Log.d("nakama", "Use debug data? " + useDebugDataCheckbox.isChecked());
            try {
                String json = useDebugDataCheckbox.isChecked() ? debugData : "{ \"debugData\": \"debug data not approved\" }";
                JSONObject j = new JSONObject(json);
                j.accumulate("user-message", userMessage.getText());
                UncaughtExceptionLogger.backgroundLogBugReport(j.toString(4));

                Toast.makeText(getContext(), "Bug report submitted. Thank you!", Toast.LENGTH_LONG).show();
            } catch (JSONException e) {
                UncaughtExceptionLogger.backgroundLogError("Error generating bug-report json! How ironic.", e);
            }
        }
        this.dismiss();
    }
}
