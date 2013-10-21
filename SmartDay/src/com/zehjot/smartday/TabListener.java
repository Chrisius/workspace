package com.zehjot.smartday;

import org.json.JSONObject;

import android.app.ActionBar.Tab;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.util.Log;

public class TabListener<T extends Fragment> implements ActionBar.TabListener {
    private Fragment fragment;
    private final Activity activity;
    private final String tag;
    private final Class<T> fragmentClass;
//    private static OnSectionSelectedListener options;
    private static JSONObject[] jObjs;
    
    public interface OnSectionSelectedListener{
    	public void onSectionSelected(int pos);
    }
    public interface OnUpdateListener{
    	public void onUpdate(JSONObject[]jObjs);
    	public void putExtra(JSONObject jObj);
    }
        
    public TabListener(Activity activity, String tag, Class<T> fragmentClass) {
        this.activity = activity;
        this.tag = tag;
        this.fragmentClass = fragmentClass;
//        options = listener;
		fragment = Fragment.instantiate(activity, fragmentClass.getName());
		//fragment..add(R.id.section_fragment_container, fragment, tag);
    }

    public void onTabSelected(Tab tab, FragmentTransaction ft) {  
        if (!fragment.isAdded()) {
        	//fragment = activity.getFragmentManager().findFragmentByTag(tag);
        	//if(fragment == null){
            	Log.d("TabSelected", "Fragment not found "+tag);
        		//fragment = Fragment.instantiate(activity, fragmentClass.getName());
        		ft.add(R.id.section_fragment_container, fragment, tag);
        	//}
        } else {
            ft.show(fragment);
        	((OnUpdateListener) fragment).onUpdate(jObjs);   
           // if(fragment.isResumed())         
        }
//        options.onSectionSelected(tab.getPosition());
    }

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        int tabPosition = tab.getPosition();
        if(tabPosition!= Tab.INVALID_POSITION){
        	((MainActivity) activity).setLastTab(tabPosition);
        }	
        if (fragment != null) {        	
            ft.hide(fragment);
        }
    }

    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        if (fragment == null) {
        	fragment = activity.getFragmentManager().findFragmentByTag(tag);            	
        	Log.d("TabReselected", "Fragment not found "+tag);
    		fragment = Fragment.instantiate(activity, fragmentClass.getName());
    		ft.add(R.id.section_fragment_container, fragment, tag);
        }
        else
        	((OnUpdateListener) fragment).onUpdate(jObjs);
    }
    
    public static void addData(JSONObject[] jObjs){
    	TabListener.jObjs = jObjs;
    }
    
    public void switchTab(JSONObject jObj){
        	((OnUpdateListener) fragment).putExtra(jObj);    	
    }
}
