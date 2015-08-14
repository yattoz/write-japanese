package dmeeuwis.nakama;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Calendar;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.primary.KanjiMasterActivity;

public class ReminderManager extends BroadcastReceiver {

    private static final String INTENT_CHARSET = "charset";

    public static void scheduleRemindersFor(Context c, CharacterStudySet charset) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.HOUR, 24);
        calendar.set(Calendar.HOUR, 9);
        calendar.set(Calendar.MINUTE, 0);

        Intent intent = new Intent(c, ReminderManager.class);
        intent.putExtra("charset", charset.pathPrefix);
        intent.putExtra("scheduled", charset.getGoalProgress().scheduledPerDay);

        int id = charset.name.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(c, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.i("nakama", "ReminderManager: scheduled a notification!");
    }

    public static void clearReminders(Context c, CharacterStudySet charset){
        AlarmManager alarmManager = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        int id = charset.name.hashCode();
        Intent intent = new Intent(c, ReminderManager.class);
        intent.putExtra("charset", charset.pathPrefix);
        intent.putExtra("scheduled", charset.getGoalProgress().scheduledPerDay);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(c, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
    }

    public void onReceive(Context context, Intent intent) {
        Log.i("nakama", "ReminderManager.onReceive: wakeup!");
        String charset = intent.getStringExtra(INTENT_CHARSET);

        CharacterStudySet set = CharacterSets.fromName(charset, null, null);
        set.load(context);
        CharacterStudySet.GoalProgress gp = set.getGoalProgress();
        int charCount = gp.neededPerDay;

        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = getNotification(context, "Study Reminder", "Try for " + charCount + " " + set.name + " characters today!");
        int id = (int) System.currentTimeMillis();
        notificationManager.notify(id, n);

        // schedule tomorrow's reminder
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(System.currentTimeMillis());
        if(gp.goal.after(now)){
            scheduleRemindersFor(context, set);
        }
    }

    private Notification getNotification(Context context, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setSmallIcon(R.drawable.ic_launcher);
        Intent intent = new Intent(context, KanjiMasterActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, PendingIntent.FLAG_ONE_SHOT, intent, 0);
        builder.setContentIntent(pi);
        return builder.build();
    }
}