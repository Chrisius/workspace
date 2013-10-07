package com.zehjot.smartday;



//import java.util.Calendar;

import com.zehjot.smartday.data_access.DataSet;
import com.zehjot.smartday.helper.Utilities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TimespanDialog extends DialogFragment implements DatePickerFragment.OnDateChosenListener, View.OnClickListener {

	private LinearLayout linearLayout;
	private long startDate;
	private long endDate;
	private OnDateChosenListener listener;
	
	public interface OnDateChosenListener{
		void onDateChosen(long startTimestamp, long endTimestamp);
	}
	
	public void setListener(OnDateChosenListener listener){
		this.listener = listener;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle saved){
		startDate = DataSet.getInstance(getActivity()).getSelectedDateStartAsTimestamp();
		endDate = DataSet.getInstance(getActivity()).getSelectedDateEndAsTimestamp();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		linearLayout = new LinearLayout(getActivity());
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);
		TextView tv1 = new TextView(getActivity());
		if(startDate==Utilities.getTodayTimestamp())
			tv1.setText(" From: Today");
		else
			tv1.setText(" From: "+Utilities.getDateShort(startDate));
			
		tv1.setTextSize(22);
		tv1.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		tv1.setId(101);
		tv1.setOnClickListener(this);
		
		TextView tv2 = new TextView(getActivity());
		if(endDate==Utilities.getTodayTimestamp())
			tv2.setText("To: Today");
		else
			tv2.setText("To: "+Utilities.getDateShort(endDate));
		tv2.setTextSize(22);
		tv2.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		tv2.setId(102);
		tv2.setOnClickListener(this);
		linearLayout.addView(tv1,
				250,50);
		linearLayout.addView(tv2,
				250,50);
		builder.setTitle("select dates") //Dialogtitle
		.setView(linearLayout)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.onDateChosen(startDate, endDate);
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
	public void onDateChosen(long timestamp, String whichDate) {
		/**
		 * Upper bound for date
		 */
		long today = Utilities.getTodayTimestamp();		
		if(timestamp>today)
			timestamp = today;
		/**
		 * Display and save selected date
		 */
		if(whichDate.equals("start")){
			startDate = timestamp;			
			TextView view = (TextView) linearLayout.getChildAt(0);	
			if(timestamp==today)
				view.setText(" From: Today");
			else
				view.setText(" From: "+Utilities.getDateShort(timestamp));
			
			//if startDate>endDate set endDate=startDate and update view
			if(timestamp>endDate){
				endDate=timestamp;
				view = (TextView) linearLayout.getChildAt(1);		
				if(endDate==today)
					view.setText("To: Today");
				else
					view.setText("To: "+Utilities.getDateShort(endDate));			
			}
		}else{
			endDate = timestamp;
			TextView view = (TextView) linearLayout.getChildAt(1);	
			if(timestamp==today)
				view.setText("To: Today");
			else
				view.setText("To: "+Utilities.getDateShort(timestamp));
			
			//if endDate<startDate set startDate=endDate and update view
			if(timestamp<startDate){
				startDate=timestamp;
				view = (TextView) linearLayout.getChildAt(0);		
				if(startDate==today)
					view.setTag(" From: Today");
				else
					view.setText(" From: "+Utilities.getDateShort(startDate));		
			}
		}
	}

	@Override
	public void onClick(View v) {
		if(v.getId()==101){
	    	DatePickerFragment dateEnd = new DatePickerFragment();
	    	dateEnd.setListener(this,"start");
	    	dateEnd.show(getFragmentManager(), getString(R.string.datepicker));			
		}else if(v.getId()==102){
	    	DatePickerFragment dateEnd = new DatePickerFragment();
	    	dateEnd.setListener(this,"end");
	    	dateEnd.show(getFragmentManager(), getString(R.string.datepicker));			
		}
	}
	
}
