package com.zehjot.smartday;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zehjot.smartday.data_access.DataSet;
import com.zehjot.smartday.helper.Utilities;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Scroller;

public class TimeLineView extends View {
	private boolean debug = false;
	
	
	private Paint mTextPaint = new Paint();
	private Paint mSubTextPaint = new Paint();
	private Paint mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint mRectanglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint mDebugTextPaint = new Paint();
	private int debugDrawCounter = 0;
	private float mTextSize = Config.getTextSizeInPx((Activity) getContext());
	private float scaleFactor = 1.f;
	private ScaleGestureDetector zoomDetector;
	private GestureDetector scrollDetector;
	private float scrollX;
	private JSONObject colors;
	
	private Scroller mScroller;
	private ValueAnimator mScrollAnimator;
	
	private float lineWidth=0;
	private float offset=0;
	private float height=0;
	
	private String selectedApp = "No App selected";
	private int selectedTime=-1;
	
	private JSONArray rectangles;
	private JSONArray positionLabels;
	private JSONObject jObj;
	private JSONObject extra;
	private int appSessionCount=0;
	
	private TimeLineDetailView detail;
	private long date;
	
	
	public TimeLineView(Context context) {
		super(context);
		
		int textColor = getResources().getColor(android.R.color.white);
		
		mTextPaint.setColor(textColor);
		mTextPaint.setTextSize(mTextSize);		
		mTextPaint.setTextAlign(Align.CENTER);

		mSubTextPaint.setColor(textColor);
		mSubTextPaint.setTextSize(mTextSize*.9f);		
		mSubTextPaint.setTextAlign(Align.CENTER);
		
		mDebugTextPaint.setColor(textColor);
		mDebugTextPaint.setTextSize(mTextSize);		
		
		mLinePaint.setColor(textColor);
		
		zoomDetector=new ScaleGestureDetector(getContext(), new ZoomListener());
		scrollDetector= new GestureDetector(getContext(), new TapListener());
		
		
        mScroller = new Scroller(getContext(), null, true);
        
        mScrollAnimator = ValueAnimator.ofFloat(0, 1);
        mScrollAnimator.addUpdateListener(new AnimatorTick());
        rectangles = new JSONArray();        
		detail=null;
		
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//The Specified Mode (Exactly=hard coded pixels, at_most=match_parent, unspecified=wrap_content)
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		
		int width;
		int height;
		
		switch (widthMode) {
		case MeasureSpec.EXACTLY:
			width = widthSize;
			break;
		case MeasureSpec.AT_MOST:
			width = widthSize;
			break;
		case MeasureSpec.UNSPECIFIED:
			width = 0;
			break;
		default:
			width = 0;
			break;
		}
		
		switch (heightMode) {
		case MeasureSpec.EXACTLY:
			height = heightSize;
			break;
		case MeasureSpec.AT_MOST:
			height = Math.min(heightSize,300);
			break;
		case MeasureSpec.UNSPECIFIED:
			height = 300;
			break;
		default:
			height = 300;
			break;
		}	
		setMeasuredDimension(width, height);
	}
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(extra!=null){
			Log.d("TimeLineView", "extra available");
			selectedApp=extra.optString("app");
			selectedTime=extra.optInt("time",-1);
			int endtime = extra.optInt("end",-1);
			if(endtime != -1 && selectedTime!=-1){
				scaleFactor = 24.f/((endtime-selectedTime)/3600.f);
			}else
				scaleFactor = 4;
			if(selectedTime!=-1){
				height = this.getHeight();
				offset = height*0.2f;
				lineWidth = this.getWidth();
				lineWidth *= scaleFactor;
				lineWidth -= 2.f*offset;
				if(endtime!=-1)
					scrollX = - (selectedTime/(60.f*60.f))*(lineWidth/24.f)-offset;					
				else
					scrollX = getWidth()/4.f - (selectedTime/(60.f*60.f))*(lineWidth/24.f)-offset;
			}
			extra = null;
			addDetail();
		}
		canvas.translate(scrollX, 0);
		float xpad = (float) (getPaddingLeft()+getPaddingRight());
		float ypad = (float) (getPaddingTop()+getPaddingBottom());
		height = this.getHeight();
		offset = height*0.2f;
		lineWidth = this.getWidth();
		lineWidth *= scaleFactor;
		lineWidth -= 2.f*offset;
		debugDrawCounter +=1;
		if(!debug){
			canvas.drawText(Utilities.getDateWithDay(date)+", Total time "+Utilities.getTimeAsString((long) (jObj.optLong("totalDuration",0))), xpad-scrollX+10, ypad+mTextSize, mDebugTextPaint);
			/*if(((LinearLayout)getParent()).getChildAt(1)!=null){
				canvas.drawText("Doubletap below to close details", xpad-scrollX+10, height-mTextSize, mDebugTextPaint); 	
			}else{			
				canvas.drawText("Tap on a bar to open details", xpad-scrollX+10, height-mTextSize, mDebugTextPaint); 
			}*/
		}
		else
			canvas.drawText("height="+this.getHeight()+" width="+this.getWidth()+" calls="+debugDrawCounter+" scale="+scaleFactor+" scrollX="+scrollX+" Selected App= "+selectedApp+" AppSessions="+appSessionCount, xpad-scrollX, ypad+mTextSize, mDebugTextPaint);
		float pxForSecond = lineWidth/(24.f*60.f*60.f);
		/**
		 * Label position
		 */
		for(int i=0;i<positionLabels.length();i++){
			JSONObject label = positionLabels.optJSONObject(i);
			int start = label.optInt("start");
			int end = label.optInt("end");
			int labelID = label.optInt("label");
			canvas.drawLine(offset+xpad+start*pxForSecond, ypad+height*0.2f, offset+xpad+end*pxForSecond, ypad+height*0.2f, mLinePaint);
			canvas.drawLine(offset+xpad+start*pxForSecond, ypad+height*0.2f, offset+xpad+start*pxForSecond, ypad+height*0.22f, mLinePaint);
			if(mSubTextPaint.measureText(""+labelID)<(end-start)*pxForSecond){
				canvas.drawText(""+labelID, offset+xpad+((start+end)/2.f)*pxForSecond-2, ypad+height*0.21f, mSubTextPaint);
			}
			if(positionLabels.length()-1==i){
				canvas.drawLine(offset+xpad+end*pxForSecond, ypad+height*0.2f, offset+xpad+end*pxForSecond, ypad+height*0.22f, mLinePaint);				
			}
		}
		/**
		 * Line with hourdisplay
		 */
		
