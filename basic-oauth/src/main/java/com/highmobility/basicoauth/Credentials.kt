package com.highmobility.basicoauth

import android.content.Context

class Credentials(context: Context) {
    private val resources = context.resources
    private val packageName = context.packageName

    private val environment: String? by lazy { getResource("environment") }

    private fun getResource(key: String): String {
        val resId = resources.getIdentifier(key, "string", packageName)
        return resources.getString(resId)
    }

    fun getEnvironmentResource(key: String): String {
        // keys use format: {dev/prod}Key, eg devDeviceSerial
        return getResource("${environment}${key.capitalize()}")
    }
}