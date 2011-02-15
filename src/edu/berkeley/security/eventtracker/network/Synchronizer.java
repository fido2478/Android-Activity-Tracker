package edu.berkeley.security.eventtracker.network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import edu.berkeley.security.eventtracker.EventActivity;
import edu.berkeley.security.eventtracker.Settings;
import edu.berkeley.security.eventtracker.eventdata.EventEntry;
import edu.berkeley.security.eventtracker.eventdata.EventManager;

public class Synchronizer extends IntentService {
	public static final String EVENT_DATA_EXTRA = "EventData";
	public static final String REQUEST_EXTRA = "Request";

	private EventManager manager;

	public Synchronizer() {
		super("Synchronizer");
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		manager = EventManager.getManager();
		Bundle bundle = intent.getExtras();

		EventEntry event = (EventEntry) bundle
				.getSerializable(EVENT_DATA_EXTRA);
		ServerRequest request = (ServerRequest) bundle
				.getSerializable(REQUEST_EXTRA);

		PostRequestResponse response;
		switch (request) {
		case SENDDATA:
			response = Networking.sendPostRequest(event, request);
			if (response.isSuccess()) {
				manager.updateDatabase(event, true);
			}
			break;
		case REGISTER:
			response = Networking.sendPostRequest(ServerRequest.REGISTER);
			if (response.isSuccess())
				Settings.confirmRegistrationWithWebServer();
			break;
		case UPDATE:
		case DELETE:
			response = Networking.sendPostRequest(event, request);
			break;
		case POLL:
			response = Networking.sendPostRequest(event, request);
			if (response.isSuccess())
				try {
					parseEventPollResponse(response.getContent());
				} catch (JSONException e) {
					Log.e(EventActivity.LOG_TAG,
							"Could not parse JSON response.", e);
				}
			break;
		}
	}

	/**
	 * Parses a poll response. Updates and creates relevant events and updates
	 * the pollTime.
	 * 
	 * @param jsonResponseString
	 *            the response from the server.
	 * @throws JSONException
	 *             if the response is poorly formatted.
	 */
	void parseEventPollResponse(String jsonResponseString) throws JSONException {
		EventManager manager = EventManager.getManager();
		JSONObject jsonResponse = new JSONObject(jsonResponseString);
		String pollTime = jsonResponse.getString("pollTime");
		JSONArray events = jsonResponse.getJSONArray("events");
		for (int eventIndex = 0; eventIndex < events.length(); eventIndex++) {
			JSONObject eventContents = events.getJSONObject(eventIndex);
			String uuid = eventContents.getString("uuid");
			EventEntry event = manager.findOrCreateByUUID(uuid);
			String updated_at = eventContents.getString("updated_at");
			if (!event.newerThan(updated_at)) {
				event.mName = eventContents.getString("name");
				event.mNotes = eventContents.getString("notes");
				event.mStartTime = eventContents.getLong("startTime");
				event.mEndTime = eventContents.getLong("endTime");
				event.deleted = eventContents.getBoolean("deleted");
				if (event.deleted && !event.persisted)
					break; // trying to create a deleted event!
				manager.updateDatabase(event, true);
			}
		}
		Settings.setPollTime(pollTime);
	}
}