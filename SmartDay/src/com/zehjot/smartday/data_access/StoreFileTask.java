package com.zehjot.smartday.data_access;

import android.app.Activity;
import android.os.AsyncTask;

import com.zehjot.smartday.R;
import com.zehjot.smartday.helper.Utilities;

public class StoreFileTask extends AsyncTask<String, Void, Boolean>{
	private Activity activity;
	
	protected StoreFileTask(Activity act){
		this.activity = act;
	}
	
	@Override
	protected void onPreExecute(){
		super.onPreExecute();
	}
	
	@Override
	protected Boolean doInBackground(String... params) {
		if(params[0]==null||params[1]==null)
			return false;
		String jsonString = params[0];
		String fileName = params[1];
		return storeData(jsonString, fileName); 
	}
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		if(!result)
			Utilities.showDialog(activity.getString(R.string.error_storing_file), activity);
	}
	
	private Boolean storeData(String jsonString, String fileName){
		return Utilities.writeFile(fileName, jsonString, activity);
	}
}
