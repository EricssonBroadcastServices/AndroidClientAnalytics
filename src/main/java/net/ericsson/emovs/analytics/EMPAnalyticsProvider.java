package net.ericsson.emovs.analytics;

import android.content.Context;
import android.content.SharedPreferences;

import net.ericsson.emovs.exposure.auth.DeviceInfo;
import net.ericsson.emovs.exposure.clients.exposure.ExposureClient;
import net.ericsson.emovs.exposure.clients.exposure.ExposureError;
import net.ericsson.emovs.exposure.interfaces.IExposureCallback;
import net.ericsson.emovs.utilities.CheckRoot;
import net.ericsson.emovs.utilities.RunnableThread;

import net.ericsson.emovs.utilities.EMPRegistry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by Joao Coelho on 2017-10-02.
 */

public class EMPAnalyticsProvider {
    final int CYCLE_TIME = 1000;
    final int EVENT_PURGE_TIME_DEFAULT = 3 * CYCLE_TIME;
    final int TIME_WITHOUT_BEAT_DEFAULT = 60 * CYCLE_TIME;
    final int DEVICE_CLOCK_CHECK_THRESHOLD = 5 * 60 * 1000;  // 5 minutes

    String ANALYTICS_SHARED_PREFERENCE_FILE = null;

    final String EVENTSINK_INIT_URL = "/eventsink/init";
    final String EVENTSINK_SEND_URL = "/eventsink/send";

    final String CUSTOMER = "Customer";
    final String BUSINESS_UNIT = "BusinessUnit";
    final String SESSION_ID = "SessionId";
    final String OFFSET_TIME = "OffsetTime";
    final String PAYLOAD = "Payload";
    final String CLOCK_OFFSET = "ClockOffset";
    final String DISPATCH_TIME = "DispatchTime";
    final String ATTRIBUTES = "Attributes";


    static final String PLAYBACK_CREATED = "Playback.Created";
    static String PLAYBACK_READY = "Playback.PlayerReady";
    static String PLAYBACK_STARTED = "Playback.Started";
    static String PLAYBACK_PAUSED = "Playback.Paused";
    static String PLAYBACK_RESUMED = "Playback.Resumed";
    static String PLAYBACK_SCRUBBED_TO = "Playback.ScrubbedTo";
    static String PLAYBACK_START_CASTING = "Playback.StartCasting";
    static String PLAYBACK_STOP_CASTING = "Playback.StopCasting";
    static String PLAYBACK_HANDSHAKE_STARTED = "Playback.HandshakeStarted";
    static String PLAYBACK_BITRATE_CHANGED = "Playback.BitrateChanged";
    static String PLAYBACK_COMPLETED = "Playback.Completed";
    static String PLAYBACK_ERROR = "Playback.Error";
    static String PLAYBACK_ABORTED = "Playback.Aborted";
    static String PLAYBACK_BUFFERING_STARTED = "Playback.BufferingStarted";
    static String PLAYBACK_BUFFERING_ENDED = "Playback.BufferingEnded";
    static String PLAYBACK_HEARTBEAT = "Playback.Heartbeat";
    static String PLAYBACK_DEVICE_INFO = "Playback.DeviceInfo";
    static final String DOWNLOAD_STARTED = "Playback.DownloadStarted";
    static final String DOWNLOAD_STOPPED = "Playback.DownloadStopped";
    static final String DOWNLOAD_PAUSED = "Playback.DownloadPaused";
    static final String DOWNLOAD_RESUMED = "Playback.DownloadResumed";
    static final String DOWNLOAD_CANCELLED = "Playback.DownloadCancelled";
    static final String DOWNLOAD_COMPLETED = "Playback.DownloadCompleted";

    private Context context;
    private HashMap<String, SessionDetails> eventPool;
    private HashMap<String, String> customAttributes;

    RunnableThread cyclicChecker;
    long serviceCurrentTime;
    long serviceLastTime;
    boolean includeDeviceMetrics;

    private static class EMPAnalyticsProviderHolder {
        private final static EMPAnalyticsProvider sInstance = new EMPAnalyticsProvider();
    }

    public EMPAnalyticsProvider() {
        this.eventPool = new HashMap<>();
        this.customAttributes = new HashMap<>();
        init();
    }

