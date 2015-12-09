package dmeeuwis.nakama.primary;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.CharacterStudySet;

public class LockableArrayAdapter extends ArrayAdapter<CharacterStudySet> {
    private List<CharacterStudySet> data;
    private final double screenWidthInches;

    public LockableArrayAdapter(Context context, List<CharacterStudySet> objects) {
        super(context, R.layout.locked_list_item_layout, R.id.text, objects);
        this.data = objects;

        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
        this.screenWidthInches = Math.sqrt(x + y);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        if (row == null) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            row = inflater.inflate(R.layout.locked_list_item_layout, parent, false);
        }

        CharacterStudySet d = data.get(position);
        ImageView lockIcon = (ImageView) row.findViewById(R.id.lock);
        lockIcon.getDrawable().setAlpha(255);
        boolean lockIconVisible = d.locked();
        lockIcon.setVisibility(lockIconVisible ? View.VISIBLE : View.INVISIBLE);
        String text;
        if(screenWidthInches > 5.0d) {
            text = d.name + " (" + d.length() + ")";
        } else {
            text = d.shortName + " (" + d.length() + ")";
        }
        ((TextView) row.findViewById(R.id.text)).setText(text);
        return row;
    }
}
