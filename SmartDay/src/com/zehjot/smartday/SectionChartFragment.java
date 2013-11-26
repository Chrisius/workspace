package com.zehjot.smartday;

import java.util.Arrays;
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
import com.zehjot.smartday.helper.Utilities;

import android.app.Fragment;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
//import android.widget.ScrollView;
import android.widget.TextView;

public class SectionChartFragment extends Fragment implements OnUpdateListener{
	private MyChart[] charts=null;
	private static double minTimeinPercent = 0.05;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		setHasOptionsMenu(true);
		return inflater.inflate(R.layout.section_chart_fragment, container, false);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.chart_menu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		onDataAvailable(DataSet.getInstance(getActivity()).getCachedDayData(), "");
//		if(charts==null){
//			Log.d("Charts", "if cace");
//			DataSet.getInstance(getActivity()).getApps((onDataAvailableListener) getActivity());	
//		}
		//DataSet.getInstance(getActivity()).getApps((onDataAvailableListener) getActivity());	
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
	
	public void onDataAvailable(JSONObject[] jObjs, String request) {
		LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.chart_1);
		if(charts==null || charts.length!=jObjs.length){
			charts = new MyChart[jObjs.length];
			layout.removeAllViews();
		}
		
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int px = displaymetrics.widthPixels;
		
		for(int i=0;i<jObjs.length;i++){
			if(layout.findViewById(i+10)==null){
				LinearLayout chartDrawContainer = new LinearLayout(getActivity());
				chartDrawContainer.setLayoutParams(new LayoutParams(px*3/5, px*3/5));
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
			
			JSONArray colorsOfApps = DataSet.getInstance(getActivity()).getColorsOfApps(jObj).optJSONArray("colors");
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
			colors = new int[apps.length];
			try{
				for(int i=0;i<apps.length;i++){
					for(int j=0; j<colorsOfApps.length();j++){
						JSONObject color =  colorsOfApps.getJSONObject(j);
						if(color.getString("app").equals(apps[i])){
							colors[i] = color.getInt("color");
							break;
						}
					}
				}
				for(int j=0; j<colorsOfApps.length();j++){
					JSONObject color =  colorsOfApps.getJSONObject(j);
					if(color.getString("app").equals("Other")){
						otherColor = color.getInt("color");
						break;
					}
				}
				
				/*
				DataSet.getInstance(getActivity()).storeColorsOfApps(new JSONObject()
																	.put("colors", colorsOfApps));*/
			}catch(JSONException e){
			}
		}
		
		
		
		public void draw(JSONObject jObj, LinearLayout drawContainer, boolean mhighlight){
			this.highlight = mhighlight;
			date = jObj.optLong("dateTimestamp");
			processData(jObj);
			LinearLayout appNames =(LinearLayout) getActivity().findViewById(R.id.chart_appNames);
			TableLayout appDetails = (TableLayout) getActivity().findViewById(R.id.chart_details);
			if(highlight){
				appNames.removeAllViews();
				appDetails.removeAllViews();
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
				renderer.setShowLegend(false);
				renderer.setPanEnabled(false);
				renderer.setZoomEnabled(false);
				renderer.setClickEnabled(true);
				renderer.setInScroll(true);
				renderer.setChartTitle(Utilities.getDateWithDay(date)+", Total time "+Utilities.getTimeAsString((long) (totaltime*60)));
				renderer.setChartTitleTextSize(Config.getTextSizeInPx(getActivity()));
				renderer.setLabelsTextSize(Config.getTextSizeInPx(getActivity()));
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
				renderer.setChartTitle(Utilities.getDateWithDay(date)+", Total time "+Utilities.getTimeAsString((long) (totaltime*60)));
				chartView.repaint();
			}
		}
		private void addDetail(int selectedSeries){
			LinearLayout appNames =(LinearLayout) getActivity().findViewById(R.id.chart_appNames);
			TableLayout appDetails = (TableLayout) getActivity().findViewById(R.id.chart_details);
			appNames.removeAllViews();
			appDetails.removeAllViews();
			
			
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
							TableLayout table = (TableLayout) getActivity().findViewById(R.id.chart_details);
							table.removeAllViews();
							addAppDetails(appName, table);
							HorizontalScrollView sv = (HorizontalScrollView) getActivity().findViewById(R.id.chart_details_scroll);
							sv.scrollBy(sv.getRight(), 0);
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
				addAppDetails(appName, appDetails);
			}
		}
		private void addAppDetails(String appName,TableLayout table){
			JSONObject app = getTimesOfApp(appName);				
			if(app == null||app.optJSONArray("usage")==null)
				return;
			JSONArray usages = app.optJSONArray("usage");		
			/**
			 * Time and duration with onClickListener
			 */	
			TextView header = createTextView("Total time:"+"\n"+"    "+Utilities.getTimeAsString(app.optLong("duration")));
			TableRow row = new TableRow(getActivity());
			row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT,TableLayout.LayoutParams.WRAP_CONTENT));
			row.addView(header);
		    header = createTextView("Locations:");
			row.addView(header);
			table.addView(row);
			for(int i=0;i<usages.length();i++){
				JSONObject appUsage = usages.optJSONObject(i);
				long start = appUsage.optLong("start");
				long duration = appUsage.optLong("end")-start;
				TextView view = createTextView("Used at "+ Utilities.getTimeFromTimeStamp(start)+"\n"+"    for "+Utilities.getTimeAsString(duration));
				start = Utilities.getSecondsOfDay(start);
				view.setOnClickListener(new TimespanClickListener((int) start));
				row = new TableRow(getActivity());
				row.addView(view);
			    table.addView(row);
			    
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
			    ImageView imageview = new ImageView(getActivity());		    
			    imageview.setOnClickListener(new LocationClickListener(lng, lat, appUsage.optLong("start")));
			    imageview.setLayoutParams(new TableRow.LayoutParams(
			       0,
		           TableRow.LayoutParams.MATCH_PARENT));
			    imageview.setImageResource(R.drawable.show_location_icon);
			    imageview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			    row.addView(imageview);
			}	
		
		}
		private TextView createTextView(String headerString){			
			TextView header = new TextView(getActivity());
			header.setText(headerString);
			header.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.WRAP_CONTENT,
					TableRow.LayoutParams.WRAP_CONTENT));
			header.setTextSize(Config.getTextSize(getActivity()));
			header.setTextColor(getResources().getColor(android.R.color.white));
			header.setGravity(Gravity.CENTER_VERTICAL);
			return header;
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
			header.setTextSize(Config.getTextSize(getActivity()));
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
		private class TimespanClickListener implements View.OnClickListener{	
			private int startDayTime=0;
			
			public TimespanClickListener(int startDayTime) {
				this.startDayTime = startDayTime;
			}
			
			@Override
			public void onClick(View v) {
				
				JSONObject jObject = new JSONObject();
				try {
					jObject.put("time", startDayTime).put("app", selectedApp ).put("date", date);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				((MainActivity)getActivity()).switchTab(2, jObject);
			}
			
		}
	}
}

