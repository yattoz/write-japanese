package dmeeuwis.kanjimaster.ui.sections.progress;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.drawing.Criticism;
import dmeeuwis.kanjimaster.logic.drawing.PointDrawing;
import dmeeuwis.kanjimaster.ui.sections.progress.PracticeLogFragment.OnListFragmentInteractionListener;
import dmeeuwis.kanjimaster.ui.views.AnimatedCurveView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link dmeeuwis.kanjimaster.ui.sections.progress.PracticeLogAsyncTask.PracticeLog} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyPracticeLogRecyclerViewAdapter extends RecyclerView.Adapter<MyPracticeLogRecyclerViewAdapter.ViewHolder> {

    private final List<PracticeLogAsyncTask.PracticeLog> mValues;
    private final OnListFragmentInteractionListener mListener;

    public MyPracticeLogRecyclerViewAdapter(List<PracticeLogAsyncTask.PracticeLog> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_practice_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.characterText.setText(mValues.get(position).character);
        holder.dateText.setText(mValues.get(position).date.toString());

        if(mValues.get(position).correct){
            holder.passText.setText("O");

            int color = Color.parseColor("#e7ffe0");
            holder.mView.setBackgroundColor(color);
            holder.curveView.setBackgroundColor(color);

        } else {
            holder.passText.setText("X");
            int color = Color.parseColor("#ffe0e0");
            holder.mView.setBackgroundColor(color);
            holder.curveView.setBackgroundColor(color);
        }

        PointDrawing drawing = mValues.get(position).drawing;
        if(drawing != null) {
            holder.curveView.setDrawing(drawing, AnimatedCurveView.DrawTime.STATIC, Collections.<Criticism.PaintColourInstructions>emptyList());
        } else {
            holder.curveView.clear();
        }

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView characterText;
        public final TextView dateText;
        public final TextView passText;
        public final AnimatedCurveView curveView;
        public PracticeLogAsyncTask.PracticeLog mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            characterText = view.findViewById(R.id.log_character);
            dateText = view.findViewById(R.id.log_date);
            passText = view.findViewById(R.id.log_passed);
            curveView = view.findViewById(R.id.log_drawn);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + characterText.getText() + "'";
        }
    }
}
