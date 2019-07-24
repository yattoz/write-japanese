package dmeeuwis.kanjimaster.ui.billing;

import android.app.*;
import android.content.*;
import android.util.Log;
import android.widget.Toast;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.*;

import java.util.HashSet;
import java.util.Set;

import dmeeuwis.kanjimaster.*;
import dmeeuwis.kanjimaster.ui.data.UncaughtExceptionLogger;

public class LockCheckerAmazonIAB extends LockChecker implements PurchasingListener {

    private final static String IN_APP_PURCHASE_KEY = "dmeeuwis.writejapanese.unlock";

    public LockCheckerAmazonIAB(Activity parent) {
        super(parent);

        PurchasingService.registerListener(parent, this);
        PurchasingService.getUserData();

        {   // request product data
            Set<String> s = new HashSet<>(1);
            s.add(IN_APP_PURCHASE_KEY);
            PurchasingService.getProductData(s);
        }
    }

    @Override
    public void runPurchase() {
        final RequestId requestId = PurchasingService.purchase(IN_APP_PURCHASE_KEY);
        Log.i("nakama-kindle", "Saw requestId as: " + requestId);
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


    // Purchasing Listener
    @Override
    public void onUserDataResponse(UserDataResponse response) {
        Log.i("nakama-kindle", "Saw user data!");

        Log.d("nakama-kindle", "onGetUserDataResponse: requestId (" + response.getRequestId()
                + ") userIdRequestStatus: "
                + response.getRequestStatus()
                + ")");

        final UserDataResponse.RequestStatus status = response.getRequestStatus();
        switch (status) {
            case SUCCESSFUL:
                Log.d("nakama-kindle", "onUserDataResponse: get user id (" + response.getUserData().getUserId()
                        + ", marketplace ("
                        + response.getUserData().getMarketplace()
                        + ") ");
                //iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
                break;

            case FAILED:
                Log.e("nakama-kindle", "onUserDataResponse failed, status code is " + status);
            case NOT_SUPPORTED:
                Log.e("nakama-kindle", "onUserDataResponse failed, status code is " + status);
                //iapManager.setAmazonUserId(null, null);
                break;
        }
    }

    @Override
    public void onProductDataResponse(ProductDataResponse response) {
        Log.i("nakama-kindle", "Saw product data!");
        final ProductDataResponse.RequestStatus status = response.getRequestStatus();
        Log.d("nakama-kindle", "onProductDataResponse: RequestStatus (" + status + ")");

        switch (status) {
            case SUCCESSFUL:
                Log.d("nakama-kindle", "onProductDataResponse: successful.  The item data map in this response includes the valid SKUs");
                final Set<String> unavailableSkus = response.getUnavailableSkus();
                Log.d("nakama-kindle", "onProductDataResponse: " + unavailableSkus.size() + " unavailable skus");
                //iapManager.enablePurchaseForSkus(response.getProductData());
                //iapManager.disablePurchaseForSkus(response.getUnavailableSkus());
                //iapManager.refreshLevel2Availability();

                break;
            case FAILED:
                //Toast.makeText(parentActivity, "Failed to contact Kindle App Store; please retry later.", Toast.LENGTH_SHORT).show();
                Log.e("nakama-kindle", "onProductDataResponse: failed, should retry request");
                break;
            case NOT_SUPPORTED:
                Toast.makeText(parentActivity, "This device is not supported by the Kindle App Store.", Toast.LENGTH_SHORT).show();
                Log.e("nakama-kindle", "onProductDataResponse: not supported device.");
                //iapManager.disableAllPurchases();
                break;
        }
    }

    @Override
    public void onPurchaseResponse(PurchaseResponse response) {
        if(BuildConfig.DEBUG){
            Log.d("nakama-kindle", "onPurchaseUpdatesResponse: requestId (" + response.getRequestId()
                    + ") purchaseUpdatesResponseStatus ("
                    + response.getRequestStatus()
                    + ") userId ("
                    + response.getUserData().getUserId()
                    + ") " + response.getUserData().toJSON()
            );
        }
        final PurchaseResponse.RequestStatus status = response.getRequestStatus();
        switch (status) {
            case ALREADY_PURCHASED:
                Toast.makeText(parentActivity, "Already purchased, now unlocking.", Toast.LENGTH_SHORT).show();
                coreUnlock();
                parentActivity.recreate();
                break;

            case SUCCESSFUL:
                Receipt receipt = response.getReceipt();
                doUnlock(receipt);
                break;

            case FAILED:
                // Toast.makeText(parentActivity, "Failed to complete Kindle App Store purchase; please try again later.", Toast.LENGTH_SHORT).show();
                Log.d("nakama-kindle", "onPurchaseDataResponse: failed, should retry request");
                break;
            case NOT_SUPPORTED:
                Log.d("nakama-kindle", "onPurchaseDataResponse: failed, device not supported");
                Toast.makeText(parentActivity, "Failed to contact Kindle App Store; please retry later.", Toast.LENGTH_SHORT).show();
                // iapManager.disableAllPurchases();
                break; }
    }

    @Override
    public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse purchaseUpdatesResponse) {
        Log.i("nakama-kindle", "Saw purchase updates!");
        for(Receipt r: purchaseUpdatesResponse.getReceipts()){
            if(r.getProductType() == ProductType.CONSUMABLE){
                doUnlock(r);
            }
        }

    }

    private void doUnlock(Receipt receipt){
        Log.i("nakama-kindle", "doUnlock called");
        String receiptId = null;
        if(receipt != null){
            receiptId = receipt.getReceiptId();
        }

        try {
            UncaughtExceptionLogger.backgroundLogPurchase(parentActivity, receiptId);
        } catch(Throwable t){
            UncaughtExceptionLogger.backgroundLogError("Error logging backround purchase", t);
        }

        coreUnlock();
        try {
            PurchasingService.notifyFulfillment(receiptId, FulfillmentResult.FULFILLED);
        } catch (Throwable t){
            UncaughtExceptionLogger.backgroundLogError("Error notifying fullfillment", t);
        }

        parentActivity.recreate();
    }
}
