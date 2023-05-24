package agency.tango.materialintroscreen;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.data.SettingsFactory;

public class CheckboxSlideFragment extends SlideFragment {

    private final static String BACKGROUND_COLOR = "background_color";
    private static final String BUTTONS_COLOR = "buttons_color";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String IMAGE = "image";
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

    private View background;

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
        bundle.putInt(IMAGE, videoResource);
        bundle.putString(TITLE, title);
        bundle.putString(DESCRIPTION, description);

        bundle.putString(CHECKTEXT_1, text1);
        bundle.putString(CHECK_PROP1, property1);
        bundle.putString(CHECKTEXT_2, text2);
        bundle.putString(CHECK_PROP2, property2);
        bundle.putString(CHECKTEXT_3, text3);
        bundle.putString(CHECK_PROP3, property3);

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
        background = view.findViewById(R.id.slide_background);
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

        check1Text = bundle.getString(CHECKTEXT_1);
        check2Text = bundle.getString(CHECKTEXT_2);
        check3Text = bundle.getString(CHECKTEXT_3);

        check1Prop = bundle.getString(CHECK_PROP1);
        check2Prop = bundle.getString(CHECK_PROP2);
        check3Prop = bundle.getString(CHECK_PROP3);

        updateViewWithValues();
    }

    private void setPref(String pref, boolean value){
        SettingsFactory.get().setBooleanSetting(pref, value);
    }

    private void updateViewWithValues() {
        titleTextView.setText(title);
        descriptionTextView.setText(description);

        String path = "android.resource://" + getContext().getPackageName() + "/" + image;
        imageView.setImageURI(Uri.parse(path));

        if(check1Text != null && check1Prop != null) {
            check1.setText(check1Text);
            check1.setChecked(SettingsFactory.get().getBooleanSetting(check1Prop, true));
            check1.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    boolean checked = ((CheckBox)v).isChecked();
                    setPref(check1Prop, ((CheckBox)v).isChecked());

                    if(!checked) {
                        setPref(check2Prop, false);
                        check2.setChecked(false);
                        check2.setEnabled(false);

                        setPref(check3Prop, false);
                        check3.setChecked(false);
                        check3.setEnabled(false);

                    } else {
                        check2.setEnabled(true);
                        setPref(check2Prop, true);

                        check3.setEnabled(true);
                        setPref(check3Prop, true);
                    }
                }
            });
        } else {
            check1.setVisibility(View.GONE);
        }

        if(check2Text != null && check2Prop != null) {
            check2.setText(check2Text);
            check2.setChecked(SettingsFactory.get().getBooleanSetting(check2Prop, true));
            check2.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    setPref(check2Prop, ((CheckBox)v).isChecked());
                }
            });
        } else {
            check2.setVisibility(View.GONE);
        }

        if(check3Text != null && check3Prop != null) {
            check3.setText(check3Text);
            check3.setChecked(SettingsFactory.get().getBooleanSetting(check3Prop, true));
            check3.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    setPref(check3Prop, ((CheckBox)v).isChecked());
                }
            });
        } else {
            check3.setVisibility(View.GONE);
        }
    }

    public int backgroundColor() {
        return backgroundColor;
    }

    public int buttonsColor() {
        return buttonsColor;
    }
}