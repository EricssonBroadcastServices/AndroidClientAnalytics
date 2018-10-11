package net.ericsson.emovs.analytics;

import net.ericsson.emovs.exposure.utils.MonotonicTimeService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Joao Coelho on 2017-10-02.
 */

public class EventBuilder {
    private JSONObject event;

    public EventBuilder(String eventType) {
        init(eventType, null);
    }

    public EventBuilder(String eventType, Map<String, String> parameters) {
        init(eventType, parameters);
    }

    public void init(String eventType, Map<String, String> parameters) {
        this.event = new JSONObject();
        withProp("EventType", eventType);
        withProp("Timestamp", MonotonicTimeService.getInstance().currentTime());
        if (parameters != null) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                withProp(entry.getKey(), entry.getValue());
            }
        }
    }

    public <T> EventBuilder withProp(String key, T value) {
        try {
            this.event.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public EventBuilder withProp(String key, HashMap<String, String> map) {
        if(map == null) {
            return this;
        }
        JSONObject customAttrs = new JSONObject();
        for (Map.Entry<String, String> customAttr : map.entrySet()) {
            try {
                customAttrs.put(customAttr.getKey(), customAttr.getValue());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (customAttrs.length() > 0) {
            try {
                this.event.put(key, customAttrs);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public JSONObject get() {
        return this.event;
    }
}
