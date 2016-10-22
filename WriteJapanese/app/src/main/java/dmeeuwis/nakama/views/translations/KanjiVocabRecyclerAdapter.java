package dmeeuwis.nakama.views.translations;

import android.app.Activity;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import dmeeuwis.Translation;
import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.kanjidraw.Criticism;
import dmeeuwis.nakama.kanjidraw.CurveDrawing;
import dmeeuwis.nakama.kanjidraw.PointDrawing;
import dmeeuwis.nakama.views.AnimatedCurveView;

import static android.R.attr.x;

public class KanjiVocabRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final float engTextSize;
    private boolean characterHeader = false;
    private boolean meaningsHeader = false;

    private final Activity context;
    private final KanjiFinder kanjiFinder;
    private final List<Translation> translations;

    private CurveDrawing knownCharacter;
    private PointDrawing drawnCharacter;

    public KanjiVocabRecyclerAdapter(Activity context, KanjiFinder kanjiFinder) {
        super();

        this.drawnCharacter = null;
        this.knownCharacter = null;

        this.context = context;
        this.kanjiFinder = kanjiFinder;
        this.translations = new ArrayList<>();

        Resources r = context.getResources();
        this.engTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
    }

    public void addKnownAndDrawnHeader(CurveDrawing known, PointDrawing drawn){
        this.knownCharacter = known;
        this.drawnCharacter = drawn;
    }

    public void addMeaningsHeader(){
        this.meaningsHeader = true;
    }

    public void addCharacterHeader(){
        this.characterHeader = true;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int p) {
        if(knownCharacter != null && p == 0){
            Log.i("nakama", "KanjiVocabRecyclerAdapter.onBindViewholder " + p + ": setting known/drawn view");
            ShowStrokesViewHolder showHolder = (ShowStrokesViewHolder)h;
            showHolder.drawn.setDrawing(drawnCharacter, AnimatedCurveView.DrawTime.STATIC, new ArrayList<Criticism.PaintColourInstructions>(0));
            showHolder.known.setDrawing(knownCharacter, AnimatedCurveView.DrawTime.STATIC, new ArrayList<Criticism.PaintColourInstructions>(0));
            return;
        }
        Log.i("nakama", "KanjiVocabRecyclerAdapter.onBindViewholder " + p + ": setting translation view");

        int translationIndex = knownCharacter != null ? p - 1 : p;

        TranslationViewHolder holder = (TranslationViewHolder)h;

        Translation t = this.translations.get(translationIndex);
        holder.bind(t, this.kanjiFinder);
    }

    @Override
    public int getItemCount() {
        return drawnCharacter == null ? this.translations.size() : this.translations.size() + 1;
    }

    public void add(Translation t){
        this.translations.add(t);
        this.notifyItemChanged(this.translations.size()-1);
    }

    public void clear(){
        this.translations.clear();
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        List<Integer> headers = new ArrayList<>();
        if(drawnCharacter != null){ headers.add(DRAWN_HEADER); }
        if(characterHeader){ headers.add(CHARACTER_HEADER); }
        if(meaningsHeader){ headers.add(MEANINGS_HEADER); }

        if(position < headers.size()){
            return headers.get(position);
        }

        return TRANSLATION_HEADER;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = this.context.getLayoutInflater();
        if(viewType == 0){
            View view = inflater.inflate(R.layout.translation_correct_drawn_row, parent, false);
            return new ShowStrokesViewHolder(view);
         } else {
            View view = inflater.inflate(R.layout.translation_slide, parent, false);
            return new TranslationViewHolder(view, engTextSize);
        }
    }
}
