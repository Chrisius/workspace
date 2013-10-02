package com.zehjot.smartday;

import org.json.JSONException;
import org.json.JSONObject;

import com.zehjot.smartday.R;
import com.zehjot.smartday.TabListener.OnUpdateListener;
import com.zehjot.smartday.data_access.DataSet;
import com.zehjot.smartday.data_access.DataSet.onDataAvailableListener;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class SectionTimelineFragment extends Fragment implements OnUpdateListener,onDataAvailableListener{
	private JSONObject extra=null;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		Log.d("Timeline", "CreateView");
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.section_timeline_fragment, container, false);
	}

	@Override
	public void onResume(){
		super.onResume();
		DataSet.getInstance(getActivity()).getApps((onDataAvailableListener) getActivity());	
	}
	
	public void onUpdate(JSONObject[] jObjs) {
		onDataAvailable(jObjs,"");
	}
	@Override
	public void putExtra(JSONObject jObj) {
		if(jObj==null)
			return;
		Activity act = getActivity();
		extra = jObj;
		int scrollto=0;
		if(act!=null){
			ViewGroup root = (ViewGroup) getActivity().findViewById(R.id.timelinell);
			for(int i=0;i<root.getChildCount();i++){
				LinearLayout timelineLinearLayout= (LinearLayout) root.getChildAt(i);
				TimeLineView timelineView = (TimeLineView) timelineLinearLayout.getChildAt(0);	
				if(extra!=null&&timelineView.getDate()==extra.optLong("date",-1)){
					try {
						extra.put("scrollY", i*300);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					scrollto=i;
					timelineView.setExtra(jObj);
					extra=null;
				}else{
					timelineView.removeDetail();					
				}
			}
			ScrollView sv = (ScrollView) act.findViewById(R.id.timelinescroll);
			sv.setScrollY(scrollto*300);
		}


	}
	@Override
	public void onDataAvailable(JSONObject[] jObjs, String requestedFunction) {
		ViewGroup root = (ViewGroup) getActivity().findViewById(R.id.timelinell);
		
		if(root!=null){		
			if(root.getChildCount()!=jObjs.length)
				root.removeAllViews();		
			for(int i=0;i<jObjs.length;i++){
				if(root.getChildAt(i)==null){
					LinearLayout linearLayout = new LinearLayout(getActivity());
					linearLayout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
					linearLayout.setOrientation(LinearLayout.VERTICAL);
					root.addView(linearLayout);
					TimeLineView timeline = new TimeLineView(getActivity());
					timeline.setLayoutParams(new LayoutParams(
							LayoutParams.WRAP_CONTENT,
							LayoutParams.WRAP_CONTENT));
					if(extra!=null&&extra.optLong("date",-1)==jObjs[i].optLong("dateTimestamp",1)){
						timeline.setExtra(extra);
						extra=null;
					}
					timeline.setData(jObjs[i]);
					linearLayout.addView(timeline);
				}else{
					LinearLayout timelineLinearLayout= (LinearLayout) root.getChildAt(i);
					TimeLineView timelineView = (TimeLineView) timelineLinearLayout.getChildAt(0);				
					if(extra!=null&&extra.optLong("date",-1)==jObjs[i].optLong("dateTimestamp",1)){
						timelineView.setExtra(extra);
						extra=null;
					}
					timelineView.setData(jObjs[i]);
				}
			}
		}
	}
}
