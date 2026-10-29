// SPDX-FileCopyrightText: 2026 VanPaulTek Inc
// SPDX-License-Identifier: AGPL-3.0-or-later
package com.onlytrusted.app.diagnostics

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the pure plain-text formatter — not the Android-dependent
 * collector. A tester-visible diagnostic has to render consistently
 * even when some sections are empty.
 */
class DiagnosticReportTest {

    private val minimalReport = DiagnosticReport.Report(
        generatedAtMs = 1_700_000_000_000L,
        app = DiagnosticReport.AppInfo(versionName = "0.1.10", versionCode = 11),
        device = DiagnosticReport.DeviceInfo(
            manufacturer = "Samsung",
            model = "SM-A245F",
            product = "a24-user",
            androidRelease = "14",
            apiLevel = 34,
            buildFingerprint = "samsung/a24/a24:14/UP1A.231005.007/A245FXXS2BXK1:user/release-keys",
            locale = "en_US",
        ),
        battery = DiagnosticReport.BatteryInfo(
            ignoringOptimizations = false,
            powerSaveMode = false,
            deviceIdleMode = false,
        ),
        lock = DiagnosticReport.LockInfo(appLockEnabled = false, micWatcherEnabled = false),
        fgs = DiagnosticReport.FgsInfo(alive = false, restartCount = 0, firstStartAtMs = null, currentUptimeMs = null),
        permissions = listOf(
            DiagnosticReport.PermissionRow("READ_CONTACTS", true),
            DiagnosticReport.PermissionRow("CAMERA", false),
        ),
        channels = emptyList(),
        oemWarnings = listOf("Samsung One UI: open Device Care …"),
    )

    @Test fun `report header always present`() {
        val text = DiagnosticReport.format(minimalReport)
        assertTrue(text.startsWith("=== OnlyTrusted — Device Compatibility Report ==="))
    }

    @Test fun `every section header appears exactly once`() {
        val text = DiagnosticReport.format(minimalReport)
        listOf("## App", "## Device", "## Battery / power", "## Locks",
               "## Foreground Service", "## Permissions", "## OEM compatibility notes")
            .forEach { header ->
                val count = text.windowed(header.length).count { it == header }
                assertTrue("Expected $header exactly once, found $count", count == 1)
            }
    }

    @Test fun `notification channels section omitted when empty`() {
        val text = DiagnosticReport.format(minimalReport)
        assertTrue(!text.contains("## Notification channels"))
    }

    @Test fun `permissions render as check or cross`() {
        val text = DiagnosticReport.format(minimalReport)
        assertTrue(text.contains("✓ READ_CONTACTS"))
        assertTrue(text.contains("✗ CAMERA"))
    }

    @Test fun `OEM warnings section renders bullet lines`() {
        val text = DiagnosticReport.format(minimalReport)
        assertTrue(text.contains("- Samsung One UI"))
    }

    @Test fun `FGS section omits uptime lines when null`() {
        val text = DiagnosticReport.format(minimalReport)
        assertTrue(!text.contains("first start:"))
        assertTrue(!text.contains("current uptime:"))
    }
}
