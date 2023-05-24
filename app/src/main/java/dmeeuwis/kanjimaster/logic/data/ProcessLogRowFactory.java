package dmeeuwis.kanjimaster.logic.data;

public class ProcessLogRowFactory {
    private static DataHelper.ProcessRow processor;

    public static void initialize(DataHelper.ProcessRow p){
        processor = p;
    }

    public static DataHelper.ProcessRow get(){
        return processor;
    }
}