		float timelabel = mSubTextPaint.measureText("99:99")*1.5f;
		float numberOfTimelabels=getWidth()*scaleFactor/timelabel;
		for(int i=0;i<25;i++){
			if((offset+xpad+(i+1)*lineWidth/24.f)+scrollX>0&&(offset+xpad+(i-1)*lineWidth/24.f)+scrollX-getWidth()<0){
				canvas.drawLine(offset+xpad+i*lineWidth/24.f, ypad+height*0.81f, offset+xpad+i*lineWidth/24.f, ypad+height*0.77f, mLinePaint);
				if(i%((int)(25/numberOfTimelabels)+1)==0 || i==24)
					canvas.drawText(""+i+":00", offset+xpad+i*lineWidth/24.f, ypad+height*0.81f+mTextSize+2, mTextPaint);
				if(numberOfTimelabels>=52 && i!=24){
					canvas.drawText(""+i+":30", offset+xpad+(i+0.5f)*lineWidth/24.f, ypad+height*0.81f+mTextSize+2, mSubTextPaint);
					canvas.drawLine(offset+xpad+(i+0.5f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.5f)*lineWidth/24.f, ypad+height*0.79f, mLinePaint);
				}
				if(numberOfTimelabels>=108 && i!=24){
					canvas.drawLine(offset+xpad+(i+0.25f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.25f)*lineWidth/24.f, ypad+height*0.79f, mLinePaint);
					canvas.drawLine(offset+xpad+(i+0.75f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.75f)*lineWidth/24.f, ypad+height*0.79f, mLinePaint);
					if(getWidth()*scaleFactor>=6500){
						canvas.drawText(""+i+":15", offset+xpad+(i+0.25f)*lineWidth/24.f, ypad+height*0.81f+mTextSize+2, mSubTextPaint);
						canvas.drawText(""+i+":45", offset+xpad+(i+0.75f)*lineWidth/24.f, ypad+height*0.81f+mTextSize+2, mSubTextPaint);
					}
				}
				if(numberOfTimelabels>=420 && i!=24){			
					for(int j=1; j<=14;j++){
						canvas.drawLine(offset+xpad+(i+0.0f+j/60.f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.0f+j/60.f)*lineWidth/24.f, ypad+height*0.80f, mLinePaint);
						canvas.drawLine(offset+xpad+(i+0.25f+j/60.f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.25f+j/60.f)*lineWidth/24.f, ypad+height*0.80f, mLinePaint);
						canvas.drawLine(offset+xpad+(i+0.5f+j/60.f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.5f+j/60.f)*lineWidth/24.f, ypad+height*0.80f, mLinePaint);
						canvas.drawLine(offset+xpad+(i+0.75f+j/60.f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.75f+j/60.f)*lineWidth/24.f, ypad+height*0.80f, mLinePaint);					
					}
				}
			}
		}
//		for(int i=0;i<25;i++){
//			if((offset+xpad+(i+1)*lineWidth/24.f)+scrollX>0&&(offset+xpad+(i-1)*lineWidth/24.f)+scrollX-getWidth()<0){
//				canvas.drawLine(offset+xpad+i*lineWidth/24.f, ypad+height*0.81f, offset+xpad+i*lineWidth/24.f, ypad+height*0.77f, mLinePaint);
//				if(i%2==0 || getWidth()*scaleFactor>=1400 || i==24)
//					canvas.drawText(""+i+":00", offset+xpad+i*lineWidth/24.f, ypad+height*0.81f+mTextSize+2, mTextPaint);
//				if(getWidth()*scaleFactor>=2878 && i!=24){
//					canvas.drawText(""+i+":30", offset+xpad+(i+0.5f)*lineWidth/24.f, ypad+height*0.81f+mTextSize+2, mSubTextPaint);
//					canvas.drawLine(offset+xpad+(i+0.5f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.5f)*lineWidth/24.f, ypad+height*0.79f, mLinePaint);
//				}
//				if(getWidth()*scaleFactor>=5040 && i!=24){
//					canvas.drawLine(offset+xpad+(i+0.25f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.25f)*lineWidth/24.f, ypad+height*0.79f, mLinePaint);
//					canvas.drawLine(offset+xpad+(i+0.75f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.75f)*lineWidth/24.f, ypad+height*0.79f, mLinePaint);
//					if(getWidth()*scaleFactor>=6500){
//						canvas.drawText(""+i+":15", offset+xpad+(i+0.25f)*lineWidth/24.f, ypad+height*0.81f+mTextSize+2, mSubTextPaint);
//						canvas.drawText(""+i+":45", offset+xpad+(i+0.75f)*lineWidth/24.f, ypad+height*0.81f+mTextSize+2, mSubTextPaint);
//					}
//				}
//				if(getWidth()*scaleFactor>=8000 && i!=24){			
//					for(int j=1; j<=14;j++){
//						canvas.drawLine(offset+xpad+(i+0.0f+j/60.f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.0f+j/60.f)*lineWidth/24.f, ypad+height*0.80f, mLinePaint);
//						canvas.drawLine(offset+xpad+(i+0.25f+j/60.f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.25f+j/60.f)*lineWidth/24.f, ypad+height*0.80f, mLinePaint);
//						canvas.drawLine(offset+xpad+(i+0.5f+j/60.f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.5f+j/60.f)*lineWidth/24.f, ypad+height*0.80f, mLinePaint);
//						canvas.drawLine(offset+xpad+(i+0.75f+j/60.f)*lineWidth/24.f, ypad+height*0.81f, offset+xpad+(i+0.75f+j/60.f)*lineWidth/24.f, ypad+height*0.80f, mLinePaint);					
//					}
//				}
//			}
//		}
		
