package dmeeuwis.nakama.primary;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import dmeeuwis.kanjimaster.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class KanjiMasterNavigateAllCharacterSets {

    @Rule
    public ActivityTestRule<KanjiMasterActivity> mActivityTestRule = new ActivityTestRule<>(KanjiMasterActivity.class);

    @Test
    public void kanjiMasterNavigateAllCharacterSets() {
        ViewInteraction appCompatSpinner = onView(
                allOf(withClassName(is("android.support.v7.widget.AppCompatSpinner")),
                        withParent(allOf(withId(R.id.action_bar),
                                withParent(withId(R.id.action_bar_container)))),
                        isDisplayed()));
        appCompatSpinner.perform(click());

        ViewInteraction frameLayout = onView(
                allOf(withClassName(is("android.widget.FrameLayout")), isDisplayed()));
        frameLayout.perform(click());

        ViewInteraction appCompatSpinner2 = onView(
                allOf(withClassName(is("android.support.v7.widget.AppCompatSpinner")),
                        withParent(allOf(withId(R.id.action_bar),
                                withParent(withId(R.id.action_bar_container)))),
                        isDisplayed()));
        appCompatSpinner2.perform(click());

        ViewInteraction frameLayout2 = onView(
                allOf(withClassName(is("android.widget.FrameLayout")), isDisplayed()));
        frameLayout2.perform(click());

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(android.R.id.button2), withText("No Thanks")));
        appCompatButton2.perform(scrollTo(), click());

        ViewInteraction appCompatSpinner3 = onView(
                allOf(withClassName(is("android.support.v7.widget.AppCompatSpinner")),
                        withParent(allOf(withId(R.id.action_bar),
                                withParent(withId(R.id.action_bar_container)))),
                        isDisplayed()));
        appCompatSpinner3.perform(click());

        ViewInteraction frameLayout3 = onView(
                allOf(withClassName(is("android.widget.FrameLayout")), isDisplayed()));
        frameLayout3.perform(click());

        ViewInteraction appCompatSpinner4 = onView(
                allOf(withClassName(is("android.support.v7.widget.AppCompatSpinner")),
                        withParent(allOf(withId(R.id.action_bar),
                                withParent(withId(R.id.action_bar_container)))),
                        isDisplayed()));
        appCompatSpinner4.perform(click());

        ViewInteraction frameLayout4 = onView(
                allOf(withClassName(is("android.widget.FrameLayout")), isDisplayed()));
        frameLayout4.perform(click());

        ViewInteraction appCompatSpinner5 = onView(
                allOf(withClassName(is("android.support.v7.widget.AppCompatSpinner")),
                        withParent(allOf(withId(R.id.action_bar),
                                withParent(withId(R.id.action_bar_container)))),
                        isDisplayed()));
        appCompatSpinner5.perform(click());

        ViewInteraction frameLayout5 = onView(
                allOf(withClassName(is("android.widget.FrameLayout")), isDisplayed()));
        frameLayout5.perform(click());

        ViewInteraction appCompatSpinner6 = onView(
                allOf(withClassName(is("android.support.v7.widget.AppCompatSpinner")),
                        withParent(allOf(withId(R.id.action_bar),
                                withParent(withId(R.id.action_bar_container)))),
                        isDisplayed()));
        appCompatSpinner6.perform(click());

        ViewInteraction frameLayout6 = onView(
                allOf(withClassName(is("android.widget.FrameLayout")), isDisplayed()));
        frameLayout6.perform(click());

        ViewInteraction appCompatSpinner7 = onView(
                allOf(withClassName(is("android.support.v7.widget.AppCompatSpinner")),
                        withParent(allOf(withId(R.id.action_bar),
                                withParent(withId(R.id.action_bar_container)))),
                        isDisplayed()));
        appCompatSpinner7.perform(click());

        ViewInteraction frameLayout7 = onView(
                allOf(withClassName(is("android.widget.FrameLayout")), isDisplayed()));
        frameLayout7.perform(click());

        ViewInteraction appCompatSpinner8 = onView(
                allOf(withClassName(is("android.support.v7.widget.AppCompatSpinner")),
                        withParent(allOf(withId(R.id.action_bar),
                                withParent(withId(R.id.action_bar_container)))),
                        isDisplayed()));
        appCompatSpinner8.perform(click());

        ViewInteraction frameLayout8 = onView(
                allOf(withClassName(is("android.widget.FrameLayout")), isDisplayed()));
        frameLayout8.perform(click());

    }

}
