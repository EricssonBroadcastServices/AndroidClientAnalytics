package net.ericsson.emovs.analytics;

import android.app.Activity;
import android.content.Context;

import net.ericsson.emovs.utilities.emp.EMPRegistry;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;


import java.util.HashMap;

/*
 * Copyright (c) 2017 Ericsson. All Rights Reserved
 *
 * This SOURCE CODE FILE, which has been provided by Ericsson as part
 * of an Ericsson software product for use ONLY by licensed users of the
 * product, includes CONFIDENTIAL and PROPRIETARY information of Ericsson.
 *
 * USE OF THIS SOFTWARE IS GOVERNED BY THE TERMS AND CONDITIONS OF
 * THE LICENSE STATEMENT AND LIMITED WARRANTY FURNISHED WITH
 * THE PRODUCT.
 */

@RunWith(RobolectricTestRunner.class)
public class EMPAnalyticsProviderTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void analyticsInitTest() throws Exception {
        Activity activity = Robolectric.setupActivity(Activity.class);
        Context appContext = activity.getApplicationContext();

        EMPRegistry.bindApplicationContext(appContext);

        Assert.assertTrue(EMPAnalyticsProviderTester.getInstance() != null);
    }

    @Test
    public void analyticsSinkSendTest() throws Exception {
        Activity activity = Robolectric.setupActivity(Activity.class);
        Context appContext = activity.getApplicationContext();

        EMPRegistry.bindApplicationContext(appContext);

        EMPAnalyticsProviderTester provider = new EMPAnalyticsProviderTester();
        provider.setApplicationContext(EMPRegistry.applicationContext());

        HashMap<String, String> parameters = new HashMap<>();

        parameters.put(EventParameters.Created.PLAY_MODE, "VOD");
        parameters.put(EventParameters.Created.VERSION, "2.0.0");
        parameters.put(EventParameters.Created.PLAYER, "EMP.Android");

        provider.created("s12345", parameters);
        provider.downloadStarted("s12345", null);
        provider.error("s12345", 0, parameters);
        provider.started("s12345", 0, parameters);
        provider.startCasting("s12345", 0, parameters);
        provider.aborted("s12345", 0, parameters);
        provider.paused("s12345", 0, parameters);
        provider.resumed("s12345", 0, parameters);
        provider.seeked("s12345", 0, parameters);
        provider.stopCasting("s12345", 0, parameters);
        provider.handshakeStarted("s12345", parameters);
        provider.bitrateChanged("s12345", 0, parameters);
        provider.completed("s12345", parameters);
        provider.waitingStarted("s12345", 0, parameters);
        provider.waitingEnded("s12345", 0, parameters);
        provider.downloadCompleted("s12345", parameters);
        provider.downloadStopped("s12345", parameters);
        provider.downloadError("s12345", parameters);
        provider.downloadPaused("s12345", parameters);
        provider.downloadResumed("s12345", parameters);
        provider.ready("s12345", parameters);
        provider.handshakeStarted("s12345", false, parameters);

        Thread.sleep(1000);

        Assert.assertTrue(provider.hasSinkInit);
        Assert.assertTrue("s12345".equals(provider.sessionId));
        Assert.assertTrue("Playback.Created".equals(provider.payload.getJSONArray("Payload").getJSONObject(0).getString("EventType")));
        Assert.assertTrue("Playback.DeviceInfo".equals(provider.payload.getJSONArray("Payload").getJSONObject(1).getString("EventType")));
        Assert.assertTrue("Playback.DownloadStarted".equals(provider.payload.getJSONArray("Payload").getJSONObject(2).getString("EventType")));
        Assert.assertTrue("Playback.Error".equals(provider.payload.getJSONArray("Payload").getJSONObject(3).getString("EventType")));
        Assert.assertTrue("Playback.Started".equals(provider.payload.getJSONArray("Payload").getJSONObject(4).getString("EventType")));
        Assert.assertTrue("Playback.StartCasting".equals(provider.payload.getJSONArray("Payload").getJSONObject(5).getString("EventType")));
        Assert.assertTrue("Playback.Aborted".equals(provider.payload.getJSONArray("Payload").getJSONObject(6).getString("EventType")));
        Assert.assertTrue("Playback.Paused".equals(provider.payload.getJSONArray("Payload").getJSONObject(7).getString("EventType")));
        Assert.assertTrue("Playback.Resumed".equals(provider.payload.getJSONArray("Payload").getJSONObject(8).getString("EventType")));
        Assert.assertTrue("Playback.ScrubbedTo".equals(provider.payload.getJSONArray("Payload").getJSONObject(9).getString("EventType")));
        Assert.assertTrue("Playback.StopCasting".equals(provider.payload.getJSONArray("Payload").getJSONObject(10).getString("EventType")));
        Assert.assertTrue("Playback.HandshakeStarted".equals(provider.payload.getJSONArray("Payload").getJSONObject(11).getString("EventType")));
        Assert.assertTrue("Playback.BitrateChanged".equals(provider.payload.getJSONArray("Payload").getJSONObject(12).getString("EventType")));
        Assert.assertTrue("Playback.Completed".equals(provider.payload.getJSONArray("Payload").getJSONObject(13).getString("EventType")));
        Assert.assertTrue("Playback.BufferingStarted".equals(provider.payload.getJSONArray("Payload").getJSONObject(14).getString("EventType")));
        Assert.assertTrue("Playback.BufferingEnded".equals(provider.payload.getJSONArray("Payload").getJSONObject(15).getString("EventType")));
        Assert.assertTrue("Playback.DownloadCompleted".equals(provider.payload.getJSONArray("Payload").getJSONObject(16).getString("EventType")));
        Assert.assertTrue("Playback.DownloadStopped".equals(provider.payload.getJSONArray("Payload").getJSONObject(17).getString("EventType")));
        Assert.assertTrue("Playback.Error".equals(provider.payload.getJSONArray("Payload").getJSONObject(18).getString("EventType")));
        Assert.assertTrue("Playback.DownloadPaused".equals(provider.payload.getJSONArray("Payload").getJSONObject(19).getString("EventType")));
        Assert.assertTrue("Playback.DownloadResumed".equals(provider.payload.getJSONArray("Payload").getJSONObject(20).getString("EventType")));
        Assert.assertTrue("Playback.PlayerReady".equals(provider.payload.getJSONArray("Payload").getJSONObject(21).getString("EventType")));
        Assert.assertTrue("Playback.HandshakeStarted".equals(provider.payload.getJSONArray("Payload").getJSONObject(22).getString("EventType")));
    }

    private class EMPAnalyticsProviderTester extends EMPAnalyticsProvider {
        public boolean hasSinkInit = false;
        public String sessionId = "";
        public JSONObject payload;

        protected void sinkInit(final String sessionId) {
            hasSinkInit = true;
            this.sessionId = sessionId;
        }

        protected void sinkSend(final String sessionId, final JSONObject payload, final Runnable onSuccess, Runnable onError) {
            this.payload = payload;
        }
    }

}