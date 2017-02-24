package dmeeuwis.nakama.primary;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.CharacterStudySet;

public class LockableArrayAdapter extends ArrayAdapter<LockableArrayAdapter.CharsetLabel> {
    private final List<CharsetLabel> data;
    private final double screenWidthInches;

    public static class CharsetLabel {
        public final String name;
        public final String shortName;
        public final int length;
        public final boolean locked;

        public CharsetLabel(String name, String shortName, int length, boolean locked){
            this.name = name;
            this.shortName = shortName;
            this.length = length;
            this.locked = locked;
        }

    }

    private static List<CharsetLabel> convertToLabels(List<CharacterStudySet> charsets){
        List<CharsetLabel> labels = new ArrayList<>(charsets.size() + 1);
        for(CharacterStudySet c: charsets){
            labels.add(new CharsetLabel(c.name, c.shortName, c.length(), c.locked()));
        }
        labels.add(new CharsetLabel("Make Custom Set", "Make Custom Set", 0, false));
        return labels;
    }

    public LockableArrayAdapter(Context context, List<CharacterStudySet> objects) {
        super(context, R.layout.locked_list_item_layout, R.id.text, convertToLabels(objects));

        List<CharsetLabel> labels = convertToLabels(objects);
        this.data = labels;

        DisplayMetrics dm = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
        this.screenWidthInches = Math.sqrt(x + y);
    }

    @Override
    public void add(CharsetLabel c){
        data.add(c);
        super.add(c);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent, false);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent, true);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent, boolean expanded) {
        View row = convertView;

        if (row == null) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            row = inflater.inflate(R.layout.locked_list_item_layout, parent, false);
        }

        ImageView lockIcon = (ImageView) row.findViewById(R.id.lock);
        TextView textView = ((TextView) row.findViewById(R.id.text));
        if(expanded){
            row.setBackgroundColor(getContext().getResources().getColor(R.color.White));
            textView.setTextColor(getContext().getResources().getColor(R.color.Black));
            lockIcon.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_lock_gray));
        }

        View divider = row.findViewById(R.id.locked_list_item_divider);
        final int NUMBER_OF_BUILTIN_SETS = 8;
        if(position == NUMBER_OF_BUILTIN_SETS || position == data.size() - 1){
            divider.setVisibility(View.VISIBLE);
        } else {
            divider.setVisibility(View.GONE);
        }

        View addButton = row.findViewById(R.id.locked_list_item_add_icon);
        if(position == data.size()-1){
            addButton.setVisibility(View.VISIBLE);
        } else {
            addButton.setVisibility(View.GONE);
        }


        CharsetLabel d = data.get(position);
        lockIcon.getDrawable().setAlpha(255);
        boolean lockIconVisible = d.locked;
        lockIcon.setVisibility(lockIconVisible ? View.VISIBLE : View.GONE);
        String text;
        String countText = d.length == 0 ? "" : " (" + d.length + ")";
        if(expanded || screenWidthInches > 5.0d) {
            text = d.name + countText;
        } else {
            text = d.shortName + countText;
        }
        textView.setText(text);
        return row;
    }
}
