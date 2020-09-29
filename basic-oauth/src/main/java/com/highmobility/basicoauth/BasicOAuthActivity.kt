/*
 * The MIT License
 *
 * Copyright (c) 2014- High-Mobility GmbH (https://high-mobility.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.highmobility.basicoauth

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import com.highmobility.autoapi.CommandResolver
import com.highmobility.autoapi.Diagnostics
import com.highmobility.autoapi.FailureMessage
import com.highmobility.crypto.value.DeviceSerial
import com.highmobility.hmkit.AccessTokenResponse
import com.highmobility.hmkit.HMKit
import com.highmobility.hmkit.Telematics
import com.highmobility.hmkit.error.DownloadAccessCertificateError
import com.highmobility.hmkit.error.TelematicsError
import com.highmobility.value.Bytes
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import timber.log.Timber.e
import timber.log.Timber.plant

class BasicOAuthActivity : Activity() {
    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("prefs", 0)
        plant(Timber.DebugTree())

        if (initialiseFromFileIfSnippetNotPresent() == false) {
            /*
            Before using HMKit, you'll have to initialise the HMKit singleton
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

            // TODO: PASTE THE INITIALISE SNIPPET HERE

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
            createAccessTokenButton.setOnClickListener {
                // TODO: SET THE OAUTH VARIABLES
                HMKit.getInstance().oAuth.getAccessToken(
                    this,
                    "appId",
                    "authUrl",
                    "clientId",
                    "redirectScheme",
                    "tokenUrl",
                    null,
                    null
                ) { accessToken, errorMessage ->
                    onAccessTokenResponse(accessToken, errorMessage)
                }
            }
        }

        // optional: if have downloaded the certificate previously, can access it from HMKit storage.
        // Need to use the vehicle serial to get the certificate from SDK storage then:
        /*val vehicleSerial = DeviceSerial("000000000000000000")

        val cert = HMKit.getInstance().getCertificate(vehicleSerial)
        if (cert != null) {
            downloadVehicleStatus(cert.gainerSerial)
            textView.text = "Have certificate. Sending Get Diagnostics"
        }*/

        // optional: can refresh the access token if have the refresh token for it.
        /*val refreshToken = refreshToken

        HMLog.d("refresh token: $refreshToken access token expired: ${accessTokenExpired()}")

        if (accessTokenExpired() && refreshToken != null) {
            HMKit.getInstance().oAuth.refreshAccessToken(
                    "tokenUrl",
                    "clientId",
                    refreshToken)
            { accessToken, errorMessage ->
                onAccessTokenResponse(accessToken, errorMessage)
            }
        }*/
    }

    /**
     * Try and initialize with values from credentials.xml
     */
    private fun initialiseFromFileIfSnippetNotPresent(): Boolean {
        if (HMKit.webUrl != null) return true // already initialised

        try {
            val credentials = Credentials(this)
            HMKit.webUrl = credentials.getEnvironmentResource("webUrl")

            if (HMKit.webUrl != null) {
                HMKit.getInstance().initialise(
                    credentials.getEnvironmentResource("deviceCert"),
                    credentials.getEnvironmentResource("privateKey"),
                    credentials.getEnvironmentResource("issuerPublicKey"),
                    applicationContext
                )

                createAccessTokenButton.setOnClickListener {
                    HMKit.getInstance().oAuth.getAccessToken(
                        this,
                        credentials.getEnvironmentResource("appId"),
                        credentials.getEnvironmentResource("authUrl"),
                        credentials.getEnvironmentResource("clientId"),
                        credentials.getEnvironmentResource("redirectScheme"),
                        credentials.getEnvironmentResource("tokenUrl"),
                        null,
                        null
                    ) { accessToken, errorMessage ->
                        onAccessTokenResponse(accessToken, errorMessage)
                    }
                }
            }

            return true
        } catch (e: Exception) {
            e(e)
            // its ok, were initialised by snippet
        }

        return false
    }

    var refreshToken: String?
        get() = prefs.getString("refreshToken", null)
        set(value) = prefs.edit().putString("refreshToken", value).apply()

    var expireDate: Int
        get() = prefs.getInt("expireDate", 0)
        set(value) = prefs.edit().putInt("expireDate", value).apply()

    fun accessTokenExpired(): Boolean {
        return (System.currentTimeMillis() / 1000).toInt() > expireDate
    }

    private fun onAccessTokenResponse(accessToken: AccessTokenResponse?, errorMessage: String?) {
        if (accessToken != null) {
            this.refreshToken = accessToken.refreshToken
            this.expireDate = (System.currentTimeMillis() / 1000).toInt() + accessToken.expiresIn

            HMKit.getInstance().downloadAccessCertificate(
                accessToken.accessToken,
                object : HMKit.DownloadCallback {
                    override fun onDownloaded(vehicleSerial: DeviceSerial) {
                        downloadDiagnostics(vehicleSerial)
                    }

                    override fun onDownloadFailed(error: DownloadAccessCertificateError) {
                        onError("error downloading access certificate" + error.type + " " + error.message)
                    }
                })
        } else {
            onError(errorMessage!!)
        }
    }

    private fun downloadDiagnostics(vehicleSerial: DeviceSerial) {
        progressBar.visibility = View.VISIBLE
        textView.text = "Sending Get Diagnostics"
        // send a simple command to see everything worked
        HMKit.getInstance().telematics.sendCommand(Diagnostics.GetState(), vehicleSerial, object :
            Telematics.CommandCallback {
            override fun onCommandResponse(responseBytes: Bytes?) {
                progressBar.visibility = View.GONE
                val response = CommandResolver.resolve(responseBytes)

                when (response) {
                    is Diagnostics.State -> textView.text =
                        "Got Diagnostics,\nmileage: ${response.fuelLevel.value}"
                    is FailureMessage.State -> textView.text =
                        "Get Diagnostics failure:\n\n${response.failureReason.value}\n${response.failureDescription.value}"
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
        textView.text = "error: $msg"
        e(msg)
    }
}