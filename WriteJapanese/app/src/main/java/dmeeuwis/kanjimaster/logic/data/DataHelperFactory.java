package dmeeuwis.kanjimaster.logic.data;

public class DataHelperFactory {
    static private DataHelper dh;

    public static void initialize(DataHelper d){
        dh = d;
    }

    public static DataHelper get(){
        return dh;
    }
}
