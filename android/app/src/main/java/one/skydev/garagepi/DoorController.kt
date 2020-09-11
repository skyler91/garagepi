package one.skydev.garagepi

import android.os.Bundle
import android.os.Handler
import android.os.Message
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URL

internal class DoorController(private val address: String, private val handler: Handler) {
    enum class DoorStatus {
        OPEN,
        CLOSED,
        LOADING,
        UNKNOWN
    }

    internal fun getDoorStatus() {
        //updateDoorStatus(DoorStatus.LOADING)
        // TODO: Set up authentication
        val httpClient = OkHttpClient()
        val request = Request.Builder()
            .url(URL(address))
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

                    // TODO: Error handling for json object
                    val jsonObj = JSONObject(response.body?.string())
                    val status = jsonObj.get("status")
                    updateDoorStatus(when(status) {
                        "open" -> DoorStatus.OPEN
                        "closed" -> DoorStatus.CLOSED
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