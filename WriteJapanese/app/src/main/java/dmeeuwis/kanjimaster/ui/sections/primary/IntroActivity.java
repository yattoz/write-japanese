package dmeeuwis.kanjimaster.ui.sections.primary;

import android.accounts.AccountManager;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import agency.tango.materialintroscreen.ButtonSlideFragment;
import agency.tango.materialintroscreen.CheckboxSlideFragment;
import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragment;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.data.Settings;
import dmeeuwis.kanjimaster.logic.data.SettingsFactory;
import dmeeuwis.kanjimaster.ui.data.SyncRegistration;

public class IntroActivity extends MaterialIntroActivity implements View.OnClickListener {

    public final static String USE_SRS_SETTING_NAME = "use_spaced_repetition";
    public final static String SRS_NOTIFICATION_SETTING_NAME = "use_srs_notifications";
    public final static String SRS_ACROSS_SETS = "use_srs_across_sets";

    public final static String REQUEST_SRS_SETTINGS = "settings_srs";
    public final static String REQUEST_SYNC_SETTINGS = "settings_sync";


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("nakama", "IntroActivity.onActivityResult(" + requestCode + "," + resultCode + "," + data);

        if(requestCode == SyncRegistration.REQUEST_CODE_PICK_ACCOUNT) {
            SyncRegistration.onAccountSelection(this, requestCode, resultCode, data);
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("nakama-intro", "IntroActivity.onCreate");
        Resources r = getResources();

        List<SlideFragment> addedSlides = new ArrayList<>();

        Intent intent = getIntent();

        // show SRS screen if not yet shown, or if specifically requested
        boolean srsNotYetShow = SettingsFactory.get().getBooleanSetting(USE_SRS_SETTING_NAME, null) == null;
        boolean srsRequested = intent != null && intent.getBooleanExtra(REQUEST_SRS_SETTINGS, false);
        boolean showSrs = srsNotYetShow || srsRequested;

        Settings.SyncStatus syncStatus = SettingsFactory.get().getCrossDeviceSyncEnabled();
        boolean syncRequested = intent != null && intent.getBooleanExtra(REQUEST_SYNC_SETTINGS, false);

        boolean isKindle = Build.MANUFACTURER.equals("Amazon");
        boolean showSync = (!syncStatus.asked || syncRequested) && !isKindle;

        int slideCount = (showSrs ? 1 : 0) + (showSync ? 1 : 0);

        if(showSrs){
            Log.i("nakama-intro", "Showing srs screen: " + srsNotYetShow + ", " + srsRequested);
            SlideFragment s =
                CheckboxSlideFragment.createInstance(
                        slideCount == 1 ? r.getColor(R.color.intro_green) : R.color.intro_green,
                        slideCount == 1 ? r.getColor(R.color.intro_blue) : R.color.intro_blue,
                        //r.getColor(R.color.intro_green),
                        //r.getColor(R.color.intro_blue),
                        R.drawable.calendar2,
                        "Spaced Repetition",
                        "Once written correctly a few times, characters will be repeated at increasing timed intervals, across many days, to really lock them in your memory. The built-in schedule repeats after 1, 3, 7, 14, and 30 days.",
                        "Use Spaced Repetition", USE_SRS_SETTING_NAME,
                        "Spaced Repetition reviews override current study-set when ready (cross-set SRS)", SRS_ACROSS_SETS,
                        "Show OS Notifications when characters are due for spaced review", SRS_NOTIFICATION_SETTING_NAME
                );
            addSlide(s);
            addedSlides.add(s);

            // on first view, set defaults
            if(srsNotYetShow) {
                SettingsFactory.get().setBooleanSetting(USE_SRS_SETTING_NAME, true);
                SettingsFactory.get().setBooleanSetting(SRS_ACROSS_SETS, true);
                SettingsFactory.get().setBooleanSetting(SRS_NOTIFICATION_SETTING_NAME, false);
            }
        }


        if(showSync) {
            Log.i("nakama-intro", "Showing sync status screen: " + !syncStatus.asked + ", " + syncRequested);
            SlideFragment s = ButtonSlideFragment.createInstance(
                            //r.getColor(R.color.intro_teal),
                            //r.getColor(R.color.intro_blue),
                            slideCount == 1 ? r.getColor(R.color.intro_teal) : R.color.intro_teal,
                            slideCount == 1 ? r.getColor(R.color.intro_blue) : R.color.intro_blue,
                            R.drawable.device_sync,
                            "Across your devices",
                            "To sync your progress across all your Android devices - or save your progress if you lose your device - click the below button to enable cross-device sync.\n\nYou may be prompted to select which Google account to sync across.",
                            "Sync Across Google Account");
            addSlide(s);
            addedSlides.add(s);
            SettingsFactory.get().setCrossDeviceSyncAsked();
        }

        Log.i("nakama-intro", "After slides, slidesShown is: " + addedSlides.size());

        if(addedSlides.size() == 0){
            Intent redirect = new Intent(this, KanjiMasterActivity.class);
            redirect.putExtra(KanjiMasterActivity.SKIP_INTRO_CHECK, true);
            startActivity(redirect);
        }
    }

    @Override
    public void onClick(View view) {
        // Hackety hack
        startActivityForResult(
                AccountManager.newChooseAccountIntent(
                        null, null, new String[]{"com.google"}, false, null, null, null, null),
                SyncRegistration.REQUEST_CODE_PICK_ACCOUNT);
    }
}
