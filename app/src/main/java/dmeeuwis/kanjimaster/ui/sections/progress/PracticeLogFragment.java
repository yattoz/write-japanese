package dmeeuwis.kanjimaster.ui.sections.progress;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dmeeuwis.kanjimaster.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class PracticeLogFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    private RecyclerView recyclerView;
    private List<PracticeLogAsyncTask.PracticeLog> practiceLogs = new ArrayList<>();
    private MyPracticeLogRecyclerViewAdapter ad;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PracticeLogFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static PracticeLogFragment newInstance(int columnCount) {
        PracticeLogFragment fragment = new PracticeLogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_practice_log_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            this.recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                this.recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                this.recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            this.ad = new MyPracticeLogRecyclerViewAdapter(practiceLogs, mListener);
            this.recyclerView.setAdapter(ad);
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setData(List<PracticeLogAsyncTask.PracticeLog> logs) {
        if(this.recyclerView != null) {
            this.recyclerView.setAdapter(new MyPracticeLogRecyclerViewAdapter(logs, mListener));
        }
        this.practiceLogs = logs;
        Log.i("nakama", "Practice log fragment receiving " + logs.size() + " logs.");
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(PracticeLogAsyncTask.PracticeLog item);
    }
}
