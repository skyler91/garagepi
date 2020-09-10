package one.skydev.garagepi

import android.os.Bundle
import android.os.Handler
import android.os.Message
import okhttp3.*
import okhttp3.tls.HandshakeCertificates
import java.io.IOException
import java.net.URL
import javax.net.ssl.HostnameVerifier

internal class DoorController(val address: String, val handler: Handler) {
    enum class DoorStatus {
        OPEN,
        CLOSED,
        LOADING,
        UNKNOWN
    }

    internal fun getDoorStatus() {
        updateDoorStatus(DoorStatus.LOADING)
        val url = URL("https://$address/")

        // TODO: Get a real cert
        val clientCerts = HandshakeCertificates.Builder()
            .addPlatformTrustedCertificates()
            // TODO: Fix hardcoded address
            .addInsecureHost("192.168.2.169")
            .build()
        val httpClient = OkHttpClient.Builder()
            .sslSocketFactory(clientCerts.sslSocketFactory(), clientCerts.trustManager)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .build()
        val request = Request.Builder()
            .url(url)
            .build()

        // TODO: Return error strings
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                updateDoorStatus(DoorStatus.UNKNOWN)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        updateDoorStatus(DoorStatus.UNKNOWN)
                        throw IOException("Unexpected return code: $response")
                    }

                    updateDoorStatus(when(response.body?.string()) {
                        "Door is open" -> DoorStatus.OPEN
                        "Door is closed" -> DoorStatus.CLOSED
                        else -> DoorStatus.UNKNOWN
                    })
                }
            }
        })
    }

    private fun updateDoorStatus(status : DoorController.DoorStatus) {
        val message = Message.obtain()
        val bundle = Bundle()
        bundle.putSerializable("status", status)
        message.data = bundle
        handler.sendMessage(message)
    }
}