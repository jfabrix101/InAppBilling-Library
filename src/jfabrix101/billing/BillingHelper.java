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

import java.util.List;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;
import android.widget.Toast;

public class BillingHelper {

	private static final String TAG = "jfabrix101-BillingHelper";
	
	/**
	 * Verifica se un item e' stato acquistato o meno
	 */
	public static boolean isPurchased(Context context, String productId) {
		BillingDatabase db = new BillingDatabase(context);
		List<String> purchased = db.getAllPurchasedItems();
		db.close();
		boolean isPurchased = purchased.contains(productId);
		return isPurchased;
	}
	
	/**
	 * Richiede il ripristino delle transazioni
	 */
	public static void restoreTransactions(Context context) {
		Log.d(TAG, "Restoring transactions");
		BillingService mBillingService = new BillingService();
        mBillingService.setContext(context);
		mBillingService.restoreTransactions();
	}
	
	/**
	 * Mostra un messaggio TOAST
	 */
	public static void showToast(Context context, int resourceId) {
		showToast(context, context.getString(resourceId));
	}
	
	/**
	 * Mostra un messaggio TOAST
	 */
	public static void showToast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Recupera informazioni sul package dell'applicazione
	 */
	public static PackageInfo getAppPackageInfo(Context context, Class<?> thisClass) {
		PackageInfo pinfo = null;
		try {
			ComponentName comp = new ComponentName(context, thisClass);
			pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
		} catch (Exception e) {}
		return pinfo;
	}
	
	/**
	 * Visualizza un dialogBox con un messaggio
	 */
	public static void showDialogBox(Context context, String title, String msg, int icon) {
		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		ad.setTitle(title);
		ad.setPositiveButton(android.R.string.ok, null);
		if (icon <= 0) ad.setIcon(android.R.drawable.ic_dialog_alert);
		else ad.setIcon(icon);
		ad.setMessage(msg);
		ad.show();
	}
}
