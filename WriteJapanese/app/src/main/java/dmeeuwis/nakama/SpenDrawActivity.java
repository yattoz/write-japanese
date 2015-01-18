package dmeeuwis.nakama;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pen.Spen;

public class SpenDrawActivity extends ActionBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Spen spenPackage = new Spen();
        boolean spenEnabled = false;
        try {
            spenPackage.initialize(this);
            spenEnabled = spenPackage.isFeatureEnabled(Spen.DEVICE_PEN);
        } catch(SsdkUnsupportedException e){
            Toast.makeText(this, "SsdkUnsupportedException", Toast.LENGTH_SHORT).show();
            finish();
        }

        LinearLayout ll = new LinearLayout(new )
    }

}