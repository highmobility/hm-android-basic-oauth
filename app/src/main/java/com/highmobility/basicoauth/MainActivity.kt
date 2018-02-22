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
import com.highmobility.autoapi.HonkAndFlash

import com.highmobility.hmkit.Error.DownloadAccessCertificateError
import com.highmobility.hmkit.Error.TelematicsError
import com.highmobility.hmkit.Manager
import com.highmobility.hmkit.Telematics

class MainActivity : Activity() {
    val TAG = "Basic OAuth"
    private val oauthManager = OAuthManager()
    private lateinit var textView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var button: Button

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

    private fun downloadAccessCertificate(accessToken: String) {
        /*
         * Before using HMKit, you'll have to initialise the Manager singleton
         * with a snippet from the Platform Workspace:
         *
         *   1. Sign in to the workspace
         *   2. Go to the LEARN section and choose Android
         *   3. Follow the Getting Started instructions
         *
         * By the end of the tutorial you will have a snippet for initialisation,
         * that looks something like this:
         *
         *   Manager.getInstance().initialize(
         *     Base64String,
         *     Base64String,
         *     Base64String,
         *     getApplicationContext()
         *   );
         */

        // PASTE THE SNIPPET HERE

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
        val command = HonkAndFlash(5,1).bytes
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
