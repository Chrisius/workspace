package com.zehjot.smartday.helper;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

import android.util.Base64;

public class Security {
	public static String encrypt(String data){
		return Base64.encodeToString(data.getBytes(), Base64.DEFAULT);
	}

	public static String decrypt(String data){
		return new String(Base64.decode(data, Base64.DEFAULT));
	}
	
	public static String sha1(String s){
        MessageDigest digest = null;
        try {
                digest = MessageDigest.getInstance("SHA-1");
	        } catch (Exception e) {
                e.printStackTrace();
        }
		digest.reset();
		byte[] data = digest.digest(s.getBytes());
		String result = String.format("%0" + (data.length*2) + "X", new BigInteger(1, data)).toLowerCase(Locale.ENGLISH);
		return 	result;
	}
	public static String getNonce() { 
		SecureRandom sr = null;	
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
		String nonce = new BigInteger(256, sr).toString(26);
		return nonce;
	}
}
