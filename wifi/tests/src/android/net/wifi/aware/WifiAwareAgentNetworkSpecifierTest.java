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
 * limitations under the License.
 */

package android.net.wifi.aware;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Parcel;

import androidx.test.filters.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.util.HashSet;
import java.util.Set;

/**
 * Unit test harness for WifiAwareAgentNetworkSpecifier class.
 */
@SmallTest
public class WifiAwareAgentNetworkSpecifierTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testParcel() {
        final int numNs = 10;

        Set<WifiAwareNetworkSpecifier> nsSet = new HashSet<>();
        for (int i = 0; i < numNs; ++i) {
            nsSet.add(getMockNetworkSpecifier(10 + i));
        }
        WifiAwareAgentNetworkSpecifier dut = new WifiAwareAgentNetworkSpecifier(
                nsSet.toArray(new WifiAwareNetworkSpecifier[numNs]));

        Parcel parcelW = Parcel.obtain();
        dut.writeToParcel(parcelW, 0);
        byte[] bytes = parcelW.marshall();
        parcelW.recycle();

        Parcel parcelR = Parcel.obtain();
        parcelR.unmarshall(bytes, 0, bytes.length);
        parcelR.setDataPosition(0);
        WifiAwareAgentNetworkSpecifier rereadDut =
                WifiAwareAgentNetworkSpecifier.CREATOR.createFromParcel(parcelR);

        assertEquals(dut, rereadDut);
        assertEquals(dut.hashCode(), rereadDut.hashCode());

        // Ensure that individual network specifiers are satisfied by both the original & marshaled
        // |WifiAwareNetworkAgentSpecifier instances.
        for (WifiAwareNetworkSpecifier ns : nsSet) {
            assertTrue(dut.satisfiesAwareNetworkSpecifier(ns));
            assertTrue(rereadDut.satisfiesAwareNetworkSpecifier(ns));
        }
    }

    /**
     * Validate that an empty agent network specifier doesn't match any base network specifier.
     */
    @Test
    public void testEmptyDoesntMatchAnything() {
        WifiAwareAgentNetworkSpecifier dut = new WifiAwareAgentNetworkSpecifier();
        WifiAwareNetworkSpecifier ns = getMockNetworkSpecifier(6);
        collector.checkThat("No match expected", ns.canBeSatisfiedBy(dut), equalTo(false));
    }

    /**
     * Validate that an agent network specifier constructed with a single entry matches that entry,
     * and only that entry.
     */
    @Test
    public void testSingleMatch() {
        WifiAwareNetworkSpecifier nsThis = getMockNetworkSpecifier(6);
        WifiAwareAgentNetworkSpecifier dut = new WifiAwareAgentNetworkSpecifier(nsThis);
        WifiAwareNetworkSpecifier nsOther = getMockNetworkSpecifier(8);
        collector.checkThat("Match expected", nsThis.canBeSatisfiedBy(dut), equalTo(true));
        collector.checkThat("No match expected", nsOther.canBeSatisfiedBy(dut), equalTo(false));
    }

    /**
     * Validate that an agent network specifier constructed with multiple entries matches all those
     * entries - but none other.
     */
    @Test
    public void testMultipleMatchesAllMembers() {
        final int numNs = 10;

        Set<WifiAwareNetworkSpecifier> nsSet = new HashSet<>();
        for (int i = 0; i < numNs; ++i) {
            nsSet.add(getMockNetworkSpecifier(10 + i));
        }

        WifiAwareAgentNetworkSpecifier dut = new WifiAwareAgentNetworkSpecifier(
                nsSet.toArray(new WifiAwareNetworkSpecifier[numNs]));
        WifiAwareNetworkSpecifier nsOther = getMockNetworkSpecifier(10000);

        for (WifiAwareNetworkSpecifier nsThis: nsSet) {
            collector.checkThat("Match expected", nsThis.canBeSatisfiedBy(dut), equalTo(true));
        }
        collector.checkThat("No match expected", nsOther.canBeSatisfiedBy(dut), equalTo(false));
    }

    /**
     * Validate that agent network specifier matches against a super-set.
     */
    @Test
    public void testMatchSuperset() {
        final int numNs = 10;

        Set<WifiAwareNetworkSpecifier> nsSet = new HashSet<>();
        for (int i = 0; i < numNs; ++i) {
            nsSet.add(getMockNetworkSpecifier(10 + i));
        }

        WifiAwareAgentNetworkSpecifier oldNs = new WifiAwareAgentNetworkSpecifier(
                nsSet.toArray(new WifiAwareNetworkSpecifier[nsSet.size()]));

        nsSet.add(getMockNetworkSpecifier(100 + numNs));
        WifiAwareAgentNetworkSpecifier newNs = new WifiAwareAgentNetworkSpecifier(
                nsSet.toArray(new WifiAwareNetworkSpecifier[nsSet.size()]));

        collector.checkThat("Match expected", oldNs.canBeSatisfiedBy(newNs), equalTo(true));
    }

    /**
     * Validate that agent network specifier does not match against a sub-set.
     */
    @Test
    public void testNoMatchSubset() {
        final int numNs = 10;

        Set<WifiAwareNetworkSpecifier> nsSet = new HashSet<>();
        for (int i = 0; i < numNs; ++i) {
            nsSet.add(getMockNetworkSpecifier(10 + i));
        }

        WifiAwareAgentNetworkSpecifier newNs = new WifiAwareAgentNetworkSpecifier(
                nsSet.toArray(new WifiAwareNetworkSpecifier[nsSet.size()]));

        nsSet.add(getMockNetworkSpecifier(100 + numNs));
        WifiAwareAgentNetworkSpecifier oldNs = new WifiAwareAgentNetworkSpecifier(
                nsSet.toArray(new WifiAwareNetworkSpecifier[nsSet.size()]));

        collector.checkThat("Match unexpected", oldNs.canBeSatisfiedBy(newNs), equalTo(false));
    }

    // utilities

    /**
     * Returns a WifiAwareNetworkSpecifier with mock (but valid) entries. Each can be
     * differentiated (made unique) by specifying a different client ID.
     */
    WifiAwareNetworkSpecifier getMockNetworkSpecifier(int clientId) {
        return new WifiAwareNetworkSpecifier(WifiAwareNetworkSpecifier.NETWORK_SPECIFIER_TYPE_OOB,
                WifiAwareManager.WIFI_AWARE_DATA_PATH_ROLE_INITIATOR, clientId, 0, 0, new byte[6],
                null, null, 10, 5, 0);
    }
}
