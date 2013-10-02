package com.zehjot.smartday;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.zehjot.smartday.TabListener.OnUpdateListener;
import com.zehjot.smartday.data_access.DataSet;
import com.zehjot.smartday.data_access.DataSet.onDataAvailableListener;
import com.zehjot.smartday.helper.Utilities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SectionMapFragment extends MapFragment implements OnUpdateListener, InfoWindowAdapter, OnInfoWindowClickListener{
	private GoogleMap mMap;
	private List<Marker> markerList = new ArrayList<Marker>();
	private JSONObject marker;
	private Polyline polyline;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		Log.d("MapView", "CreateView");
		return super.onCreateView(inflater, container, savedInstanceState);
		
	}
	
	
	
	public void onUpdate(JSONObject[] jObjs) {
		onDataAvailable(jObjs, "");
	}
	 @Override
	public void putExtra(JSONObject jObj) {
		double lat = jObj.optDouble("lat",0);
		double lng = jObj.optDouble("lng",0);
		long start = jObj.optLong("time",0);
		if(getMap()!=null){
			if(mMap==null)
				mMap = this.getMap();
			zoomTo(lat, lng,15);
		}
		if(start != 0){
			JSONArray positions = marker.optJSONArray("positions");
			for(int i=0; i<positions.length(); i++){
				JSONObject position = positions.optJSONObject(i);
				if(start >= position.optLong("start") && start <= position.optLong("end")){
					markerList.get(i).showInfoWindow();
					break;
				}
			}
		}
	}
	@Override
	public void onResume() {
		super.onResume();
		DataSet.getInstance(getActivity()).getApps((onDataAvailableListener) getActivity());	
	}
	
	public void zoomTo(double lat, double lng, float zoom){
		 mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), zoom));
	}
	public void zoomTo(double lat, double lng){
		 mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 12));
	}
	
	public void onDataAvailable(JSONObject[] jObjs, String requestedFunction) {
		if(getMap()!=null){
			if(mMap==null)
				mMap = this.getMap();
			if(jObjs==null)
				return;
			mMap.setInfoWindowAdapter(this);
			mMap.setOnInfoWindowClickListener(this);
			double zoomLat=50.45;
			double zoomLng=6.06;
			CameraPosition camera = mMap.getCameraPosition();
			try{
				for(int i=0; i<markerList.size();i++){
					markerList.get(i).remove();
				}
				if(polyline!=null){
					polyline.remove();
				}

				PolylineOptions polylineOptions = new PolylineOptions();
				for(int j=0; j<jObjs.length;j++){
					JSONArray locations = jObjs[j].getJSONArray("locations");
					for(int i=0;i<locations.length();i++){
						polylineOptions.add(new LatLng(locations.getJSONObject(i).getDouble("lat"),locations.getJSONObject(i).getDouble("lng")));
					}
				}
				polyline = mMap.addPolyline(polylineOptions);
				int color = getResources().getColor(android.R.color.holo_blue_light);
				polyline.setColor(color);
				polyline.setWidth(4);
				constructMarker(jObjs);
				markerList = new ArrayList<Marker>();
				JSONArray positions = marker.optJSONArray("positions");
				for(int i=0;i<positions.length();i++){
					JSONObject position = positions.getJSONObject(i);
					double lat = position.optDouble("lat",-1);
					double lng = position.optDouble("lng", -1);
					zoomLat =lat;
					zoomLng =lng;
					MarkerOptions markerOptions = new MarkerOptions()
						.position(new LatLng(lat, lng))
						.title(position.getLong("start")+"!!..!!"+position.getLong("end")+"!!..!!"+position.getJSONArray("dates").toString());
					JSONArray apps = position.optJSONArray("apps");
					for(int j=0; j<apps.length();j++){
						JSONObject app = apps.optJSONObject(j);
						String appName = app.optString("app");
						String oldApps = markerOptions.getSnippet();
						if(oldApps==null)
							oldApps = "";
						else 
							appName = "!!..!!"+appName;
						markerOptions.snippet(oldApps+appName);
						if(app.optBoolean("highlight"))
							markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
					}
					Marker mMarker = mMap.addMarker(markerOptions);
					
					markerList.add(mMarker);
				}
			}catch (JSONException e){
				e.printStackTrace();
			}
			if(camera.target.latitude==0&&camera.target.longitude==0)
				zoomTo(zoomLat, zoomLng);
		}
	}
	/**
	 * {
	 *  positions:
	 *  [
	 *    {
	 *    	"highlight":bool
	 *  	"lat":double
	 *  	"lng":double
	 *  	"start":long
	 *  	"end":long
	 *  	"apps":
	 *  	[
	 *  	  {
	 *  		"app":String
	 *  		"usage":
	 *  		[
	 *  		  {
	 *  			"start":long
	 *  			"end":long
	 *  		  }
	 *  		  ...
	 *  		]
	 *  	  }
	 *  	  ...
	 *  	]
	 *    }
	 *    ...
	 *  ]
	 * }
	 */
	private JSONObject constructMarker(JSONObject[] jObjs){
		if(jObjs == null)
			return null;
		JSONObject displayedApps = DataSet.getInstance(getActivity()).getSelectedApps();
		try {
			JSONObject highlightApps = DataSet.getInstance(getActivity()).getSelectedHighlightApps();
			marker = new JSONObject();
			JSONArray positions = new JSONArray();
			for(int x=0; x<jObjs.length;x++){
			JSONArray apps = jObjs[x].getJSONArray("result");
			marker.put("positions",positions);
			for(int i=0; i<apps.length();i++){
				JSONObject app = apps.getJSONObject(i);
				String appName = app.getString("app");
				if(displayedApps.optBoolean(appName,true)){					
					JSONArray usages = app.getJSONArray("usage");
					for(int j=0;j<usages.length();j++){
						JSONObject usage = usages.getJSONObject(j);
						JSONArray location = usage.optJSONArray("location");
						if(location != null){						
							long start=usage.optLong("start", -1);
							long end=usage.optLong("end", -1);
							double lat;
							double lng;
							if(end==-1)
								end=start;
							if(start!=-1){
								if(location.getJSONObject(0).getString("key").equals("lat")){
									lat = location.getJSONObject(0).getDouble("value");
									lng = location.getJSONObject(1).getDouble("value");	
								}else{
									lng = location.getJSONObject(0).getDouble("value");
									lat = location.getJSONObject(1).getDouble("value");						
								}
								boolean found = false;
								for(int k=0;k<positions.length();k++){
									JSONObject position = positions.getJSONObject(k);
									if(position.getDouble("lat")==lat&&position.getDouble("lng")==lng){
										JSONArray markerApps = position.getJSONArray("apps");
										long date = jObjs[x].getLong("dateTimestamp");
										JSONArray dates = position.getJSONArray("dates");
										boolean foundDate=false;
										for(int l=0;l<dates.length();l++){
											if(dates.getLong(l)==date){
												foundDate = true;
												break;
											}
										}
										if(!foundDate){									
											position.getJSONArray("dates").put(jObjs[x].getLong("dateTimestamp"));
										}
										for(int l=0;l<markerApps.length();l++){
											JSONObject markerApp = markerApps.getJSONObject(l);
											if(markerApp.getString("app").equals(appName)){
												markerApp.getJSONArray("usage").put(new JSONObject()
												.put("start", start)
												.put("end",end));
											found =true;
											break;
											}
										}
										if(!found){
											markerApps.put(new JSONObject()
												.put("highlight",highlightApps.optBoolean(appName))
												.put("app", appName)
												.put("usage", new JSONArray()
													.put(new JSONObject()
														.put("start", start)
														.put("end",end)
													)
												)
											);
											found = true;
										}
										break;
									}
								}
								if(!found){
									long locStart=-1;
									long locEnd=-1;
									JSONArray locations = jObjs[x].getJSONArray("locations");
									for(int k=0;k<locations.length();k++ ){
										JSONObject tmpLocation = locations.getJSONObject(k);
										if(tmpLocation.getDouble("lat")==lat&&tmpLocation.getDouble("lng")==lng){
											locStart = tmpLocation.getLong("timestamp");
											if(k<locations.length()-1)
												locEnd = locations.getJSONObject(k+1).getLong("timestamp");
										break;
										}
									}
									
									
									positions.put(new JSONObject()
										.put("dates", new JSONArray()
											.put(jObjs[x].getLong("dateTimestamp"))
										)
										.put("lat", lat)
										.put("lng", lng)
										.put("start", locStart)
										.put("end", locEnd)
										.put("apps", new JSONArray()
											.put(new JSONObject()
												.put("highlight",highlightApps.optBoolean(appName))
												.put("app", appName)
												.put("usage", new JSONArray()
													.put(new JSONObject()
														.put("start", start)
														.put("end",end)
													)
												)
											)
										)
									);
								}
							}
						}
					}
				}
			}
			for(int i=0; i<positions.length();i++){
				JSONObject position = positions.getJSONObject(i);
				long markerEnd = position.getLong("end");
				long markerStart = position.getLong("start");
					JSONArray tmpApps = position.getJSONArray("apps");
					for(int j = 0; j<tmpApps.length();j++){
						JSONObject tmpApp = tmpApps.getJSONObject(j);
						JSONArray tmpUsages = tmpApp.getJSONArray("usage");
						for(int k=0;k<tmpUsages.length();k++){
							JSONObject tmpUsage = tmpUsages.getJSONObject(k);
							long tmpStart = tmpUsage.optLong("start",-1);
							long tmpEnd = tmpUsage.optLong("end", -1);
							if(tmpEnd!=-1 && markerEnd<tmpEnd){
								markerEnd = tmpEnd;
							}
							if(markerStart>tmpStart&&tmpStart!=-1){
								markerStart = tmpStart;
							}
						}
					}
					position.put("end", markerEnd);
					position.put("start", markerStart);
			}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		//zoomTo(testlat, testlng);
		return marker;
	}


	@Override
	public View getInfoWindow(Marker arg0) {
		return null;
	}

	@Override
	public View getInfoContents(Marker arg0) {
		String times[] = arg0.getTitle().split("!!..!!");
		long start =Long.valueOf(times[0]);
		long end = Long.valueOf(times[1]);
		String title = "Apps at ";
		try{
		JSONArray dates = new JSONArray(times[2]);
		for(int i=0; i<dates.length();i++){
			title += Utilities.getDate(dates.getLong(i))+", "; 
		}
		}catch(JSONException e){
			e.printStackTrace();
		}
		title +=" from "+Utilities.getTimeFromTimeStamp(start)+" to "+Utilities.getTimeFromTimeStamp(end);
		String apps[]= arg0.getSnippet().split("!!..!!");
		LinearLayout ll = new LinearLayout(getActivity());
		ll.setOrientation(LinearLayout.VERTICAL);
		TextView tv = new TextView(getActivity());
		tv.setTextColor(getResources().getColor(android.R.color.black));
		tv.setTextSize(12);
		tv.setText(title);
		ll.addView(tv);
		for(int i=0; i<apps.length;i++){
			tv= new TextView(getActivity());
			tv.setText("  "+apps[i]);
			tv.setTextColor(getResources().getColor(android.R.color.black));
			ll.addView(tv);			
		}
		return ll;
	}



	@Override
	public void onInfoWindowClick(Marker arg0) {
		String times[] = arg0.getTitle().split("!!..!!");
		long start = Long.valueOf(times[0]);
		long end = Long.valueOf(times[1]);
		Log.d("marker time", ""+start);
		JSONObject jObject = new JSONObject();
		try {
			JSONArray dates = new JSONArray(times[2]);
			long date = dates.getLong(0);
			jObject.put("time", Utilities.getTimeOfDay(start));
			jObject.put("end", Utilities.getTimeOfDay(end));
			jObject.put("date",date);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		((MainActivity)getActivity()).switchTab(2, jObject);
		
	}

}
