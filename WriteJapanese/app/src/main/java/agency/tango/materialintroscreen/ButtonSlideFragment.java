package agency.tango.materialintroscreen;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import dmeeuwis.kanjimaster.*;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.Settings;

public class ButtonSlideFragment extends SlideFragment {

    private final static String BACKGROUND_COLOR = "background_color";
    private static final String BUTTONS_COLOR = "buttons_color";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String IMAGE = "image";
    private static final String BUTTON_LABEL = "buttonLabel";

    private int backgroundColor;
    private int buttonsColor;
    private int image;
    private String title;
    private String description;
    private String buttonLabel;

    private TextView titleTextView;
    private TextView descriptionTextView;
    private ImageView imageView;

    private Button buttonView;


    public static ButtonSlideFragment createInstance(int backgroundColor, int buttonsColor, int videoResource, String title, String description,
                                                       String buttonMessage){

        ButtonSlideFragment slideFragment = new ButtonSlideFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(BACKGROUND_COLOR, backgroundColor);
        bundle.putInt(BUTTONS_COLOR, buttonsColor);
        bundle.putInt(IMAGE, videoResource);
        bundle.putString(TITLE, title);
        bundle.putString(DESCRIPTION, description);
        bundle.putSerializable(BUTTON_LABEL, buttonMessage);

        slideFragment.setArguments(bundle);
        return slideFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_slide_button, container, false);
        titleTextView = (TextView) view.findViewById(dmeeuwis.kanjimaster.R.id.txt_title_slide);
        descriptionTextView = (TextView) view.findViewById(dmeeuwis.kanjimaster.R.id.txt_description_slide);
        imageView = (ImageView) view.findViewById(R.id.checkbox_image_slide);
        buttonView = (Button) view.findViewById(R.id.button_slide_button);
        initializeView();
        return view;
    }

    public void initializeView() {
        Bundle bundle = getArguments();
        backgroundColor = bundle.getInt(BACKGROUND_COLOR);
        buttonsColor = bundle.getInt(BUTTONS_COLOR);
        image = bundle.getInt(IMAGE, 0);
        title = bundle.getString(TITLE);
        description = bundle.getString(DESCRIPTION);
        buttonLabel = bundle.getString(BUTTON_LABEL);

        // hard assumption that only one button slide in the current activity. Hackety hack.
        buttonView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                ((View.OnClickListener)getActivity()).onClick(view);
            }
        });
        updateViewWithValues();
    }

    private void updateViewWithValues() {
        //background.setBackgroundColor(backgroundColor | 0xFF000000);
        //background.setBackgroundColor(backgroundColor);
        titleTextView.setText(title);
        descriptionTextView.setText(description);

        String path = "android.resource://" + getContext().getPackageName() + "/" + image;
        imageView.setImageURI(Uri.parse(path));

        buttonView.setText(buttonLabel);
    }

    public int backgroundColor() {
        return backgroundColor;
    }

    public int buttonsColor() {
        return buttonsColor;
    }
}