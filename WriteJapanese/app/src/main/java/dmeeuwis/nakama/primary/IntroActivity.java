package dmeeuwis.nakama.primary;

import android.os.Bundle;

import agency.tango.materialintroscreen.CheckboxSlideFragment;
import agency.tango.materialintroscreen.MaterialIntroActivity;
import dmeeuwis.kanjimaster.R;

public class IntroActivity extends MaterialIntroActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(CheckboxSlideFragment.createInstance(R.color.intro_blue, R.color.LightBlue, android.R.drawable.ic_lock_idle_alarm,
                "Spaced Repetition (SRS)",
                "Once written correctly, characters will be repeated at increasing timed intervals, across many days. This time repetition is often known as a 'spaced repetition system' (SRS).",
                "Used Spaced Repetition", "use_spaced_repetition",
                "Show Notifications when characters are due for review", "use_srs_notifications",
                "Review characters even across sets, when due for SRS", "use_srs_across_sets"
        ));
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
                .backgroundColor(R.color.intro_teal)
                .buttonsColor(R.color.LightBlue)
                .title("Across your devices")
                .description("To sync your progress across standardSets your devices - or save your progress if you lose your device - click the below button to enable cross device sync.\n\nYou may be prompted to select a Google account.")
                .possiblePermissions(new String[] { Manifest.permission.ACCOUNT_MANAGER } )
                .build(),
                new MessageButtonBehaviour(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showMessage("We provide solutions to make you love your work");
                    }
                }, "Enable Cross Device Sync"));

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.intro_green)
                .buttonsColor(R.color.LightBlue)
                .title("Good luck!")
                .description("Good luck with your studies in Japanese!")
                .build());
    */
    }
}
