package com.zehjot.smartday;



import java.util.Calendar;

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
		long startDate = DataSet.getInstance(getActivity()).getSelectedDateStartAsTimestamp();
		long endDate = DataSet.getInstance(getActivity()).getSelectedDateEndAsTimestamp();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		linearLayout = new LinearLayout(getActivity());
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);
		TextView tv1 = new TextView(getActivity());
		tv1.setText(" From: "+Utilities.getDate(startDate));
		tv1.setTextSize(22);
		tv1.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		tv1.setId(101);
		tv1.setOnClickListener(this);
		TextView tv2 = new TextView(getActivity());
		tv2.setText("To: "+DataSet.getInstance(getActivity()).getSelectedDateEndAsString());
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
				long startTimestamp = Utilities.getTimestamp(startYear, startMonth, startDay, 0, 0, 0);
				long endTimestamp = Utilities.getTimestamp(endYear, endMonth, endDay, 0, 0, 0);
				listener.onDateChosen(startTimestamp, endTimestamp);
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
	public void onDateChosen(int year, int month, int day, String whichDate) {
		long timestamp = Utilities.getTimestamp(year, month, day, 0, 0, 0);
		/**
		 * Upper bound for date
		 */
		long today = Utilities.getTodayTimestamp();
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(today*1000);
		int maxYear = c.get(Calendar.YEAR);
		int maxMonth = c.get(Calendar.MONTH);
		int maxDay = c.get(Calendar.DAY_OF_MONTH);
		
		if(year>maxYear)
			year=maxYear;	
		if(month>maxMonth&&year==maxYear){
			month = maxMonth;
			day = maxDay;
		}
		if(day>maxDay&&month>=maxMonth&&year>=maxYear)
			day = maxDay;	
		/**
		 * Display and save selected date
		 */
		if(whichDate.equals("start")){			
			TextView view = (TextView) linearLayout.getChildAt(0);	
			if(day==maxDay&&month==maxMonth&&year==maxYear)
				view.setText("  From: Today");
			else
				view.setText("  From: "+Utilities.getDateShort(timestamp));
			
			//if startDate>endDate set endDate=startDate and update view
			if(year>endYear)
				endYear=year;	
			if(month>endMonth&&year>=endYear){
				endMonth = month;
				endDay = day;
			}
			if(day>endDay&&month>=endMonth&&year>=endYear)
				endDay = day;
			view = (TextView) linearLayout.getChildAt(1);		
			if(endDay==maxDay&&endMonth==maxMonth&&endYear==maxYear)
				view.setTag("  To: Today");
			else
				view.setText("  To: "+Utilities.getDateShort(timestamp));			
			
			
			startDay = day;
			startMonth = month;
			startYear = year;
		}else{
			TextView view = (TextView) linearLayout.getChildAt(1);	
			if(day==maxDay&&month==maxMonth&&year==maxYear)
				view.setText("To: Today");
			else
				view.setText("To: "+day+". "+getActivity().getResources().getStringArray(R.array.months)[month]+" "+year);
			
			//if endDate<startDate set startDate=endDate and update view
			if(year<startYear)
				startYear=year;	
			if(month<startMonth&&year<=startYear){
				startMonth = month;
				startDay = day;
			}
			if(day<startDay&&month<=startMonth&&year<=startYear)
				startDay=day;	
			view = (TextView) linearLayout.getChildAt(0);		
			if(startDay==maxDay&&startMonth==maxMonth&&startYear==maxYear)
				view.setTag("  From: Today");
			else
				view.setText("  From: "+startDay+". "+getActivity().getResources().getStringArray(R.array.months)[startMonth]+" "+startYear);		
			
			endDay = day;
			endMonth = month;
			endYear = year;
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
