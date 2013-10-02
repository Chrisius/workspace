package com.zehjot.smartday;

import java.util.Arrays;
import java.util.Random;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.SeriesSelection;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zehjot.smartday.R;
import com.zehjot.smartday.TabListener.OnUpdateListener;
import com.zehjot.smartday.data_access.DataSet;
import com.zehjot.smartday.data_access.DataSet.onDataAvailableListener;
import com.zehjot.smartday.helper.Utilities;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
//import android.widget.ScrollView;
import android.widget.TextView;

public class SectionChartFragment extends Fragment implements onDataAvailableListener, OnUpdateListener{
	private MyChart[] charts=null;
	private static double minTimeinPercent = 0.05;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		return inflater.inflate(R.layout.section_chart_fragment, container, false);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		DataSet.getInstance(getActivity()).getApps((onDataAvailableListener) getActivity());	
	}
	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
	}
	private void removeHighlights(){
		for(int i=0; i<charts.length;i++){
			charts[i].removeHighlight();
		}
	}
	@Override
	public void onDataAvailable(JSONObject[] jObjs, String request) {
		LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.chart_1);
		if(charts==null || charts.length!=jObjs.length){
			charts = new MyChart[jObjs.length];
			layout.removeAllViews();
		}
		for(int i=0;i<jObjs.length;i++){
			if(layout.findViewById(i+10)==null){
				LinearLayout chartDrawContainer = new LinearLayout(getActivity());
				chartDrawContainer.setLayoutParams(new LayoutParams(600, 600));
				chartDrawContainer.setId(i+10);
				layout.addView(chartDrawContainer);
			}
			if(layout.findViewById(i+10).getParent()==null)
				layout.addView(layout.findViewById(i+10));
			if(charts[i]==null)
				charts[i] = new MyChart();
			boolean highlight=false;
			if(i==0)
				highlight=true;
			charts[i].draw(jObjs[i],(LinearLayout)(layout.findViewById(i+10)),highlight);			
		}
	}

	public void onUpdate(JSONObject[] jObjs) {
		onDataAvailable(jObjs, "");
	}
	
	@Override
	public void putExtra(JSONObject jObj) {		
	}
	
	private class MyChart{
		private CategorySeries categories; 
		private DefaultRenderer renderer;
		private String[] apps = {"No Data available"};
		private JSONObject data;
		private double[] time = {1.0};
		private int[] colors = {0xA4A4A4FF};
		private GraphicalView chartView;
		private JSONObject rendererToArrayIndex;
		private JSONArray otherRendererToArrayIndex;
		private int otherColor;
		private String selectedApp="";
		private boolean highlight = false;
		private boolean wasClicked = false;
		private long date=-1;		
		private boolean repaint=true;
		
		public void removeHighlight(){
      	  SimpleSeriesRenderer[] renederers = renderer.getSeriesRenderers();
      	  for(SimpleSeriesRenderer renderer : renederers){
      		  renderer.setHighlighted(false);
      		  wasClicked =false;
      	  }
      	  if(repaint)
      		  chartView.repaint();
      	  else
      		  repaint = true;
		}
		
		private void processData(JSONObject jObj){
			data = jObj;
			JSONArray jArray = null;
			try {
				jArray = jObj.getJSONArray("result");
				apps = new String[jArray.length()];
				time = new double[jArray.length()];
				for(int i=0; i<jArray.length(); i++){
					JSONObject app = jArray.getJSONObject(i);
					apps[i] = app.getString("app");
					time[i] = 0;
					JSONArray usages = app.getJSONArray("usage");
					for(int j=0; j<usages.length();j++){
						JSONObject usage = usages.getJSONObject(j);
						long start = usage.optLong("start", -1);
						long end = usage.optLong("end", -1);
						if(start!=-1 && end!=-1)
							time[i] += end-start;
					}
					time[i] /=60.f;
					time[i] = Math.round(time[i]*100.f);
					time[i] /=100;
				}	
			} catch (JSONException e) {
				apps = new String[]{"No Data available"};
				time = new double[]{1.0};
				colors = new int[]{0xA4A4A4FF};
				e.printStackTrace();
				return;
			}
			
			JSONArray colorsOfApps = DataSet.getInstance(getActivity()).getColorsOfApps().optJSONArray("colors");
			if(colorsOfApps==null)
				colorsOfApps = new JSONArray();
			/**
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
			Random rnd = new Random();
			colors = new int[apps.length];
			boolean found = false;
			try{
				for(int i=0;i<apps.length;i++){
					found = false;
					for(int j=0; j<colorsOfApps.length();j++){
						JSONObject color =  colorsOfApps.getJSONObject(j);
						if(color.getString("app").equals(apps[i])){
							colors[i] = color.getInt("color");
							found = true;
							break;
						}
					}
					if(!found){
						colors[i]=rnd.nextInt();
						colorsOfApps.put(new JSONObject()
							.put("app",apps[i])
							.put("color",colors[i])							
						);
						
					}
				}
				
				
				found = false;
				for(int j=0; j<colorsOfApps.length();j++){
					JSONObject color =  colorsOfApps.getJSONObject(j);
					if(color.getString("app").equals("Other")){
						otherColor = color.getInt("color");
						found = true;
						break;
					}
				}
				if(!found){
					otherColor=rnd.nextInt();
					colorsOfApps.put(new JSONObject()
						.put("app","Other")
						.put("color",otherColor)							
					);
					
				}
				
				
				DataSet.getInstance(getActivity()).storeColorsOfApps(new JSONObject()
																	.put("colors", colorsOfApps));
			}catch(JSONException e){
			}
		}
		
		
		
		public void draw(JSONObject jObj, LinearLayout drawContainer, boolean mhighlight){
			this.highlight = mhighlight;
			date = jObj.optLong("dateTimestamp");
			processData(jObj);
			LinearLayout appNames =(LinearLayout) getActivity().findViewById(R.id.chart_appNames);
			LinearLayout appTimes =(LinearLayout) getActivity().findViewById(R.id.chart_time);
			LinearLayout appLocations =(LinearLayout) getActivity().findViewById(R.id.chart_location);
			if(highlight){
				appNames.removeAllViews();
				appTimes.removeAllViews();
				appLocations.removeAllViews();
			}
			rendererToArrayIndex = new JSONObject();
			otherRendererToArrayIndex = new JSONArray();
			double totaltime = 0;
			JSONObject selectedApps = DataSet.getInstance(getActivity()).getSelectedApps();
			for(int i=0; i < apps.length; i++){
				if(selectedApps.optBoolean(apps[i], true)){
					totaltime += time[i];
				}
			}
			totaltime = Math.round(totaltime*100.f);
			totaltime /=100;
			if(chartView==null){
				double otherTime = 0;
				categories = new CategorySeries("Number1");
				renderer = new DefaultRenderer();
				for(int i=0; i < apps.length; i++){
					if(selectedApps.optBoolean(apps[i], true)){
						if(time[i]/totaltime>minTimeinPercent){
						SimpleSeriesRenderer r = new SimpleSeriesRenderer();
						categories.add(apps[i], Math.round((time[i]/totaltime)*10000.f)/100);
						r.setColor(colors[i]);
						renderer.addSeriesRenderer(r);						
						try {
							rendererToArrayIndex.put(""+(renderer.getSeriesRendererCount()-1), i);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						}else{
							otherTime += time[i];
							otherRendererToArrayIndex.put(i);
						}
					}
				}
				if(apps.length>0){
					if(otherTime > 0){
						SimpleSeriesRenderer r = new SimpleSeriesRenderer();
						otherTime = Math.round((otherTime/totaltime)*10000.f);
						otherTime /=100;
						categories.add("Other", otherTime);
						r.setColor(otherColor);
						r.setHighlighted(highlight);
						renderer.addSeriesRenderer(r);	
					}else{		
						renderer.getSeriesRendererAt(renderer.getSeriesRendererCount()-1).setHighlighted(highlight);
					}
					if(highlight){
						addDetail(renderer.getSeriesRendererCount()-1);
						wasClicked=true;
					}
				}
				renderer.setFitLegend(true);	
				renderer.setDisplayValues(true);
				renderer.setPanEnabled(false);
				renderer.setZoomEnabled(false);
				renderer.setClickEnabled(true);
				renderer.setInScroll(true);
				renderer.setChartTitle(Utilities.getDateWithDay(date)+", Total time "+totaltime+" min");
				chartView = ChartFactory.getPieChartView(getActivity(), categories, renderer);	
				chartView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
				          SeriesSelection seriesSelection = chartView.getCurrentSeriesAndPoint();
				          if (seriesSelection != null) {
				        	  repaint = false;
				        	  removeHighlights();
				        	  wasClicked = true;
				        	  addDetail(seriesSelection.getPointIndex());
				        	  renderer.getSeriesRendererAt(seriesSelection.getPointIndex()).setHighlighted(true);
				        	  chartView.repaint();
				          }
					}
				});
				
				LinearLayout layout = drawContainer;
				layout.addView(chartView);
			}else{
				
				double otherTime = 0;
				categories.clear();
				renderer.removeAllRenderers();
				boolean highlighted=false;
				int selectedRenderer = -1;
				for(int i=0; i < apps.length; i++){
					if(selectedApps.optBoolean(apps[i], true)){
						if(time[i]/totaltime>minTimeinPercent){
						SimpleSeriesRenderer r = new SimpleSeriesRenderer();
						categories.add(apps[i],  Math.round((time[i]/totaltime)*10000.f)/100);
						if(apps[i].equals(selectedApp)){
							r.setHighlighted(wasClicked);
							highlighted=true;
							selectedRenderer = renderer.getSeriesRendererCount();
						}
						r.setColor(colors[i]);
						renderer.addSeriesRenderer(r);						
						try {
							rendererToArrayIndex.put(""+(renderer.getSeriesRendererCount()-1), i);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						}else{
							otherTime += time[i];
							otherRendererToArrayIndex.put(i);
						}
					}
				}
				if(apps.length>0){
					if(otherTime > 0){
						SimpleSeriesRenderer r = new SimpleSeriesRenderer();
						otherTime = Math.round((otherTime/totaltime)*10000.f);
						otherTime /=100;
						categories.add("Other", otherTime);
						r.setColor(otherColor);
						if(!highlighted&&wasClicked){
							r.setHighlighted(wasClicked);
							highlighted=true;
							selectedRenderer =renderer.getSeriesRendererCount();
							selectedApp="";
						}
						renderer.addSeriesRenderer(r);				
					}else if(!highlighted&&wasClicked){
						renderer.getSeriesRendererAt(renderer.getSeriesRendererCount()-1).setHighlighted(highlight);
						highlighted=true;
						selectedRenderer = renderer.getSeriesRendererCount()-1;
						selectedApp="";
					}
				}
				if(wasClicked)
					addDetail(selectedRenderer);
				renderer.setChartTitle(Utilities.getDate(date)+", Total time "+totaltime+" min");
				chartView.repaint();
			}
		}
		private void addDetail(int selectedSeries){
			LinearLayout appNames =(LinearLayout) getActivity().findViewById(R.id.chart_appNames);
			LinearLayout appTimes =(LinearLayout) getActivity().findViewById(R.id.chart_time);
			LinearLayout appLocations =(LinearLayout) getActivity().findViewById(R.id.chart_location);
			appNames.removeAllViews();
			appTimes.removeAllViews();
			appLocations.removeAllViews();
			
			
			if(apps.length<1)
				return;
			if(selectedSeries==renderer.getSeriesRendererCount()-1&&otherRendererToArrayIndex.length()>0){
				selectedApp="";
				String[] sortedArray = new String[otherRendererToArrayIndex.length()];
				for(int i = 0; i<otherRendererToArrayIndex.length(); i++){
					sortedArray[i]=apps[otherRendererToArrayIndex.optInt(i)];
				}
				Arrays.sort(sortedArray);
				for(int i = 0; i<otherRendererToArrayIndex.length(); i++){
				    TextView valueTV = getView(sortedArray[i]);
				    valueTV.setPadding(10, 5, 10, 5);
				    valueTV.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							String appName = ((TextView)v).getText().toString();
							selectedApp = appName;
							LinearLayout apps = (LinearLayout)v.getParent();
							for(int i=0;i<apps.getChildCount();i++){
								apps.getChildAt(i).setBackgroundResource(0);
							}
							v.setBackgroundResource(android.R.color.holo_blue_dark);
							LinearLayout appTimes =(LinearLayout) getActivity().findViewById(R.id.chart_time);
							LinearLayout appLocations =(LinearLayout) getActivity().findViewById(R.id.chart_location);
							appTimes.removeAllViews();
							appLocations.removeAllViews();
							JSONObject app = getTimesOfApp(appName);
							JSONArray appUsages = app.optJSONArray("usage");
							if(appUsages==null)
								return;
							/**
							 * Header
							 */
							TextView header = getView("Total time:"+"\n"+"    "+Utilities.getTimeString(app.optInt("duration")));
							header.setPadding(10, 5, 10, 5);
							appTimes.addView(header);
						    header = getView("Locations:"+"\n"+"    ");
							header.setPadding(10, 5, 11, 5);
							appLocations.addView(header);
						    
							for(int i = 0; i<appUsages.length();i++){
								/**
								 * Time and duration
								 */
								JSONObject appUsage = appUsages.optJSONObject(i);
								long start = appUsage.optLong("start",-1);
								long end = appUsage.optLong("end",-1);
								if(end ==-1){
									end = start;
								}
								long duration = end-start;
								TextView view = getView("Used at "+ Utilities.getTimeFromTimeStamp(start));
								view.setOnClickListener(new View.OnClickListener() {									
									@Override
									public void onClick(View v) {
										String time = ((TextView)v).getText().toString();
										String times[] = time.split("Used at ");
										times = times[1].split(":");
										for(int i=0;i<times.length;i++){
											Log.d("Time Strings",times[i]);
										}
										int h = Integer.valueOf(times[0]);
										int m = Integer.valueOf(times[1]);
										int s = Integer.valueOf(times[2]);
										int timestamp = h*60*60 + m*60 + s;
										JSONObject jObject = new JSONObject();
										try {
											jObject.put("time", timestamp).put("app", selectedApp ).put("date", date);
										} catch (JSONException e) {
											e.printStackTrace();
										}
										((MainActivity)getActivity()).switchTab(2, jObject);
										Log.d("Time selected",""+timestamp);
										
									}
								});

								view.setPadding(10, 5, 10, 0);
								view.setId((i*2));
								appTimes.addView(view);
							
							    view = getView("    for "+Utilities.getTimeString(duration));				
							    view.setOnClickListener(new View.OnClickListener() {									
									@Override
									public void onClick(View v) {
										int id = v.getId();
										v = ((LinearLayout)v.getParent()).findViewById(id-1);
										String time = ((TextView)v).getText().toString();
										String times[] = time.split("Used at ");
										times = times[1].split(":");
										for(int i=0;i<times.length;i++){
											Log.d("Time Strings",times[i]);
										}
										int h = Integer.valueOf(times[0]);
										int m = Integer.valueOf(times[1]);
										int s = Integer.valueOf(times[2]);
										int timestamp = h*60*60 + m*60 + s;
										JSONObject jObject = new JSONObject();
										try {
											jObject.put("time", timestamp).put("app", selectedApp ).put("date", date);
										} catch (JSONException e) {
											e.printStackTrace();
										}
										((MainActivity)getActivity()).switchTab(2, jObject);
										Log.d("Time selected",""+timestamp);
										
									}
								});
								view.setPadding(10, 0, 10, 5);
								view.setId((i*2)+1);						    
								appTimes.addView(view);
								
							    /**
							     * Locations
							     */
							    JSONArray location = appUsage.optJSONArray("location");
							    double lat=0;
							    double lng=0;
							    if(location!=null){
							    	JSONObject tmpJObj = location.optJSONObject(0);
							    	if(tmpJObj!=null){
								    	if(tmpJObj.optString("key").equals("lat")){
								    		lat=tmpJObj.optDouble("value");
								    		lng=location.optJSONObject(1).optDouble("value");
								    	}else{
								    		lng=tmpJObj.optDouble("value");
								    		lat=location.optJSONObject(1).optDouble("value");
								    	}
							    	}
							    }
							    view = getView("show location");
							    view.setOnClickListener(new LocationClickListener(lng, lat, appUsage.optLong("start")));
							    view.setPadding(10, 11, 10, 24);//TODO maybe not just trail and error...
							    appLocations.addView(view);
							}
							
						}
					});
				    appNames.addView(valueTV);
				}
			}else{
				TextView valueTV = getView(apps[rendererToArrayIndex.optInt(""+selectedSeries)]);
			    valueTV.setPadding(10, 5, 10, 5);	
			    appNames.addView(valueTV);
			    

				String appName = ((TextView)valueTV).getText().toString();
				selectedApp = appName;
				valueTV.setBackgroundResource(android.R.color.holo_blue_dark);
				JSONObject app = getTimesOfApp(appName);
				if(app == null)
					return;
				JSONArray appUsages = app.optJSONArray("usage");				
				if(appUsages==null)
					return;
				/**
				 * Header
				 */
				TextView header = getView("Total time:"+"\n"+"    "+Utilities.getTimeString(app.optInt("duration")));
				header.setPadding(10, 5, 10, 5);
			    appTimes.addView(header);
			    header = getView("Locations:"+"\n"+"    ");
				header.setPadding(10, 5, 11, 5);
				appLocations.addView(header);
			    
				for(int i = 0; i<appUsages.length();i++){
					/**
					 * Time and duration
					 */
					JSONObject appUsage = appUsages.optJSONObject(i);
					long start = appUsage.optLong("start",-1);
					long end = appUsage.optLong("end",-1);
					if(end ==-1){
						end = start;
					}
					long duration = end-start;
					TextView view = getView("Used at "+ Utilities.getTimeFromTimeStamp(start));					
					view.setOnClickListener(new View.OnClickListener() {									
						@Override
						public void onClick(View v) {
							String time = ((TextView)v).getText().toString();
							String times[] = time.split("Used at ");
							times = times[1].split(":");
							for(int i=0;i<times.length;i++){
								Log.d("Time Strings",times[i]);
							}
							int h = Integer.valueOf(times[0]);
							int m = Integer.valueOf(times[1]);
							int s = Integer.valueOf(times[2]);
							int timestamp = h*60*60 + m*60 + s;
							JSONObject jObject = new JSONObject();
							try {
								jObject.put("time", timestamp).put("app", selectedApp ).put("date", date);
							} catch (JSONException e) {
								e.printStackTrace();
							}
							((MainActivity)getActivity()).switchTab(2, jObject);
							Log.d("Time selected",""+timestamp);
							
						}
					});
					view.setPadding(10, 5, 10, 0);
					view.setId((i*2));
				    appTimes.addView(view);
				    
				    view = getView("    for "+Utilities.getTimeString(duration));		    
				    view.setOnClickListener(new View.OnClickListener() {									
						@Override
						public void onClick(View v) {
							int id = v.getId();
							v = ((LinearLayout)v.getParent()).findViewById(id-1);
							String time = ((TextView)v).getText().toString();
							String times[] = time.split("Used at ");
							times = times[1].split(":");
							for(int i=0;i<times.length;i++){
								Log.d("Time Strings",times[i]);
							}
							int h = Integer.valueOf(times[0]);
							int m = Integer.valueOf(times[1]);
							int s = Integer.valueOf(times[2]);
							int timestamp = h*60*60 + m*60 + s;
							JSONObject jObject = new JSONObject();
							try {
								jObject.put("time", timestamp).put("app", selectedApp ).put("date", date);
							} catch (JSONException e) {
								e.printStackTrace();
							}
							((MainActivity)getActivity()).switchTab(2, jObject);
							Log.d("Time selected",""+timestamp);
							
						}
					});
					view.setPadding(10, 0, 10, 5);
					view.setId((i*2)+1);				    
				    appTimes.addView(view);
				    /**
				     * Locations
				     */
				    JSONArray location = appUsage.optJSONArray("location");
				    double lat=0;
				    double lng=0;
				    if(location!=null){
				    	JSONObject tmpJObj = location.optJSONObject(0);
				    	if(tmpJObj!=null){
					    	if(tmpJObj.optString("key").equals("lat")){
					    		lat=tmpJObj.optDouble("value");
					    		lng=location.optJSONObject(1).optDouble("value");
					    	}else{
					    		lng=tmpJObj.optDouble("value");
					    		lat=location.optJSONObject(1).optDouble("value");
					    	}
				    	}
				    }
				    view = getView("show location");
				    view.setOnClickListener(new LocationClickListener(lng, lat, appUsage.optLong("start")));
				    view.setPadding(10, 11, 10, 24);// maybe not just trail and error...
				    appLocations.addView(view);
				}
				
			}
		}
		private JSONObject getTimesOfApp(String appName){
			/**
			 * returns
			 * {
			 * 	"times":[
			 * 		{
			 * 		"start":long
			 * 		"duration":long
			 * 		}
			 * 		...
			 * 	],
			 * 	"total":int
			 *  "location":[
			 *  	{
			 *  		...
			 *  	}
			 *  ]
			 * }
			 */
			JSONObject result = new JSONObject();
			JSONArray jArray = data.optJSONArray("result");
			if(jArray == null)
				return null;
			for(int i=0; i<jArray.length();i++){
				JSONObject app = jArray.optJSONObject(i);
				if(app.optString("app").equals(appName)){
					return app;
				}					
			}
			return result;
		}
		
		private TextView getView(String headerString){			
			TextView header = new TextView(getActivity());
			header.setText(headerString);
			header.setLayoutParams(new LayoutParams(
		            LayoutParams.MATCH_PARENT,
		            LayoutParams.WRAP_CONTENT));
			header.setTextSize(18);
			header.setTextColor(getResources().getColor(android.R.color.white));
			return header;
		}
		
		private class LocationClickListener implements View.OnClickListener{
	    	private double lng;
	    	private double lat;
	    	private long start;
			public LocationClickListener(double lng,double lat, long start) {
	    		this.lng = lng;
	    		this.lat = lat;
	    		this.start = start;
			}
			@Override
			public void onClick(View v) {
				Log.d("location clicked", "lng"+lng+"lat"+lat);										
				JSONObject jObject = new JSONObject();
				try {
					jObject.put("date", date);
					jObject.put("time", start);
					jObject.put("lng",lng);
					jObject.put("lat",lat);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				((MainActivity)getActivity()).switchTab(0, jObject);
			}
			
		}
	}
}
