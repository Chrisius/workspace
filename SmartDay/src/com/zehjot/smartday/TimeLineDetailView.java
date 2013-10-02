package com.zehjot.smartday;

import java.util.Arrays;
import java.util.Comparator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zehjot.smartday.data_access.DataSet;
import com.zehjot.smartday.helper.Utilities;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TimeLineDetailView extends View {
	private JSONObject jObj;
	private Paint mTextPaint;
	private float textSize;
	private JSONObject colors;
	private float totalDuration;
	private float longestDuration;
	private JSONObject[] orderedApps;
	private int pxLongestWord = 0;
	private int maxBarLength;
	private float yOffset;
	private float xOffset;
	private long date;

	private Paint mRectanglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private String selectedApp;
	
	private GestureDetector tapDetector;
	
	public TimeLineDetailView(Context context) {
		super(context);
		textSize = 18;
		yOffset = textSize/3.f;
		xOffset = 20; //was 20
		
		mTextPaint = new Paint();
		mTextPaint.setTextSize(textSize);
		mTextPaint.setColor(getResources().getColor(android.R.color.white));
		
		tapDetector = new GestureDetector(getContext(), new TapListener());
		
		selectedApp ="";
	}

	
	@Override
	protected void onDraw(Canvas canvas) {
		float xpad = (float) (getPaddingLeft()+getPaddingRight());
		float ypad = (float) (getPaddingTop()+getPaddingBottom())+yOffset/2.f;
		if(maxBarLength<0){
			maxBarLength = (int)(getWidth())+maxBarLength; // width available after after setData
		}
		String appName;
		float relativeBarLength;
		for(int i=0;i<orderedApps.length;i++){
			if(orderedApps[i].optString("app").equals(selectedApp)){
				mRectanglePaint.setColor(getResources().getColor(android.R.color.holo_blue_light));
				canvas.drawRect(
						xpad+xOffset-5, 
						ypad+(i)*(textSize+yOffset)+Math.round(textSize*0.2f+0.5)-yOffset/2.f,
						xpad+xOffset+pxLongestWord, 
						ypad+(i+1)*textSize+(yOffset)*i+Math.round(textSize*0.2f+0.5)+yOffset/2.f,
						mRectanglePaint);
			}

			float y=ypad+(i+1)*textSize+i*yOffset;
			canvas.drawText(orderedApps[i].optString("app", "Error"), xpad+20, y, mTextPaint);
			
			appName = orderedApps[i].optString("app", "Error");
			relativeBarLength = orderedApps[i].optLong("duration")/longestDuration;
			mRectanglePaint.setColor(colors.optInt(appName));
			canvas.drawRect(
					xpad+pxLongestWord+xOffset,
					ypad+(i)*(textSize+yOffset)+Math.round(textSize*0.2f+0.5),
					xpad+pxLongestWord+xOffset+relativeBarLength*maxBarLength,
					ypad+(i+1)*textSize+(yOffset)*i+Math.round(textSize*0.2f+0.5),//crazy way to find top and bottom of drawn Text
					mRectanglePaint);
			float percent = ((float)orderedApps[i].optLong("duration")/totalDuration)*100.f;
			percent = Math.round(percent*100.f);
			percent /= 100.f;
			canvas.drawText(percent+"%", xpad+20+10+pxLongestWord+relativeBarLength*maxBarLength, ypad+(i+1)*textSize+(yOffset)*i, mTextPaint);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		tapDetector.onTouchEvent(event);
		return true;
	}
	public void setData(JSONObject jObj){
		if(jObj == null)
			return;
		this.jObj = jObj;
		JSONObject selectedApps = DataSet.getInstance(getContext()).getSelectedApps();
		/**
		 * Get App Bar Length
		 */
		try{
			date = jObj.getLong("dateTimestamp");
			JSONArray apps = jObj.getJSONArray("result");
			totalDuration=0;
			longestDuration=0;
			int numberSelectedApps=0;
			for(int i=0;i<apps.length();i++){
				if(selectedApps.optBoolean(apps.getJSONObject(i).getString("app"),true))
					numberSelectedApps += 1;
			}
			int j=0;
			orderedApps = new JSONObject[numberSelectedApps];
			for(int i=0;i<apps.length();i++){
				if(selectedApps.optBoolean(apps.getJSONObject(i).getString("app"),true)){
					totalDuration += apps.getJSONObject(i).optLong("duration", 0);
					orderedApps[j] = (new JSONObject()
						.put("app", apps.getJSONObject(i).getString("app"))
						.put("duration", apps.getJSONObject(i).optLong("duration", 0))
					);
					j++;
					
					Rect bounds = new Rect();
					String text = apps.getJSONObject(i).getString("app");
					mTextPaint.getTextBounds(text, 0, text.length(), bounds);
					if(bounds.width()>pxLongestWord){
						pxLongestWord = bounds.width();
					}
					
					if(apps.getJSONObject(i).optLong("duration", 0)>longestDuration){
						longestDuration = apps.getJSONObject(i).optLong("duration", 0);
					}
				}
			}
			Arrays.sort(orderedApps, new Comparator<JSONObject>() {
				@Override
				public int compare(JSONObject lhs, JSONObject rhs) {
					int i= ((Long)rhs.optLong("duration", 0)).compareTo(lhs.optLong("duration",0));
					return i;
				}
			});
			
			for(int i=0;i<orderedApps.length;i++){
				float length = orderedApps[i].getLong("duration")/longestDuration;
				orderedApps[i].put("barLength",length);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		Rect bounds = new Rect();
		String text = "99.99%";
		mTextPaint.getTextBounds(text, 0, text.length(), bounds);
		int percentageSize = bounds.width();
		int offset = 10 ;
		
		maxBarLength = -pxLongestWord-percentageSize-offset-(int)xOffset;
		/**
		 * GetColors for Bars
		 */
		if(colors==null)
			colors=new JSONObject();
		JSONObject rawObj =	DataSet.getInstance(getContext()).getColorsOfApps(jObj);
		try {
			JSONArray jArray = rawObj.getJSONArray("colors");
			String appName;
			int appColor;
			JSONObject color;
			for(int i = 0; i<jArray.length();i++){
				color = jArray.getJSONObject(i);
				appName = color.getString("app");
				appColor = color.getInt("color");
				colors.put(appName, appColor);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		getParent().requestLayout();
		if(orderedApps!=null){
			int height = (int) ((orderedApps.length)*textSize+(yOffset)*(orderedApps.length+1));
			this.setLayoutParams(new LinearLayout.LayoutParams(500, height));
		}
		invalidate();
	}
	
	private class TapListener extends GestureDetector.SimpleOnGestureListener{
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			return true;
		}
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			close();
			return true;
		}
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			String app = getAppAtPos(e);
			selectApp(app);
			LinearLayout linearLayout = (LinearLayout)getParent().getParent();
			((TimeLineView)linearLayout.getChildAt(0)).selectApp(app);
			return true;
		}
	}
	public void selectApp(String app, int time){
		selectedApp = app;
		addDetails(app,time);
		invalidate();
	}
	public void selectApp(String app){
		selectedApp = app;
		addDetails(app,-1);
		invalidate();
	}
	private String getAppAtPos(MotionEvent e){
		float ypad = (float) (getPaddingTop()+getPaddingBottom())+yOffset/2.f;
		float y = e.getY();
		for(int i=0;i<orderedApps.length;i++){
			float start = ypad+(i)*(textSize+yOffset)+Math.round(textSize*0.2f+0.5)-yOffset/2.f;
			float end = ypad+(i+1)*textSize+(yOffset)*i+Math.round(textSize*0.2f+0.5)+yOffset/2.f;
			if(y>=start && y<=end){
				return orderedApps[i].optString("app");
			}
		}
		return "";
	}		
	private TextView createTextView(String headerString){			
		TextView header = new TextView(getContext());
		header.setText(headerString);
		header.setLayoutParams(new LayoutParams(
	            LayoutParams.MATCH_PARENT,
	            LayoutParams.WRAP_CONTENT));
		header.setTextSize(18);
		header.setTextColor(getResources().getColor(android.R.color.white));
		return header;
	}


	private void addDetails(String appName, int time) {
		LinearLayout layout = (LinearLayout) getParent();	//Check if extra layouts for time and location are initialized - if not, do so
		if(layout.getChildAt(1)==null){
			LinearLayout tmp = new LinearLayout(getContext());
			tmp.setOrientation(LinearLayout.VERTICAL);
			layout.addView(tmp);
			tmp = new LinearLayout(getContext());
			tmp.setOrientation(LinearLayout.VERTICAL);
			layout.addView(tmp);
		}
		LinearLayout details = (LinearLayout) layout.getChildAt(1);
		LinearLayout places = (LinearLayout) layout.getChildAt(2);
		details.removeAllViews();
		places.removeAllViews();
		JSONArray apps = jObj.optJSONArray("result"); //find the JSONObj which represents appName
		JSONObject app=null;
		for(int i=0;i<apps.length();i++){
			if(apps.optJSONObject(i).optString("app").equals(appName)){
				app = apps.optJSONObject(i);
				break;
			}
		}	
		if(app==null)
			return;
		/**
		 * Time and duration with onClickListener
		 */	
		TextView header = createTextView("Total time:"+"\n"+"    "+Utilities.getTimeString(app.optLong("duration")));
		header.setPadding(10, 0, 10, 2);
	    details.addView(header);
	    header = createTextView("Locations:"+"\n"+"    ");
		header.setPadding(10, 0, 10, 2);
	    places.addView(header);
		JSONArray usages = app.optJSONArray("usage"); //iterate over all usages 
		for(int i=0;i<usages.length();i++){
			JSONObject appUsage = usages.optJSONObject(i);
			long start = appUsage.optLong("start");
			long duration = appUsage.optLong("end")-start;
			TextView view = createTextView("Used at "+ Utilities.getTimeFromTimeStamp(start));
			start = Utilities.getTimeOfDay(start);
			view.setOnClickListener(new View.OnClickListener() {									
				@Override
				public void onClick(View v) {
					LinearLayout apps = (LinearLayout)v.getParent();
					for(int i=0;i<apps.getChildCount();i++){
						apps.getChildAt(i).setBackgroundResource(0);
					}
					
					v.setBackgroundResource(android.R.color.holo_blue_dark);
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
					LinearLayout linearLayout = (LinearLayout)getParent().getParent();
					((TimeLineView)linearLayout.getChildAt(0)).selectApp(timestamp);
					int id = v.getId();
					v = ((LinearLayout)v.getParent()).findViewById(id+1);
					v.setBackgroundResource(android.R.color.holo_blue_dark);
				}
			});
			view.setPadding(10, 2, 10, 0);
			view.setId((i*2));
			if(time==start)
				view.setBackgroundResource(android.R.color.holo_blue_dark);
		    details.addView(view);
		
		    view = createTextView("    for "+Utilities.getTimeString(duration));					
		    view.setOnClickListener(new View.OnClickListener() {									
				@Override
				public void onClick(View v) {
					LinearLayout apps = (LinearLayout)v.getParent();
					for(int i=0;i<apps.getChildCount();i++){
						apps.getChildAt(i).setBackgroundResource(0);
					}
					
					v.setBackgroundResource(android.R.color.holo_blue_dark);
					int id = v.getId();
					v = ((LinearLayout)v.getParent()).findViewById(id-1);
					v.setBackgroundResource(android.R.color.holo_blue_dark);
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
					LinearLayout linearLayout = (LinearLayout)getParent().getParent();
					((TimeLineView)linearLayout.getChildAt(0)).selectApp(timestamp);
					
				}
			});
			view.setPadding(10, 0, 10, 2);
			view.setId((i*2)+1);					
			if(time==start)
				view.setBackgroundResource(android.R.color.holo_blue_dark);	    
		    details.addView(view);
		    view.getHeight();
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
		    view = createTextView("show location");
		    view.setOnClickListener(new LocationClickListener(lng, lat, appUsage.optLong("start")));
		    view.setPadding(10, 11, 10, 18);//TODO maybe not just trail and error...
		    places.addView(view);
		}		
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
			((MainActivity)getContext()).switchTab(0, jObject);
		}
		
	}
	public void close(){
		setId(1);
		LinearLayout linearLayout = (LinearLayout)getParent().getParent();
		((TimeLineView)linearLayout.getChildAt(0)).selectApp("");
		linearLayout.removeView(linearLayout.getChildAt(1));		
	}
}
