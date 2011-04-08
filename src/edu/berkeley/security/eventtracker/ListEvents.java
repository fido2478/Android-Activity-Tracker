package edu.berkeley.security.eventtracker;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter.ViewBinder;
import edu.berkeley.security.eventtracker.eventdata.EventCursor;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventDbAdapter.EventKey;
import edu.berkeley.security.eventtracker.network.Networking;
import edu.berkeley.security.eventtracker.network.ServerRequest;

/**
 * Handles the event list view that displays all events from most recent to
 * least recent.
 */
public class ListEvents extends EventActivity {
	private static Date dateListed;
	/**
	 * An array that specifies the fields we want to display in the list (only
	 * TITLE)
	 */
	private String[] from = new String[] { EventKey.NAME.columnName(),
			EventKey.START_TIME.columnName(), EventKey.END_TIME.columnName(),
			EventKey.ROW_ID.columnName() };

	/**
	 * An array that specifies the layout elements we want to map event fields
	 * to.
	 */
	private int[] to = new int[] { R.id.row_event_title,
			R.id.row_event_start_time, R.id.row_event_end_time,
			R.id.row_event_delete_button };

	private EventCursor mEventsCursor;
	SimpleCursorAdapter eventsCursorAdapter;
	private ListView eventList;
	private TextView titleHeader;
	private TextView listHeader;
	
	//Variables for the date picker
	private ImageView mPickDate;
	private int mYear;
	private int mMonth;
	private int mDay;

	private static final int DATE_DIALOG_ID = 0;
	private static final int DIALOG_DELETE_EVENT = 1;
	 // the callback received when the user "sets" the date in the dialog
    private DatePickerDialog.OnDateSetListener mDateSetListener =
            new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year, 
                                      int monthOfYear, int dayOfMonth) {
                    mYear = year;
                    mMonth = monthOfYear;
                    mDay = dayOfMonth;
                    Calendar c=Calendar.getInstance();
                    c.set(mYear, mMonth, mDay);
                    dateListed=c.getTime();
                 
                    if(mEventsCursor != null){
 	            	   
 	            	   stopManagingCursor(mEventsCursor);
 	            	   mEventsCursor.close();
 	            	   eventList.invalidate();
 	            
 	               }
 	              
 	               fillData();
                }
            };
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dateListed=Calendar.getInstance().getTime();
		eventList = (ListView) findViewById(R.id.events_list_view);
		ImageView leftArrow= (ImageView) findViewById(R.id.leftArrow);
		ImageView rightArrow= (ImageView) findViewById(R.id.rightArrow);
		fillData();
		leftArrow.setOnClickListener(new View.OnClickListener() {
			 public void onClick(View view) {
	              
	               dateListed.setDate(dateListed.getDate()-1);
	               
	               if(mEventsCursor != null){
	            	   
	            	   stopManagingCursor(mEventsCursor);
	            	   mEventsCursor.close();
	            	   eventList.invalidate();
	            
	               }
	              
	               fillData();
	            }
	        });
		
		rightArrow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
              
               dateListed.setDate(dateListed.getDate()+1);
               
               if(mEventsCursor != null){
            	   
            	   stopManagingCursor(mEventsCursor);
            	   mEventsCursor.close();
            	   eventList.invalidate();
            
               }
              
               fillData();
            }
        });
		 mPickDate = (ImageView) findViewById(R.id.calendar);
		 // get the current date
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
     // add a click listener to the button
        mPickDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });

	}
	
	@Override
	protected int getLayoutResource() {
		return R.layout.events_list;
	}

	/**
	 * Sets the adapter to fill the rows of the ListView from the database rows.
	 */
	private void fillData() {
		mEventsCursor = mEventManager.fetchSortedEvents(dateListed);
		
//		 Get all of the rows from the database and create the item list
//		mEventsCursor = mEventManager.fetchSortedEvents();
		startManagingCursor(mEventsCursor);

		
		
		SimpleCursorAdapter eventsCursorAdapter = new SimpleCursorAdapter(ListEvents.this,
				R.layout.events_row, mEventsCursor, from, to);
		eventsCursorAdapter.setViewBinder(new EventRowViewBinder());
		

		initializeHeaders(eventList);

		eventList.setEmptyView(findViewById(R.id.empty_list_view));

		eventList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position < 1)
					return;
				else if (position == 1 && isTracking())
					finish(); // trying to edit event in progress
				else
					startEditEventActivity(id);
			}
		});

		eventList.setAdapter(eventsCursorAdapter);

		
	}

	/**
	 * Initializes the headers for the given list.
	 * 
	 * @param list
	 *            The list to add headers to.
	 */
	private void initializeHeaders(ListView list) {
		int headerCount=list.getHeaderViewsCount();
		TextView titleHeader=(TextView) findViewById(R.id.titleHeader);

		String titleForList="Activites for ";
		
		titleForList +=(isToday(dateListed)) ? "Today" : DateFormat.getDateInstance().format(dateListed);
		
		titleHeader.setText(titleForList);
//		textTitle.setText(R.string.activityListHeader);
		titleHeader.setTextSize(20);
		titleHeader.setGravity(Gravity.CENTER);
		
		
		if(headerCount==0){
//		list.addHeaderView(titleHeader);
		View listHeader = View.inflate(this, R.layout.event_row_header, null);
		list.addHeaderView(listHeader);
		}
		
		
	}
	private boolean isToday(Date date){
		String dateString=DateFormat.getDateInstance().format(date);
		String todayString=DateFormat.getDateInstance().format(new Date());
		return dateString.equals(todayString);
	}

	/**
	 * The listener associated with a delete button. Deletes the event
	 * corresponding to the row the button is in.
	 */
	private class DeleteRowListener implements OnClickListener {
		private long rowId;
		private boolean isInProgress;

		private DeleteRowListener(long rowId, boolean isInProgress) {
			this.rowId = rowId;
			this.isInProgress = isInProgress;
		}

		@Override
		public void onClick(View v) {
			Bundle bundle=new Bundle();
			bundle.putString("nameOfEvent",mEventManager.fetchEvent(rowId).mName);
			bundle.putLong("rowId", rowId);
			bundle.putBoolean("isInProgress", isInProgress);	
			showDialog(DIALOG_DELETE_EVENT, bundle);

		}
	}

	/**
	 * Helps interface the Cursor with the view, updating the views of a row
	 * with values in the DB.
	 */
	private class EventRowViewBinder implements ViewBinder {

		@Override
		public boolean setViewValue(View view, final Cursor cursor,
				int columnIndex) {
			EventCursor eCursor = new EventCursor(cursor, mEventManager);
			EventKey colType = eCursor.getColumnType(columnIndex);
			switch (colType) {
			case ROW_ID:
				// Initializing the delete button
				long rowId = cursor.getLong(columnIndex);
				boolean isInProgress = cursor.getLong(cursor
						.getColumnIndex(EventKey.END_TIME.columnName())) == 0;
				view.setOnClickListener(new DeleteRowListener(rowId,
						isInProgress));
				return true;
			case START_TIME:
			case END_TIME:
				EventEntry event = eCursor.getEvent();
				String dateString = event.formatColumn(eCursor
						.getColumnType(columnIndex));
				((TextView) view).setText(dateString);
				return true;
			default:
				return false;
			}
		}
	}

	@Override
	protected void startTrackingActivity() {
		super.startTrackingActivity();
		overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
	}
	//TODO fix the flinging
