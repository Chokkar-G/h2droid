package com.frankcalise.h2droid;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class h2droid extends Activity {
	private int mConsumption = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set up main layout
        setContentView(R.layout.main);
    }
    
    /** Called when activity returns to foreground */
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	loadTodaysEntriesFromProvider();
    }
    
    /** Set up menu for main activity */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_menu, menu);
    	
    	return true;
    }
    
    /** Handle menu selection */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.menu_settings:
    			startActivity(new Intent(this, Settings.class));
    			return true;
    	}
    	
    	return false;
    }
    
    /** Handle "add one serving" action */
    public void onOneServingClick(View v) {
		Log.d("ADD", "One serving");
		Entry oneServing = new Entry(8, true);
		addNewEntry(oneServing);
    }
    
    /** Handle "add two servings" action */
    public void onTwoServingsClick(View v) {
		Log.d("ADD", "Two servings");
		Entry twoServings = new Entry(16, true);
		addNewEntry(twoServings);
    }
    
    /** Handle "add custom serving" action */
    public void onCustomServingClick(View v) {
    	Log.d("ADD", "Custom serving");
    	// TODO bring up dialog or another activity for
    	// adding some amount of water other than
    	// one or two servings
    }
    
    /** Handle "undo last serving" action */
    public void onUndoClick(View v) {
    	Log.d("UNDO", "Last serving");
		// TODO remove last entry from today
		undoTodaysLastEntry();
    }
    
    private void addNewEntry(Entry _entry) {
    	Log.d("CONTENT", "in addNewEntry");
    	    	
    	ContentResolver cr = getContentResolver();
    	
    	// Insert the new entry into the provider
    	ContentValues values = new ContentValues();
    	
    	values.put(WaterProvider.KEY_DATE, _entry.getDate());
    	values.put(WaterProvider.KEY_AMOUNT, _entry.getMetricAmount());
    	
    	cr.insert(WaterProvider.CONTENT_URI, values);
    	
    	mConsumption += _entry.getNonMetricAmount();
    	
    	// Make a toast displaying add complete
    	String toastMsg = String.format("Added %.1f fl oz", _entry.getNonMetricAmount());
    	Toast toast = Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT);
    	toast.setGravity(Gravity.BOTTOM, 0, 0);
    	toast.show();
    	
    	// Update the amount of consumption on UI
    	updateConsumptionTextView();
    }
    
    private void undoTodaysLastEntry() {
    	Log.d("UNDO", "in undo");
    	String order = WaterProvider.KEY_DATE + " DESC LIMIT 1";
    	//String where = "date('" + now + "') = date('" + WaterProvider.KEY_DATE + "')";
    	//Log.d("UNDO", "WHERE: " + where);
    	ContentResolver cr = getContentResolver();
    	
    	// Get latest entry
    	Cursor c = cr.query(WaterProvider.CONTENT_URI,
    					    null, null, null, order);
    	//int results = cr.delete(WaterProvider.CONTENT_URI, where, null)
    	if (c.moveToFirst()) {
			String date = c.getString(WaterProvider.DATE_COLUMN);			
			double metricAmount = c.getDouble(WaterProvider.AMOUNT_COLUMN);
			boolean isNonMetric = false;
			
			Entry e = new Entry(date, metricAmount, isNonMetric);
			
			mConsumption -= e.getNonMetricAmount();
			updateConsumptionTextView();
			
			Log.d("UNDO", e.toString());
			
			// TODO actually remove this entry
    	} else {
    		// No entries for today
    		Log.d("UNDO", "No entries");
    	}
    	
    	c.close();	
    }
    
    private void loadTodaysEntriesFromProvider() {
    	mConsumption = 0;
    	
    	Log.d("CONTENT", "in loadTodaysEntriesFromProvider()");
    	
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");// HH:mm:ss");
    	Date now = new Date();
    	String where = "'" + sdf.format(now) + "' = date(" + WaterProvider.KEY_DATE + ")";
    	
    	ContentResolver cr = getContentResolver();
    	
    	// Return all saved entries
    	Cursor c = cr.query(WaterProvider.CONTENT_URI,
    					    null, where, null, null);
    	
    	if (c.moveToFirst()) {
    		do {
    			String date = c.getString(WaterProvider.DATE_COLUMN);
    			double metricAmount = c.getDouble(WaterProvider.AMOUNT_COLUMN);
    			boolean isNonMetric = false;
    			Entry e = new Entry(date, metricAmount, isNonMetric);
    			
    			mConsumption += e.getNonMetricAmount();
    			
    			Log.d("CONTENT", e.toString());
    		} while (c.moveToNext());
    	}
    	
    	c.close();
    	
    	updateConsumptionTextView();
    }
    
    /** Update the today's consumption TextView */
    private void updateConsumptionTextView() {
    	// TODO really need to load total from db here
    	double percentGoal = (mConsumption / 64.0) * 100.0;
    	double delta = mConsumption - 64.0;
    	String sign;
    	if (delta >= 0) {
    		sign = "+";
    	} else {
    		sign = "�";
    	}
    	if (percentGoal > 100.0) {
    		percentGoal = 100.0;
    	}
    	TextView tv = (TextView)findViewById(R.id.consumption_textview);
    	tv.setText("Today's water consumption: " + mConsumption + " oz\n" + 
    			   "Goal percent complete: " + percentGoal + "% (" + 
    			   sign + delta + " fl oz)");
    }
}