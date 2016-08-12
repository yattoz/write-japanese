package com.android.vending.billing.util;

import android.content.Context;

/**
 * Fix IabHelper so that an orientation change during purchase flow won't break callback to
 * OnIabPurchaseFinishedListener. See
 * http://stackoverflow.com/questions/18223130/in-app-billing-rapid-device-orientation-causes-crash-illegalstateexception
 */
public class FixedIabHelper extends IabHelper {

    public FixedIabHelper(Context ctx, String base64PublicKey) {
        super(ctx, base64PublicKey);
    }

    public void forcePurchaseListener(int requestCode, OnIabPurchaseFinishedListener p){
        this.mRequestCode = requestCode;
        this.mPurchaseListener = p;
    }
}
