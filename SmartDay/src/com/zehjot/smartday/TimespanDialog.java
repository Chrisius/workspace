package com.zehjot.smartday;



//import java.util.Calendar;

import com.zehjot.smartday.data_access.DataSet;
import com.zehjot.smartday.helper.Utilities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TableLayout;
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
		linearLayout.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT));
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);
		TextView tv1 = new TextView(getActivity());
		if(startDate==Utilities.getTodayTimestamp())
			tv1.setText("From:\nToday");
		else
			tv1.setText("From:\n"+Utilities.getDateShort(startDate));
			
		tv1.setTextSize(22);
		tv1.setGravity(Gravity.CENTER);
		tv1.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT,1.f));
		tv1.setId(101);
		tv1.setOnClickListener(this);
		
		TextView tv2 = new TextView(getActivity());
		if(endDate==Utilities.getTodayTimestamp())
			tv2.setText("To:\nToday");
		else
			tv2.setText("To:\n"+Utilities.getDateShort(endDate));
		tv2.setTextSize(22);
		tv2.setGravity(Gravity.CENTER);
		tv2.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT,1.f));
		tv2.setId(102);
		tv2.setOnClickListener(this);
		linearLayout.addView(tv1);
		linearLayout.addView(tv2);
		builder.setTitle("select dates") //Dialogtitle
		.setView(linearLayout)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
		    	if(listener==null)
		    		DataSet.getInstance(getActivity()).setSelectedDates(startDate, endDate);
		    	else
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
				view.setText("From:\nToday");
			else
				view.setText("From:\n"+Utilities.getDateShort(timestamp));
			
			//if startDate>endDate set endDate=startDate and update view
			if(timestamp>endDate){
				endDate=timestamp;
				view = (TextView) linearLayout.getChildAt(1);		
				if(endDate==today)
					view.setText("To:\nToday");
				else
					view.setText("To:\n"+Utilities.getDateShort(endDate));			
			}
		}else{
			endDate = timestamp;
			TextView view = (TextView) linearLayout.getChildAt(1);	
			if(timestamp==today)
				view.setText("To:\nToday");
			else
				view.setText("To:\n"+Utilities.getDateShort(timestamp));
			
			//if endDate<startDate set startDate=endDate and update view
			if(timestamp<startDate){
				startDate=timestamp;
				view = (TextView) linearLayout.getChildAt(0);		
				if(startDate==today)
					view.setTag("From:\nToday");
				else
					view.setText("From:\n"+Utilities.getDateShort(startDate));		
			}
		}
	}

	@Override
	public void onClick(View v) {
		if(v.getId()==101){
	    	DatePickerFragment dateStart = new DatePickerFragment();
	    	dateStart.setTimestamp(startDate);
	    	dateStart.setListener(this,"start");
	    	dateStart.show(getFragmentManager(), getString(R.string.datepicker));			
		}else if(v.getId()==102){
	    	DatePickerFragment dateEnd = new DatePickerFragment();
	    	dateEnd.setTimestamp(endDate);
	    	dateEnd.setListener(this,"end");
	    	dateEnd.show(getFragmentManager(), getString(R.string.datepicker));			
		}
	}
	
}
