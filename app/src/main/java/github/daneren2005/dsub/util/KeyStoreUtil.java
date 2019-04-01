package github.daneren2005.dsub.util;

import android.annotation.TargetApi;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;

@TargetApi(23)
public class KeyStoreUtil {
    private static String TAG = KeyStoreUtil.class.getSimpleName();
    private static final String KEYSTORE_ALIAS = "DSubKeyStoreAlias";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEYSTORE_CIPHER_PROVIDER = "AndroidKeyStoreBCWorkaround";
    private static final String KEYSTORE_TRANSFORM = "AES/CBC/PKCS7Padding";
    private static final String KEYSTORE_BYTE_ENCODING = "UTF-8";

    public static void loadKeyStore() throws KeyStoreException, IOException,
            CertificateException, NoSuchAlgorithmException {

        // Load keystore
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        // Check if keystore has been used before
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            // If alias does not exist, keystore hasn't been used before
            // Create a new secret key to store in the keystore
            try {
                Log.w(TAG, "Generating keys.");
                generateKeys();
            } catch (Exception e) {
                Log.w(TAG, "Key generation failed.");
                Log.w(TAG, Log.getStackTraceString(e));
            }
        }
    }

    private static void generateKeys() throws InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException {
        KeyGenerator keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);
        keyGen.init(new KeyGenParameterSpec.Builder(KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build());
        keyGen.generateKey();
    }

    private static Key getKey() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException, UnrecoverableEntryException {

        // Attempt to load keystore
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        // Fetch and return secret key
        return keyStore.getKey(KEYSTORE_ALIAS, null);
    }

    public static String encrypt(@NonNull String plainTextString) {
        Log.d(TAG, "Encrypting password...");
        try {
            // Retrieve secret key
            final Key key = getKey();

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(KEYSTORE_TRANSFORM, KEYSTORE_CIPHER_PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            // Create stream for storing data
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            // Write the IV length first so the IV can be split from the encrypted password
            outputStream.write(cipher.getIV().length);

            // Write the auto-generated IV
            outputStream.write(cipher.getIV());

            // Encrypt the plaintext and write the encrypted string
            outputStream.write(cipher.doFinal(plainTextString.getBytes(KEYSTORE_BYTE_ENCODING)));

            // Encode the return full stream for storage
            Log.d(TAG, "Password encryption successful");
            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);

        } catch (Exception e) {
            Log.w(TAG, "Password encryption failed");
            Log.d(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    public static String decrypt(@NonNull String encryptedString) {
        Log.d(TAG, "Decrypting password...");
        try {
            // Retrieve secret key
            final Key key = getKey();

            // Decode the string from Base64
            byte[] decodedBytes = Base64.decode(encryptedString, Base64.NO_WRAP);
            int ivLength = decodedBytes[0];
            int encryptedLength = decodedBytes.length - (ivLength + 1);

            // Get IV from decoded string
            byte[] ivBytes = new byte[ivLength];
            System.arraycopy(decodedBytes, 1, ivBytes, 0, ivLength);

            // Get encrypted password from decoded string
            byte[] encryptedBytes = new byte[encryptedLength];
            System.arraycopy(decodedBytes, ivLength + 1, encryptedBytes, 0, encryptedLength);

            // Initialize cipher using the IV from the dencoded string
            Cipher cipher = Cipher.getInstance(KEYSTORE_TRANSFORM, KEYSTORE_CIPHER_PROVIDER);
            IvParameterSpec ivParamSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParamSpec);

            // Decrypt the password
            String decryptedString = new String(cipher.doFinal(encryptedBytes));

            // Return the decrypted password string
            Log.d(TAG, "Password successfully decrypted");
            return decryptedString;

        } catch (Exception e) {
            Log.w(TAG, "Password decryption failed");
            Log.w(TAG, Log.getStackTraceString(e));
            return null;
        }
    }
}
