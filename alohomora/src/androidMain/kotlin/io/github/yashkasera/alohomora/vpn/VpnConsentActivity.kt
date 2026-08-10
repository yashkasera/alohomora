package io.github.yashkasera.alohomora.vpn

import android.app.Activity
import android.net.VpnService
import android.os.Bundle

/**
 * Transparent activity that handles the VPN consent dialog.
 *
 * `VpnService.prepare()` returns null when consent is already granted, or an Intent for the
 * system consent dialog otherwise. This activity launches that intent and reports the result
 * back to [VpnThrottleController], then finishes immediately.
 *
 * `taskAffinity=""` keeps it off the host app's task stack.
 */
class VpnConsentActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent == null) {
            VpnThrottleController.onConsentResult(granted = true)
            finish()
        } else {
            @Suppress("DEPRECATION")
            startActivityForResult(prepareIntent, REQUEST_VPN_CONSENT)
        }
    }

    @Deprecated("Use registerForActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VPN_CONSENT) {
            VpnThrottleController.onConsentResult(granted = resultCode == RESULT_OK)
        }
        finish()
    }

    companion object {
        private const val REQUEST_VPN_CONSENT = 0xA10D
    }
}
