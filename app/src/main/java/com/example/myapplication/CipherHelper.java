package com.example.myapplication;
import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CipherHelper {
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_ALGORITHM = "AES";
    private static final String SECRET_KEY  = "cW2eHrRFRyjOr4UKwSv4WA==";
    private static final String INIT_VECTOR = "encryptionIntVec";

    public static String encrypt(String decryptedString) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), SECRET_KEY_ALGORITHM);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(INIT_VECTOR.getBytes());

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

        byte[] encryptedBytes = cipher.doFinal(decryptedString.getBytes());
        String encryptedString = Base64.encodeToString(encryptedBytes, Base64.DEFAULT);

        return encryptedString;
    }

    public static String decrypt(String encryptedString) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), SECRET_KEY_ALGORITHM);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(INIT_VECTOR.getBytes());

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] encryptedBytes = Base64.decode(encryptedString, Base64.DEFAULT);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        String decryptedString = new String(decryptedBytes);

        return decryptedString;
    }
}