		canvas.drawLine(offset+xpad, ypad+height*0.8f, offset+xpad+lineWidth, ypad+height*0.8f, mLinePaint);
		/**
		 * App Bars
		 */
		if(rectangles!=null){
			//float pxForSecond = lineWidth/(24.f*60.f*60.f);
			int startInSec;
			int endInSec;
			String appName;
			for(int i=0;i<rectangles.length();i++){
				JSONObject rect = rectangles.optJSONObject(i);
				startInSec = rect.optInt("start");
				endInSec = startInSec+rect.optInt("length");
				float xEnd= xpad-1+offset+pxForSecond*endInSec;
				float xStart = xpad+1+offset+pxForSecond*startInSec;
				if(xEnd+scrollX>=0||xStart+scrollX-getWidth()>=0){				
					appName = rect.optString("app");
					mRectanglePaint.setColor(colors.optInt(appName));
					float selectedOffset = 0;
					if(appName.equals(selectedApp)){
						if(startInSec==selectedTime)
							selectedOffset = height*0.1f;
						else
							selectedOffset = height*0.05f;					
					}
					int extraPixel=0;
					if(pxForSecond*endInSec-pxForSecond*startInSec-2<1)
						extraPixel=(int) Math.round((1-(pxForSecond*endInSec-pxForSecond*startInSec-2))+0.5f);
					canvas.drawRect(
							xStart,
							ypad+height*0.3f+selectedOffset,
							xEnd+extraPixel,
							ypad+height*0.7f+selectedOffset,
							mRectanglePaint);
				}
			}
		}
	}
	
	public void selectApp(String appName){
		selectedApp = appName;
		selectedTime = -1;
		invalidate();
	}
	public void selectApp(String appName,int time){
		selectedApp = appName;
		selectedTime = time;
		if(time!=-1&&scaleFactor>1){
			scrollX = getWidth()/4.f - (time/(60.f*60.f))*(lineWidth/24.f)-offset;
		}
		invalidate();
	}
	public void selectApp(int time){
		selectedTime = time;
		if(time!=-1&&scaleFactor>1){
			scrollX = getWidth()/4.f - (time/(60.f*60.f))*(lineWidth/24.f)-offset;
		}
		invalidate();		
	}
	public void setExtra(JSONObject jObj){
		extra = jObj;
		invalidate();
	}
	public long getDate(){
		return this.date;
	}
	public void setData(JSONObject jObj){
		if(jObj == null)
			return;
		try {
			this.jObj = jObj;
			date = jObj.getLong("dateTimestamp");
			rectangles=new JSONArray();
			String appName;
			long start;
			long end;
			int startInSec;
			int endInSec;
			JSONObject selectedApps = DataSet.getInstance(getContext()).getSelectedApps();
			//Construct rectangles
			for(int i=0; i<jObj.getJSONArray("result").length();i++){				
				JSONObject app = jObj.getJSONArray("result").getJSONObject(i);
				appName = app.getString("app");
				if(selectedApps.optBoolean(appName,true)){				
					JSONArray usages = app.getJSONArray("usage");
					for(int j=0;j<usages.length();j++){
						JSONObject usage = usages.getJSONObject(j);
						start = usage.optLong("start",-1);
						end = usage.optLong("end",-1);
						if(start!=-1&&end!=-1){
							startInSec = Utilities.getSecondsOfDay(start);
							endInSec = Utilities.getSecondsOfDay(end);						
							rectangles.put(new JSONObject().put("start",startInSec).put("length", endInSec-startInSec).put("app", appName));
						}
					}
				}
			}
			//construct position labels
			positionLabels = new JSONArray();
			positionLabels.put(
					new JSONObject()
					.put("start",0));
			int lastLabel = -2;
			int label = -1;
			double lng=0;
			double lat=0;
			long time=0;
			for(int i=0;i<jObj.optJSONArray("locations").length();i++){
				JSONObject location = jObj.optJSONArray("locations").optJSONObject(i);
				lng = location.optDouble("lng");
				lat = location.optDouble("lat");
				time = Utilities.getSecondsOfDay(location.optLong("timestamp"));
				label = DataSet.getInstance(getContext()).getPOI(lat, lng);
				if(label != lastLabel){
					if(lastLabel==-2){//case for first object
						positionLabels.getJSONObject(positionLabels.length()-1).put("label",label);
					}else{
						positionLabels.getJSONObject(positionLabels.length()-1).put("end",time);
						positionLabels.put(new JSONObject().put("start",time).put("label",label));
					}
					lastLabel = label;
				}
				if(i==jObj.optJSONArray("locations").length()-1){
					if(date==Utilities.getTodayTimestamp()){
						positionLabels.getJSONObject(positionLabels.length()-1).put("end",Utilities.getSecondsOfDay(Utilities.getSystemTime()/1000));
					}else{
						positionLabels.getJSONObject(positionLabels.length()-1).put("end",24*60*60);
					}
				}
			}
			
			
			if(this.getParent()!=null&&((LinearLayout)getParent()).getChildAt(1)!=null){
				if(jObj.getJSONArray("result").length()==0)
					((TimeLineDetailView)((LinearLayout)((LinearLayout)this.getParent()).getChildAt(1)).getChildAt(0)).close();
				else
				((TimeLineDetailView)((LinearLayout)((LinearLayout)this.getParent()).getChildAt(1)).getChildAt(0)).setData(jObj);
			}
				
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		
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
		invalidate();
		requestLayout();
	}
	
	/**
	 * Important calculations for zooming:
	 * 
	 * notes:
	 * 	'time' is time on timeline, for example 13:30 is 13.5
	 *	'lineWidth' = getWidth()*scalefactor-2*offset
	 *	'offset' creates space between viewborder and start of line
	 *	'scrollX' translation of canvas. should not be >0 or else the canvas is translated to the right
	 * pxOnCanvas = offset+time*width/24
	 * 
	 * when tapping on screen:
	 * 	pxOnScreen = offset+time*lineWidth/24+scrollX
	 * 	time = ((pxOnScreen-offset-scrollX)/lineWidth)*24
	 * zooming in to a tapped time requires recalculation of scrollX:
	 * 	scrollX = pxOnScreen-time*((getWidth()*scaleFactor-2*offset)/24.f)-offset -- calculate new 'lineWidth' calculating with old 'width' leads to ugly shaking
	 * while scrolling:
	 * 	maxScroll = getWidth()*(1.f-scaleFactor)
	 */
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		zoomDetector.onTouchEvent(event);
		scrollDetector.onTouchEvent(event);
		return true;
	}
	public void removeDetail(){
		LinearLayout linearLayout = (LinearLayout)getParent();
		selectApp("");
		linearLayout.removeView(linearLayout.getChildAt(1));
	}
	
	private class ZoomListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{
		float time;
		float pointOnScreen;
		boolean zoomStart=true;
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			if(zoomStart){
				disableVerticalScroll();
				time = ((detector.getFocusX()-offset-scrollX)/lineWidth)*24.f;
				pointOnScreen = detector.getFocusX();
				zoomStart = false;
			}
			return true;
		}
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			scaleFactor *= detector.getScaleFactor();
			if(scaleFactor<1.f){
				scrollX =0;
				scaleFactor=1.f;
				invalidate();
				return true;
			}
			scrollX = pointOnScreen-time*((getWidth()*scaleFactor-offset*2.f)/24.f)-offset;
			invalidate();
			return true;
		}
		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			zoomStart = true;			
			super.onScaleEnd(detector);
		}
	}
	private class TapListener extends GestureDetector.SimpleOnGestureListener{
		boolean scrollerKilled=false;
		@Override
		public boolean onDown(MotionEvent e) {
			if(!mScroller.isFinished()){
				mScroller.forceFinished(true);
				scrollerKilled = true;
			}else
				scrollerKilled=false;
			return true;
		}
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			if(velocityX>velocityY){
				disableVerticalScroll();
			}else{
				enableVerticalScroll();
			}
            mScroller.fling(
                    (int)e1.getX(),
                    0,
                    (int)(velocityX/2.f),
                    0,
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    0,
                    0);

            // Start the animator and tell it to animate for the expected duration of the fling.
            
            mScrollAnimator.setDuration(mScroller.getDuration());
            mScrollAnimator.start();
            return true;
		};
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			if(Math.abs(distanceX)>Math.abs(distanceY*.2f)){
				disableVerticalScroll();
			}else{
				enableVerticalScroll();
			}
			scrollX-=distanceX;
			if(scrollX>0)
				scrollX=0.f;
			else if(scrollX<getWidth()*(1.f-scaleFactor))
				scrollX=getWidth()*(1.f-scaleFactor);
			invalidate();
			return true;
		}
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			/*Doubletab to zoom in
			Log.d("Gesture", "doubletap");
			if(scaleFactor!=1.f){
				scaleFactor =1.f;
				scrollX = 0;
			}
			else{
				scaleFactor =8.f;
				float time = ((e.getX()-offset-scrollX)/lineWidth)*24.f;
				scrollX = e.getX()-time*((getWidth()*scaleFactor-2.f*offset)/24.f)-offset;
			}
			invalidate();*/
			if(detail!=null&&detail.isVisible()){
				detail.close();
			}else{
				addDetail();
			}
			return true;
		}
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if(!scrollerKilled){ //needed so you dont tap if you want to stop the fling
				//float time = ((e.getX()-offset-scrollX)/lineWidth)*24.f;
				//Log.d("Time tapped",""+time);
				JSONObject app = getAppAtPos(e);
				if(app!=null){
					selectedApp = app.optString("app");
					selectedTime = app.optInt("start");
					invalidate();
					addDetail();
				}
			}
			return true;
		}
	}
	private class AnimatorTick implements ValueAnimator.AnimatorUpdateListener{
		float tmp=0;
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			if(!mScroller.isFinished()){
	            mScroller.computeScrollOffset();     
	        	scrollX += mScroller.getCurrX()-mScroller.getStartX()-tmp;
	            tmp = mScroller.getCurrX()-mScroller.getStartX();
	            if(scrollX<getWidth()*(1.f-scaleFactor)){
	            	scrollX = getWidth()*(1.f-scaleFactor);
	    			tmp = 0;
	            	mScrollAnimator.cancel();
	            }else if(scrollX>0){
	            	scrollX = 0;
	    			tmp = 0;
	            	mScrollAnimator.cancel();
	            }
			}else{
				tmp = 0;
	        	mScrollAnimator.cancel();
			}
	        invalidate();
	    }
	}
	private JSONObject getAppAtPos(MotionEvent e){
		float nearest = 10*24*60*60/lineWidth;
		int nearestI=-1;
		float time = ((e.getX()-offset-scrollX)/lineWidth)*24.f;
		int timeInSec = (int)(time*3600.f);
		try{
			JSONObject rect;
			int start;
			int length;
			for(int i=0; i<rectangles.length();i++){
				rect = rectangles.getJSONObject(i);
				start = rect.getInt("start");
				length = rect.getInt("length");
				if(timeInSec-start>=0 && timeInSec-start<=length){
					return rect;
				}
				else{
					if(timeInSec-start<0&&Math.abs(timeInSec-start)<nearest){
						nearest = Math.abs(timeInSec-start);
						nearestI = i;
					}
					else if(Math.abs(timeInSec-start-length)<nearest){
						nearest = Math.abs(timeInSec-start-length);
						nearestI = i;
					}
						
				}
			}
			if(nearestI!=-1)
				return rectangles.getJSONObject(nearestI);
		}catch(JSONException ex){
		}
		return null;
	}
	
	private static int disableScrollCalls = 0;
	private void disableVerticalScroll(){
		disableScrollCalls +=1;
		ViewGroup root = (ViewGroup) ((Activity)getContext()).findViewById(R.id.timelinell);
		((ScrollView)root.getParent()).requestDisallowInterceptTouchEvent(true);
	}
	private void enableVerticalScroll(){
		disableScrollCalls -=1;
		if(disableScrollCalls<=0){
			ViewGroup root = (ViewGroup) ((Activity)getContext()).findViewById(R.id.timelinell);
			if(root!=null&&root.getParent().getClass().getSimpleName().equals("ScrollView")){
				((ScrollView)root.getParent()).requestDisallowInterceptTouchEvent(false);
				Log.d("ScrollView","enabled");
			}
		}
	}
	
	private void addDetail(){
		if(((LinearLayout)getParent()).getChildAt(1)==null){
			LinearLayout lLayout = new LinearLayout(getContext());
			lLayout.setOrientation(LinearLayout.HORIZONTAL);
			((LinearLayout)getParent()).addView(lLayout);					
			detail=new TimeLineDetailView(getContext());
			lLayout.addView(detail);
		}else{
		}
		detail.setData(jObj);
		detail.selectApp(selectedApp,selectedTime);
	}
	
}
