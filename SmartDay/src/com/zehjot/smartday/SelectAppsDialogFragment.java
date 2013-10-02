package com.zehjot.smartday;

import java.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;

import com.zehjot.smartday.R;
import com.zehjot.smartday.data_access.DataSet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class SelectAppsDialogFragment extends DialogFragment{
	public static final int SELECT_APPS=0;
	public static final int IGNORE_APPS=1;
	public static final int SELECT_HIGHLIGHT_APPS=2;
	private DataSet dataSet = null;
	private String[]strings = {"No Data Available"};
	private int mode=0;
	private static JSONObject selectedApps;
	private boolean[] boolSelectedApps;
	
	public void setStrings(String[] strings) {
		Arrays.sort(strings);
		this.strings = strings;
	}
	public void setMode(int mode) {
		this.mode = mode;
	}
	@Override
	public void onCreate(Bundle saved){
		super.onCreate(saved);
		if(saved!=null){
			strings= saved.getStringArray("strings");
			boolSelectedApps = saved.getBooleanArray("checked");
		}
	}
	@Override
	public Dialog onCreateDialog(Bundle saved){
		dataSet = DataSet.getInstance(getActivity());
		if(saved==null){	
			try{
				if(mode == SELECT_APPS){
					selectedApps = new JSONObject(dataSet.getSelectedApps().toString());
				}else if(mode == IGNORE_APPS){
					selectedApps = new JSONObject(dataSet.getIgnoreApps().toString());
				}else if(mode == SELECT_HIGHLIGHT_APPS){
					selectedApps = new JSONObject(dataSet.getSelectedHighlightApps().toString());
				}
			}catch(JSONException e){
				selectedApps = new JSONObject();
			}

			boolSelectedApps = new boolean[strings.length];
			if(mode == SELECT_APPS){
				for(int i=0 ; i<strings.length;i++){
					boolSelectedApps[i] = selectedApps.optBoolean(strings[i],true);
				}
			}else if(mode == IGNORE_APPS){
				for(int i=0 ; i<strings.length;i++){
					boolSelectedApps[i] = selectedApps.optBoolean(strings[i]);
				}
			}else if(mode == SELECT_HIGHLIGHT_APPS){
				for(int i=0 ; i<strings.length;i++){
					boolSelectedApps[i] = selectedApps.optBoolean(strings[i]);
				}
			}
		}	
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		builder.setTitle(R.string.options_app_select) //Dialogtitle
			   .setMultiChoiceItems(strings, boolSelectedApps,
				new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				try{
					if(isChecked){
						selectedApps.put(strings[which], "true");
					} else {
						selectedApps.put(strings[which], "false");
					}
				}catch(JSONException e){
					e.printStackTrace();
				}
				
			}
		})
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(mode == SELECT_APPS)
					dataSet.setSelectedApps(selectedApps);
				else if(mode == IGNORE_APPS)
					dataSet.setIgnoreApps(selectedApps);
				else if(mode == SELECT_HIGHLIGHT_APPS)
					dataSet.setSelectedHighlightApps(selectedApps);
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		return builder.create();
	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putStringArray("strings", strings);
		outState.putBooleanArray("checked", boolSelectedApps);
		super.onSaveInstanceState(outState);
	}
}
