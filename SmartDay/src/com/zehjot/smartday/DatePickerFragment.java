package com.zehjot.smartday;

import java.util.Calendar;

import com.zehjot.smartday.data_access.DataSet;
import com.zehjot.smartday.helper.Utilities;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.DatePicker;

public class DatePickerFragment extends DialogFragment 
					implements DatePickerDialog.OnDateSetListener, OnClickListener {
	OnDateChosenListener mCallback;
	String whichDate;
	private long timestamp=-1;
	
	public void setTimestamp(long timestamp){
		this.timestamp=timestamp;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Calendar c = Calendar.getInstance();
		if(timestamp==-1){
			if(whichDate.equals("start")){
				c.setTimeInMillis(DataSet.getInstance(getActivity()).getSelectedDateStartAsTimestamp()*1000);
			}
			else{
				c.setTimeInMillis(DataSet.getInstance(getActivity()).getSelectedDateEndAsTimestamp()*1000);			
			}
		}
		else{
			c.setTimeInMillis(timestamp*1000);		
		}
		int day = c.get(Calendar.DAY_OF_MONTH);
		int month = c.get(Calendar.MONTH);
		int year = c.get(Calendar.YEAR);
		DatePickerDialog dpd =new DatePickerDialog(getActivity(), this, year, month, day);
		dpd.setButton(DatePickerDialog.BUTTON_NEUTRAL, getActivity().getString(R.string.today), this);
		return dpd;
	}
	public void onDateSet(DatePicker view, int year, int month, int day){
		mCallback.onDateChosen(Utilities.getTimestamp(year, month, day, 0, 0, 0), whichDate);
	}
	
	public interface OnDateChosenListener{
		public void onDateChosen(long timestamp,String whichDate);
	}

	public void setListener(OnDateChosenListener optionsListFragment,String whichDate) {
		mCallback = optionsListFragment;
		this.whichDate=whichDate;		
	}
	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		Calendar c = Calendar.getInstance();
		int day = c.get(Calendar.DAY_OF_MONTH);
		int month=c.get(Calendar.MONTH);
		int year = c.get(Calendar.YEAR);
		mCallback.onDateChosen(Utilities.getTimestamp(year, month, day, 0, 0, 0), whichDate);
	}
}
