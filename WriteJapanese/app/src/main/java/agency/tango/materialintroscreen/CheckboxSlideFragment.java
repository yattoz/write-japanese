package agency.tango.materialintroscreen;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import dmeeuwis.kanjimaster.*;
import dmeeuwis.kanjimaster.R;

public class CheckboxSlideFragment extends SlideFragment {

    private final static String BACKGROUND_COLOR = "background_color";
    private static final String BUTTONS_COLOR = "buttons_color";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String VIDEO = "video";
    private static final String CHECKTEXT_1 = "checktext_1";
    private static final String CHECKTEXT_2 = "checktext_2";
    private static final String CHECKTEXT_3 = "checktext_3";
    private static final String CHECK_PROP1 = "checkprop_1";
    private static final String CHECK_PROP2 = "checkprop_2";
    private static final String CHECK_PROP3 = "checkprop_3";

    private int backgroundColor;
    private int buttonsColor;
    private int image;
    private String title;
    private String description;

    private TextView titleTextView;
    private TextView descriptionTextView;
    private ImageView imageView;

    private CheckBox check1;
    private CheckBox check2;
    private CheckBox check3;

    private String check1Text, check2Text, check3Text;
    private String check1Prop, check2Prop, check3Prop;

    public static CheckboxSlideFragment createInstance(int backgroundColor, int buttonsColor, int videoResource, String title, String description,
            String text1, String property1, String text2, String property2, String text3, String property3){

        CheckboxSlideFragment slideFragment = new CheckboxSlideFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(BACKGROUND_COLOR, backgroundColor);
        bundle.putInt(BUTTONS_COLOR, buttonsColor);
        bundle.putInt(VIDEO, videoResource);
        bundle.putString(TITLE, title);
        bundle.putString(DESCRIPTION, description);

        bundle.putString(CHECKTEXT_1, text1);
        bundle.putString(CHECK_PROP1, property1);
        bundle.putString(CHECKTEXT_2, text2);
        bundle.putString(CHECK_PROP1, property2);
        bundle.putString(CHECKTEXT_3, text3);
        bundle.putString(CHECK_PROP1, property3);

        slideFragment.setArguments(bundle);
        return slideFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_slide_check, container, false);
        titleTextView = (TextView) view.findViewById(dmeeuwis.kanjimaster.R.id.txt_title_slide);
        descriptionTextView = (TextView) view.findViewById(dmeeuwis.kanjimaster.R.id.txt_description_slide);
        imageView = (ImageView) view.findViewById(R.id.checkbox_image_slide);
        check1 = (CheckBox) view.findViewById(R.id.fragment_check1);
        check2 = (CheckBox) view.findViewById(R.id.fragment_check2);
        check3 = (CheckBox) view.findViewById(R.id.fragment_check3);
        initializeView();
        return view;
    }

    public void initializeView() {
        Bundle bundle = getArguments();
        backgroundColor = bundle.getInt(BACKGROUND_COLOR);
        buttonsColor = bundle.getInt(BUTTONS_COLOR);
        image = bundle.getInt(VIDEO, 0);
        title = bundle.getString(TITLE);
        description = bundle.getString(DESCRIPTION);

        check1Text = bundle.getString(CHECKTEXT_1);
        check2Text = bundle.getString(CHECKTEXT_2);
        check3Text = bundle.getString(CHECKTEXT_3);

        check1Prop = bundle.getString(CHECK_PROP1);
        check2Prop = bundle.getString(CHECK_PROP2);
        check3Prop = bundle.getString(CHECK_PROP3);

        updateViewWithValues();
    }

    private void updateViewWithValues() {
        titleTextView.setText(title);
        descriptionTextView.setText(description);

        imageView.setImageURI();

//        String path = "android.resource://" + getContext().getPackageName() + "/" + dmeeuwis.kanjimaster.R.raw.correct_draw;
//        imageView.setVideoURI(Uri.parse(path));
//        imageView.start();
    }


    public int backgroundColor() {
        return backgroundColor;
    }

    public int buttonsColor() {
        return buttonsColor;
    }
}