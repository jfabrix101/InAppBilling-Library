/*   Copyright 2011 jfabrix101 (http://www.fabrizio-russo.it)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package jfabrix101.billing;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jfabrix101.billing.BillingCatalogEntry.Managed;
import jfabrix101.billing.BillingConsts.PurchaseState;
import jfabrix101.billing.BillingConsts.ResponseCode;
import jfabrix101.billing.BillingService.RequestPurchase;
import jfabrix101.billing.BillingService.RestoreTransactions;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * A sample application that demonstrates in-app billing.
 */
public abstract class AbstractBillingActivity extends Activity 
implements OnClickListener, OnItemSelectedListener {
    
	private static final String TAG = "jfabrix101-BillingActivity";
	private void trace(String msg) { Log.d(TAG, msg); }

	/**
     * The SharedPreferences key for recording whether we initialized the
     * database.  If false, then we perform a RestoreTransactions request
     * to get all the purchases for this user.
     */
    private static final String DB_INITIALIZED = "db_initialized";

    private BillingPurchaseObserver mBillingPurchaseObserver;
    private Handler mHandler;

    private BillingService mBillingService;
    
    private Button mBuyButton;
    private WebView mDescriptionWebView = null;
    
    private Spinner mSelectItemSpinner;
    private Set<String> mOwnedItems = new HashSet<String>();
    
    private BillingCatalogEntry selectCategoryItem = null;

    /**
     * A {@link PurchaseObserver} is used to get callbacks when Android Market sends
     * messages to this application so that we can update the UI.
     */
    private class BillingPurchaseObserver extends BillingObserver {
        public BillingPurchaseObserver(Handler handler) {
            super(AbstractBillingActivity.this, handler);
        }

        @Override
        public void onBillingSupported(boolean supported, String type) {
            trace("onBillingSupported() - supported: " + supported);
            
            if (type == null || type.equals(BillingConsts.ITEM_TYPE_INAPP)) {
                if (supported) {
                    restoreDatabase();
                    mBuyButton.setEnabled(true);
                } else {
                	BillingHelper.showToast(AbstractBillingActivity.this, R.string.jfabrix101_billing_inAppBillingNotSupported);
                }
            } else if (type.equals(BillingConsts.ITEM_TYPE_SUBSCRIPTION)) {
                mCatalogAdapter.setSubscriptionsSupported(supported);
            } else {
            	BillingHelper.showToast(AbstractBillingActivity.this, R.string.jfabrix101_billing_inSubscriptionNotSupported);
            }
        }

        @Override
        public void onPurchaseStateChange(PurchaseState purchaseState, String itemId,
               long purchaseTime, String developerPayload) {
            trace("onPurchaseStateChange() itemId: " + itemId + " " + purchaseState);
            
            if (purchaseState == PurchaseState.PURCHASED) {
            	mOwnedItems.add(itemId);
            	mBuyButton.setVisibility(View.GONE);
            }
            mCatalogAdapter.setOwnedItems(mOwnedItems);
            
        }

        @Override
        public void onRequestPurchaseResponse(RequestPurchase request, ResponseCode responseCode) {
            trace("onRequestPurchaseResponse() - " + request.mProductId + ": " + responseCode);
                    
            if (responseCode == ResponseCode.RESULT_OK) {
            } else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
            } else {
            }
        }

        @Override
        public void onRestoreTransactionsResponse(RestoreTransactions request, ResponseCode responseCode) {
        	Log.d(TAG, "onRestoreTransactionsResponse() - request=" + request + " , responseCode= " + responseCode);
            if (responseCode == ResponseCode.RESULT_OK) {
            	                
                // Update the shared preferences so that we don't perform a RestoreTransactions again.
                SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(DB_INITIALIZED, true);
                edit.commit();
            } else {
                Log.e(TAG, "onRestoreTransactionsResponse() - RestoreTransactions error: " + responseCode);
            }
        }
        
    } 

    public abstract List<BillingCatalogEntry> getBillingCatalogEntry();

    /**
     * Invocato al termine di una transazione sul market e quando un item cambia stato
     * (tipicamente quando viene acquistato o restituito)
     */
    public abstract void onPurchaseStateChange(String productId, PurchaseState purchaseState);
    
    private BillingCatalogSpinnerAdapter mCatalogAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jfabrix101_billing_main);

        mBuyButton = (Button) findViewById(R.id.jfabrix101_billingBuyButton);
        mDescriptionWebView = (WebView) findViewById(R.id.jfabrix101_billingWebView);
        mSelectItemSpinner = (Spinner) findViewById(R.id.jfabrix101_billingSpinner);
        
	    trace("onCreate()");
	    
        mHandler = new Handler();
        mBillingPurchaseObserver = new BillingPurchaseObserver(mHandler);
        mBillingService = new BillingService();
        mBillingService.setContext(this);
       
        setupWidgets();

        // Check if billing is supported.
        BillingHandler.register(mBillingPurchaseObserver);
        if (!mBillingService.checkBillingSupported(null)) {
        	BillingHelper.showDialogBox(this, 
        			getString(R.string.jfabrix101_billing_cannot_connect_title), 
        			getString(R.string.jfabrix101_billing_cannot_connect_message), -1);
        }
    }

    /**
     * Called when this activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        trace("onStart() - registering observer");
        BillingHandler.register(mBillingPurchaseObserver);
        initializeOwnedItems();
    }

    /**
     * Called when this activity is no longer visible.
     */
    @Override
    protected void onStop() {
        super.onStop();
        trace("onStop() - unregistering observer");
        BillingHandler.unregister(mBillingPurchaseObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        trace("onDestroy() - unbinding service");
        mBillingService.unbind();
    }

 
    /**
     * If the database has not been initialized, we send a
     * RESTORE_TRANSACTIONS request to Android Market to get the list of purchased items
     * for this user. This happens if the application has just been installed
     * or the user wiped data. We do not want to do this on every startup, rather, we want to do
     * only when the database needs to be initialized.
     */
    private void restoreDatabase() {
    	
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        boolean initialized = prefs.getBoolean(DB_INITIALIZED, false);
        trace("restoreDatabase() - initialized =" + initialized);
        if (!initialized) {
            mBillingService.restoreTransactions();
            Toast.makeText(this, R.string.jfabrix101_billing_restoring_transactions, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Creates a background thread that reads the database and initializes the
     * set of owned items.
     */
    private void initializeOwnedItems() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                doInitializeOwnedItems();
            }
        }).start();
    }

    /**
     * Reads the set of purchased items from the database in a background thread
     * and then adds those items to the set of owned items in the main UI
     * thread.
     */
    private void doInitializeOwnedItems() {
    	   	
    	BillingDatabase mBillingDatabase = new BillingDatabase(this);
    	List<String> purchasedItems = mBillingDatabase.getAllPurchasedItems();
    	mBillingDatabase.close();
    	trace("doInitializeOwnedItems() - Found " + purchasedItems.size() + " purchased items");
    	final Set<String> ownedItems = new HashSet<String>();
    	for (String productId : purchasedItems) {
    		 ownedItems.add(productId);
		}
    	
        // We will add the set of owned items in a new Runnable that runs on
        // the UI thread so that we don't need to synchronize access to
        // mOwnedItems.
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mOwnedItems.addAll(ownedItems);
                mCatalogAdapter.setOwnedItems(mOwnedItems);
            }
        });
    }

    /**
     * Callback method alla pressione del tasto di acquisto.
     * 
     * @param item - Item selezionato per l'acquisto
     * @return TRUE: Si puo' procedere con l'acquisto (default), 
     * 	FALSE: Non procedere con l'acquisto  
     */
    public abstract boolean onBuyButtonPressed(BillingCatalogEntry item);
    
    /**
     * Called when a button is pressed.
     */
    @Override
    public void onClick(View v) {
        if (v == mBuyButton) {
            trace("AbstractBillingActivity : Pressed 'Buy' button for :  productID: " + 
            		selectCategoryItem.getProductId());
            
            boolean canContinue = onBuyButtonPressed(selectCategoryItem);
            if (!canContinue) return;
            
            Managed managedType = selectCategoryItem.getManagedState();
            
            String mPayloadContents = selectCategoryItem.getDeveloperPayload();
            
            trace("AbstractBillingActivity : mPayload = " + mPayloadContents);
            if (managedType != Managed.SUBSCRIPTION &&
                    !mBillingService.requestPurchase(selectCategoryItem.getProductId(), 
                    		BillingConsts.ITEM_TYPE_INAPP, mPayloadContents)) {
            	BillingHelper.showToast(this, R.string.jfabrix101_billing_not_supported_message);
            } else if (managedType == Managed.SUBSCRIPTION && 
            		!mBillingService.requestPurchase(selectCategoryItem.getProductId(), 
            				BillingConsts.ITEM_TYPE_SUBSCRIPTION, mPayloadContents)) {
                BillingHelper.showToast(this, R.string.jfabrix101_billing_subscriptions_not_supported_message);
            }
        } 
    }


    /**
     * Called when an item in the spinner is selected.
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    	selectCategoryItem = getBillingCatalogEntry().get(position);
        configureWebView(selectCategoryItem);
    }


    /**
     * Sets up the UI.
     */
    private void setupWidgets() {
        
        mBuyButton.setEnabled(false);
        mBuyButton.setOnClickListener(this);

        mDescriptionWebView.setWebViewClient(new WebViewClient(){
        	public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        
        List<BillingCatalogEntry> catalog = getBillingCatalogEntry();
        if (catalog == null) catalog = new ArrayList<BillingCatalogEntry>();
       
        mCatalogAdapter = new BillingCatalogSpinnerAdapter(this, catalog);
        mSelectItemSpinner.setAdapter(mCatalogAdapter);
        
        if (catalog.size() == 1) {
        	mSelectItemSpinner.setVisibility(View.GONE);
        	selectCategoryItem = catalog.get(0);
        	configureWebView(selectCategoryItem);
        } else {
	        mSelectItemSpinner.setOnItemSelectedListener(this);
        }

    }


    
	private void configureWebView(BillingCatalogEntry item) {
		
		String webLink = item.getWebDescriptionURI();
		if (webLink == null) webLink = "";
		if (item.getButtonLabel() == null) mBuyButton.setText(R.string.jfabrix101_billing_buy_button);
		else mBuyButton.setText(item.getButtonLabel());
		
		if (webLink.startsWith("http://")) {
			mDescriptionWebView.loadUrl(item.getWebDescriptionURI());
		} else if (webLink.endsWith(".html")) {
				mDescriptionWebView.loadUrl("file:///android_res/raw/" + webLink);
		} else {
			StringBuilder html = new StringBuilder("<html><body>");
			html.append("<p align='center'>").append(webLink).append("</p>");
			html.append("</body></html>");
			mDescriptionWebView.loadData(html.toString(), "text/html", "UTF-8");
		}
			

		if (mOwnedItems.contains((item.getProductId()))) mBuyButton.setVisibility(View.GONE);
		else mBuyButton.setVisibility(View.VISIBLE);
		
		// Nasconde il pulsante di acquisto per l'introduzione
		String productId = item.getProductId();
 		if (productId == null || productId.length()==0) mBuyButton.setVisibility(View.GONE);
	}
    
	
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    /**
     * An adapter used for displaying a catalog of products.  If a product is
     * managed by Android Market and already purchased, then it will be "grayed-out" in
     * the list and not selectable.
     */
    private static class BillingCatalogSpinnerAdapter extends ArrayAdapter<String> {
        private List<BillingCatalogEntry> mCatalog;
        private Set<String> mOwnedItems = new HashSet<String>();
        private boolean mIsSubscriptionsSupported = false;

        public BillingCatalogSpinnerAdapter(Context context, List<BillingCatalogEntry> catalog) {
            super(context, android.R.layout.simple_spinner_item);
            mCatalog = catalog;
            for (BillingCatalogEntry element : catalog) {
            	add(element.getTitle());
            }
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        public void setOwnedItems(Set<String> ownedItems) {
            mOwnedItems = ownedItems;
            notifyDataSetChanged();
        }

        public void setSubscriptionsSupported(boolean supported) {
            mIsSubscriptionsSupported = supported;
        }

        @Override
        public boolean areAllItemsEnabled() {
            // Return false to have the adapter call isEnabled()
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            // If the item at the given list position is not purchasable,
            // then prevent the list item from being selected.
            BillingCatalogEntry entry = mCatalog.get(position);
            if (entry.getManagedState() == Managed.MANAGED && mOwnedItems.contains(entry.getProductId())) {
                return false;
            }
            if (entry.getManagedState() == Managed.SUBSCRIPTION && !mIsSubscriptionsSupported) {
                return false;
            }
            return true;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            // If the item at the given list position is not purchasable, then
            // "gray out" the list item.
            View view = super.getDropDownView(position, convertView, parent);
            view.setEnabled(isEnabled(position));
            return view;
        }
    }
    
}