    public static EMPAnalyticsProvider getInstance() {
        EMPAnalyticsProvider.EMPAnalyticsProviderHolder.sInstance.setApplicationContext(EMPRegistry.applicationContext());
        return EMPAnalyticsProvider.EMPAnalyticsProviderHolder.sInstance;
    }

    protected void setApplicationContext(Context applicationContext) {
        this.context = applicationContext;
    }

    private void init() {
        clear();
        serviceCurrentTime = 0;
        serviceLastTime = 0;
        this.cyclicChecker = new RunnableThread(new Runnable() {
            @Override
            public void run() {
                for(;;) {
                    try {
                        Thread.sleep(CYCLE_TIME);
                        cycle();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        });
        this.cyclicChecker.start();
        new RunnableThread(new Runnable() {
            @Override
            public void run() {
                trySendOfflineRequests();
            }
        }).start();
    }

    public void clear() {
        if (this.cyclicChecker != null && this.cyclicChecker.isInterrupted() == false && this.cyclicChecker.isAlive()) {
            this.cyclicChecker.interrupt();
        }
        serviceCurrentTime = 0;
    }

    public void dispatchNow() {
        sendData();
    }

    public void exitOngoingSessions() {
        for (Map.Entry<String, SessionDetails> entry : this.eventPool.entrySet()) {
            aborted(entry.getKey(), entry.getValue().getCurrentTime(), null);
        }
    }

    public void setCustomAttribute(String k, String v) {
        this.customAttributes.put(k, v);
    }

    public void clearCustomAttributes() {
        this.customAttributes.clear();
    }

    public void created(String sessionId, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_CREATED, parameters);
        addEventToPool(sessionId, builder, false);
    }

    public void ready(String sessionId, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_READY, parameters);
        addEventToPool(sessionId, builder, false);
    }

    public void started(String sessionId, long currentTime, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_STARTED, parameters)
                .withProp(ATTRIBUTES, this.customAttributes);
        setCurrentTime(sessionId, currentTime);
        addEventToPool(sessionId, builder, true);
        changeSessionState(sessionId, SessionDetails.SESSION_STATE_PLAYING);
    }

