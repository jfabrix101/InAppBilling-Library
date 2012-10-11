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
import java.util.List;

import jfabrix101.billing.BillingConsts.PurchaseState;
import jfabrix101.security.AESObfuscator;
import jfabrix101.security.SecurityHelper;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * An example database that records the state of each purchase. You should use
 * an obfuscator before storing any information to persistent storage. The
 * obfuscator should use a key that is specific to the device and/or user.
 * Otherwise an attacker could copy a database full of valid purchases and
 * distribute it to others.
 */
public class BillingDatabase  {
    
	private static final String TAG = "jfabrix101-PurchaseDatabase";
    
    private static final String DATABASE_NAME = "billing.db";
    private static final int DATABASE_VERSION = 1;
    
    private static final String PURCHASED_TABLE_NAME = "purchased";

    // These are the column names for the "purchased items" table.
    static final String PURCHASED_PRODUCT_ID_COL  = "productId";
    static final String PURCHASED_STATE_COL = "state";
    static final String PURCHASED_TIME_COL = "purchaseTime";
    static final String PURCHASED_DEVELOPER_PAYLOAD_COL = "developerPayload";
    

    private static final String[] PURCHASED_COLUMNS = {
        PURCHASED_PRODUCT_ID_COL, PURCHASED_STATE_COL, PURCHASED_TIME_COL, PURCHASED_DEVELOPER_PAYLOAD_COL
    };
    
    // Tabella dello storico 
    static final String HISTORY_TABLE_NAME = "history";
//    static final String HISTORY_ID_COL = "_id";
    static final String HISTORY_PRODUCT_ID_COL = "productID";
    static final String HISTORY_TIMESTAMP_COL = "purchaseTime";
    static final String HISTORY_STATE_COL = "state";
    static final String HISTORY_DEVELOPER_PAYLOAD_COL = "payload";

//    private static final String[] HISTORY_COLUMNS = {
//    	HISTORY_PRODUCT_ID_COL, HISTORY_TIMESTAMP_COL, HISTORY_STATE_COL, HISTORY_DEVELOPER_PAYLOAD_COL
//    };
    
    private SQLiteDatabase mDb;
    private DatabaseHelper mDatabaseHelper;
    private Context mContext = null;
    private static AESObfuscator _obfuscator;
    
    // Restiuisce l'istanza (singleton per motivi di performance) che cifra i campi del database
    private AESObfuscator getObfuscator() {
    	if (_obfuscator == null) {
    		_obfuscator = new AESObfuscator(BillingSecurity.secretKey, 
            		SecurityHelper.md5digest(SecurityHelper.getDeviceSignature(mContext)));       	
    	}
    	return _obfuscator;
    }

    public BillingDatabase(Context context) {
    	mContext = context;
        mDatabaseHelper = new DatabaseHelper(context);
        mDb = mDatabaseHelper.getWritableDatabase();
        	
        
    }

    public void close() {  mDatabaseHelper.close();  }

    /**
     * Inserts a purchased product into the database. There may be multiple
     * rows in the table for the same product if it was purchased multiple times
     * or if it was refunded.
     * @param orderId the order ID (matches the value in the product list)
     * @param productId the product ID (sku)
     * @param state the state of the purchase
     * @param purchaseTime the purchase time (in milliseconds since the epoch)
     * @param developerPayload the developer provided "payload" associated with
     *     the order.
     */
    public synchronized void insertOrder(String orderId, String productId, PurchaseState state,
            long purchaseTime, String developerPayload) {
        
    	ContentValues values = new ContentValues();
        values.put(PURCHASED_PRODUCT_ID_COL, getObfuscator().obfuscate(productId));
        values.put(PURCHASED_STATE_COL, state.ordinal());
        values.put(PURCHASED_TIME_COL, purchaseTime);
        values.put(PURCHASED_DEVELOPER_PAYLOAD_COL, developerPayload);
        mDb.replace(PURCHASED_TABLE_NAME, null /* nullColumnHack */, values);
        
        ContentValues history = new ContentValues();
        history.put(HISTORY_PRODUCT_ID_COL, productId);
        history.put(HISTORY_STATE_COL, state.toString());
        history.put(HISTORY_TIMESTAMP_COL, purchaseTime);
        history.put(HISTORY_DEVELOPER_PAYLOAD_COL, developerPayload);
        mDb.replace(HISTORY_TABLE_NAME, null, history);

    }

    
    /**
     * Restituisce l'elenco degli item acquistati
     * @return
     */
    public List<String> getAllPurchasedItems() {
    	Cursor c = mDb.query(PURCHASED_TABLE_NAME, PURCHASED_COLUMNS, 
    			PURCHASED_STATE_COL + " = ?", 
    			new String[] { String.valueOf(PurchaseState.PURCHASED.ordinal()) }, 
    			null, null, null);
    	if (c == null) return null;
    	
    	List<String> result = new ArrayList<String>();
    	while (c.moveToNext()) {
    		String itemId = null;
    		try { 
    			itemId = getObfuscator().unobfuscate(c.getString(0)); 
    		} catch (Exception e) {
    			Log.e(TAG, "SecurityException : " + e.getMessage());
    		}
    		if (itemId != null) result.add(itemId);
    	}
    	c.close();
    	return result;
    	
    }

    /**
     * This is a standard helper class for constructing the database.
     */
    private class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	Log.w(TAG, "Creating database");
            createPurchaseTable(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//            if (newVersion != DATABASE_VERSION) {
                Log.w(TAG, "Database upgrade from old: " + oldVersion + " to: " + newVersion);
                db.execSQL("DROP TABLE IF EXISTS " + PURCHASED_TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + HISTORY_TABLE_NAME);
                createPurchaseTable(db);
                return;
//            }
        }

        private void createPurchaseTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE  if not exists  " + PURCHASED_TABLE_NAME + "(" +
            		PURCHASED_PRODUCT_ID_COL + " TEXT PRIMARY KEY, " +
            		PURCHASED_STATE_COL + " INTEGER, " +
            		PURCHASED_TIME_COL + " INTEGER, " + 
            		PURCHASED_DEVELOPER_PAYLOAD_COL + " TEXT" +
            	")");
            db.execSQL("CREATE TABLE  if not exists " + HISTORY_TABLE_NAME + "(" +
            		HISTORY_PRODUCT_ID_COL + " TEXT, " +
            		HISTORY_STATE_COL + " TEXT, " +
            		HISTORY_TIMESTAMP_COL + " INTEGER, " + 
            		HISTORY_DEVELOPER_PAYLOAD_COL + " TEXT" +
            	")");
        }
        

       
    }
}
