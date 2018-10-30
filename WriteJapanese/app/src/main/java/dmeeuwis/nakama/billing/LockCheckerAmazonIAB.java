package dmeeuwis.nakama.billing;

import android.app.*;
import android.content.*;
import android.util.Log;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.ResponseReceiver;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.RequestId;
import com.amazon.device.iap.model.UserDataResponse;

import java.util.HashSet;
import java.util.Set;

public class LockCheckerAmazonIAB extends LockChecker implements PurchasingListener {

    private final static String IN_APP_PURCHASE_KEY = "dmeeuwis.writejapanese.unlock";

    public LockCheckerAmazonIAB(Activity parent) {
        super(parent);

        PurchasingService.registerListener(parent, this);

        {   // request product data
            Set<String> s = new HashSet<>(1);
            s.add(IN_APP_PURCHASE_KEY);
            PurchasingService.getProductData(s);
            PurchasingService.getPurchaseUpdates(false);
        }

        // request user data
        PurchasingService.getUserData();
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
            case NOT_SUPPORTED:
                Log.d("nakama-kindle", "onUserDataResponse failed, status code is " + status);
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
            case NOT_SUPPORTED:
                Log.d("nakama-kindle", "onProductDataResponse: failed, should retry request");
                //iapManager.disableAllPurchases();
                break;
        }
    }

    @Override
    public void onPurchaseResponse(PurchaseResponse response) {
        Log.i("nakama-kindle", "Saw purchase response!");

        Log.d("nakama-kindle", "onPurchaseUpdatesResponse: requestId (" + response.getRequestId()
                + ") purchaseUpdatesResponseStatus ("
                + response.getRequestStatus()
                + ") userId ("
                + response.getUserData().getUserId()
                + ") " + response.getUserData().toJSON()
             );
        final PurchaseResponse.RequestStatus status = response.getRequestStatus();
        switch (status) {
            case SUCCESSFUL:

                String userId = response.getUserData().getUserId();
                String market = response.getUserData().getMarketplace();

                /*
                for (final Receipt receipt : response.getReceipts()) {
                    iapManager.handleReceipt(response.getRequestId().toString(), receipt, response.getUserData());
                }
                if (response.hasMore()) {
                    PurchasingService.getPurchaseUpdates(false);
                }
                */

                coreUnlock();
                parentActivity.recreate();

                break;
            case FAILED:
            case NOT_SUPPORTED:
                Log.d("nakama-kindle", "onProductDataResponse: failed, should retry request");
                // iapManager.disableAllPurchases();
                break;
        }

    }

    @Override
    public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse purchaseUpdatesResponse) {
        Log.i("nakama-kindle", "Saw purchase updates!");

    }
}
