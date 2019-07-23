package dmeeuwis.kanjimaster.logic;

import dmeeuwis.kanjimaster.logic.data.CharacterStudySet;

public interface LockChecker {
    void runPurchase();

    void startConsume();

    void coreLock();

    void coreUnlock();

    CharacterStudySet.LockLevel getPurchaseStatus();
}