    public void paused(String sessionId, long currentTime, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_PAUSED, parameters);
        setCurrentTime(sessionId, currentTime);
        addEventToPool(sessionId, builder, true);
    }

    public void resumed(String sessionId, long currentTime, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_RESUMED, parameters);
        setCurrentTime(sessionId, currentTime);
        addEventToPool(sessionId, builder, true);
    }

    public void seeked(String sessionId, long currentTime, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_SCRUBBED_TO, parameters);
        setCurrentTime(sessionId, currentTime);
        addEventToPool(sessionId, builder, true);
    }

    public void startCasting(String sessionId, long currentTime, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_START_CASTING, parameters);
        setCurrentTime(sessionId, currentTime);
        addEventToPool(sessionId, builder, true);
        changeSessionState(sessionId, SessionDetails.SESSION_STATE_DIRTY);
    }

    public void stopCasting(String sessionId, long currentTime, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_STOP_CASTING, parameters);
        setCurrentTime(sessionId, currentTime);
        addEventToPool(sessionId, builder, true);
    }

    public void handshakeStarted(String sessionId, boolean offline, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_HANDSHAKE_STARTED, parameters);
        addEventToPool(sessionId, builder, false);
        setOffline(sessionId, offline);
    }

    public void handshakeStarted(String sessionId, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_HANDSHAKE_STARTED, parameters);
        addEventToPool(sessionId, builder, false);
        setOffline(sessionId, false);
    }

    public void bitrateChanged(String sessionId, long currentTime, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_BITRATE_CHANGED, parameters);
        setCurrentTime(sessionId, currentTime);
        addEventToPool(sessionId, builder, true);
    }

    public void completed(String sessionId, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_COMPLETED, parameters);
        addEventToPool(sessionId, builder, false);
        changeSessionState(sessionId, SessionDetails.SESSION_STATE_FINISHED);
    }

    public void error(String sessionId, long currentTime, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_ERROR, parameters);
        setCurrentTime(sessionId, currentTime);
        addEventToPool(sessionId, builder, true);
        changeSessionState(sessionId, SessionDetails.SESSION_STATE_DIRTY);
    }

    public void aborted(String sessionId, long currentTime, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_ABORTED, parameters);
        setCurrentTime(sessionId, currentTime);
        addEventToPool(sessionId, builder, true);
        changeSessionState(sessionId, SessionDetails.SESSION_STATE_FINISHED);
    }

    public void waitingStarted(String sessionId, long currentTime, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_BUFFERING_STARTED, parameters);
        setCurrentTime(sessionId, currentTime);
        addEventToPool(sessionId, builder, true);
    }

    public void waitingEnded(String sessionId, long currentTime, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_BUFFERING_ENDED, parameters);
        setCurrentTime(sessionId, currentTime);
        addEventToPool(sessionId, builder, true);
    }

    public void downloadStarted(String sessionId, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(DOWNLOAD_STARTED, parameters);
        addEventToPool(sessionId, builder, false);
        changeSessionState(sessionId, SessionDetails.SESSION_STATE_PLAYING);
    }

    public void downloadPaused(String sessionId, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(DOWNLOAD_PAUSED, parameters);
        addEventToPool(sessionId, builder, false);
    }

    public void downloadResumed(String sessionId, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(DOWNLOAD_RESUMED, parameters);
        addEventToPool(sessionId, builder, false);
    }

    public void downloadStopped(String sessionId, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(DOWNLOAD_STOPPED, parameters);
        addEventToPool(sessionId, builder, false);
        changeSessionState(sessionId, SessionDetails.SESSION_STATE_FINISHED);
    }

    public void downloadCompleted(String sessionId, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(DOWNLOAD_COMPLETED, parameters);
        addEventToPool(sessionId, builder, false);
        changeSessionState(sessionId, SessionDetails.SESSION_STATE_FINISHED);
    }

    public void downloadError(String sessionId, HashMap<String, String> parameters) {
        EventBuilder builder = new EventBuilder(PLAYBACK_ERROR, parameters);
        addEventToPool(sessionId, builder, false);
        changeSessionState(sessionId, SessionDetails.SESSION_STATE_DIRTY);
    }

    public void setCurrentTime(String sessionId, long currentTime) {
        if (eventPool.containsKey(sessionId) == false) {
            return;
        }
        eventPool.get(sessionId).setCurrentTime(currentTime);
    }

    public void refresh() {
        cycle();
    }

    private void sendData() {
        synchronized (eventPool) {
            for (Map.Entry<String, SessionDetails> entry : this.eventPool.entrySet()) {
                final String sessionId = entry.getKey();
                final SessionDetails details = entry.getValue();
                if (details == null) {
                    continue;
                }
                if (details.getCurrentState() == SessionDetails.SESSION_STATE_PLAYING && details.getEvents().length() == 0) {
                    addEventToPool(sessionId, new EventBuilder(PLAYBACK_HEARTBEAT), true);
                }
                if (details.getCurrentState() != SessionDetails.SESSION_STATE_IDLE && details.getCurrentState() != SessionDetails.SESSION_STATE_REMOVED) {
                    if (details.getEvents().length() == 0) {
                        if (details.getCurrentState() != SessionDetails.SESSION_STATE_FINISHED) {
                            removeSession(sessionId, false);
                        }
                        continue;
                    }

                    try {
                        // TODO: retry mechanism

                        if (Math.abs(this.serviceCurrentTime - this.serviceLastTime) > DEVICE_CLOCK_CHECK_THRESHOLD) {
                            sinkInit(sessionId);
                        }

                        JSONObject payload = new JSONObject();

                        payload.put(SESSION_ID, sessionId);
                        payload.put(DISPATCH_TIME, System.currentTimeMillis());
                        payload.put(PAYLOAD, details.getEvents());
                        payload.put(CLOCK_OFFSET, details.getClockOffset());

                        sinkSend(sessionId, payload, new Runnable() {
                            @Override
                            public void run() {
                                clearSessionEvents(sessionId);
                                if (details.getCurrentState() == SessionDetails.SESSION_STATE_FINISHED) {
                                    removeSession(sessionId, false);
                                }
                            }
                        }, null);
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                        continue;
                    }
                }
            }
        }
    }

    private void includeDeviceMetrics(boolean include) {
        this.includeDeviceMetrics = include;
    }

    private void clearSessionEvents(String sessionId) {
        if (this.eventPool.containsKey(sessionId)) {
            this.eventPool.get(sessionId).clearEvents();
            this.eventPool.get(sessionId).setCurrentRetries(0);
        }
    }

    private void cycle() {
        serviceCurrentTime = System.currentTimeMillis();
        if (hasData()) {
            if (serviceLastTime + EVENT_PURGE_TIME_DEFAULT < serviceCurrentTime) {
                sendData();
                serviceLastTime = System.currentTimeMillis();
            }
        }
        else {
            if (serviceLastTime + TIME_WITHOUT_BEAT_DEFAULT < serviceCurrentTime) {
                sendData();
                serviceLastTime = System.currentTimeMillis();
            }
        }
    }

    private boolean hasData() {
        for (SessionDetails details : eventPool.values()) {
            if (details.getEvents().length() > 0) {
                return true;
            }
        }
        return false;
    }

    private void removeSession(String sessionId, boolean removeFromMemory) {
        if (removeFromMemory) {
            synchronized (eventPool) {
                eventPool.remove(sessionId);
            }
        }
        else if (eventPool.containsKey(sessionId)) {
            eventPool.get(sessionId).setCurrentState(SessionDetails.SESSION_STATE_REMOVED);
        }
    }

    private void setOffline(String sessionId, boolean offline) {
        if (eventPool.containsKey(sessionId) == false) {
            return;
        }
        eventPool.get(sessionId).setOffline(offline);
    }

    private boolean isOffline(String sessionId) {
        if (eventPool.containsKey(sessionId) == false) {
            return false;
        }
        return eventPool.get(sessionId).getOffline();
    }

    private void changeSessionState(String sessionId, String state) {
        if (eventPool.containsKey(sessionId) == false) {
            return;
        }
        eventPool.get(sessionId).setCurrentState(state);
    }

    private void addEventToPool(final String sessionId, EventBuilder eventBuilder, boolean includeOffset) {
        boolean addDeviceInfo = false;
        if (this.eventPool.containsKey(sessionId) == false) {
            synchronized (eventPool) {
                this.eventPool.put(sessionId, new SessionDetails());
            }
            this.sinkInit(sessionId);
            addDeviceInfo = true;
        }

        SessionDetails details = this.eventPool.get(sessionId);

        if (includeOffset) {
            eventBuilder.withProp(OFFSET_TIME, details.currentTime);
        }

        details.addEvent(eventBuilder.get());

        if (addDeviceInfo) {
            addEventToPool(sessionId, addDeviceInfo(), false);
        }
    }

    private EventBuilder addDeviceInfo() {
        DeviceInfo deviceInfo = DeviceInfo.getInstance(context);
        return new EventBuilder(PLAYBACK_DEVICE_INFO)
                .withProp("DeviceId", deviceInfo.getDeviceId())
                .withProp("DeviceModel", deviceInfo.getModel())
                .withProp("OS", deviceInfo.getOS())
                .withProp("OSVersion", deviceInfo.getOSVersion())
                .withProp("Manufacturer", deviceInfo.getManufacturer())
                .withProp("IsRooted", CheckRoot.isDeviceRooted());
    }

    private void calculateClockOffset(String sessionId, JSONObject initResponse, long initInitialTime) {
        if (this.eventPool.containsKey(sessionId) == false) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        try {
            initResponse.getLong("repliedTime");
            long clockOfsset = (currentTime - initResponse.getLong("repliedTime") + initInitialTime - initResponse.getLong("receivedTime")) / 2;
            this.eventPool.get(sessionId).setClockOffset(clockOfsset);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void fetchIncludeDeviceMetrics(JSONObject initResponse) {
        boolean includeDeviceMetrics = false;
        if (initResponse.has("settings")) {
            try {
                includeDeviceMetrics = initResponse.getJSONObject("settings").optBoolean("includeDeviceMetrics", false);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        includeDeviceMetrics(includeDeviceMetrics);
    }

    protected void sinkInit(final String sessionId) {
        ExposureClient exposureClient = ExposureClient.getInstance();
        if (exposureClient.getSessionToken() == null) {
            //listener.onError(ExposureError.NO_SESSION_TOKEN);
            // TODO: handle case where no session token
            return;
        }

        final JSONObject initPayload = new JSONObject();
        try {
            initPayload.put(CUSTOMER, exposureClient.getCustomer());
            initPayload.put(BUSINESS_UNIT, exposureClient.getBusinessUnit());
            initPayload.put(SESSION_ID, sessionId);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        // TODO: handle error
        final long initInitialTime = System.currentTimeMillis();
        ExposureClient.getInstance().postSync(EVENTSINK_INIT_URL, initPayload, new IExposureCallback() {
            @Override
            public void onCallCompleted(JSONObject response, ExposureError error) {
                if (error == null) {
                    if(response != null) {
                        calculateClockOffset(sessionId, response, initInitialTime);
                        fetchIncludeDeviceMetrics(response);
                    }
                }
                else if (isOffline(sessionId)) {
                    saveOfflineRequest(initPayload);
                }
            }
        });
    }

    protected void sinkSend(final String sessionId, final JSONObject payload, final Runnable onSuccess, Runnable onError) {
        ExposureClient exposureClient = ExposureClient.getInstance();
        if (exposureClient.getSessionToken() == null) {
            // TODO: handle case where no session token
            return;
        }

        try {
            payload.put(CUSTOMER, exposureClient.getCustomer());
            payload.put(BUSINESS_UNIT, exposureClient.getBusinessUnit());
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        // TODO: handle error
        ExposureClient.getInstance().postSync(EVENTSINK_SEND_URL, payload, new IExposureCallback() {
            @Override
            public void onCallCompleted(JSONObject response, ExposureError error) {
                if(error == null) {
                    if(onSuccess != null) {
                        onSuccess.run();
                    }
                }
                else if (isOffline(sessionId)) {
                    saveOfflineRequest(payload);
                }
                else {
                    // TODO: implement on error
                }
            }
        });
    }


    private void saveOfflineRequest(JSONObject payload) {
        try {
            SharedPreferences sharedPref = getPreferences();
            SharedPreferences.Editor editor = sharedPref.edit();

            String payloadsString = sharedPref.getString("pending", "[]");
            JSONArray payloads = new JSONArray(payloadsString);
            payloads.put(payload);

            editor.putString("pending", payloads.toString());
            editor.commit();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void trySendOfflineRequests() {
        SharedPreferences sharedPref = getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();

        String payloadsString = sharedPref.getString("pending", "[]");
        try {
            JSONArray payloads = new JSONArray(payloadsString);
            LinkedList<Integer> elementsToRemove = new LinkedList<>();
            for (int i = 0; i < payloads.length(); ++i) {
                final int iFinal = i;
                final LinkedList<Integer> elementsToRemoveFinal = elementsToRemove;
                JSONObject payload = payloads.getJSONObject(i);
                if (payload.has(PAYLOAD)) {
                    // send request
                    ExposureClient.getInstance().postSync(EVENTSINK_SEND_URL, payload, new IExposureCallback() {
                        @Override
                        public void onCallCompleted(JSONObject response, ExposureError error) {
                            if (error == null) {
                                elementsToRemoveFinal.addFirst(iFinal);
                            }
                        }
                    });
                }
                else if (payload.has(SESSION_ID)) {
                    // init request
                    ExposureClient.getInstance().postSync(EVENTSINK_INIT_URL, payload, new IExposureCallback() {
                        @Override
                        public void onCallCompleted(JSONObject response, ExposureError error) {
                            if (error == null) {
                                elementsToRemoveFinal.addFirst(iFinal);
                            }
                        }
                    });
                }

                if (elementsToRemove.size() > 0 && elementsToRemove.peek().intValue() != iFinal) {
                    break;
                }
            }

            while(elementsToRemove.size() > 0) {
                Integer i = elementsToRemove.poll();
                payloads.remove(i);
            }

            editor.putString("pending", payloads.toString());
            editor.commit();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private SharedPreferences getPreferences() {
        return EMPRegistry.applicationContext().getSharedPreferences(ANALYTICS_SHARED_PREFERENCE_FILE = "OFFLINE_ANALYTICS_" + EMPRegistry.applicationContext().getPackageName(), Context.MODE_PRIVATE);
    }
}
