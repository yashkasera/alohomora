package io.github.yashkasera.alohomora.data.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The fixtures mirror the plist Xcode embeds, including its tab-and-newline layout between a key and
 * its value, wrapped in stand-in binary bytes for the CMS envelope the parser has to see past.
 */
class DevelopmentSigningTest {

    @Test
    fun developmentProfileAllowsDebugging() {
        assertTrue(ProvisioningProfile.allowsDebugging(profile(getTaskAllow = "<true/>")))
    }

    @Test
    fun distributionProfileDoesNot() {
        // TestFlight and ad-hoc builds carry a profile too; only the entitlement value separates them.
        assertFalse(ProvisioningProfile.allowsDebugging(profile(getTaskAllow = "<false/>")))
    }

    @Test
    fun profileWithoutTheEntitlementDoesNot() {
        assertFalse(ProvisioningProfile.allowsDebugging(profile(getTaskAllow = null)))
    }

    @Test
    fun aTrueElsewhereInTheProfileIsNotMistakenForTheEntitlement() {
        // Several booleans sit next to each other in Entitlements; only get-task-allow's own value counts.
        val text = profile(getTaskAllow = "<false/>")
            .replace("<key>beta-reports-active</key>\n\t\t<false/>", "<key>beta-reports-active</key>\n\t\t<true/>")
        assertFalse(ProvisioningProfile.allowsDebugging(text))
    }

    @Test
    fun simulatorIsAlwaysDevelopment() {
        // Simulator builds are not provisioned at all.
        assertTrue(isDevelopmentBuild(profile = null, isSimulator = true))
    }

    @Test
    fun deviceWithoutAProfileIsAnAppStoreBuild() {
        // The App Store strips embedded.mobileprovision.
        assertFalse(isDevelopmentBuild(profile = null, isSimulator = false))
    }

    @Test
    fun deviceFollowsTheProfile() {
        assertTrue(isDevelopmentBuild(profile(getTaskAllow = "<true/>"), isSimulator = false))
        assertFalse(isDevelopmentBuild(profile(getTaskAllow = "<false/>"), isSimulator = false))
    }

    private fun profile(getTaskAllow: String?): String {
        val entitlement = getTaskAllow?.let { "\t\t<key>get-task-allow</key>\n\t\t$it\n" }.orEmpty()
        return "0\u0082\u001e¤\u0006\t*\u0086H\u0086÷\r\u0001\u0007\u0002" +
            """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>AppIDName</key>
	<string>Sample</string>
	<key>Entitlements</key>
	<dict>
		<key>application-identifier</key>
		<string>ABCDE12345.com.example.sample</string>
		<key>beta-reports-active</key>
		<false/>
""" + entitlement + """		<key>keychain-access-groups</key>
		<array>
			<string>ABCDE12345.*</string>
		</array>
	</dict>
	<key>Name</key>
	<string>Sample Profile</string>
</dict>
</plist>""" + " \u0082\u0003"
    }
}
