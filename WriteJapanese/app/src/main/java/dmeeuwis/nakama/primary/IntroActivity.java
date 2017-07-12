package dmeeuwis.nakama.primary;

import android.Manifest;
import android.os.Bundle;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragmentBuilder;
import agency.tango.materialintroscreen.VideoSlideFragment;
import dmeeuwis.kanjimaster.R;


public class IntroActivity extends MaterialIntroActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(VideoSlideFragment.createInstance(R.color.intro_blue, R.color.LightBlue, R.raw.correct_draw,
                        "Welcome to Write Japanese",
                        "Learn to draw the Japanese writing systems: Hiragana, Katakana, and the Kanji.\n\nWrite Japanese uses a time-based repetition (aka. spaced repetition system) to manage your learning. It helps you study the right characters, when you need to."
                ));

        addSlide(new SlideFragmentBuilder()
            .backgroundColor(R.color.intro_teal)
                .buttonsColor(R.color.LightBlue)
            .title("Draw the Character")
            .description("At the main screen, draw the character indicated in the instruction card. You can choose to be prompted based on character meaning, readings, or vocab examples.")
            .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.intro_green)
                .buttonsColor(R.color.LightBlue)
                .title("You got it!")
                .description("If you've drawn correctly, you'll see a green screen with some example usages of the character. Then continue on to the next!")
                .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.intro_red)
                .buttonsColor(R.color.LightBlue)
                .title("Not so fast!")
                .description("If incorrect, you'll have a chance to review in study mode. Trace the character, and make a story to help you remember it for next time.")
                .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.intro_teal)
                .buttonsColor(R.color.LightBlue)
                .title("Across your devices")
                .description("To sync your progress across all your devices - or save your progress if you lose your device - click the below button to enable cross device sync.\n\nYou may be prompted to select a Google account.")
                .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.intro_green)
                .buttonsColor(R.color.LightBlue)
                .title("Good luck!")
                .description("Good luck with your studies in Japanese!")
                .build());
    }
}
