package dmeeuwis.nakama;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.primary.Iid;
import dmeeuwis.nakama.primary.KanjiMasterActivity;

public class ReminderManager extends BroadcastReceiver {

    private static final String INTENT_CHARSET = "charset";

    private static Intent makeIntent(Context c, CharacterStudySet charset){
        int id = charset.name.hashCode();
        Intent intent = new Intent(c, ReminderManager.class);
        intent.putExtra("charset", charset.pathPrefix);
        return intent;
    }

    private static int makePendingId(CharacterStudySet charset) {
        return charset.name.hashCode();
    }

    public static void scheduleRemindersFor(Context c, CharacterStudySet charset) {

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Calendar calendar = GregorianCalendar.getInstance();
        Log.i("nakama", "Current time is: " + df.format(calendar.getTime()));

        calendar.add(Calendar.HOUR, 24);
        calendar.set(Calendar.HOUR_OF_DAY, 6);
        calendar.set(Calendar.MINUTE, 00);

        Log.i("nakama", "Setting study reminder for " + df.format(calendar.getTime()));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(c, makePendingId(charset),
                    makeIntent(c, charset), PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.i("nakama", "ReminderManager: scheduled a notification!");
    }

    public static void clearAllReminders(Activity c){
        final String[] set = { "j1", "j2", "j3", "j4", "j5", "j6", "hiragana", "katakana" };
        for(String s: set){
            CharacterStudySet charset = CharacterSets.fromName(s, null, null, Iid.get(c.getApplication()));
            charset.load(c.getApplicationContext());
            clearReminders(c.getApplicationContext(), charset);
        }
        Log.i("nakama", "ReminderManager: cleared all notification!");
    }

    public static boolean reminderExists(Context c, CharacterStudySet charset){
        PendingIntent pendingIntent = PendingIntent.getBroadcast(c, makePendingId(charset),
                makeIntent(c, charset), PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }

    public static void clearReminders(Context c, CharacterStudySet charset){
        AlarmManager alarmManager = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        int id = charset.name.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(c, id, makeIntent(c, charset), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
        Log.i("nakama", "ReminderManager: cleared notification for " + charset.pathPrefix);
    }

    public void onReceive(Context context, Intent intent) {
        Log.i("nakama", "ReminderManager.onReceive: wakeup!");
        String charset = intent.getStringExtra(INTENT_CHARSET);

        //TODO: Is this evil?
        UUID iid = Iid.get(((Activity)context).getApplication());
        CharacterStudySet set = CharacterSets.fromName(charset, null, null, iid);
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