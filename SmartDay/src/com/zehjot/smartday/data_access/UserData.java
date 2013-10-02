package com.zehjot.smartday.data_access;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.zehjot.smartday.Config;
import com.zehjot.smartday.MainActivity;
import com.zehjot.smartday.R;
import com.zehjot.smartday.helper.Security;
import com.zehjot.smartday.helper.Utilities;

public class UserData {
	private static Activity activity = null;
	private static JSONObject user = null;
	private static JSONObject tmpUser = null;
	private static OnUserDataAvailableListener mCallBack;
	/**
	 * Functions to handle user data (Name, Password, Email)
	 * ask  - opens an alertDialog in which data can be written
	 * set  - stores if wanted login data in external file 
	 * get  - gets name, pw, email
	 * test - verifies login data with server
	 */

	public interface OnUserDataAvailableListener{
		public void onUserDataAvailable(JSONObject jObj);
	}
	public void getUserLoginData(Context context){
		getUserLoginData(context, false);
	}
	public void updateActivity(Activity act){
		activity = act;
	}
	public void getUserLoginData(Context context, boolean createNew){
		if(context != null)
			activity = (Activity) context;
		if(createNew || user == null){
			mCallBack = (OnUserDataAvailableListener) DataSet.getInstance(activity);
			File file = activity.getFileStreamPath(Security.sha1(activity.getString(R.string.user_file)));
			if(!file.exists()||createNew){
				askForUserLogInData();
			}
			else{
				loadUserLogInData();
			}
		}else{
			mCallBack.onUserDataAvailable(user);
		}
	}
	
	private static void askForUserLogInData(){
		LoginDialogFragment login = new LoginDialogFragment(); 
		if(((MainActivity) activity).isRunning())
			login.show(activity.getFragmentManager(), "loginDialog");
	}

	private static void createUserLogInData(String name, String pass, String email, Boolean saveData){			
		JSONObject jObj = new JSONObject();
		try {
			jObj.put(activity.getString(R.string.user_pass), Security.sha1(pass));
			jObj.put(activity.getString(R.string.user_name), name);
			if(email != null)
				jObj.put(activity.getString(R.string.user_email), name);
				
		} catch (JSONException e) {
			Utilities.showDialog("JSONError:USER CREATE",activity);				
		}
		if(saveData){
			Utilities.writeFile(Security.sha1(activity.getString(R.string.user_file)),jObj.toString(),activity);
			loadUserLogInData();
		}else{
			File file = activity.getFileStreamPath(Security.sha1(activity.getString(R.string.user_file)));
			if(file.exists()){
				file.delete();
			}
			tmpUser = jObj;
			testUserLogInData();
		}
		
	}

	private static void testUserLogInData(){
		String url = Utilities.getURL(Config.Request.testuser,null,tmpUser,activity);
		new DownloadTask().execute(url);
	}

	private static void loadUserLogInData(){
		String data = Utilities.readFile(Security.sha1(activity.getString(R.string.user_file)),activity);
		JSONObject jObj = null;
		try {
			jObj = new JSONObject(data);
		} catch (JSONException e) {
			Utilities.showDialog("JSON Object failed",activity);
			e.printStackTrace();
		}
		tmpUser = jObj;
		testUserLogInData();
	}
	private static class DownloadTask extends AsyncTask<String, Void, Boolean>{
		private ProgressDialog progress;
		@Override
		protected Boolean doInBackground(String... url) {
			ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if(networkInfo == null || !networkInfo.isConnected()){
				return null;
			}
			try{
				return downloadData(url[0]);
			} catch (IOException e){	
				return null;
			} 
		}
		@Override
		protected void onPreExecute(){
			super.onPreExecute();
			progress = new ProgressDialog(activity);
//			progress.setCancelable(false);
			progress.setCanceledOnTouchOutside(false);
			progress.setMessage("Testing user data...");
			if(((MainActivity) activity).isRunning())
				progress.show();
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			if(((MainActivity)activity).isRunning())
				progress.cancel();
			super.onPostExecute(result);
			ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if(networkInfo == null || !networkInfo.isConnected()){
				Utilities.showDialog(activity.getString(R.string.info_no_data_connection), activity);
				mCallBack.onUserDataAvailable(tmpUser);
				return;
			}
			if(result==null || result== false){
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setMessage(activity.getString(R.string.error_authentication_fail))
					.setCancelable(false)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				           @Override
				           public void onClick(DialogInterface dialog, int id) {
								askForUserLogInData();
				           }
				       });
				AlertDialog dialog = builder.create();
				if(((MainActivity) activity).isRunning())
					dialog.show();
			}
			else{
				user = tmpUser;
				tmpUser = null;
				mCallBack.onUserDataAvailable(user);
			}
		}
		
		private Boolean downloadData(String urlString) throws IOException{
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
				Log.d("Verify User", "Response: "+conn.getResponseCode());
				is = conn.getInputStream();
				
				//InpuStream to JSONObeject
				try{
					BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
					StringBuilder sb = new StringBuilder();
		            String line = null;
		            while ((line = reader.readLine()) != null) {
		                sb.append(line + "\n");
		            }
		            json = sb.toString();
				}	catch (Exception e) {
		            Log.e("Buffer Error", "Error converting result " + e.toString());
		        }
				try{
					JSONObject jObj = new JSONObject(json);
					if(jObj.get("result").toString().equals("1"))
						return true;
					else
						return false;
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}finally{
		        if (is != null) {
		        	//close inputstream
		            is.close();
		        } 
			}
			
		return false;	
		}
		
	}
	public static class LoginDialogFragment extends DialogFragment{
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			LayoutInflater inflater = activity.getLayoutInflater();
			final View view = inflater.inflate(R.layout.dialog_auth,null);
			builder.setView(view)
				.setTitle("Authentication")
				.setCancelable(false)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
		           @Override
		           public void onClick(DialogInterface dialog, int id) {
		        	   EditText editText = (EditText) view.findViewById(R.id.auth_user_name);
		        	   String user_name = editText.getText().toString();
		        	   editText = (EditText) view.findViewById(R.id.auth_user_pass);
		        	   String user_pass = editText.getText().toString();
		        	   editText = (EditText) view.findViewById(R.id.auth_user_email);
		        	   String user_email = editText.getText().toString();
		        	   CheckBox saveData = (CheckBox) view.findViewById(R.id.auth_save_user_data);
		        	   Boolean save = saveData.isChecked();
		        	   createUserLogInData(user_name, user_pass, user_email, save);
		           }
		       })
		       
		       .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
		           @Override
		           public void onClick(DialogInterface dialog, int id) {
		           }
		       });
			return builder.create();
		}
		
	}
}