//	@Override
//	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
//			float velocityY) {
//		if (velocityX < 0) { // going to left screen
//			startTrackingActivity();
//			return true;
//		}
//		return false;
//	}
	@Override
	protected Dialog onCreateDialog(int id) {
	    switch (id) {
	    case DATE_DIALOG_ID:
	        return new DatePickerDialog(this,
	                    mDateSetListener,
	                    mYear, mMonth, mDay);
	    }
	    return null;
	}
	
	@Override
	protected void onPrepareDialog(final int id, final Dialog dialog, Bundle bundle) {
	  switch (id) {
	  case DIALOG_DELETE_EVENT:
	    TextView deleteEvent= (TextView) dialog.findViewById(R.id.delete_description);
	    deleteEvent.setText("Are you sure you want to delete the event "+ bundle.getString("nameOfEvent")+"?");
	    break;
	  }
	}
	
	
	/*
	 * Dialog box for presenting a dialog box when a user tries to delete an event
	 */
	@Override

	protected Dialog onCreateDialog(int id, final Bundle bundle) {
		switch (id) {

		case DIALOG_DELETE_EVENT:
			
			LayoutInflater te_factory = LayoutInflater.from(this);
			final View textEntryView = te_factory.inflate(
					R.layout.delete_dialog, null);
			return new AlertDialog.Builder(this)
					.setIcon(R.drawable.alert_dialog_icon)
					.setTitle(R.string.delete_dialog_title)
					.setView(textEntryView)
					.setPositiveButton(R.string.delete_dialog_yes,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int whichButton) {
									long rowId=bundle.getLong("rowId");
									boolean isInProgress=bundle.getBoolean("isInProgress");
									
									mEventManager.markEventDeleted(rowId);
									Networking.sendToServer(ServerRequest.DELETE,
											mEventManager.fetchEvent(rowId), ListEvents.this);
									mEventsCursor.requery();
									if (isInProgress) {
										updateTrackingStatus(false);
									}	

								}
							})
					.setNegativeButton(R.string.alert_dialog_cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked cancel so do some stuff */
								}
							}).create();
		default:
			return super.onCreateDialog(id, bundle);
		}
	}

	
	
}
