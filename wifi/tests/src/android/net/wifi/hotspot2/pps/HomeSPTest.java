/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.net.wifi.hotspot2.pps;

import static org.junit.Assert.assertTrue;

import android.os.Parcel;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.HashMap;

import org.junit.Test;

/**
 * Unit tests for {@link android.net.wifi.hotspot2.pps.HomeSP}.
 */
@SmallTest
public class HomeSPTest {

    private static HashMap<String, Long> createHomeNetworkIds() {
        HashMap<String, Long> homeNetworkIds = new HashMap<String, Long>();
        homeNetworkIds.put("ssid", 0x1234L);
        return homeNetworkIds;
    }

    private static HomeSP createHomeSp(HashMap<String, Long> homeNetworkIds) {
        HomeSP homeSp = new HomeSP();
        homeSp.fqdn = "fqdn";
        homeSp.friendlyName = "friendly name";
        homeSp.iconUrl = "icon.url";
        homeSp.homeNetworkIds = homeNetworkIds;
        homeSp.matchAllOIs = new long[] {0x11L, 0x22L};
        homeSp.matchAnyOIs = new long[] {0x33L, 0x44L};
        homeSp.otherHomePartners = new String[] {"partner1", "partner2"};
        homeSp.roamingConsortiumOIs = new long[] {0x55, 0x66};
        return homeSp;
    }

    private static HomeSP createHomeSpWithNetworkIds() {
        return createHomeSp(createHomeNetworkIds());
    }

    private static HomeSP createHomeSpWithoutNetworkIds() {
        return createHomeSp(null);
    }
    private static void verifyParcel(HomeSP writeHomeSp) throws Exception {
        Parcel parcel = Parcel.obtain();
        writeHomeSp.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);    // Rewind data position back to the beginning for read.
        HomeSP readHomeSp = HomeSP.CREATOR.createFromParcel(parcel);
        assertTrue(readHomeSp.equals(writeHomeSp));
    }

    @Test
    public void verifyParcelWithDefault() throws Exception {
        verifyParcel(new HomeSP());
    }

    @Test
    public void verifyParcelWithHomeNetworkIds() throws Exception {
        verifyParcel(createHomeSpWithNetworkIds());
    }

    @Test
    public void verifyParcelWithoutHomeNetworkIds() throws Exception {
        verifyParcel(createHomeSpWithoutNetworkIds());
    }
}
