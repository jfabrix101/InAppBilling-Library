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


/**
 * Singolo elemento della vetrina virtuale.
 * Un insieme di questi oggetti verrà gestito dall'activity che si
 * occupa di controllare lo stato dell'acquisto.
 * 
 * Regole:
 * <ul>
 * <li>Se il productId è vuoto, l'item viene mostrato ma non e' 
 * 		acquistabile (introduzione)</li>
 * 
 * <li>Se il title == null, come label viene mostrato il productId</li>
 * 
 * <li>Se il webDescriptionURI è null, come descrizione viene costruita 
 * 		una pagina html di defaul con all'interno il productId</li>
 * </ul>
 * 
 * Vengono definite le costanti per l'item di test : ANDROID_TEST_PURCHASED, 
 * 	ANDROID_TEST_CANCELED, ANDROID_TEST_REFUNDED, ANDROID_TEST_ITEM_UNAVAILABLE.
 */
public final class BillingCatalogEntry {

	public enum Managed { MANAGED, UNMANAGED, SUBSCRIPTION }
	
	public static final BillingCatalogEntry ANDROID_TEST_PURCHASED = 
			new BillingCatalogEntry(BillingConsts.BILLING_ITEM_AndroidTestPurchased);
	
	public static final BillingCatalogEntry ANDROID_TEST_CANCELED = 
			new BillingCatalogEntry(BillingConsts.BILLING_ITEM_AndroidTestCanceled);
	
	public static final BillingCatalogEntry ANDROID_TEST_REFUNDED = 
			new BillingCatalogEntry(BillingConsts.BILLING_ITEM_AndroidTestRefunded);
	
	public static final BillingCatalogEntry ANDROID_TEST_ITEM_UNAVAILABLE = 
			new BillingCatalogEntry(BillingConsts.BILLING_ITEM_AndroidTestItemUnavailable);
	
	private String productId;
	private String title;
	private Managed managedState;
	private String webDescriptionURI;
	private String developerPayload;
	private String buttonLabel = null;
	
	public BillingCatalogEntry(String productId) {
		this.productId = productId;
		this.title = productId;
		this.managedState = Managed.MANAGED;
		this.webDescriptionURI = null;
		this.developerPayload = "";
	}
	
	public BillingCatalogEntry(String productId, String title, String descriptionId) {
		this.productId = productId;
		this.title = title;
		this.managedState = Managed.MANAGED;
		this.webDescriptionURI = descriptionId;
	}
	
	public BillingCatalogEntry(String productId, String title, String descriptionId, String payload) {
		this(productId, title, descriptionId);
		this.developerPayload = payload;
	}
	
	
	public Managed getManagedState() { return managedState; }
	public String getProductId() { return productId; }

	
	public void setTitle(String title) { this.title = title; }

	public String getWebDescriptionURI() { return webDescriptionURI; }
	public void setWebDescriptionURI(String webDescriptionURI) { this.webDescriptionURI = webDescriptionURI; }

	public String getDeveloperPayload() { return developerPayload; }
	public void setDeveloperPayload(String developerPayload) { this.developerPayload = developerPayload; }

	public void setProductId(String productId) { this.productId = productId; }
	public void setManagedState(Managed managedState) {	this.managedState = managedState; }
	
	public String getButtonLabel() { return buttonLabel; }
	public void setButtonLabel(String buttonLabel) { this.buttonLabel = buttonLabel; }
	
	public String getTitle() { 
		if (title != null) return title; 
		else return productId;
	}
}
