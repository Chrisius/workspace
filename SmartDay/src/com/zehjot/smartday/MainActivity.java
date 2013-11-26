package com.zehjot.smartday;

import java.io.File;

import org.json.JSONObject;

import com.zehjot.smartday.R;
import com.zehjot.smartday.data_access.DataSet;
import com.zehjot.smartday.helper.Security;
import com.zehjot.smartday.helper.Utilities;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.res.Configuration;

public class MainActivity extends Activity 
		implements  DataSet.onDataAvailableListener	{
	private FragmentManager fm;
//	private OptionsListFragment optionsListFragment;
	private static DataSet dataSet;
	private boolean isRunning = true;
	private static Activity activity;
	private TabListener<SectionMapFragment> map;
	private TabListener<SectionChartFragment> chart;
	private TabListener<SectionTimelineFragment> time;
	private int lastTab = -1;
	
	
    public boolean isRunning(){
		return isRunning;
	}
    
    public void setLastTab(int tab){
    	this.lastTab = tab;
    }
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fm = getFragmentManager();
        if(savedInstanceState==null && dataSet!=null){
        	init(savedInstanceState);
        }else{
        	init(null);
        	dataSet = DataSet.getInstance(this);   
        }

        if(savedInstanceState != null){
        	init(savedInstanceState);
        	getActionBar().setSelectedNavigationItem(0);
        	getActionBar().setSelectedNavigationItem(1);
        	getActionBar().setSelectedNavigationItem(2);
        	getActionBar().setSelectedNavigationItem(savedInstanceState.getInt(getString(R.string.start_view)));
       }
    }
    @Override
    public void onResume(){
    	super.onResume();
    	isRunning = true;
    	DataSet.updateActivity(this);
    	activity = this;
    }
    
    @Override
    public void onStop(){
    	this.isRunning = false;
    	super.onStop();
    }
    @Override
    public void onPause() {
    	super.onPause();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);    	
    	return true;
    }
    
    @Override
    public void onBackPressed() {
    	Log.d("Main Activity", "Back button pressed");

    	if(lastTab != -1){
    		getActionBar().setSelectedNavigationItem(lastTab);
    		setLastTab(-1);
    	}else{
    		super.onBackPressed();
    	}
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	super.onOptionsItemSelected(item);
    	switch (item.getItemId()) {
    	case R.id.action_about:
    		String message = "This application was created for the Bachelor thesis 'Native Visualization of Mobile Activity Patterns'.\n" +
    				"The thesis and application was written and developed by Christian Janﬂen in 2013.\n" +
    				"It is available at RWTH-Aachen University, Germany.\n" +
    				"\n"+
    				"All credits for the charts go to the AChartEngine library.\n" +
    				"http://www.achartengine.org/\n" +
    				"\n"+
    				"All credits for the color picker go to Yuku Sugianto and his Android Color Picker project.\n" +
    				"https://code.google.com/p/android-color-picker/";
    		Utilities.showDialog(message, activity);
    		break;
		case R.id.action_blacklist:
			dataSet.getAllApps(this);
			break;
		case R.id.action_color_apps:
			ColorPickerDialog colorPicker = new ColorPickerDialog();
			colorPicker.show(fm, "ColorPicker");
			break;
		case R.id.action_delete_files:
			String[] list = getFilesDir().list();
			for(int i=0; i<list.length;i++){
			File file = new File(getFilesDir(),list[i]);
				if(!file.equals(new File(getFilesDir(),Security.sha1(getString(R.string.user_file)))))
					file.delete();
//					Utilities.showDialog(item.toString()+item.getItemId(), this);
//				else
//					file.delete();			
			}
			break;
		case R.id.action_new_user:
			File file = new File(getFilesDir(),Security.sha1(getString(R.string.user_file)));
			file.delete();
			dataSet.createNewUser();
			break;
		case R.id.timespan:
			TimespanDialog date = new TimespanDialog();
	    	date.show(getFragmentManager(), getString(R.string.datepicker));	
			break;
		case R.id.select_apps:
			SelectAppsDialogFragment appsA = new SelectAppsDialogFragment();
			appsA.setStrings(null);
			appsA.setMode(SelectAppsDialogFragment.SELECT_APPS);
			appsA.show(getFragmentManager(), getString(R.string.datepicker));
			break;
		case R.id.highlight_apps:
			SelectAppsDialogFragment appsB = new SelectAppsDialogFragment();
			appsB.setStrings(null);
			appsB.setMode(SelectAppsDialogFragment.SELECT_HIGHLIGHT_APPS);
			appsB.show(getFragmentManager(), getString(R.string.datepicker));	
			break;
		default:
			break;
		}
    	return true;
    }
    
    
    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        if(getActionBar().getTabCount()!= 0)
        	outState.putInt(getString(R.string.start_view), getActionBar().getSelectedNavigationIndex());
    }
    @Override
    public void onDestroy(){
    	super.onDestroy();
    }
    

    
	@Override
	public void onDataAvailable(JSONObject[] jObjs, String request) {
		/*if(request.equals(DataSet.RequestedFunction.initDataSet)){
			init(null);
		}else*/ if(request.equals(DataSet.RequestedFunction.getAllApps)){
			SelectAppsDialogFragment ignoreAppsDialog = new SelectAppsDialogFragment();
			ignoreAppsDialog.setStrings(Utilities.jObjValuesToArrayList(jObjs).toArray(new String[0]));
			ignoreAppsDialog.setMode(SelectAppsDialogFragment.IGNORE_APPS);
			ignoreAppsDialog.show(fm, "nada");
		}else if(request.equals(DataSet.RequestedFunction.getEventsAtDate) || request.equals(DataSet.RequestedFunction.updatedFilter)||request.equals(DataSet.RequestedFunction.getPositions)){
			if(jObjs!=null)
				TabListener.addData(jObjs);
			getActionBar().setSelectedNavigationItem(getActionBar().getSelectedNavigationIndex());
		}
	}
	
	public void switchTab(int tab, JSONObject jObj){
		/**
		 * jObj
		 * { "app":String
		 * 	 "time":int //time in sec -- start off app
		 * 	 "date":long //date as timestamp at 00:00
		 * 	 "lng": double -- longitude
		 *   "lat": double -- latitude
		 * }
		 */
        ActionBar actionBar = getActionBar();
        	
        if(tab>=actionBar.getTabCount()||tab<0)
        	return;
        actionBar.setSelectedNavigationItem(tab);

        if(jObj!=null){
	        switch (tab) {
			case 0:
				map.switchTab(jObj);
				break;
			case 1:
				chart.switchTab(jObj);
				break;
			case 2:
				time.switchTab(jObj);
				break;
			default:
				break;
			}
        }
	}
	
	private void init(Bundle savedInstanceState){
		if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) <
			        Configuration.SCREENLAYOUT_SIZE_LARGE) {
			getActionBar().setDisplayShowTitleEnabled(false);
			getActionBar().setDisplayShowHomeEnabled(false); 
		}
        /**
         * Set Up Tabs
         */        
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        Tab tab = actionBar.newTab();
        tab.setText("MapView");
        map = new TabListener<SectionMapFragment>(this, "mapView", SectionMapFragment.class);
        tab.setTabListener(map);
        actionBar.addTab(tab);
        
        tab = actionBar.newTab();
        tab.setText("ChartView");
        chart = new TabListener<SectionChartFragment>(this, "chartView", SectionChartFragment.class);
        tab.setTabListener(chart);
        actionBar.addTab(tab);
        
        tab = actionBar.newTab();
        tab.setText("Timeline");
        time = new TabListener<SectionTimelineFragment>(this, "timeline", SectionTimelineFragment.class);
        tab.setTabListener(time);
        actionBar.addTab(tab);

	}
}
