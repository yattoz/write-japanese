package dmeeuwis.nakama.billing;

import android.app.*;
import android.content.*;

public class LockCheckerAmazonIAB extends LockChecker {


    public LockCheckerAmazonIAB(Activity parent) {
        super(parent);
    }

    @Override
    public void runPurchase() {

    }

    @Override
    public void startConsume() {

    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }

    @Override
    public void dispose() {

    }
}
