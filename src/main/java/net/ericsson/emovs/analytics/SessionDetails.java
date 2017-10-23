package net.ericsson.emovs.analytics;

import android.support.v4.util.Pair;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Joao Coelho on 2017-10-02.
 */

public class SessionDetails {
    public final static String SESSION_STATE_IDLE = "IDLE";
    public final static String SESSION_STATE_PLAYING = "PLAYING";
    public final static String SESSION_STATE_DIRTY = "DIRTY";
    public final static String SESSION_STATE_FINISHED = "FINISHED";
    public final static String SESSION_STATE_REMOVED = "REMOVED";

    boolean isForbidden;
    int currentRetries;
    long currentTime;
    long clockOffset;
    String currentState;
    String sessionId;
    JSONArray events;

    public SessionDetails() {
        this.currentState = SESSION_STATE_IDLE;
        this.events = new JSONArray();
    }

    public boolean isForbidden() {
        return isForbidden;
    }

    public void setForbidden(boolean forbidden) {
        isForbidden = forbidden;
    }

    public int getCurrentRetries() {
        return currentRetries;
    }

    public void setCurrentRetries(int currentRetries) {
        this.currentRetries = currentRetries;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getClockOffset() {
        return clockOffset;
    }

    public void setClockOffset(long clockOffset) {
        this.clockOffset = clockOffset;
    }

    public JSONArray getEvents() {
        return events;
    }

    public void addEvent(JSONObject event) {
        this.events.put(event);
    }

    public void clearEvents() {
        this.events = new JSONArray();
    }
}
