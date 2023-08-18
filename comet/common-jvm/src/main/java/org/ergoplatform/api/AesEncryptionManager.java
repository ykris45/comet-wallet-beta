package org.ergoplatform.api;

import org.ergoplatform.appkit.SecretString;
import org.jetbrains.annotations.NotNull;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * Encryption / Decryption service using the AES algorithm
 * example for nullbeans.com
 */
public class AesEncryptionManager {

    // On Android API below 19, we need to use another Parameter Spec as the current one is not available.
    // It is compatible with the newer one, so when RoboVM switched to API 26 we can remove this code.
    public static boolean isOnLegacyApi = false;

    /**
     * This method will encrypt the given data using the given password.
     *
     * @param password the password that will be used to encrypt the data
     * @param data     the data that will be encrypted
     * @return Encrypted data in a byte array
     */
    public static byte[] encryptData(SecretString password, byte[] data) throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException, InvalidKeySpecException {

        // Prepare the initialization vector (IV, aka `salt`)
        // IV should be 12 bytes
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[12];
        secureRandom.nextBytes(iv);

        //Prepare your key based on the password and the salt
        SecretKey secretKey = generateSecretKey(password, iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        AlgorithmParameterSpec parameterSpec = getParameterSpec(iv);

        //Initialize the cipher for encryption mode!
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        return encryptWithCipher(data, iv, cipher);
    }

    @NotNull
    private static AlgorithmParameterSpec getParameterSpec(byte[] iv) {
        if (isOnLegacyApi)
            return new IvParameterSpec(iv);
        else
            return new GCMParameterSpec(128, iv);
    }

    /**
     * Encrypts the given data using the given {@link Cipher} and the salt.
     *
     * @param data   data to be encrypted
     * @param iv     initialization vector (aka salt)
     * @param cipher which is initialized for encryption
     * @return encrypted data packed with the salt.
     */
    @NotNull
    static byte[] encryptWithCipher(byte[] data, byte[] iv, Cipher cipher) throws BadPaddingException, IllegalBlockSizeException {
        //Encrypt the data
        byte[] encryptedData = cipher.doFinal(data);

        // wipe data
        Arrays.fill(data, (byte) 0);

        //Concatenate everything and return the final data
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + encryptedData.length);
        byteBuffer.putInt(iv.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedData);
        return byteBuffer.array();
    }


    public static byte[] decryptData(SecretString password, byte[] encryptedData)
            throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException,
            InvalidKeySpecException {

        DecryptionData data = new DecryptionData(encryptedData);

        //Prepare your key based on the password and the salt
        SecretKey secretKey = generateSecretKey(password, data.iv);

        return decryptWithSecretKey(data, secretKey);
    }

    static byte[] decryptWithSecretKey(DecryptionData data, SecretKey secretKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        AlgorithmParameterSpec parameterSpec = getParameterSpec(data.iv);

        //Encryption mode on!
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        //decrypt the data
        return cipher.doFinal(data.cipherBytes);
    }

    /**
     * Function to generate a 128 bit key from the given password and iv
     *
     * @param password used to derive the secret key
     * @param iv initialization vector (aka salt)
     * @return Secret key
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private static SecretKey generateSecretKey(SecretString password, byte[] iv) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.getData(), iv, 65536, 128); // AES-128
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] key = secretKeyFactory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }

    static class DecryptionData {
        final byte[] iv;
        final byte[] cipherBytes;

        DecryptionData(byte[] encryptedData) {
            //Wrap the data into a byte buffer to ease the reading process
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);

            int nonceSize = byteBuffer.getInt();

            //Make sure that the file was encrypted properly
            if (nonceSize < 12 || nonceSize >= 16) {
                throw new IllegalArgumentException("Nonce size is incorrect. Make sure that the incoming data is an AES encrypted file.");
            }
            iv = new byte[nonceSize];
            byteBuffer.get(iv);

            //get the rest of encrypted data
            cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);
        }
    }
}