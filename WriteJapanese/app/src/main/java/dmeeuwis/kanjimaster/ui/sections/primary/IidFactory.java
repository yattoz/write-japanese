package dmeeuwis.kanjimaster.ui.sections.primary;

import java.util.UUID;

public class IidFactory {
    static Iid iid;

    public static void initialize(Iid iidGen){
        iid = iidGen;
    }

    public static UUID get(){
        return iid.get();
    }
}
