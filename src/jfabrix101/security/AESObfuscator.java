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

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;

/**
 * An obfuscator that uses AES to encrypt data.
 */
public class AESObfuscator {
	
    private static final String UTF8 = "UTF-8";
    private static final String KEYGEN_ALGORITHM = "PBEWITHSHAAND256BITAES-CBC-BC";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final byte[] IV = { 16, 74, 71, -80, 32, 101, -47, 72, 117, -14, 0, -29, 70, 65, -12, 74 };
    private static final String header = AESObfuscator.class.getName() + "-1|";

    private Cipher mEncryptor;
    private Cipher mDecryptor;

    public static AESObfuscator getDefaultObfuscator(Context c) {
    	return new AESObfuscator(SecurityHelper.getAndroidId(c).getBytes(),
    			SecurityHelper.getDeviceSignature(c));
    }
    
    
    public AESObfuscator(byte[] salt, String password) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEYGEN_ALGORITHM);
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 1024, 256);
            SecretKey tmp = factory.generateSecret(keySpec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
            mEncryptor = Cipher.getInstance(CIPHER_ALGORITHM);
            mEncryptor.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(IV));
            mDecryptor = Cipher.getInstance(CIPHER_ALGORITHM);
            mDecryptor.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(IV));
        } catch (GeneralSecurityException e) {
            // This can't happen on a compatible Android device.
            throw new RuntimeException("Invalid environment", e);
        }
    }

    public String obfuscate(String original) {
        if (original == null)  return null;
        
        try {
            return Base64.encodeBytes(mEncryptor.doFinal((header + original).getBytes(UTF8)));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Invalid environment", e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Invalid environment", e);
        }
    }

    public String unobfuscate(String obfuscated) throws ValidationException {
        if (obfuscated == null) return null;
        try {
            String result = new String(mDecryptor.doFinal(Base64.decode(obfuscated)));
            // Check for presence of header. This serves as a final integrity check, for cases
            // where the block size is correct during decryption.
            int headerIndex = result.indexOf(header);
            if (headerIndex != 0) {
                throw new ValidationException("Header not found (invalid data or key)" + ":" +
                        obfuscated);
            }
            return result.substring(header.length(), result.length());
        } catch (Exception e) {
            throw new ValidationException(e.getMessage() + ":" + obfuscated);
        } 
    }
    
    public class ValidationException extends Exception {
        public ValidationException() {
          super();
        }

        public ValidationException(String s) {
          super(s);
        }

        private static final long serialVersionUID = 1L;
    }
    
}