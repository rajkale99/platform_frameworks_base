/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.server.connectivity;

import static android.Manifest.permission.CHANGE_NETWORK_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;
import static android.Manifest.permission.CONNECTIVITY_INTERNAL;
import static android.Manifest.permission.CONNECTIVITY_USE_RESTRICTED_NETWORKS;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.NETWORK_STACK;
import static android.Manifest.permission.UPDATE_DEVICE_STATS;
import static android.content.pm.ApplicationInfo.PRIVATE_FLAG_OEM;
import static android.content.pm.ApplicationInfo.PRIVATE_FLAG_PRODUCT;
import static android.content.pm.ApplicationInfo.PRIVATE_FLAG_VENDOR;
import static android.content.pm.PackageManager.GET_PERMISSIONS;
import static android.content.pm.PackageManager.MATCH_ANY_USER;
import static android.net.NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK;
import static android.os.Process.SYSTEM_UID;

import static com.android.server.connectivity.PermissionMonitor.NETWORK;
import static com.android.server.connectivity.PermissionMonitor.SYSTEM;

import static junit.framework.Assert.fail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageList;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.net.INetd;
import android.net.UidRange;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.SparseIntArray;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.server.LocalServices;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PermissionMonitorTest {
    private static final int MOCK_USER1 = 0;
    private static final int MOCK_USER2 = 1;
    private static final int MOCK_UID1 = 10001;
    private static final int MOCK_UID2 = 10086;
    private static final int SYSTEM_UID1 = 1000;
    private static final int SYSTEM_UID2 = 1008;
    private static final int VPN_UID = 10002;
    private static final String MOCK_PACKAGE1 = "appName1";
    private static final String MOCK_PACKAGE2 = "appName2";
    private static final String SYSTEM_PACKAGE1 = "sysName1";
    private static final String SYSTEM_PACKAGE2 = "sysName2";
    private static final String PARTITION_SYSTEM = "system";
    private static final String PARTITION_OEM = "oem";
    private static final String PARTITION_PRODUCT = "product";
    private static final String PARTITION_VENDOR = "vendor";
    private static final int VERSION_P = Build.VERSION_CODES.P;
    private static final int VERSION_Q = Build.VERSION_CODES.Q;

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;
    @Mock private INetd mNetdService;
    @Mock private PackageManagerInternal mMockPmi;
    @Mock private UserManager mUserManager;
    @Mock private PermissionMonitor.Dependencies mDeps;

    private PermissionMonitor mPermissionMonitor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(eq(Context.USER_SERVICE))).thenReturn(mUserManager);
        when(mUserManager.getUsers(eq(true))).thenReturn(
                Arrays.asList(new UserInfo[] {
                        new UserInfo(MOCK_USER1, "", 0),
                        new UserInfo(MOCK_USER2, "", 0),
                }));
        doReturn(PackageManager.PERMISSION_DENIED).when(mDeps).uidPermission(anyString(), anyInt());

        mPermissionMonitor = spy(new PermissionMonitor(mContext, mNetdService, mDeps));

        LocalServices.removeServiceForTest(PackageManagerInternal.class);
        LocalServices.addService(PackageManagerInternal.class, mMockPmi);
        when(mMockPmi.getPackageList(any())).thenReturn(new PackageList(new ArrayList<String>(),
                  /* observer */ null));
        when(mPackageManager.getInstalledPackages(anyInt())).thenReturn(/* empty app list */ null);
        mPermissionMonitor.startMonitoring();
        verify(mMockPmi).getPackageList(mPermissionMonitor);
    }

    private boolean wouldBeCarryoverPackage(String partition, int targetSdkVersion, int uid) {
        final PackageInfo packageInfo = buildPackageInfo(partition, uid, MOCK_USER1);
        packageInfo.applicationInfo.targetSdkVersion = targetSdkVersion;
        return mPermissionMonitor.isCarryoverPackage(packageInfo.applicationInfo);
    }

    private static PackageInfo packageInfoWithPartition(String partition) {
        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.applicationInfo = new ApplicationInfo();
        int privateFlags = 0;
        switch (partition) {
            case PARTITION_OEM:
                privateFlags = PRIVATE_FLAG_OEM;
                break;
            case PARTITION_PRODUCT:
                privateFlags = PRIVATE_FLAG_PRODUCT;
                break;
            case PARTITION_VENDOR:
                privateFlags = PRIVATE_FLAG_VENDOR;
                break;
        }
        packageInfo.applicationInfo.privateFlags = privateFlags;
        return packageInfo;
    }

    private static PackageInfo buildPackageInfo(String partition, int uid, int userId) {
        final PackageInfo pkgInfo = packageInfoWithPartition(partition);
        pkgInfo.applicationInfo.uid = UserHandle.getUid(userId, UserHandle.getAppId(uid));
        return pkgInfo;
    }

    /** This will REMOVE all previously set permissions from given uid. */
    private void removeAllPermissions(int uid) {
        doReturn(PackageManager.PERMISSION_DENIED).when(mDeps).uidPermission(anyString(), eq(uid));
    }

    /** Set up mocks so that given UID has the requested permissions. */
    private void addPermissions(int uid, String... permissions) {
        for (String permission : permissions) {
            doReturn(PackageManager.PERMISSION_GRANTED)
                    .when(mDeps).uidPermission(eq(permission), eq(uid));
        }
    }

    @Test
    public void testHasPermission() {
        addPermissions(MOCK_UID1);
        assertFalse(mPermissionMonitor.hasPermission(CHANGE_NETWORK_STATE, MOCK_UID1));
        assertFalse(mPermissionMonitor.hasPermission(NETWORK_STACK, MOCK_UID1));
        assertFalse(mPermissionMonitor.hasPermission(
                CONNECTIVITY_USE_RESTRICTED_NETWORKS, MOCK_UID1));
        assertFalse(mPermissionMonitor.hasPermission(CONNECTIVITY_INTERNAL, MOCK_UID1));

        addPermissions(MOCK_UID1, CHANGE_NETWORK_STATE, NETWORK_STACK);
        assertTrue(mPermissionMonitor.hasPermission(CHANGE_NETWORK_STATE, MOCK_UID1));
        assertTrue(mPermissionMonitor.hasPermission(NETWORK_STACK, MOCK_UID1));
        assertFalse(mPermissionMonitor.hasPermission(
                CONNECTIVITY_USE_RESTRICTED_NETWORKS, MOCK_UID1));
        assertFalse(mPermissionMonitor.hasPermission(CONNECTIVITY_INTERNAL, MOCK_UID1));
        assertFalse(mPermissionMonitor.hasPermission(CHANGE_NETWORK_STATE, MOCK_UID2));
        assertFalse(mPermissionMonitor.hasPermission(NETWORK_STACK, MOCK_UID2));

        addPermissions(MOCK_UID2, CONNECTIVITY_USE_RESTRICTED_NETWORKS, CONNECTIVITY_INTERNAL);
        assertFalse(mPermissionMonitor.hasPermission(
                CONNECTIVITY_USE_RESTRICTED_NETWORKS, MOCK_UID1));
        assertFalse(mPermissionMonitor.hasPermission(CONNECTIVITY_INTERNAL, MOCK_UID1));
        assertTrue(mPermissionMonitor.hasPermission(
                CONNECTIVITY_USE_RESTRICTED_NETWORKS, MOCK_UID2));
        assertTrue(mPermissionMonitor.hasPermission(CONNECTIVITY_INTERNAL, MOCK_UID2));
    }

    @Test
    public void testIsVendorApp() {
        PackageInfo app = packageInfoWithPartition(PARTITION_SYSTEM);
        assertFalse(mPermissionMonitor.isVendorApp(app.applicationInfo));
        app = packageInfoWithPartition(PARTITION_OEM);
        assertTrue(mPermissionMonitor.isVendorApp(app.applicationInfo));
        app = packageInfoWithPartition(PARTITION_PRODUCT);
        assertTrue(mPermissionMonitor.isVendorApp(app.applicationInfo));
        app = packageInfoWithPartition(PARTITION_VENDOR);
        assertTrue(mPermissionMonitor.isVendorApp(app.applicationInfo));
    }

    /**
     * Remove all permissions from the uid then setup permissions to uid for checking restricted
     * network permission.
     */
    private void assertRestrictedNetworkPermission(boolean hasPermission, int uid,
            String... permissions) {
        removeAllPermissions(uid);
        addPermissions(uid, permissions);
        assertEquals(hasPermission, mPermissionMonitor.hasRestrictedNetworkPermission(uid));
    }

    @Test
    public void testHasRestrictedNetworkPermission() {
        assertRestrictedNetworkPermission(false, MOCK_UID1);
        assertRestrictedNetworkPermission(false, MOCK_UID1, CHANGE_NETWORK_STATE);
        assertRestrictedNetworkPermission(true, MOCK_UID1, NETWORK_STACK);
        assertRestrictedNetworkPermission(false, MOCK_UID1, CONNECTIVITY_INTERNAL);
        assertRestrictedNetworkPermission(true, MOCK_UID1, CONNECTIVITY_USE_RESTRICTED_NETWORKS);
        assertRestrictedNetworkPermission(false, MOCK_UID1, CHANGE_WIFI_STATE);
        assertRestrictedNetworkPermission(true, MOCK_UID1, PERMISSION_MAINLINE_NETWORK_STACK);

        assertFalse(mPermissionMonitor.hasRestrictedNetworkPermission(MOCK_UID2));
        assertFalse(mPermissionMonitor.hasRestrictedNetworkPermission(SYSTEM_UID));
    }

    @Test
    public void testIsCarryoverPackage() {
        doReturn(VERSION_P).when(mDeps).getDeviceFirstSdkInt();
        assertTrue(wouldBeCarryoverPackage(PARTITION_SYSTEM, VERSION_P, SYSTEM_UID));
        assertTrue(wouldBeCarryoverPackage(PARTITION_VENDOR, VERSION_P, SYSTEM_UID));
        assertFalse(wouldBeCarryoverPackage(PARTITION_SYSTEM, VERSION_P, MOCK_UID1));
        assertTrue(wouldBeCarryoverPackage(PARTITION_VENDOR, VERSION_P, MOCK_UID1));
        assertTrue(wouldBeCarryoverPackage(PARTITION_SYSTEM, VERSION_Q, SYSTEM_UID));
        assertTrue(wouldBeCarryoverPackage(PARTITION_VENDOR, VERSION_Q, SYSTEM_UID));
        assertFalse(wouldBeCarryoverPackage(PARTITION_SYSTEM, VERSION_Q, MOCK_UID1));
        assertFalse(wouldBeCarryoverPackage(PARTITION_VENDOR, VERSION_Q, MOCK_UID1));

        doReturn(VERSION_Q).when(mDeps).getDeviceFirstSdkInt();
        assertFalse(wouldBeCarryoverPackage(PARTITION_SYSTEM, VERSION_P, SYSTEM_UID));
        assertTrue(wouldBeCarryoverPackage(PARTITION_VENDOR, VERSION_P, SYSTEM_UID));
        assertFalse(wouldBeCarryoverPackage(PARTITION_SYSTEM, VERSION_P, MOCK_UID1));
        assertTrue(wouldBeCarryoverPackage(PARTITION_VENDOR, VERSION_P, MOCK_UID1));
        assertFalse(wouldBeCarryoverPackage(PARTITION_SYSTEM, VERSION_Q, SYSTEM_UID));
        assertFalse(wouldBeCarryoverPackage(PARTITION_VENDOR, VERSION_Q, SYSTEM_UID));
        assertFalse(wouldBeCarryoverPackage(PARTITION_SYSTEM, VERSION_Q, MOCK_UID1));
        assertFalse(wouldBeCarryoverPackage(PARTITION_VENDOR, VERSION_Q, MOCK_UID1));

        assertFalse(wouldBeCarryoverPackage(PARTITION_OEM, VERSION_Q, SYSTEM_UID));
        assertFalse(wouldBeCarryoverPackage(PARTITION_PRODUCT, VERSION_Q, SYSTEM_UID));
        assertFalse(wouldBeCarryoverPackage(PARTITION_OEM, VERSION_Q, MOCK_UID1));
        assertFalse(wouldBeCarryoverPackage(PARTITION_PRODUCT, VERSION_Q, MOCK_UID1));
    }

    private void assertBackgroundPermission(boolean hasPermission, String name, int uid,
            String... permissions) throws Exception {
        when(mPackageManager.getPackageInfo(eq(name), anyInt()))
                .thenReturn(buildPackageInfo(PARTITION_SYSTEM, uid, MOCK_USER1));
        addPermissions(uid, permissions);
        mPermissionMonitor.onPackageAdded(name, uid);
        assertEquals(hasPermission, mPermissionMonitor.hasUseBackgroundNetworksPermission(uid));
    }

    @Test
    public void testHasUseBackgroundNetworksPermission() throws Exception {
        assertFalse(mPermissionMonitor.hasUseBackgroundNetworksPermission(MOCK_UID1));
        assertBackgroundPermission(false, "mock1", MOCK_UID1);
        assertBackgroundPermission(false, "mock2", MOCK_UID1, CONNECTIVITY_INTERNAL);
        assertBackgroundPermission(true, "mock3", MOCK_UID1, NETWORK_STACK);

        assertFalse(mPermissionMonitor.hasUseBackgroundNetworksPermission(MOCK_UID2));
        assertBackgroundPermission(false, "mock4", MOCK_UID2);
        assertBackgroundPermission(true, "mock5", MOCK_UID2,
                CONNECTIVITY_USE_RESTRICTED_NETWORKS);

        doReturn(VERSION_Q).when(mDeps).getDeviceFirstSdkInt();
        assertFalse(mPermissionMonitor.hasUseBackgroundNetworksPermission(SYSTEM_UID));
        assertBackgroundPermission(false, "system1", SYSTEM_UID);
        assertBackgroundPermission(true, "system2", SYSTEM_UID, CHANGE_NETWORK_STATE);
        doReturn(VERSION_P).when(mDeps).getDeviceFirstSdkInt();
        removeAllPermissions(SYSTEM_UID);
        assertBackgroundPermission(true, "system3", SYSTEM_UID);
    }

    private class NetdMonitor {
        private final HashMap<Integer, Boolean> mApps = new HashMap<>();

        NetdMonitor(INetd mockNetd) throws Exception {
            // Add hook to verify and track result of setPermission.
            doAnswer((InvocationOnMock invocation) -> {
                final Object[] args = invocation.getArguments();
                final Boolean isSystem = args[0].equals(INetd.PERMISSION_SYSTEM);
                for (final int uid : (int[]) args[1]) {
                    // TODO: Currently, permission monitor will send duplicate commands for each uid
                    // corresponding to each user. Need to fix that and uncomment below test.
                    // if (mApps.containsKey(uid) && mApps.get(uid) == isSystem) {
                    //     fail("uid " + uid + " is already set to " + isSystem);
                    // }
                    mApps.put(uid, isSystem);
                }
                return null;
            }).when(mockNetd).networkSetPermissionForUser(anyInt(), any(int[].class));

            // Add hook to verify and track result of clearPermission.
            doAnswer((InvocationOnMock invocation) -> {
                final Object[] args = invocation.getArguments();
                for (final int uid : (int[]) args[0]) {
                    // TODO: Currently, permission monitor will send duplicate commands for each uid
                    // corresponding to each user. Need to fix that and uncomment below test.
                    // if (!mApps.containsKey(uid)) {
                    //     fail("uid " + uid + " does not exist.");
                    // }
                    mApps.remove(uid);
                }
                return null;
            }).when(mockNetd).networkClearPermissionForUser(any(int[].class));
        }

        public void expectPermission(Boolean permission, int[] users, int[] apps) {
            for (final int user : users) {
                for (final int app : apps) {
                    final int uid = UserHandle.getUid(user, app);
                    if (!mApps.containsKey(uid)) {
                        fail("uid " + uid + " does not exist.");
                    }
                    if (mApps.get(uid) != permission) {
                        fail("uid " + uid + " has wrong permission: " +  permission);
                    }
                }
            }
        }

        public void expectNoPermission(int[] users, int[] apps) {
            for (final int user : users) {
                for (final int app : apps) {
                    final int uid = UserHandle.getUid(user, app);
                    if (mApps.containsKey(uid)) {
                        fail("uid " + uid + " has listed permissions, expected none.");
                    }
                }
            }
        }
    }

    @Test
    public void testUserAndPackageAddRemove() throws Exception {
        final NetdMonitor mNetdMonitor = new NetdMonitor(mNetdService);

        // MOCK_UID1: MOCK_PACKAGE1 only has network permission.
        // SYSTEM_UID: SYSTEM_PACKAGE1 has system permission.
        // SYSTEM_UID: SYSTEM_PACKAGE2 only has network permission.
        doReturn(SYSTEM).when(mPermissionMonitor).highestPermissionForUid(eq(SYSTEM),
                anyString(), anyInt());
        doReturn(SYSTEM).when(mPermissionMonitor).highestPermissionForUid(any(),
                eq(SYSTEM_PACKAGE1), anyInt());
        doReturn(NETWORK).when(mPermissionMonitor).highestPermissionForUid(any(),
                eq(SYSTEM_PACKAGE2), anyInt());
        doReturn(NETWORK).when(mPermissionMonitor).highestPermissionForUid(any(),
                eq(MOCK_PACKAGE1), anyInt());

        // Add SYSTEM_PACKAGE2, expect only have network permission.
        mPermissionMonitor.onUserAdded(MOCK_USER1);
        addPackageForUsers(new int[]{MOCK_USER1}, SYSTEM_PACKAGE2, SYSTEM_UID);
        mNetdMonitor.expectPermission(NETWORK, new int[]{MOCK_USER1}, new int[]{SYSTEM_UID});

        // Add SYSTEM_PACKAGE1, expect permission escalate.
        addPackageForUsers(new int[]{MOCK_USER1}, SYSTEM_PACKAGE1, SYSTEM_UID);
        mNetdMonitor.expectPermission(SYSTEM, new int[]{MOCK_USER1}, new int[]{SYSTEM_UID});

        mPermissionMonitor.onUserAdded(MOCK_USER2);
        mNetdMonitor.expectPermission(SYSTEM, new int[]{MOCK_USER1, MOCK_USER2},
                new int[]{SYSTEM_UID});

        addPackageForUsers(new int[]{MOCK_USER1, MOCK_USER2}, MOCK_PACKAGE1, MOCK_UID1);
        mNetdMonitor.expectPermission(SYSTEM, new int[]{MOCK_USER1, MOCK_USER2},
                new int[]{SYSTEM_UID});
        mNetdMonitor.expectPermission(NETWORK, new int[]{MOCK_USER1, MOCK_USER2},
                new int[]{MOCK_UID1});

        // Remove MOCK_UID1, expect no permission left for all user.
        mPermissionMonitor.onPackageRemoved(MOCK_PACKAGE1, MOCK_UID1);
        removePackageForUsers(new int[]{MOCK_USER1, MOCK_USER2}, MOCK_PACKAGE1, MOCK_UID1);
        mNetdMonitor.expectNoPermission(new int[]{MOCK_USER1, MOCK_USER2}, new int[]{MOCK_UID1});

        // Remove SYSTEM_PACKAGE1, expect permission downgrade.
        when(mPackageManager.getPackagesForUid(anyInt())).thenReturn(new String[]{SYSTEM_PACKAGE2});
        removePackageForUsers(new int[]{MOCK_USER1, MOCK_USER2}, SYSTEM_PACKAGE1, SYSTEM_UID);
        mNetdMonitor.expectPermission(NETWORK, new int[]{MOCK_USER1, MOCK_USER2},
                new int[]{SYSTEM_UID});

        mPermissionMonitor.onUserRemoved(MOCK_USER1);
        mNetdMonitor.expectPermission(NETWORK, new int[]{MOCK_USER2}, new int[]{SYSTEM_UID});

        // Remove all packages, expect no permission left.
        when(mPackageManager.getPackagesForUid(anyInt())).thenReturn(new String[]{});
        removePackageForUsers(new int[]{MOCK_USER2}, SYSTEM_PACKAGE2, SYSTEM_UID);
        mNetdMonitor.expectNoPermission(new int[]{MOCK_USER1, MOCK_USER2},
                new int[]{SYSTEM_UID, MOCK_UID1});

        // Remove last user, expect no redundant clearPermission is invoked.
        mPermissionMonitor.onUserRemoved(MOCK_USER2);
        mNetdMonitor.expectNoPermission(new int[]{MOCK_USER1, MOCK_USER2},
                new int[]{SYSTEM_UID, MOCK_UID1});
    }

    @Test
    public void testUidFilteringDuringVpnConnectDisconnectAndUidUpdates() throws Exception {
        when(mPackageManager.getInstalledPackages(eq(GET_PERMISSIONS | MATCH_ANY_USER))).thenReturn(
                Arrays.asList(new PackageInfo[] {
                        buildPackageInfo(PARTITION_SYSTEM, SYSTEM_UID1, MOCK_USER1),
                        buildPackageInfo(PARTITION_SYSTEM, MOCK_UID1, MOCK_USER1),
                        buildPackageInfo(PARTITION_SYSTEM, MOCK_UID2, MOCK_USER1),
                        buildPackageInfo(PARTITION_SYSTEM, VPN_UID, MOCK_USER1)
                }));
        when(mPackageManager.getPackageInfo(eq(MOCK_PACKAGE1), eq(GET_PERMISSIONS))).thenReturn(
                buildPackageInfo(PARTITION_SYSTEM, MOCK_UID1, MOCK_USER1));
        addPermissions(SYSTEM_UID,
                CHANGE_NETWORK_STATE, NETWORK_STACK, CONNECTIVITY_USE_RESTRICTED_NETWORKS);
        mPermissionMonitor.startMonitoring();
        // Every app on user 0 except MOCK_UID2 are under VPN.
        final Set<UidRange> vpnRange1 = new HashSet<>(Arrays.asList(new UidRange[] {
                new UidRange(0, MOCK_UID2 - 1),
                new UidRange(MOCK_UID2 + 1, UserHandle.PER_USER_RANGE - 1)}));
        final Set<UidRange> vpnRange2 = Collections.singleton(new UidRange(MOCK_UID2, MOCK_UID2));

        // When VPN is connected, expect a rule to be set up for user app MOCK_UID1
        mPermissionMonitor.onVpnUidRangesAdded("tun0", vpnRange1, VPN_UID);
        verify(mNetdService).firewallAddUidInterfaceRules(eq("tun0"),
                aryEq(new int[] {MOCK_UID1}));

        reset(mNetdService);

        // When MOCK_UID1 package is uninstalled and reinstalled, expect Netd to be updated
        mPermissionMonitor.onPackageRemoved(
                MOCK_PACKAGE1, UserHandle.getUid(MOCK_USER1, MOCK_UID1));
        verify(mNetdService).firewallRemoveUidInterfaceRules(aryEq(new int[] {MOCK_UID1}));
        mPermissionMonitor.onPackageAdded(MOCK_PACKAGE1, UserHandle.getUid(MOCK_USER1, MOCK_UID1));
        verify(mNetdService).firewallAddUidInterfaceRules(eq("tun0"),
                aryEq(new int[] {MOCK_UID1}));

        reset(mNetdService);

        // During VPN uid update (vpnRange1 -> vpnRange2), ConnectivityService first deletes the
        // old UID rules then adds the new ones. Expect netd to be updated
        mPermissionMonitor.onVpnUidRangesRemoved("tun0", vpnRange1, VPN_UID);
        verify(mNetdService).firewallRemoveUidInterfaceRules(aryEq(new int[] {MOCK_UID1}));
        mPermissionMonitor.onVpnUidRangesAdded("tun0", vpnRange2, VPN_UID);
        verify(mNetdService).firewallAddUidInterfaceRules(eq("tun0"),
                aryEq(new int[] {MOCK_UID2}));

        reset(mNetdService);

        // When VPN is disconnected, expect rules to be torn down
        mPermissionMonitor.onVpnUidRangesRemoved("tun0", vpnRange2, VPN_UID);
        verify(mNetdService).firewallRemoveUidInterfaceRules(aryEq(new int[] {MOCK_UID2}));
        assertNull(mPermissionMonitor.getVpnUidRanges("tun0"));
    }

    @Test
    public void testUidFilteringDuringPackageInstallAndUninstall() throws Exception {
        when(mPackageManager.getInstalledPackages(eq(GET_PERMISSIONS | MATCH_ANY_USER))).thenReturn(
                Arrays.asList(new PackageInfo[] {
                        buildPackageInfo(PARTITION_SYSTEM, SYSTEM_UID1, MOCK_USER1),
                        buildPackageInfo(PARTITION_SYSTEM, VPN_UID, MOCK_USER1)
                }));
        when(mPackageManager.getPackageInfo(eq(MOCK_PACKAGE1), eq(GET_PERMISSIONS))).thenReturn(
                        buildPackageInfo(PARTITION_SYSTEM, MOCK_UID1, MOCK_USER1));

        mPermissionMonitor.startMonitoring();
        final Set<UidRange> vpnRange = Collections.singleton(UidRange.createForUser(MOCK_USER1));
        mPermissionMonitor.onVpnUidRangesAdded("tun0", vpnRange, VPN_UID);

        // Newly-installed package should have uid rules added
        mPermissionMonitor.onPackageAdded(MOCK_PACKAGE1, UserHandle.getUid(MOCK_USER1, MOCK_UID1));
        verify(mNetdService).firewallAddUidInterfaceRules(eq("tun0"),
                aryEq(new int[] {MOCK_UID1}));

        // Removed package should have its uid rules removed
        mPermissionMonitor.onPackageRemoved(
                MOCK_PACKAGE1, UserHandle.getUid(MOCK_USER1, MOCK_UID1));
        verify(mNetdService).firewallRemoveUidInterfaceRules(aryEq(new int[] {MOCK_UID1}));
    }


    // Normal package add/remove operations will trigger multiple intent for uids corresponding to
    // each user. To simulate generic package operations, the onPackageAdded/Removed will need to be
    // called multiple times with the uid corresponding to each user.
    private void addPackageForUsers(int[] users, String packageName, int uid) {
        for (final int user : users) {
            mPermissionMonitor.onPackageAdded(packageName, UserHandle.getUid(user, uid));
        }
    }

    private void removePackageForUsers(int[] users, String packageName, int uid) {
        for (final int user : users) {
            mPermissionMonitor.onPackageRemoved(packageName, UserHandle.getUid(user, uid));
        }
    }

    private class NetdServiceMonitor {
        private final HashMap<Integer, Integer> mPermissions = new HashMap<>();

        NetdServiceMonitor(INetd mockNetdService) throws Exception {
            // Add hook to verify and track result of setPermission.
            doAnswer((InvocationOnMock invocation) -> {
                final Object[] args = invocation.getArguments();
                final int permission = (int) args[0];
                for (final int uid : (int[]) args[1]) {
                    mPermissions.put(uid, permission);
                }
                return null;
            }).when(mockNetdService).trafficSetNetPermForUids(anyInt(), any(int[].class));
        }

        public void expectPermission(int permission, int[] apps) {
            for (final int app : apps) {
                if (!mPermissions.containsKey(app)) {
                    fail("uid " + app + " does not exist.");
                }
                if (mPermissions.get(app) != permission) {
                    fail("uid " + app + " has wrong permission: " + mPermissions.get(app));
                }
            }
        }
    }

    @Test
    public void testPackagePermissionUpdate() throws Exception {
        final NetdServiceMonitor mNetdServiceMonitor = new NetdServiceMonitor(mNetdService);
        // MOCK_UID1: MOCK_PACKAGE1 only has internet permission.
        // MOCK_UID2: MOCK_PACKAGE2 does not have any permission.
        // SYSTEM_UID1: SYSTEM_PACKAGE1 has internet permission and update device stats permission.
        // SYSTEM_UID2: SYSTEM_PACKAGE2 has only update device stats permission.

        SparseIntArray netdPermissionsAppIds = new SparseIntArray();
        netdPermissionsAppIds.put(MOCK_UID1, INetd.PERMISSION_INTERNET);
        netdPermissionsAppIds.put(MOCK_UID2, INetd.PERMISSION_NONE);
        netdPermissionsAppIds.put(SYSTEM_UID1, INetd.PERMISSION_INTERNET
                | INetd.PERMISSION_UPDATE_DEVICE_STATS);
        netdPermissionsAppIds.put(SYSTEM_UID2, INetd.PERMISSION_UPDATE_DEVICE_STATS);

        // Send the permission information to netd, expect permission updated.
        mPermissionMonitor.sendPackagePermissionsToNetd(netdPermissionsAppIds);

        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET,
                new int[]{MOCK_UID1});
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_NONE, new int[]{MOCK_UID2});
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET
                | INetd.PERMISSION_UPDATE_DEVICE_STATS, new int[]{SYSTEM_UID1});
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_UPDATE_DEVICE_STATS,
                new int[]{SYSTEM_UID2});

        // Update permission of MOCK_UID1, expect new permission show up.
        mPermissionMonitor.sendPackagePermissionsForUid(MOCK_UID1,
                INetd.PERMISSION_INTERNET | INetd.PERMISSION_UPDATE_DEVICE_STATS);
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET
                | INetd.PERMISSION_UPDATE_DEVICE_STATS, new int[]{MOCK_UID1});

        // Change permissions of SYSTEM_UID2, expect new permission show up and old permission
        // revoked.
        mPermissionMonitor.sendPackagePermissionsForUid(SYSTEM_UID2,
                INetd.PERMISSION_INTERNET);
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET, new int[]{SYSTEM_UID2});

        // Revoke permission from SYSTEM_UID1, expect no permission stored.
        mPermissionMonitor.sendPackagePermissionsForUid(SYSTEM_UID1, INetd.PERMISSION_NONE);
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_NONE, new int[]{SYSTEM_UID1});
    }

    private PackageInfo setPackagePermissions(String packageName, int uid, String[] permissions)
            throws Exception {
        final PackageInfo packageInfo = buildPackageInfo(PARTITION_SYSTEM, uid, MOCK_USER1);
        when(mPackageManager.getPackageInfo(eq(packageName), anyInt())).thenReturn(packageInfo);
        when(mPackageManager.getPackagesForUid(eq(uid))).thenReturn(new String[]{packageName});
        addPermissions(uid, permissions);
        return packageInfo;
    }

    private PackageInfo addPackage(String packageName, int uid, String[] permissions)
            throws Exception {
        PackageInfo packageInfo = setPackagePermissions(packageName, uid, permissions);
        mPermissionMonitor.onPackageAdded(packageName, uid);
        return packageInfo;
    }

    @Test
    public void testPackageInstall() throws Exception {
        final NetdServiceMonitor mNetdServiceMonitor = new NetdServiceMonitor(mNetdService);

        addPackage(MOCK_PACKAGE1, MOCK_UID1, new String[] {INTERNET, UPDATE_DEVICE_STATS});
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET
                | INetd.PERMISSION_UPDATE_DEVICE_STATS, new int[]{MOCK_UID1});

        addPackage(MOCK_PACKAGE2, MOCK_UID2, new String[] {INTERNET});
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET, new int[]{MOCK_UID2});
    }

    @Test
    public void testPackageInstallSharedUid() throws Exception {
        final NetdServiceMonitor mNetdServiceMonitor = new NetdServiceMonitor(mNetdService);

        addPackage(MOCK_PACKAGE1, MOCK_UID1, new String[] {INTERNET, UPDATE_DEVICE_STATS});
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET
                | INetd.PERMISSION_UPDATE_DEVICE_STATS, new int[]{MOCK_UID1});

        // Install another package with the same uid and no permissions should not cause the UID to
        // lose permissions.
        final PackageInfo packageInfo2 = buildPackageInfo(PARTITION_SYSTEM, MOCK_UID1, MOCK_USER1);
        when(mPackageManager.getPackageInfo(eq(MOCK_PACKAGE2), anyInt())).thenReturn(packageInfo2);
        when(mPackageManager.getPackagesForUid(MOCK_UID1))
              .thenReturn(new String[]{MOCK_PACKAGE1, MOCK_PACKAGE2});
        mPermissionMonitor.onPackageAdded(MOCK_PACKAGE2, MOCK_UID1);
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET
                | INetd.PERMISSION_UPDATE_DEVICE_STATS, new int[]{MOCK_UID1});
    }

    @Test
    public void testPackageUninstallBasic() throws Exception {
        final NetdServiceMonitor mNetdServiceMonitor = new NetdServiceMonitor(mNetdService);

        addPackage(MOCK_PACKAGE1, MOCK_UID1, new String[] {INTERNET, UPDATE_DEVICE_STATS});
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET
                | INetd.PERMISSION_UPDATE_DEVICE_STATS, new int[]{MOCK_UID1});

        when(mPackageManager.getPackagesForUid(MOCK_UID1)).thenReturn(new String[]{});
        mPermissionMonitor.onPackageRemoved(MOCK_PACKAGE1, MOCK_UID1);
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_UNINSTALLED, new int[]{MOCK_UID1});
    }

    @Test
    public void testPackageRemoveThenAdd() throws Exception {
        final NetdServiceMonitor mNetdServiceMonitor = new NetdServiceMonitor(mNetdService);

        addPackage(MOCK_PACKAGE1, MOCK_UID1, new String[] {INTERNET, UPDATE_DEVICE_STATS});
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET
                | INetd.PERMISSION_UPDATE_DEVICE_STATS, new int[]{MOCK_UID1});

        when(mPackageManager.getPackagesForUid(MOCK_UID1)).thenReturn(new String[]{});
        removeAllPermissions(MOCK_UID1);
        mPermissionMonitor.onPackageRemoved(MOCK_PACKAGE1, MOCK_UID1);
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_UNINSTALLED, new int[]{MOCK_UID1});

        addPackage(MOCK_PACKAGE1, MOCK_UID1, new String[] {INTERNET});
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET, new int[]{MOCK_UID1});
    }

    @Test
    public void testPackageUpdate() throws Exception {
        final NetdServiceMonitor mNetdServiceMonitor = new NetdServiceMonitor(mNetdService);

        addPackage(MOCK_PACKAGE1, MOCK_UID1, new String[] {});
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_NONE, new int[]{MOCK_UID1});

        addPackage(MOCK_PACKAGE1, MOCK_UID1, new String[] {INTERNET});
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET, new int[]{MOCK_UID1});
    }

    @Test
    public void testPackageUninstallWithMultiplePackages() throws Exception {
        final NetdServiceMonitor mNetdServiceMonitor = new NetdServiceMonitor(mNetdService);

        addPackage(MOCK_PACKAGE1, MOCK_UID1, new String[] {INTERNET, UPDATE_DEVICE_STATS});
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET
                | INetd.PERMISSION_UPDATE_DEVICE_STATS, new int[]{MOCK_UID1});

        // Mock another package with the same uid but different permissions.
        final PackageInfo packageInfo2 = buildPackageInfo(PARTITION_SYSTEM, MOCK_UID1, MOCK_USER1);
        when(mPackageManager.getPackageInfo(eq(MOCK_PACKAGE2), anyInt())).thenReturn(packageInfo2);
        when(mPackageManager.getPackagesForUid(MOCK_UID1)).thenReturn(new String[]{
                MOCK_PACKAGE2});
        removeAllPermissions(MOCK_UID1);
        addPermissions(MOCK_UID1, INTERNET);

        mPermissionMonitor.onPackageRemoved(MOCK_PACKAGE1, MOCK_UID1);
        mNetdServiceMonitor.expectPermission(INetd.PERMISSION_INTERNET, new int[]{MOCK_UID1});
    }

    @Test
    public void testRealSystemPermission() throws Exception {
        // Use the real context as this test must ensure the *real* system package holds the
        // necessary permission.
        final Context realContext = InstrumentationRegistry.getContext();
        final PermissionMonitor monitor = new PermissionMonitor(realContext, mNetdService);
        assertTrue(monitor.hasPermission(CONNECTIVITY_USE_RESTRICTED_NETWORKS, SYSTEM_UID));
    }
}
