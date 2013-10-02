package com.zehjot.smartday.data_access;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.zehjot.smartday.MainActivity;
import com.zehjot.smartday.data_access.DataSet.onDataAvailableListener;

public class DownloadTask extends AsyncTask<String, Void, JSONObject> implements OnCancelListener{
	private int serverResponse = -1;
	private ProgressDialog progress;
	private String request;
	private long timestamp;
	private onDataAvailableListener requester;
	private static Activity activity;
	private String fileName = null;
	private static onDataDownloadedListener listener = DataSet.getInstance(activity);
	
	protected interface onDataDownloadedListener{
		public void onDataDownloaded(int serverResponse, JSONObject jObj, long timestamp,String request, onDataAvailableListener requester, String fileName);
	}
	protected DownloadTask(onDataAvailableListener requester, Activity activity){
		this.requester = requester;
		DownloadTask.activity = activity;
	}
	protected DownloadTask(){
	}
	
	
	@Override
	protected void onPreExecute(){
		super.onPreExecute();
		progress = new ProgressDialog(activity);
		progress.setCanceledOnTouchOutside(false);
		progress.setMessage("Downloading data from server...");
		progress.setOnCancelListener( this);
		if(((MainActivity) activity).isRunning())
			progress.show();
	}
	
	@Override
	protected JSONObject doInBackground(String... url) {
		while(!isCancelled()){
			if(url.length>2){
				request = url[1];
				fileName = url[2];
				if(url.length>3)
					timestamp = Long.valueOf(url[3]);
				else
					timestamp=-1;
			}
			ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if(networkInfo == null || !networkInfo.isConnected()){
				return null;
			}
			try{
				return downloadData(url[0]);
			} catch (IOException e){	
				return null;
			} catch (JSONException e) {	
				return null;
			}
		}
		return null;
	}
	@Override
	protected void onPostExecute(JSONObject result) {
		super.onPostExecute(result);
		if(((MainActivity)activity).isRunning())
			progress.cancel();
		try {
			result.put("date", timestamp);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		listener.onDataDownloaded(serverResponse, result, timestamp,request, requester, fileName);
	}
	
	private JSONObject downloadData(String urlString) throws IOException, JSONException{
		InputStream is = null;
		String json = null;
		try{
			//creating URL object and open connection
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000 /*ms*/);
			conn.setConnectTimeout(15000/*ms*/);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			
			//Start query
			conn.connect();
			serverResponse = conn.getResponseCode();
			Log.d("HTTPDebug", "Response: "+serverResponse);
			is = conn.getInputStream();
			//InpuStream to JSONObeject
			try{
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
				StringBuilder sb = new StringBuilder();
	            String line = null;
	            while ((line = reader.readLine()) != null) {
					if(isCancelled())
						return null;
	                sb.append(line + "\n");
	            }
	            json = sb.toString();
			}catch (Exception e) {
	            Log.e("Buffer Error", "Error converting result " + e.toString());
	        }
			return new JSONObject(json);
		}finally{
	        if (is != null) {
	        	//close inputstream
	            is.close();
	        } 
		}
		
		
	}
	@Override
	protected void onCancelled(JSONObject result) {
		super.onCancelled(result);
		listener.onDataDownloaded(-2, null, timestamp,request, requester, fileName);		
	}
	@Override
	public void onCancel(DialogInterface dialog) {
		cancel(true);
	}
	
}
