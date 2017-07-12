package agency.tango.materialintroscreen;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.VideoView;

public class VideoSlideFragment extends SlideFragment {

    private final static String BACKGROUND_COLOR = "background_color";
    private static final String BUTTONS_COLOR = "buttons_color";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String VIDEO = "video";

    private int backgroundColor;
    private int buttonsColor;
    private int image;
    private String title;
    private String description;

    private TextView titleTextView;
    private TextView descriptionTextView;
    private VideoView imageView;

    public static VideoSlideFragment createInstance(int backgroundColor, int buttonsColor, int videoResource, String title, String description) {
        VideoSlideFragment slideFragment = new VideoSlideFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(BACKGROUND_COLOR, backgroundColor);
        bundle.putInt(BUTTONS_COLOR, buttonsColor);
        bundle.putInt(VIDEO, videoResource);
        bundle.putString(TITLE, title);
        bundle.putString(DESCRIPTION, description);

        slideFragment.setArguments(bundle);
        return slideFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(dmeeuwis.kanjimaster.R.layout.fragment_video_slide, container, false);
        titleTextView = (TextView) view.findViewById(dmeeuwis.kanjimaster.R.id.txt_title_slide);
        descriptionTextView = (TextView) view.findViewById(dmeeuwis.kanjimaster.R.id.txt_description_slide);
        imageView = (VideoView) view.findViewById(dmeeuwis.kanjimaster.R.id.video_slide);
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                imageView.seekTo(0);
                imageView.start();

                return true;
            }
        });
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

        updateViewWithValues();
    }

    private void updateViewWithValues() {
        titleTextView.setText(title);
        descriptionTextView.setText(description);

        String path = "android.resource://" + getContext().getPackageName() + "/" + dmeeuwis.kanjimaster.R.raw.correct_draw;
        imageView.setVideoURI(Uri.parse(path));
        imageView.start();
    }


    public int backgroundColor() {
        return backgroundColor;
    }

    public int buttonsColor() {
        return buttonsColor;
    }
}