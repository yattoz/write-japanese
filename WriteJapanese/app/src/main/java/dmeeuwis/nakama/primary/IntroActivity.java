package dmeeuwis.nakama.primary;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import agency.tango.materialintroscreen.CheckboxSlideFragment;
import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.MessageButtonBehaviour;
import agency.tango.materialintroscreen.SlideFragmentBuilder;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.Settings;
import dmeeuwis.nakama.data.SyncRegistration;
import dmeeuwis.nakama.data.UncaughtExceptionLogger;

public class IntroActivity extends MaterialIntroActivity {

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

        int slidesShown = 0;

        Intent intent = getIntent();

        // show SRS screen if not yet shown, or if specifically requested
        boolean srsNotYetShow = Settings.getBooleanSetting(getApplicationContext(), USE_SRS_SETTING_NAME, null) == null;
        boolean srsRequested = intent != null && intent.getBooleanExtra(REQUEST_SRS_SETTINGS, false);

        if(srsNotYetShow || srsRequested){
            Log.i("nakama-intro", "Showing srs screen: " + srsNotYetShow + ", " + srsRequested);
            addSlide(CheckboxSlideFragment.createInstance(R.color.intro_teal, R.color.intro_blue, R.drawable.ic_calendar2,
                    "Spaced Repetition",
                    "Once written correctly a few times, characters will be repeated at increasing timed intervals, across many days, to really lock them in your memory. The built-in schedule repeats after 1, 3, 7, 14, and 30 days.",
                    "Use Spaced Repetition", USE_SRS_SETTING_NAME,
                    "Show spaced repetition characters to display even while studying in a different character set", SRS_ACROSS_SETS,
                    "Show OS Notifications when characters are due for review", SRS_NOTIFICATION_SETTING_NAME
            ));
            slidesShown++;

            // on first view, set defaults
            if(srsNotYetShow) {
                Settings.setBooleanSetting(getApplicationContext(), USE_SRS_SETTING_NAME, true);
                Settings.setBooleanSetting(getApplicationContext(), SRS_ACROSS_SETS, true);
                Settings.setBooleanSetting(getApplicationContext(), SRS_NOTIFICATION_SETTING_NAME, false);
            }
        }

        Settings.SyncStatus syncStatus = Settings.getCrossDeviceSyncEnabled(getApplicationContext());
        boolean syncRequested = intent != null && intent.getBooleanExtra(REQUEST_SYNC_SETTINGS, false);

        if(!syncStatus.asked || syncRequested) {
            Log.i("nakama-intro", "Showing sync status screen: " + !syncStatus.asked + ", " + syncRequested);
            addSlide(new SlideFragmentBuilder()
                            .backgroundColor(R.color.intro_teal)
                            .image(R.drawable.device_sync_layered)
                            .buttonsColor(R.color.intro_blue)
                            .title("Across your devices")
                            .description("To sync your progress across all your Android devices - or save your progress if you lose your device - click the below button to enable cross-device sync.\n\nYou may be prompted to select which Google account to sync across.")
                            .build(),
                    new MessageButtonBehaviour(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivityForResult(
                                    AccountManager.newChooseAccountIntent(
                                            null, null, new String[]{"com.google"}, false, null, null, null, null),
                                    SyncRegistration.REQUEST_CODE_PICK_ACCOUNT);
                            return;
                        }
                    }, "Choose Account for Sync"));


            Settings.setCrossDeviceSyncAsked(getApplicationContext());
            slidesShown++;
        }

        Log.i("nakama-intro", "After slides, slidesShown is: " + slidesShown);
        if(slidesShown == 0){
            Log.i("nakama-intro", "Showing EMERGENCY SCREEN, WHY? slidesShown=" + slidesShown);
            // fake to satisfy library
            // should never happen?
            UncaughtExceptionLogger.backgroundLogError("Error: no slides added on IntroActivity", new RuntimeException(), this);
            addSlide(new SlideFragmentBuilder()
                    .backgroundColor(R.color.intro_green)
                    .buttonsColor(R.color.LightBlue)
                    .title("Good luck!")
                    .description("Good luck with your studies in Japanese!")
                    .build());

        }
/*
        addSlide(VideoSlideFragment.createInstance(R.color.intro_blue, R.color.LightBlue, R.raw.correct_draw,
                "Welcome to Write Japanese",
                "Learn to draw the Japanese writing systems: Hiragana, Katakana, and the Kanji.\n\nDraw the character indicated in the instruction card. You can choose to be prompted based on character meaning, readings, or vocab examples.\n\nPress the 'back' button while drawing to undo a mistaken stroke."
        ));

        addSlide(VideoSlideFragment.createInstance(R.color.intro_teal, R.color.LightBlue, R.raw.correct_draw,
                "Timed Repetition",
                "Write Japanese uses a time-based repetition (aka. spaced repetition system) to manage your learning. It helps you study the right characters, when you need to."
        ));

        addSlide(VideoSlideFragment.createInstance(R.color.intro_green, R.color.LightBlue, R.raw.correct_draw,
                "You got it!",
                "If you've drawn correctly, you'll see a happy green screen with vocab examples of the character. Then continue on to the next!"));

        addSlide(VideoSlideFragment.createInstance(R.color.intro_red, R.color.LightBlue, R.raw.incorrect_demo,
                "Not so fast!",
                "If incorrect, you'll see an angry red screen, and have a chance to review in study mode. Trace the character, and make a story to help you remember it for next time."));

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.intro_green)
                .buttonsColor(R.color.LightBlue)
                .title("Good luck!")
                .description("Good luck with your studies in Japanese!")
                .build());
    */
    }
}
