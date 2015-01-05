package dmeeuwis.masterlibrary;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import dmeeuwis.Translation;
import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.views.AdvancedFuriganaTextView;

public class KanjiVocabRecyclerAdapter extends RecyclerView.Adapter<KanjiVocabRecyclerAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final AdvancedFuriganaTextView furigana;
        private final TextView englishText;

        public ViewHolder(View view){
            super(view);
            this.furigana = (AdvancedFuriganaTextView)view.findViewById(R.id.kanji);
            this.englishText = (TextView)view.findViewById(R.id.english);
        }
    }

    private final Activity context;
    private final KanjiFinder kanjiFinder;
    private final List<Translation> translations;

    public KanjiVocabRecyclerAdapter(Activity context, KanjiFinder kanjiFinder) {
        super();
        this.context = context;
        this.kanjiFinder = kanjiFinder;
        this.translations = new ArrayList<>();
    }

    @Override
    public KanjiVocabRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = this.context.getLayoutInflater();
        View view = inflater.inflate(R.layout.translation_slide, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Translation t = this.translations.get(position);
        holder.englishText.setText(t.toEnglishString());
        holder.furigana.setTranslation(t, this.kanjiFinder);
    }

    @Override
    public int getItemCount() {
        return this.translations.size();
    }

    public void add(Translation t){
        this.translations.add(t);
        this.notifyItemChanged(this.translations.size()-1);
    }

    public void clear(){
        this.translations.clear();
        this.notifyDataSetChanged();
    }
}
