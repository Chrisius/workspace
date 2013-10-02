package com.zehjot.smartday;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;

import com.zehjot.smartday.data_access.DataSet;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;

public class ColorPickerDialog extends DialogFragment{
	JSONArray newColors = new JSONArray();
	JSONArray colorsOfApps;
	List<App> apps = new ArrayList<App>();

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		ScrollView view = new ScrollView(getActivity());
		view.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT));
		//view.setFillViewport(true);
		
		LinearLayout layout = new LinearLayout(getActivity());
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		layout.setOrientation(LinearLayout.VERTICAL);
		view.addView(layout);
		
		TextView textView = null;
		
		try {
			colorsOfApps = DataSet.getInstance(getActivity()).getColorsOfApps().getJSONArray("colors");
			for(int i=0; i<colorsOfApps.length();i++){
				
				apps.add(new App(colorsOfApps.getJSONObject(i).getString("app"),colorsOfApps.getJSONObject(i).getInt("color"),i));		
			}
			Collections.sort(apps, new Comparator<App>(){
				@Override
				public int compare(App a1, App a2) {
					return a1.appname.compareTo(a2.appname);
				}
			});
		} catch (JSONException e) {
			e.printStackTrace();
		}
		for(int i =0; i< apps.size(); i++){
			LinearLayout row = new LinearLayout(getActivity());
			row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
			row.setGravity(Gravity.CENTER);
			row.setOrientation(LinearLayout.HORIZONTAL);
			row.setClickable(true);
			row.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final LinearLayout view = (LinearLayout) v;
					final int index = ((LinearLayout)view.getParent()).indexOfChild(v);
					final String appName = ((TextView) ((LinearLayout)view).getChildAt(0)).getText().toString();
					Log.d("row", "clicked"+appName);
					OnAmbilWarnaListener listener = new OnAmbilWarnaListener() {
						
						@Override
						public void onOk(AmbilWarnaDialog dialog, int color) {
							TextView colorField = (TextView) ((LinearLayout)view).getChildAt(1);
							colorField.setBackgroundColor(color);
							try {
								if(newColors.length()<1){
									newColors.put(new JSONObject()
									.put("app", appName)
									.put("color", color)
									.put("index", index)
									);
								}else{
									boolean found = false;
									for(int i=0; i<newColors.length();i++){
										JSONObject appColor = newColors.getJSONObject(i);
										if(appColor.getString("app").equals(appName)){
											appColor.put("color", color);
											found = true;
											break;
										}
									}
									if(!found){
										newColors.put(new JSONObject()
										.put("app", appName)
										.put("color", color)
										.put("index", index)
										);
									}
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
							
						}
						
						@Override
						public void onCancel(AmbilWarnaDialog dialog) {
							
						}
					};
						int color = ((ColorDrawable)(view.getChildAt(1).getBackground())).getColor();
						AmbilWarnaDialog dialog = new AmbilWarnaDialog(getActivity(), color, listener);
						dialog.show();
				}
			});
			
			textView = new TextView(getActivity());
			textView.setText(apps.get(i).appname);
			textView.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1.f));
			textView.setTextSize(22);
			textView.setPadding(20, 5, 0, 5);
			row.addView(textView);
			
			
			TextView color = new TextView(getActivity());
			color.setBackgroundColor(apps.get(i).color);
			color.setLayoutParams(new LayoutParams(50, LayoutParams.WRAP_CONTENT));
			color.setPadding(0, 5, 5, 5);
			row.addView(color);
			
			layout.addView(row);
		}
		builder.setTitle("Chose a color");
		builder.setView(view);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try{
					for(int i=0; i < newColors.length();i++){
						JSONObject newColorOfApp = newColors.getJSONObject(i);
						JSONObject oldColorOfApp = colorsOfApps.getJSONObject(apps.get(newColorOfApp.getInt("index")).indexAtJSONArray);
						oldColorOfApp.put("color", newColorOfApp.getInt("color"));
					}
					DataSet.getInstance(getActivity()).setColorsOfApps(new JSONObject().put("colors", colorsOfApps));
				}catch(JSONException e){
					
				}
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		return builder.create();
	}
	private class App{		
		public String appname;
		public int color;
		public int indexAtJSONArray;
		App(String appname, int color, int index){
			this.appname = appname;
			this.color = color;
			this.indexAtJSONArray = index;
		}
	}
}
