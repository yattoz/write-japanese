package dmeeuwis.kanjimaster.ui.data;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.Html;

import org.threeten.bp.LocalDate;

import java.util.List;
import java.util.Map;

import dmeeuwis.kanjimaster.logic.data.SRSScheduleHtmlGenerator;

public class SRSSScheduleDialog {
    public static void displayScheduleDialog(Context ctx, Map<LocalDate, List<Character>> schedule){
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //do things
            }
        });

        AlertDialog d = b.create();
        String html = SRSScheduleHtmlGenerator.generateHtml(schedule);
        d.setMessage(Html.fromHtml(html));
        d.show();
    }


}
