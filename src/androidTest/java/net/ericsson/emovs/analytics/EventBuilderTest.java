package net.ericsson.emovs.analytics;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


import static org.junit.Assert.*;

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

//@RunWith(AndroidJUnit4.class)
public class EventBuilderTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void buildTest() throws Exception {
        EventBuilder builder = new EventBuilder("Playback.Created");
        builder.withProp("myProp", "12345");
        JSONObject event = builder.get();
        Assert.assertTrue("Playback.Created".equals(event.getString("EventType")));
        Assert.assertTrue(event.has("Timestamp"));
        Assert.assertTrue("12345".equals(event.getString("myProp")));
    }


}