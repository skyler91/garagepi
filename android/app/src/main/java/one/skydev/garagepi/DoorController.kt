package one.skydev.garagepi

import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Message
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URL

internal class DoorController(private val handler: Handler) {
    private val baseUrl = BuildConfig.BASE_URL
    private val statusEndpoint = BuildConfig.DOOR_STATUS_ENDPOINT
    private val commandEndpoint = BuildConfig.DOOR_COMMAND_ENDPOINT

    enum class DoorStatus(val statusString: String) {
        OPEN("open"),
        CLOSED("closed"),
        LOADING("loading"),
        UNKNOWN("unknown"),
        OPENING("opening"),
        CLOSING("closing");

        companion object {
            fun fromStatusString(statusString : String) : DoorStatus = values().find {
                it.statusString == statusString
            }?: UNKNOWN
        }
    }

    enum class DoorCommand(val commandString : String) {
        OPEN("{\"command\": \"open\"}"),
        CLOSE("{\"command\": \"close\"}")
    }

    internal fun sendDoorCommand(userToken: String?, cmd: DoorCommand) {
        // TODO: use gson?
        val httpClient = OkHttpClient()
        val request = Request.Builder()
            .url(URL(baseUrl + commandEndpoint))
            .header("Authorization", "Bearer $userToken")
            .post(cmd.commandString.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                TODO("sendDoorCommand onFailure not yet implemented")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        TODO("sendDoorCommand onResponse (fail) not yet implemented")
                    }

                    // TODO: Handle result
                    val resultBody = response.body?.string()
                }
            }
        })
    }

    internal fun getDoorStatus(userToken : String?) {
        val httpClient = OkHttpClient()
        val request = Request.Builder()
            .url(URL(baseUrl + statusEndpoint))
            .header("Authorization", "Bearer $userToken")
            .build()

        // TODO: Return error strings
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                updateDoorStatus(DoorStatus.UNKNOWN, "DoorController onHttpClientFailure", e.message.orEmpty())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        updateDoorStatus(DoorStatus.UNKNOWN, "DoorController unsuccessfulHttpResponse", response.message)
                        throw IOException("Unexpected return code: $response")
                    }

                    // TODO: Error handling for json object
                    val jsonObj = JSONObject(response.body?.string())
                    val status = jsonObj.get("status")
                    updateDoorStatus(
                        DoorStatus.fromStatusString(status.toString()),
                    "DoorController httpUpdate")
                }
            }
        })
    }

    internal fun updateDoorStatus(
        status: DoorStatus,
        source: String,
        statusMessage: String = ""
    ) {
        val message = Message.obtain()
        val bundle = Bundle()
        bundle.putSerializable("status", status)
        bundle.putString("source",  source)
        bundle.putString("message", statusMessage)
        message.data = bundle
        handler.sendMessage(message)
    }
}