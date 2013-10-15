package com.zehjot.smartday.data_access;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zehjot.smartday.Config;
import com.zehjot.smartday.MainActivity;
import com.zehjot.smartday.R;
import com.zehjot.smartday.data_access.DownloadTask.onDataDownloadedListener;
import com.zehjot.smartday.data_access.LoadFileTask.onDataLoadedListener;
import com.zehjot.smartday.data_access.UserData.OnUserDataAvailableListener;
import com.zehjot.smartday.helper.Utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class DataSet implements OnUserDataAvailableListener, onDataDownloadedListener, onDataLoadedListener{
	private static DataSet instance = null;
	private static UserData userData = null;
	private static Activity activity = null;
	private static JSONObject user = null;
	private static long queryStart;
	private static JSONObject selectedApps = null;
	private static JSONObject ignoreApps = null;
	private static JSONObject selectedHighlightApps=null;
	private static JSONObject tmpJSONResult = null;
	private static JSONObject tmpJSONResultToday = null;
	private static long todayCacheMin = 5*60*1000;
	private static JSONObject colorsOfApps = null;
	private static boolean changedIgnoreApps=false;
	
	private static long startDate;
	private static long endDate;
	
	private static int numberSelectedDays;
	private static JSONObject[] days=null;
	
	public static class RequestedFunction{
		public static final String getAllApps= "getAllApps";
		public static final String getEventsAtDate= "getEventsAtDate";
		public static final String updatedFilter= "updatedFilter";
		public static final String getPositions = "getPositions";
	}


	
	protected DataSet(){
		//For Singleton
	}
	
	public static DataSet getInstance(Context context){
		if(instance == null){
			init(context);
		}
		return instance;
	}
	public static void updateActivity(Activity act){
		if(act == null)
			return;
		activity = act;
		userData.updateActivity(act);
	}
	
	public static JSONObject getUser() {
		return user;
	}
	
	private static void init(Context context){
		instance = new DataSet();
		activity = (Activity) context;
		selectedApps = new JSONObject();
		selectedHighlightApps = new JSONObject();
		startDate=endDate=Utilities.getTodayTimestamp();
		if( activity.getFileStreamPath(activity.getString(R.string.file_ignored_apps)).exists()){
			try {
				ignoreApps = new JSONObject(Utilities.readFile(activity.getString(R.string.file_ignored_apps), activity));
			} catch (JSONException e) {
				e.printStackTrace();
				ignoreApps = new JSONObject();
			}
		}else
			ignoreApps = new JSONObject();
		
		if( activity.getFileStreamPath(activity.getString(R.string.file_app_colors)).exists()){
			try {
				colorsOfApps = new JSONObject(Utilities.readFile(activity.getString(R.string.file_app_colors), activity));
			} catch (JSONException e) {
				colorsOfApps = new JSONObject();
				e.printStackTrace();
			}
		}else
			colorsOfApps = new JSONObject();
		tmpJSONResult = new JSONObject();//TODO not capable of multi day support
		tmpJSONResultToday = new JSONObject();

		createUserData();
	}
	
	
	private static void createUserData(){
		if(userData==null)
			userData = new UserData(activity);
		userData.getUserLoginData();
	}
	
	public void createNewUser(){
		userData.getNewUserLoginData();
	}
	
	public interface onDataAvailableListener{
		public void onDataAvailable(JSONObject[] results, String requestedFunction);//TODO no string parameter
	}
	
	public void getApps(onDataAvailableListener listener){
		long sec = (getSelectedDateEndAsTimestamp()-getSelectedDateStartAsTimestamp());
		int numberOfdays = (int) (sec/(24*60*60))+1;
		numberSelectedDays = numberOfdays;
		if(days!=null &&!changedIgnoreApps&&days[0].optLong("dateTimestamp")==getSelectedDateStartAsTimestamp()&&days[days.length-1].optLong("dateTimestamp")==getSelectedDateEndAsTimestamp()){
			listener.onDataAvailable(days, RequestedFunction.getEventsAtDate);
			return;
		}
		changedIgnoreApps=false;
		DataSet.days = new JSONObject[numberSelectedDays];
		manageMultipleDays(0,listener);
	}
	
	private void manageMultipleDays(int i,onDataAvailableListener listener){
			long date=getSelectedDateStartAsTimestamp()+(24*60*60*i);
			getAppsAtDate(date,listener);
	}
	
	
	private void getAppsAtDate(long timestamp, onDataAvailableListener listener){
		long start=timestamp;
		long end=getNextDayAsTimestamp(start);
		
		JSONObject data = new JSONObject();
		try {
			data.put("model", "SPECIFIC");
			data.put("start", start);
			data.put("end", end);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if(listener != null)
			getData(listener, RequestedFunction.getEventsAtDate, data);
	}
	
	public JSONObject getSelectedApps(){
		return selectedApps;
	}
	public JSONObject getSelectedHighlightApps(){
		return selectedHighlightApps;
	}
	public JSONObject getIgnoreApps() {
		return ignoreApps;
	}
	
	public void setIgnoreApps(JSONObject ignoreApps) {
		DataSet.ignoreApps = ignoreApps;
		Utilities.writeFile(activity.getString(R.string.file_ignored_apps), ignoreApps.toString(), activity);
		changedIgnoreApps=true;
		getApps((onDataAvailableListener) activity);//(null, RequestedFunction.updatedFilter);
		//TODO Notify in another more clean way
	}
	public void setSelectedApps(JSONObject selectedApps){
		DataSet.selectedApps = selectedApps;//TODO Notify in another more clean way
		((onDataAvailableListener)activity).onDataAvailable(null, RequestedFunction.updatedFilter);
	}
	
	public void setSelectedHighlightApps(JSONObject selectedHighlightApps){
		DataSet.selectedHighlightApps = selectedHighlightApps;//TODO Notify in another more clean way
		((onDataAvailableListener)activity).onDataAvailable(null, RequestedFunction.updatedFilter);
	}
	
	public JSONObject[] getCachedDayData(){
		return days;
	}
	
	public String getSelectedDateEndAsString(){
		if(endDate==Utilities.getTodayTimestamp())
			return activity.getString(R.string.today);
		return Utilities.getDate(endDate);
	}
	
	public String getSelectedDateStartAsString(){
		if(startDate==Utilities.getTodayTimestamp())
			return activity.getString(R.string.today);
		return Utilities.getDate(startDate);
	}
	
	//Gets start and end date as timestamp at 00:00
	public void setSelectedDates(long startTimestamp, long endTimestamp){
		startDate=startTimestamp;
		endDate=endTimestamp;
		getApps((onDataAvailableListener) activity);	
	}
	
	public long getSelectedDateEndAsTimestamp(){
		return endDate;
	}
	public long getSelectedDateStartAsTimestamp(){	
		return startDate;
	}
	public long getNextDayAsTimestamp(long timestamp){	
		return timestamp+24*60*60;
	}
	
	/** ColorsOfApps:
	 * {
	 * 	"colors":
	 * 		[
	 * 			{
	 * 				"app":String,
	 * 				"color": int
	 * 			}
	 * 			...
	 * 		]
	 * }
	 */
	public JSONObject getColorsOfApps() {
		return colorsOfApps;
	}
	public JSONObject getColorsOfApps(JSONObject jObj){
		if(colorsOfApps==null){
			colorsOfApps = new JSONObject();
		}
		try {
			JSONArray apps = jObj.getJSONArray("result");
			JSONArray colors = colorsOfApps.optJSONArray("colors");
			if(colors == null){
				colorsOfApps.put("colors", new JSONArray());	
				colors = colorsOfApps.optJSONArray("colors");
			}
			String appName;
			int color;
			Random rnd = new Random();
			for(int i=0;i<apps.length();i++){
				appName = apps.getJSONObject(i).getString("app");
				boolean found = false;
				for(int j=0;j<colors.length();j++){
					if(colors.getJSONObject(j).getString("app").equals(appName)){
						found = true;
						break;
					}
				}
				if(!found){
					color = rnd.nextInt();
					colors.put(new JSONObject().put("app", appName).put("color", color));
				}
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return colorsOfApps;
	}
	
	
	
	public void setColorsOfApps(JSONObject colorsOfApps) {
		DataSet.colorsOfApps = colorsOfApps;
		((onDataAvailableListener)activity).onDataAvailable(null, RequestedFunction.updatedFilter);
		Utilities.writeFile(activity.getString(R.string.file_app_colors), colorsOfApps.toString(), activity);
	}
	public void storeColorsOfApps(JSONObject jObj){
		DataSet.colorsOfApps = jObj;
		Utilities.writeFile(activity.getString(R.string.file_app_colors), colorsOfApps.toString(), activity);		
	}
	@Override
	public void onUserDataAvailable(JSONObject jObj) {
		if(user==null){//first call of DataSet
			user = jObj;
			instance.getApps((onDataAvailableListener) activity);//TODO UGLY AS HELL
		}else{
			user = jObj;
			tmpJSONResult = null;
			tmpJSONResultToday = null;
		}
	}

	public void onDataDownloaded(int serverResponse, JSONObject jObj, long timestamp, String requestedFunction, onDataAvailableListener requester, String fileName){
		/**
		 * Gets called after server has send data
		 * if result jObj is null it will ask for new Login data
		 * else calls the requester and if a filename is given the data will be stored
		 */
		Log.d("Downloadtime", "Download time: "+(Utilities.getSystemTime()-queryStart)+"ms");
	
		if(jObj == null){
			downloadTaskErrorHandler(jObj, serverResponse,requestedFunction);
			dataReady(new JSONObject(), requestedFunction, requester);
		}else{
			if(requestedFunction.equals(RequestedFunction.getAllApps)){
				if(requester!=null)					
					requester.onDataAvailable(new JSONObject[]{constructAllAppNamesJSONObject(jObj)}, requestedFunction);
				return;
			}else if(requestedFunction.equals(RequestedFunction.getEventsAtDate)){
				JSONObject result = constructBasicJSONObj(jObj);
				if(timestamp<Utilities.getTodayTimestamp()){
					tmpJSONResult = result;					
					if(fileName != null)
						new StoreFileTask(activity).execute(result.toString(),fileName);
				}else if(timestamp==Utilities.getTodayTimestamp()){
					tmpJSONResultToday = result;
				}
				if(requester!=null){
					result = filterIgnoredApps(result);
					dataReady(result, requestedFunction, requester);
					return;				
				}
			/*}else if(requestedFunction.equals(RequestedFunction.initDataSet)){
				JSONObject result = constructBasicJSONObj(jObj);
				if(getSelectedDateEndAsTimestamp()==Utilities.getTodayTimestamp())
					tmpJSONResultToday = result;
				else
					tmpJSONResult = result;
				((onDataAvailableListener) activity).onDataAvailable(null, requestedFunction);
				return;*/
			}else if(requestedFunction.equals(RequestedFunction.getPositions)){
				requester.onDataAvailable(new JSONObject[]{constructPositionJSONObject(jObj)}, requestedFunction);
			}
		}
	}
	public void onDataLoaded(JSONObject jObj, String requestedFunction, onDataAvailableListener requester, String fileName){
		/**
		 * gets called after internal data is loaded
		 * deletes file if jObj==null
		 * else calls the requester
		 */
		
		if(jObj == null && fileName!= null){
			File file = activity.getFileStreamPath(fileName);
			if(file.exists())
				file.delete();
		}else{
			if(fileName!=null){
				Log.d("InternalLoadtime", "Load time: "+(Utilities.getSystemTime()-queryStart)+"ms");
				tmpJSONResult = jObj;
			}
			if(requester!=null){
				jObj = filterIgnoredApps(jObj);
				dataReady(jObj, requestedFunction, requester);
				return;
			}
		}
	}
	private void dataReady(JSONObject jObj, String requestedFunction, onDataAvailableListener requester){
		for(int i=0;i<numberSelectedDays;i++){ //TODO Whole function is crappy for multiple days... at least i guess 
			if(days[i]==null){
				try {
					days[i]=new JSONObject(jObj.toString());
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if(i==numberSelectedDays-1){
					Arrays.sort(days, new Comparator<JSONObject>() {
						@Override
						public int compare(JSONObject lhs, JSONObject rhs) {
							int i= ((Long)lhs.optLong("dateTimestamp", 0)).compareTo(rhs.optLong("dateTimestamp",0));
							return i;
						}
					});					
					requester.onDataAvailable(days, requestedFunction);
					break;
				}else{
					manageMultipleDays(i+1, requester);
					return;
				}
			}
		}
	}
	public void getAllApps(onDataAvailableListener requester){
		JSONObject data = new JSONObject();
		try{
			data.put("type", "APPSTART");
			data.put("key", "app");
			data.put("start", Utilities.getTimestamp(2012, 0, 1, 0, 0, 0));
			data.put("end",Utilities.getTodayTimestamp());
		}catch(JSONException e){
			
		}
		getData(requester, RequestedFunction.getAllApps, data);
	}
	public void getPositions(onDataAvailableListener requester){
		long start=getSelectedDateEndAsTimestamp();
		long end=getNextDayAsTimestamp(start);
		
		JSONObject data = new JSONObject();
		try {
			data.put("model", "SPECIFIC");
			data.put("start", start);
			data.put("end", end);
			data.put("source", "MOBILE");
			data.put("type", "POSITION");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		getData(requester, RequestedFunction.getPositions, data);
	}
	
	private JSONObject constructBasicJSONObj(JSONObject jObj){
		JSONArray jArrayInput=null;
		JSONArray lastKnownPos = null;
		JSONArray jArrayOutput= new JSONArray();
		JSONArray locations = new JSONArray();
		JSONObject result=new JSONObject();
		long totalDuration=0;
		try{
			result.put("dateTimestamp", jObj.getLong("date"));
			result.put("downloadTimestamp", Utilities.getSystemTime());
			jArrayInput = jObj.getJSONArray("events");					
			for(int i=0; i<jArrayInput.length();i++){
				JSONObject jObjInput = jArrayInput.getJSONObject(i);
				if(jObjInput.getString("type").equals("APPSTART")){
					boolean found = false;
					
					String appName = "NoAppNameFound";
					JSONArray entities = jObjInput.getJSONArray("entities");
					for(int x = 0; x<entities.length(); x++){
						if(entities.getJSONObject(x).getString("key").equals("app")){
							appName = entities.getJSONObject(x).getString("value");
							break;
						}
					}
					
					String appSession = jObjInput.getString("session");
					long time = jObjInput.getLong("timestamp");
					for(int j = 0; j<jArrayOutput.length();j++){
						JSONObject jObjOutput = jArrayOutput.getJSONObject(j);
						if(jObjOutput.getString("app").equals(appName)){
							JSONArray usages = jObjOutput.getJSONArray("usage");
							for(int k = 0; k<usages.length();k++){
								JSONObject usage = usages.getJSONObject(k);
								if(usage.getString("session").equals(appSession)){
									if(jObjInput.getString("action").equals("START")){
										usage.put("start", time);														
									}else{
										usage.put("end", time);								
									}
									int tmp_duration = (int) Math.abs(usage.optLong("end",time)-usage.optLong("start",time));
									totalDuration += tmp_duration;
									jObjOutput.put("duration", jObjOutput.optLong("duration", 0)+tmp_duration);
									found = true;
									break;
								}
							}
							if(!found){
								JSONObject usage = new JSONObject();
								jObjOutput.put("sessionCount", jObjOutput.optInt("sessionCount",0)+1);									
								if(jObjInput.getString("action").equals("START")){
									usage.put("start", time);														
								}else{
									usage.put("end", time);										
								}
								usage.put("session",appSession);
								usages.put(usage);
								found = true;
								break;
							}
						}
					}
					if(!found){
						JSONObject app = new JSONObject();
							JSONArray usages = new JSONArray();
								JSONObject usage = new JSONObject();									
								if(jObjInput.getString("action").equals("START")){
									usage.put("start", time);														
								}else{
									usage.put("end", time);										
								}
								usage.put("session",appSession);
							usages.put(usage);
						app.put("usage",usages);
						app.put("app", appName);
						app.put("sessionCount", 1);
						jArrayOutput.put(app);
					}
				}else if(jObjInput.getString("type").equals("POSITION")){
					long time = jObjInput.getLong("timestamp");
					JSONArray location = jObjInput.getJSONArray("entities");
					lastKnownPos = location;
					/**
					 * for separate location array
					 */
					double lng=0;
					double lat=0;
					if(location.getJSONObject(0).getString("key").equals("lat")){
						lat=location.getJSONObject(0).getDouble("value");
						lng=location.getJSONObject(1).getDouble("value");
					}else{
						lng=location.getJSONObject(0).getDouble("value");
						lat=location.getJSONObject(1).getDouble("value");
					}
					locations.put(new JSONObject()
						.put("timestamp", time)
						.put("lng",lng)
						.put("lat", lat)						
					);		
					
					
					for(int j = 0; j<jArrayOutput.length();j++){
						JSONArray appUsages = jArrayOutput.getJSONObject(j).getJSONArray("usage");
						for(int k = 0; k<appUsages.length();k++){
							JSONObject usage = appUsages.getJSONObject(k);
							if(usage.has("start")&& usage.getLong("start")<=time && !usage.has("location")){
								usage.put("location",location);
							}else if(usage.has("start")&& usage.getLong("start")<=time && usage.has("end")&& usage.getLong("end")>=time){
								usage.put("location",location);
							}else if(usage.has("end")&& usage.getLong("end")>=time && !usage.has("location")){
								usage.put("location",location);
							}
						}
					}
				}
			}
			for(int i = 0; i<jArrayOutput.length(); i++){					
				JSONObject [] arrayOfJSONObjects = new JSONObject[jArrayOutput.getJSONObject(i).getJSONArray("usage").length()];
				for(int j = 0; j < jArrayOutput.getJSONObject(i).getJSONArray("usage").length(); j++){
					arrayOfJSONObjects[j] = jArrayOutput.getJSONObject(i).getJSONArray("usage").getJSONObject(j);
					if(!jArrayOutput.getJSONObject(i).getJSONArray("usage").getJSONObject(j).has("location")){
						jArrayOutput.getJSONObject(i).getJSONArray("usage").getJSONObject(j).put("location", lastKnownPos);
					}
				}
				Arrays.sort(arrayOfJSONObjects, new Comparator<JSONObject>() {
					@Override
					public int compare(JSONObject lhs, JSONObject rhs) {
						int res= ((Long)lhs.optLong("start", 0)).compareTo(rhs.optLong("start",0));
						return res;
					}
				});
				JSONArray newJSONArray = new JSONArray();
				for(int j=0;j<arrayOfJSONObjects.length;j++){
					newJSONArray.put(arrayOfJSONObjects[j]);
				}
				jArrayOutput.getJSONObject(i).put("usage", newJSONArray);
			}
			result.put("result", jArrayOutput);
			result.put("totalDuration", totalDuration);
			result.put("locations",locations);
		}catch(JSONException e){
			e.printStackTrace();			
		}
		return result;
	}
	private JSONObject filterIgnoredApps(JSONObject jObj){
		JSONObject result = null;
		try{
			result = new JSONObject(jObj, new String[]{"locations","downloadTimestamp","dateTimestamp","totalDuration"});
			JSONArray output=result.put("result", new JSONArray()).getJSONArray("result");
			JSONArray locations = new JSONArray(jObj.getJSONArray("locations").toString());			
			result.put("locations", locations);
			for(int i=0; i<jObj.getJSONArray("result").length();i++){
				JSONObject app = jObj.getJSONArray("result").getJSONObject(i);
				if(!ignoreApps.optBoolean(app.getString("app")))
					output.put(app);
			}
			jObj = null;
			return result;
		}catch(JSONException e){
			return result;
		}
	}

	private JSONObject constructAllAppNamesJSONObject(JSONObject jObj){
		JSONObject result= new JSONObject();
		JSONArray output= new JSONArray();
		try{
			result.put("downloadTimestamp", Utilities.getSystemTime());
			for(int i = 0; i< jObj.getJSONArray("values").length(); i++){
				JSONObject app = jObj.getJSONArray("values").getJSONObject(i);
				output.put(new JSONObject().put("app",app.getString("value")));			
			}
			result.put("result", output);
		}catch(JSONException e){
			return result;			
		}
		return result;
	}
	private void getData(onDataAvailableListener requester, String requestedFunction, JSONObject jObj){
		queryStart = Utilities.getSystemTime();
		/*if(requestedFunction.equals(RequestedFunction.initDataSet)){
			String fileName = Utilities.getFileName(requestedFunction, user, jObj,activity);
			String url = Utilities.getURL(Config.Request.events, jObj.toString(), user, activity);
			new DownloadTask(requester,activity).execute(url,requestedFunction,fileName,jObj.optLong("start",1)+"");			
		}else */if(requestedFunction.equals(RequestedFunction.getAllApps)){
			String url = Utilities.getURL(Config.Request.values,jObj.toString(),user, activity);
			if(Config.getDebug())
				queryStart = Utilities.getSystemTime();
			new DownloadTask(requester,activity).execute(url,RequestedFunction.getAllApps,null);
		}
		else if(requestedFunction.equals(RequestedFunction.getEventsAtDate)){
			/**
			 * Checks if requested data is offline available and loads it.
			 * If it's not available the server is requested.
			 */			
			//Check cached data	

			if(tmpJSONResultToday!=null 
					&& jObj.optLong("start",-1) == Utilities.getTodayTimestamp() 
					&& (Utilities.getSystemTime()-tmpJSONResultToday.optLong("downloadTimestamp"))<todayCacheMin ){
				onDataLoaded(tmpJSONResultToday, requestedFunction, requester, null);
				return;
			}else if(tmpJSONResult!=null 
					&& tmpJSONResult.optLong(("dateTimestamp"),-1)==jObj.optLong("start",1)){
				onDataLoaded(tmpJSONResult, requestedFunction, requester, null);
				return;				
			}
			
			//Check stored Data fileExists not needed for Basic file		
			boolean fileExists = false;
			String fileName = Utilities.getFileName(requestedFunction, user, jObj,activity);
			if(fileName != null)
				fileExists = activity.getFileStreamPath(fileName).exists();
			if(fileExists){
				new LoadFileTask(requester, activity).execute(fileName, requestedFunction);
			}else{
				String url = Utilities.getURL(Config.Request.events, jObj.toString(), user, activity);
				new DownloadTask(requester,activity).execute(url,requestedFunction,fileName,jObj.optLong("start",1)+"");
			}
		}
	}
	private JSONObject constructPositionJSONObject(JSONObject jObj){
		JSONObject result = new JSONObject();
		try {
			result.put("result",new JSONArray());
			JSONArray positions = result.getJSONArray("result");
			JSONArray events = jObj.getJSONArray("events");
			for(int i=0; i<events.length();i++){
				JSONObject event = events.getJSONObject(i);
				JSONArray lnglat = event.getJSONArray("entities");
				double lng=0;
				double lat=0;
				if(lnglat.getJSONObject(0).getString("key").equals("lat")){
					lat=lnglat.getJSONObject(0).getDouble("value");
					lng=lnglat.getJSONObject(1).getDouble("value");
				}else{
					lng=lnglat.getJSONObject(0).getDouble("value");
					lat=lnglat.getJSONObject(1).getDouble("value");
				}
				long timestamp = event.getLong("timestamp");
				positions.put(new JSONObject()
					.put("timestamp", timestamp)
					.put("lng",lng)
					.put("lat", lat)
				);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	private void downloadTaskErrorHandler(JSONObject jObj, int serverResponse, String requestedFunction){
		ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if(networkInfo == null || !networkInfo.isConnected()){
			Utilities.showDialog(activity.getString(R.string.info_no_data_connection),activity);
			/*if(requestedFunction.equals(RequestedFunction.initDataSet))
					((onDataAvailableListener) activity).onDataAvailable(null, requestedFunction);*/
			return;
		}
		if(jObj == null){
			String errorMessage;
			switch (serverResponse) {
			case -1:
				if(user==null)
					errorMessage = activity.getString(R.string.error_no_user_jObj);
				else
					errorMessage = activity.getString(R.string.error);
				break;
			case -2:
				//task was canceled by user
				return;
			case 403:
				errorMessage = activity.getString(R.string.error_authentication_fail)+serverResponse;
				break;
			case 404:
				errorMessage = activity.getString(R.string.error)+serverResponse;
				break;
			default:
				errorMessage = activity.getString(R.string.error);
				break;
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setMessage(errorMessage)
				.setCancelable(false)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			           @Override
			           public void onClick(DialogInterface dialog, int id) {
			        	  //Create or Recreate user data
			        	  createUserData();
			           }
			       });
			AlertDialog dialog = builder.create();
			if(((MainActivity) activity).isRunning())
				dialog.show();			
		}else{
			try {
				Utilities.showDialog("Error occurred: "+jObj.getString("reason"),activity);
			} catch (JSONException e) {
				Utilities.showDialog("Error occured while trying to read server error, no reason stated.",activity);
			}
		}
	}
}