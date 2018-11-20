package com.highmobility.basicoauth

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.JsonRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class OAuthManager {
    /*
     Before using the OAuth, it's required variables must be set:
     - go to https://developers.high-mobility.com/oauth to get and paste:
        * authURI
        * clientID
        * tokenURI
        * redirectScheme (for Android app, it's under "URL-SCHEME FOR IOS & ANDROID", not the "REDIRECT URI")
     - go to https://developers.high-mobility.com/develop/applications/device-apps/
        * choose one app and copy its app id (hex number)
        * IMPORTANT: The app id must be the same that you initialize the SDK with in MainActivity
     - figure out the "scope"
        * an example, that would be needed for this sample – "door-locks.read,door-locks.write"
     - now fill in all the needed vars
     */

    val appId:String = ""
    val authUrl:String = ""
    val clientId:String = ""
    val redirectScheme:String = ""
    val scope:String = ""
    val tokenUrl:String = ""

    val startDate:Calendar? = null
    val endDate:Calendar? = null
    val state:String? = null

    private val TAG: String = "OauthManager"

    init {
        if (appId == "" || authUrl == "" || clientId == ""
                || redirectScheme == "" || scope == "" || tokenUrl == "") {
            throw IllegalArgumentException("OAuth variables not set. Set them in OAuthManager.kt")
        }
    }

    fun webViewUrl() : String {
        var webUrl = authUrl
        webUrl += "?client_id=" + clientId
        webUrl += "&redirect_uri=" + redirectScheme
        webUrl += "&scope=" + scope
        webUrl += "&app_id=" + appId

        if (state != null) webUrl += "&state=" + state

        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ")
        if (startDate != null) webUrl += "&validity_start_date=" + df.format(startDate)
        if (endDate != null) webUrl += "&validity_end_date=" + df.format(endDate)

        return webUrl
    }

    fun downloadAccessToken(uri: Uri, ctx:Context, success : (accessToken:String) -> Unit,
                            failure : (errorMessage:String) -> Unit) {
        val code = uri.getQueryParameter("code")
        var tokenUrl = tokenUrl

        tokenUrl += "?client_id=" + clientId
        tokenUrl += "&code=" + code
        tokenUrl += "&redirect_uri=" + redirectScheme

        val request = JsonObjectRequest(Request.Method.POST, tokenUrl, null,
                Response.Listener { jsonObject ->
            try {
                Log.d(TAG, "response " + jsonObject.toString(2))
                val accessToken = jsonObject["access_token"] as String
                success(accessToken)
            } catch (e: JSONException) {
                e.printStackTrace()
                failure("invalid download access token response")
            }
        }, Response.ErrorListener { error: VolleyError? ->
            if (error?.networkResponse != null) {
                failure("" + error.networkResponse.statusCode + ": " + String(error.networkResponse.data))
            } else {
                failure("no connection")
            }
        })

        printRequest(request)
        Volley.newRequestQueue(ctx).add(request)
    }

    private fun printRequest(request: JsonRequest<*>) {
        try {
            val body = request.body
            val bodyString = if (body != null) "\n" + String(request.body) else ""
            val headers = JSONObject(request.headers)

            try {
                Log.d(TAG, request.url.toString() + "\n" + headers.toString(2) + bodyString)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        } catch (authFailureError: AuthFailureError) {
            authFailureError.printStackTrace()
        }
    }
}
