package com.highmobility.basicoauth

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.highmobility.hmkit.ByteUtils
import com.highmobility.hmkit.Command.Command
import com.highmobility.hmkit.Error.DownloadAccessCertificateError
import com.highmobility.hmkit.Error.TelematicsError
import com.highmobility.hmkit.Manager
import com.highmobility.hmkit.Telematics

class MainActivity : Activity() {
    val TAG = "Basic OAuth"
    private val oauthManager = OAuthManager()
    private lateinit var textView:TextView
    private lateinit var progressBar:ProgressBar
    private lateinit var button:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        /*
            Two things need to be set for this app to work.

            Look at:
             * OAuthManager.kt fields
             * method downloadAccessCertificate in this class
         */

        textView = findViewById(R.id.text_view)
        progressBar = findViewById(R.id.progress_bar)
        button = findViewById(R.id.access_token_text_button)

        button.setOnClickListener {
            openOAuthUrl()
        }
    }

    private fun openOAuthUrl() {
        val url = oauthManager.webViewUrl()
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    override fun onResume() {
        super.onResume()

        if (intent?.data != null) {
            Log.d(TAG, "new intent: " + intent.data)
            progressBar.visibility = View.VISIBLE
            button.visibility = View.GONE

            oauthManager.downloadAccessToken(intent.data, this, { accessToken ->
                downloadAccessCertificate(accessToken)
            }, { errorMessage ->
                progressBar.visibility = View.GONE
                onError("error downloading access token " + errorMessage)
            })
        }
    }

    private fun downloadAccessCertificate(accessToken:String) {
        /*
         Before using HMKit, you must initialise it with a snippet from the Developer Center:
         - go to the Developer Center
         - LOGIN
         - choose DEVELOP (in top-left, the (2nd) button with a spanner)
         - choose APPLICATIONS (in the left)
         - look for the app you have chosen, note that the app needs to be the same that is
           used in the OAuthManagers variable appId
         - click on the "Device Certificates" on the app
         - choose the SANDBOX DEVICE
         - copy the whole snippet
         - paste it below this comment box
         - you made it!

         Bonus steps after completing the above:
         - relax
         - celebrate
         - explore the APIs


         An example of a snippet copied from the Developer Center (do not use, will obviously not work):

            manager.initialize(
                Base64String,
                Base64String,
                Base64String,
                getApplicationContext()
            );
         */

        Manager.getInstance().downloadCertificate(accessToken,
            object : Manager.DownloadCallback {
                override fun onDownloaded(vehicleSerial: ByteArray) {
                    sendHonkFlash(vehicleSerial)
                }

                override fun onDownloadFailed(error: DownloadAccessCertificateError) {
                    onError("error downloading access certificate" + error.type + " " + error.message)
                }
            })
    }

    private fun sendHonkFlash(vehicleSerial: ByteArray) {
        // send a simple command to see everything worked
        val command = Command.HonkFlash.honkFlash(5, 10)
        Manager.getInstance().telematics.sendCommand(command, vehicleSerial,
                object : Telematics.CommandCallback {
                    override fun onCommandResponse(p0: ByteArray?) {
                        progressBar.visibility = View.GONE
                        textView.text = "Successfully sent honk and flash."
                    }

                    override fun onCommandFailed(p0: TelematicsError?) {
                        onError("failed to send honk and flash: " + p0?.type + " " + p0?.message)
                    }
                })
    }

    private fun onError(msg: String) {
        progressBar.visibility = View.GONE
        textView.text = msg
        Log.e(TAG, msg)
    }
}
