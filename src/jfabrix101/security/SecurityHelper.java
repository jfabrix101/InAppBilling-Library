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
package jfabrix101.security;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public class SecurityHelper {

	/**
	 * Restituisce l' androidID del dispositivo
	 */
	public static String getAndroidId(Context c) {
		if (c == null) return "";
		return Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
	}

	/**
	 * Restituisce un codice univoco per un dato dispositivo.
	 * Utilizza i deti dell' androidID + i dati propri del dispositivo (Build.*)
	 * La stringa restituita è in formato MD5
	 * @param c
	 * @return
	 */
	public static String getUniqueDeviceId(Context c) {
        return md5sum(getDeviceSignature(c));
	}
	
	/**
	 * Restituisce la "firma" di un dispositivo. In particolare
	 * restituisce : AndroidID - Brand - Device - Version - Board - ID
	 */
	public static String getDeviceSignature(Context c) {
		StringBuffer b = new StringBuffer();
        String deviceId = getAndroidId(c);
        b.append(deviceId);
        b.append("-" + Build.BRAND);
        b.append("-" + Build.DEVICE);
        b.append("-" + Build.VERSION.RELEASE);
        b.append("-" + Build.BOARD);
        b.append("-" + Build.ID);
        return b.toString();
	}

	/**
	 * Restituisce il codice MD5 di una string
	 * @param s
	 * @return
	 */
	public static String md5sum(String s) {

		try {
			MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			md.update(s.getBytes());

			byte resultSum[] = md.digest();

			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < resultSum.length; i++) {
				String h = Integer.toHexString(0xFF & resultSum[i]);
				while (h.length() < 2) h = "0" + h;
				hexString.append(h);
			}

			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	/*
	 * Restituisce il token di firma (numerico) di una stringa
	 */
	public static String md5digest(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			return new BigInteger(1, md.digest(input.getBytes())).toString(16)
					.toUpperCase();
		} catch (Exception e) {
			return null;
		}
	}
	
}
