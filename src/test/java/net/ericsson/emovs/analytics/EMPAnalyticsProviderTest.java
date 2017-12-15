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

        Thread.sleep(1000);

        Assert.assertTrue(provider.hasSinkInit);
        Assert.assertTrue("s12345".equals(provider.sessionId));
        Assert.assertTrue("Playback.Created".equals(provider.payload.getJSONArray("Payload").getJSONObject(0).getString("EventType")));
        Assert.assertTrue("Playback.DeviceInfo".equals(provider.payload.getJSONArray("Payload").getJSONObject(1).getString("EventType")));
        Assert.assertTrue("Playback.DownloadStarted".equals(provider.payload.getJSONArray("Payload").getJSONObject(2).getString("EventType")));
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