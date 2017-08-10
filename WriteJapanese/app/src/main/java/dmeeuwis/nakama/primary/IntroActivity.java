package dmeeuwis.nakama.primary;

import android.Manifest;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.view.View;

import agency.tango.materialintroscreen.CheckboxSlideFragment;
import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.MessageButtonBehaviour;
import agency.tango.materialintroscreen.SlideFragmentBuilder;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.SyncRegistration;

public class IntroActivity extends MaterialIntroActivity {

    public final static String USE_SRS_SETTING_NAME = "use_spaced_repetition";
    public final static String SRS_NOTIFICATION_SETTING_NAME = "use_srs_notifications";
    public final static String SRS_ACROSS_SETS = "use_srs_across_sets";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(CheckboxSlideFragment.createInstance(R.color.intro_teal, R.color.intro_blue, R.drawable.ic_calendar2,
                "Spaced Repetition (SRS)",
                "Once written correctly, characters will be repeated at increasing timed intervals, across many days. This time repetition is often known as a 'spaced repetition system' (SRS). The built-in schedule repeats after 1, 3, 7, 14, and 30 days.",
                "Use Spaced Repetition", USE_SRS_SETTING_NAME,
                "Notify when characters are due for review", SRS_NOTIFICATION_SETTING_NAME,
                "Allow SRS repetitions to display even while studying in a different character set", SRS_ACROSS_SETS
        ));

        addSlide(new SlideFragmentBuilder()
                        .backgroundColor(R.color.intro_teal)
                        .image(R.drawable.device_sync_layered)
                        .buttonsColor(R.color.intro_blue)
                        .title("Across your devices")
                        .description("To sync your progress across all your Android devices - or save your progress if you lose your device - click the below button to enable cross device sync.\n\nYou may be prompted to select which Google account to sync across.")
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
