package com.zehjot.smartday;

public class Config {
	private static final String appID ="5";
	private static final String appSecret ="9lycn2n42mgave0pgatx5s6po6zg4b0rfm39exbs6fdll0iuvm";
	private static final String domain ="http://api.learning-context.de/";
	private static final String apiVersion = "2/";
	private static final boolean debug = true;
	
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
}
