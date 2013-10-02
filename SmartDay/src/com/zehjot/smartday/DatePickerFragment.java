package com.zehjot.smartday;

import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;

public class DatePickerFragment extends DialogFragment 
					implements DatePickerDialog.OnDateSetListener {
	OnDateChosenListener mCallback;
	String whichDate;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Calendar c = Calendar.getInstance();
		int day = c.get(Calendar.DAY_OF_MONTH);
		int month = c.get(Calendar.MONTH);
		int year = c.get(Calendar.YEAR);
		return new DatePickerDialog(getActivity(), this, year, month, day);
	}
	public void onDateSet(DatePicker view, int year, int month, int day){
		mCallback.onDateChosen(year, month, day, whichDate);
	}
	
	public interface OnDateChosenListener{
		public void onDateChosen(int year, int month, int day,String whichDate);
	}

	public void setListener(OnDateChosenListener optionsListFragment,String whichDate) {
		mCallback = optionsListFragment;
		this.whichDate=whichDate;
		
	}
}
