package com.highmobility.basicoauth

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import com.highmobility.autoapi.*
import com.highmobility.autoapi.value.Lock
import com.highmobility.crypto.value.DeviceSerial
import com.highmobility.hmkit.*
import com.highmobility.hmkit.error.*
import com.highmobility.value.Bytes
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import timber.log.Timber.*

class BasicOAuthActivity : Activity() {
    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("prefs", 0)
        plant(Timber.DebugTree())

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
        button.setOnClickListener {
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

        workWithBluetooth()
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

    private fun workWithBluetooth() {
        // Start Bluetooth broadcasting, so that the car can connect to this device
        val broadcaster = HMKit.getInstance().broadcaster

        if (broadcaster == null) return  // emulator

        broadcaster!!.setListener(object : BroadcasterListener {
            override fun onStateChanged(state: Broadcaster.State) {
                d("Broadcasting state changed: %s", state)
            }

            override fun onLinkReceived(connectedLink: ConnectedLink) {
                connectedLink.setListener(object : ConnectedLinkListener {
                    override fun onAuthenticationRequested(connectedLink: ConnectedLink,
                                                           authorizationCallback: ConnectedLinkListener.AuthenticationRequestCallback) {

                        // Approving without user input
                        authorizationCallback.approve()
                    }

                    override fun onAuthenticationRequestTimeout(connectedLink: ConnectedLink) {

                    }

                    override fun onAuthenticationFailed(link: Link?, error: AuthenticationError?) {

                    }

                    override fun onStateChanged(link: Link, state: Link.State) {
                        revoke.visibility =
                                if (link.state == Link.State.AUTHENTICATED) VISIBLE else GONE

                        revoke.setOnClickListener {
                            link.revoke(object : Link.RevokeCallback {
                                override fun onRevokeSuccess(customData: Bytes?) {
                                    d("revokeS")
                                }

                                override fun onRevokeFailed(error: RevokeError?) {
                                    d("revokeF")
                                }
                            })
                        }

                        if (link.state == Link.State.AUTHENTICATED) {
                            val command = GetVehicleStatus()
                            link.sendCommand(command, object : Link.CommandCallback {
                                override fun onCommandSent() {
                                    d("Command successfully sent through Bluetooth")
                                }

                                override fun onCommandFailed(linkError: LinkError) {

                                }
                            })
                        }
                    }

                    override fun onCommandReceived(link: Link, bytes: Bytes) {
                        val command = CommandResolver.resolve(bytes)
                        d("command " + command)
                    }
                })
            }

            override fun onLinkLost(connectedLink: ConnectedLink) {
                // Bluetooth disconnected
            }
        })

        broadcaster.startBroadcasting(object : Broadcaster.StartCallback {
            override fun onBroadcastingStarted() {
                d("Bluetooth broadcasting started")
            }

            override fun onBroadcastingFailed(broadcastError: BroadcastError) {
                d("Bluetooth broadcasting started: $broadcastError")
            }
        })
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

            HMKit.getInstance().downloadAccessCertificate(accessToken.accessToken, object : HMKit.DownloadCallback {
                override fun onDownloaded(vehicleSerial: DeviceSerial) {
                    downloadVehicleStatus(vehicleSerial)
                }

                override fun onDownloadFailed(error: DownloadAccessCertificateError) {
                    onError("error downloading access certificate" + error.type + " " + error.message)
                }
            })
        }
        else {
            onError(errorMessage!!)
        }
    }

    private fun downloadVehicleStatus(vehicleSerial: DeviceSerial) {
        progressBar.visibility = View.VISIBLE
        textView.text = "Sending Get Diagnostics"
        // send a simple command to see everything worked
        HMKit.getInstance().telematics.sendCommand(LockUnlockDoors(Lock.UNLOCKED), vehicleSerial, object :
                Telematics.CommandCallback {
            override fun onCommandResponse(p0: Bytes?) {
                progressBar.visibility = View.GONE
                val command = CommandResolver.resolve(p0)

                when (command) {
                    is DiagnosticsState -> textView.text =
                            "Got Diagnostics,\nmileage: ${command.mileage.value}"
                    is Failure -> textView.text =
                            "Get Diagnostics failure:\n\n${command.failureReason.value}\n${command.failureDescription.value}"
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