package com.highmobility.basicoauth

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.highmobility.autoapi.CommandResolver
import com.highmobility.autoapi.DiagnosticsState
import com.highmobility.autoapi.Failure
import com.highmobility.autoapi.GetDiagnosticsState
import com.highmobility.crypto.value.DeviceSerial
import com.highmobility.hmkit.HMKit
import com.highmobility.hmkit.HMLog
import com.highmobility.hmkit.Telematics
import com.highmobility.hmkit.error.DownloadAccessCertificateError
import com.highmobility.hmkit.error.TelematicsError
import kotlinx.android.synthetic.main.activity_main.*

class BasicOAuthActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
        Before using HMKit, you'll have to initialise the Manager singleton
        with a snippet from the Platform Workspace:

          1. Sign in to the workspace
          2. Go to the LEARN section and choose Android
          3. Follow the Getting Started instructions

        By the end of the tutorial you will have a snippet for initialisation,
        that looks something like this:

          HMKit.getInstance().initialise(
            Base64String,
            Base64String,
            Base64String,
            applicationContext
          );
        */

        // TODO: REPLACE THE INITIALISE SNIPPET HERE
        HMKit.getInstance().initialise(
                "",
                "",
                "",
                applicationContext
        )


        /*
         Before using the OAuth, it's required variables must be set:
         - go to https://high-mobility.com/profile/oauth-client to get and paste:
            * Auth URI
            * Client ID
            * redirectScheme (URL-scheme for iOS & Android)
            * Token URI
         - go to https://high-mobility.com/develop/
            * choose one app and copy its Serial#/App Id (hex number)
            * IMPORTANT: The app id must be the same that you initialise the SDK with.
         - now fill in all the needed vars
         */
        button.setOnClickListener {
            // TODO: SET THE OAUTH VARIABLES
            HMKit.getInstance().oAuth.getAccessToken(
                    this,
                    "",
                    "",
                    "",
                    "",
                    null,
                    null,
                    null
            ) { accessToken, errorMessage ->
                if (accessToken != null) {
                    onAccessTokenDownloaded(accessToken)
                }
                else {
                    onError(errorMessage!!)
                }
            }
        }


        // optional: if have downloaded the certificate previously, can access it from HMKit storage.
        val serial = DeviceSerial("000000000000000000")
        val cert = HMKit.getInstance().getCertificate(serial)
        if (cert != null) {
            downloadVehicleStatus(cert.gainerSerial)
            textView.text = "Have certificate. Sending Get Diagnostics"
        }
    }

    private fun onAccessTokenDownloaded(accessToken: String) {
        HMKit.getInstance().downloadAccessCertificate(accessToken, object : HMKit.DownloadCallback {
            override fun onDownloaded(vehicleSerial: DeviceSerial) {
                downloadVehicleStatus(vehicleSerial)
            }

            override fun onDownloadFailed(error: DownloadAccessCertificateError) {
                onError("error downloading access certificate" + error.type + " " + error.message)
            }
        })
    }

    private fun downloadVehicleStatus(vehicleSerial: DeviceSerial) {
        progressBar.visibility = View.VISIBLE
        textView.text = "Sending Get Diagnostics"
        // send a simple command to see everything worked
        HMKit.getInstance().telematics.sendCommand(GetDiagnosticsState(), vehicleSerial, object :
                Telematics.CommandCallback {
            override fun onCommandResponse(p0: com.highmobility.value.Bytes?) {
                progressBar.visibility = View.GONE
                val command = CommandResolver.resolve(p0)

                when (command) {
                    is DiagnosticsState -> textView.text = "Got Diagnostics,\nmileage: ${command.mileage}"
                    is Failure -> textView.text = "Get Diagnostics failure:\n\n${command.failureReason}\n${command.failureDescription}"
                    else -> textView.text = "Unknown command response"
                }
            }

            override fun onCommandFailed(p0: TelematicsError?) {
                onError("failed to get VS:\n" + p0?.type + " " + p0?.message)
            }
        })
    }

    private fun onError(msg: String) {
        progressBar.visibility = View.GONE
        textView.text = msg
        HMLog.e(msg)
    }
}