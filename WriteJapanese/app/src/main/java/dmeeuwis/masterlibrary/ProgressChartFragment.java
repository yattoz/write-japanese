package dmeeuwis.masterlibrary;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import dmeeuwis.kanjimaster.R;

public class ProgressChartFragment extends Fragment {

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.progress_chart, container, false);
		return view;
	}
}
