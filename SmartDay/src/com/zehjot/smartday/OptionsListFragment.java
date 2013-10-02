package com.zehjot.smartday;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.zehjot.smartday.data_access.DataSet;
import com.zehjot.smartday.data_access.DataSet.onDataAvailableListener;
import com.zehjot.smartday.helper.Utilities;
import com.zehjot.smartday.TabListener.OnSectionSelectedListener;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class OptionsListFragment extends ListFragment implements onDataAvailableListener, OnSectionSelectedListener,TimespanDialog.OnDateChosenListener {
	private SimpleAdapter optionsListAdapter;
	private List<Map<String,String>> displayedOptions;
	private static final String TEXT1 = "text1";
	private static final String TEXT2 = "text2";
	private int startview = -1;
	private DataSet dataSet = null;
	private boolean special = false;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		dataSet = DataSet.getInstance(getActivity());
		/*---Initialize data for list---*/
		final String[] fromMapKey = new String[] {TEXT1, TEXT2};
		final int[] toLayoutId = new int[] {android.R.id.text1, android.R.id.text2};
		if(displayedOptions==null){
			displayedOptions = new ArrayList<Map<String,String>>();
			displayedOptions.add(displayDate());
			displayedOptions.add(toMap(getResources().getString(R.string.options_app_text1), getResources().getString(R.string.options_app_text2)));
		}
		setStartView(startview);
		/*---Set up list adapter---*/
		optionsListAdapter = new SimpleAdapter(
				getActivity(), 
				displayedOptions, //the data
				android.R.layout.simple_list_item_2, //the layout
				fromMapKey, 
				toLayoutId);
	}
	
	@Override
	public void onStart(){
		super.onStart();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view = inflater.inflate(R.layout.options_list, container, false);
		((ListView)view).addHeaderView(inflater.inflate(R.layout.header_view,null),null,false);
		setListAdapter(optionsListAdapter);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
	}
	
	@Override
	public void onListItemClick(ListView l,View v,int position, long id){
		switch (position-1) {
		case 0:
			TimespanDialog date = new TimespanDialog();
			date.setListener(this);
	    	date.show(getFragmentManager(), getString(R.string.datepicker));
			break;			
		case 1:
			dataSet.getApps(this);			
			break;
		case 2:
			special = true;
			dataSet.getApps(this);			
			break;
		default:
			break;
		}
	}
	public void onDataAvailable(JSONObject[] jObjs, String request){
		//check if fragment is added to activity
		if(!isAdded())
			return;
		if(request.equals(DataSet.RequestedFunction.getEventsAtDate)&&!special){			
			SelectAppsDialogFragment apps = new SelectAppsDialogFragment();
			apps.setStrings(Utilities.jObjValuesToArrayList(jObjs).toArray(new String[0]));
			apps.setMode(SelectAppsDialogFragment.SELECT_APPS);
			apps.show(getFragmentManager(), getString(R.string.datepicker));
		}else if(request.equals(DataSet.RequestedFunction.getEventsAtDate)&&special){
			special=false;
			SelectAppsDialogFragment apps = new SelectAppsDialogFragment();
			apps.setStrings(Utilities.jObjValuesToArrayList(jObjs).toArray(new String[0]));
			apps.setMode(SelectAppsDialogFragment.SELECT_HIGHLIGHT_APPS);
			apps.show(getFragmentManager(), getString(R.string.datepicker));			
		}
	}
	@Override
	public void onSectionSelected(int pos) {
		if(displayedOptions == null){
			startview = pos;
		}else
		updateOptions(pos);
	}
	
    public void onDateChosen(int startyear, int startmonth, int startday, int endyear, int endmonth, int endday){
    	dataSet.setSelectedDates(startyear, startmonth, startday, endyear, endmonth, endday);
    	updateDate();
    }
	public void updateDate(){
		displayedOptions.set(0, displayDate());
		optionsListAdapter.notifyDataSetChanged();
	}
	
	private void updateOptions(int pos){
		switch (pos) {
		case 0:
			if(displayedOptions.size()<3){//check if exists because of nullpointer
				displayedOptions.add(toMap(getString(R.string.options_map_text1), getString(R.string.options_map_text2)));}
			else{
				displayedOptions.set(2,toMap(getString(R.string.options_map_text1), getString(R.string.options_map_text2)));}
			optionsListAdapter.notifyDataSetChanged();
			break;
		case 1:
			if(displayedOptions.size()>2){
				displayedOptions.remove(2);
				optionsListAdapter.notifyDataSetChanged();}
			break;
		case 2:
			if(displayedOptions.size()>2){
				displayedOptions.remove(2);
				optionsListAdapter.notifyDataSetChanged();}
			break;
		default:
			break;
		}
	}
	
	private Map<String,String> displayDate(){
		String s1 = dataSet.getSelectedDateStartAsString();
		String s2 = dataSet.getSelectedDateEndAsString();
		if(s1.equals(s2))
			return toMap(s2,getResources().getString(R.string.options_date_text2));
		else
			return toMap(s1+"-\n"+s2,getResources().getString(R.string.options_date_text2));
	}
	
	private Map<String,String> toMap(String text1, String text2){
		Map<String, String> map = new HashMap<String, String>();
		map.put(TEXT1, text1);
		map.put(TEXT2, text2);
		return map;
	}
	
	private void setStartView(int pos){
		switch (pos) {
		case 0:
			displayedOptions.add(toMap(getResources().getString(R.string.options_map_text1), getResources().getString(R.string.options_map_text2)));			
			break;
		case 1:
			displayedOptions.add(toMap(getResources().getString(R.string.options_chart_text1), getResources().getString(R.string.options_chart_text2)));
			break;
		default:
			break;
		}
		
	}
}
