package dmeeuwis.nakama.data;

import java.text.SimpleDateFormat;
import java.util.Date;

import static android.R.attr.level;
import static android.R.id.message;

public class LogWrapConsole implements LogWrap {
    @Override
    public void i(String message) {
        print("INFO", message);
    }

    @Override
    public void i(String message, Throwable t) {
        print("INFO", message);
        print(t);
    }

    @Override
    public void d(String message) {
        print("DEBUG", message);
    }

    @Override
    public void d(String message, Throwable t) {
        print("DEBUG", message);
        print(t);
    }

    @Override
    public void w(String message) {
        print("WARN", message);
    }

    @Override
    public void w(String message, Throwable t) {
        print("WARN", message);
        print(t);
    }

    @Override
    public void e(String message) {
        print("ERROR", message);
    }

    @Override
    public void e(String message, Throwable t) {
        print("ERROR", message);
        print(t);
    }

    private String timestamp(){
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
        return sdf.format(date);
    }

    private void print(Throwable t){
        System.out.println(timestamp() + " " + level + ": " + message);
        t.printStackTrace();
    }
    private void print(String level, String message){
        System.out.println(timestamp() + " " + level + ": " + message);
    }
}
