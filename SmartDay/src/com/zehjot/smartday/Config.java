package com.zehjot.smartday;

import android.app.Activity;
import android.content.res.Configuration;
import android.util.TypedValue;

public class Config {
	private static final String appID ="5";
	private static final String appSecret ="9lycn2n42mgave0pgatx5s6po6zg4b0rfm39exbs6fdll0iuvm";
	private static final String domain ="http://api.learning-context.de/";
	private static final String apiVersion = "2/";
	private static final boolean debug = true;
	
	private static final int textSizeBigScreen = 18;
	private static final int textSizeSmallScreen = 14;
	private static int textSizeBigScreenPx = -1;
	private static int textSizeSmallScreenPx = -1;
	private static int bigScreen = -1;
	public static String getAppID(){
		return appID;
	}

	public static String getAppSecret() {
		return appSecret;
	}

	public static String getDomain() {
		return domain;
	}

	public static String getApiVersion() {
		return apiVersion;
	}
	
	public static boolean getDebug() {
		return debug;
	}
	
	public static class Request{
		public static final String values = "values";
		public static final String events = "events";
		public static final String testuser = "testcredentials";		
	}
	
	public static int getTextSize(Activity act){
		if(bigScreen==-1){
			if((act.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) <
		        Configuration.SCREENLAYOUT_SIZE_LARGE){
				bigScreen = 0;
			}else{
				bigScreen = 1;
			}
		}
		if(bigScreen == 0){
			return textSizeSmallScreen;
		}else{
			return textSizeBigScreen;
		}
	}
	public static int getTextSizeInPx(Activity act){
		if(bigScreen==-1){
			if((act.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) <
		        Configuration.SCREENLAYOUT_SIZE_LARGE){
				bigScreen = 0;
			}else{
				bigScreen = 1;
			}
		}
		if(bigScreen == 0){
			if(textSizeSmallScreenPx==-1)
				textSizeSmallScreenPx = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, textSizeSmallScreen, act.getResources().getDisplayMetrics())+0.5);
			return textSizeSmallScreenPx;
		}else{
			if(textSizeBigScreenPx==-1)
				textSizeBigScreenPx = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, textSizeBigScreen, act.getResources().getDisplayMetrics())+0.5);
			return textSizeBigScreenPx;
		}
	}
}
