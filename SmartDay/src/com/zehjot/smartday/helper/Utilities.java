package com.zehjot.smartday.helper;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.zehjot.smartday.Config;
import com.zehjot.smartday.MainActivity;
import com.zehjot.smartday.R;

public class Utilities{
	public static String getFileName(String requestedFunction, JSONObject user,JSONObject jObj,Activity activity){
		String fileName = requestedFunction;
		
		try {
			fileName += user.getString(activity.getString(R.string.user_pass));
			fileName += user.getString(activity.getString(R.string.user_name));
			fileName += jObj.getString("start");
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		return Security.sha1(fileName);
	}
	public static long getTimestamp(int year, int month, int day, int h, int m, int s){
		Calendar c = Calendar.getInstance();
		c.set(year, month, day,h,m,s);
		long date = c.getTimeInMillis();
		return date/1000;
	}
	
	public static long getSystemTime(){
		Calendar c = Calendar.getInstance();
		return c.getTimeInMillis();
	}

	public static Boolean writeFile(String filename, String data, Activity activity){
		String encryptedData = Security.encrypt(data);
		FileOutputStream fos;
		try{
			fos = activity.openFileOutput(filename, Context.MODE_PRIVATE);
			fos.write(encryptedData.getBytes());
			fos.close();
		}catch(IOException e){
			showDialog("Error while storing data to file: "+filename, activity);
			return false;
		}		
		return true;
	}
	public static String readFile(String filename, Activity activity){
		FileInputStream fis;
		StringBuilder sb = null;
		String data = null;
		try{
			fis = activity.openFileInput(filename);
			InputStreamReader inputStreamReader = new InputStreamReader(fis);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			sb = new StringBuilder();
			while ((data = bufferedReader.readLine()) != null) {
				sb.append(data);
			}
			fis.close();
		}catch(IOException e){
			showDialog("File: "+filename+" not found",activity);
			return null;
		}
		data=sb.toString();
		String decryptedData = Security.decrypt(data);
		return decryptedData;
	}
	
	public static String getURL(String queryType,String data,JSONObject user, Activity activity){
		if(user==null||activity==null){
			return null;
		}
		try {
		    List<NameValuePair> params = new LinkedList<NameValuePair>();
		    String nonce = Security.getNonce();
    	    if(data != null){
    	    	params.add(new BasicNameValuePair("data", data));
    	    }else{
    	    	data="";
    	    }
	    	params.add(new BasicNameValuePair("nonce", nonce));		    
	    	params.add(new BasicNameValuePair("aid", Config.getAppID()));
			params.add(new BasicNameValuePair("user", user.getString(activity.getString(R.string.user_name))));
			String dataAsURL ="";
			try {
				dataAsURL = URLEncoder.encode(data, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			params.add(new BasicNameValuePair("h", 
				Security.sha1(	
					dataAsURL
					+Config.getAppID()
					+user.getString(activity.getString(R.string.user_name))
					+nonce
					+Config.getAppSecret()
					+user.getString(activity.getString(R.string.user_pass))
				)
			));
			return Config.getDomain()
					+Config.getApiVersion()
					+queryType
					+"?"
					+URLEncodedUtils.format(params, "utf-8");
				
		}catch (JSONException e) {
			showDialog("JSONError in getURL",activity);
			e.printStackTrace();
			return null;
		}
	}
	public static ArrayList<String> jObjValuesToArrayList(JSONObject[] jObjs){
		ArrayList<String> list = new ArrayList<String>();
		for(int h=0;h<jObjs.length;h++){
			JSONObject jObj = jObjs[h];
			JSONArray jArray;
			try {
				jArray = jObj.getJSONArray("result");
				for(int i=0;i < jArray.length();i++ ){
					if(!list.contains(jArray.getJSONObject(i).getString("app")))
						list.add(jArray.getJSONObject(i).getString("app"));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return list;
	}
	public static void showDialog(String message, Activity activity){
	
		ShowDialogFragment dialog = new ShowDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString("string", message);
		dialog.setArguments(bundle);
		if(((MainActivity) activity).isRunning())
			dialog.show(activity.getFragmentManager(),"Dialog");
	}
	public static class ShowDialogFragment extends DialogFragment{
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getActivity());
			builder.setMessage(getArguments().getString("string"))
				.setCancelable(false)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			           @Override
			           public void onClick(DialogInterface dialog, int id) {
			           }
			       });
			return builder.create();			 
		}
	}
	public static String getTimeFromTimeStamp(long timestampInSec){
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timestampInSec*1000);
		String result = "";
		int h = c.get(Calendar.HOUR_OF_DAY);
		if(h<10)
			result += "0"+h+":";
		else
			result += h+":";
		int m = c.get(Calendar.MINUTE);
		if(m<10)
			result += "0"+m+":";
		else
			result += m+":";
		int s = c.get(Calendar.SECOND);
		if(s<10)
			result += "0"+s;
		else
			result += s;
		return result;
	}
	public static String getTimeString(long sec){
		String durationAsString = "";
	    int h = (int)sec/3600;
	    int m = ((int)sec%3600)/60;
	    int s = ((int)sec%3600)%60;
	    if(h>0)
	    	durationAsString += h+"h ";
	    if(m>0)
	    	durationAsString += m+"min ";
	    if(s>0)
	    	durationAsString += s+"sec";
	    if(durationAsString.equals(""))
	    	durationAsString = "0 sec";
	    return durationAsString;
	}
	public static String getDate(long timestampInSec){
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timestampInSec*1000);
		int day = c.get(Calendar.DAY_OF_MONTH);
		String month=c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
		int year = c.get(Calendar.YEAR);
		return day+". "+month+" "+year;
	}
	
	public static String getDateWithDay(long timestampInSec){
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timestampInSec*1000);
		int day = c.get(Calendar.DAY_OF_MONTH);
		String weekday = c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US);
		String month=c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
		int year = c.get(Calendar.YEAR);
		return weekday+", "+day+". "+month+" "+year;
	}
	
	public static int getTimeOfDay(long sec){
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(sec*1000);
		int h = c.get(Calendar.HOUR_OF_DAY);
		int m = c.get(Calendar.MINUTE);
		int s = c.get(Calendar.SECOND);
		return (h*60+m)*60+s;
	}
}