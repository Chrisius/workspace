package com.zehjot.smartday.data_access;

import org.json.JSONException;
import org.json.JSONObject;

import com.zehjot.smartday.MainActivity;
import com.zehjot.smartday.data_access.DataSet.onDataAvailableListener;
import com.zehjot.smartday.helper.Utilities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

public class LoadFileTask extends AsyncTask<String, Void, JSONObject>{
	private ProgressDialog progress;
	private String request;
	private onDataAvailableListener requester;
	private String fileName;
	private static Activity activity;
	private static onDataLoadedListener listener = (onDataLoadedListener) DataSet.getInstance(activity);
	
	protected interface onDataLoadedListener{
		public void onDataLoaded(JSONObject jObj, String request, onDataAvailableListener requester, String fileName);
	}
	
	protected LoadFileTask(onDataAvailableListener requester, Activity activity){
		this.requester = requester;
		LoadFileTask.activity = activity;
	}
	protected LoadFileTask(){
	}
	
	@Override
	protected void onPreExecute(){
		super.onPreExecute();
		progress = new ProgressDialog(activity);
		progress.setCanceledOnTouchOutside(false);
		progress.setMessage("Loading data from internal file...");
		if(((MainActivity) activity).isRunning())
			progress.show();
	}
	
	@Override
	protected JSONObject doInBackground(String... params) {
		fileName = params[0];
		request = params[1];
		return loadData(fileName);
	}
	@Override
	protected void onPostExecute(JSONObject result) {
		super.onPostExecute(result);
		progress.cancel();
		listener.onDataLoaded(result, request, requester, fileName);		
	}
	
	private JSONObject loadData(String fileName){
		String json = Utilities.readFile(fileName, activity);
		try {
			return new JSONObject(json);
		} catch (JSONException e) {
			e.printStackTrace();
		}	
		return null;
	}
	
}


