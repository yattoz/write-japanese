package dmeeuwis.kanjimaster.logic.data;

public interface LogWrap {
    void i(String message);
    void i(String message, Throwable t);
    void d(String message);
    void d(String message, Throwable t);
    void w(String message);
    void w(String message, Throwable t);
    void e(String message);
    void e(String message, Throwable t);
}
