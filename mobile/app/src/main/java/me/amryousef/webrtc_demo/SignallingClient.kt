package me.amryousef.webrtc_demo

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
class SignallingClient(
    private val listener: SignallingClientListener
) : CoroutineScope {

// WORKS
//    companion object {
//        private const val HOST_ADDRESS = "10.1.1.17"
//        private const val HOST_PORT = 8080
//        private const val HOST_URL = "/connect"
//    }
 // EOS config for local Cirrus instance
    companion object {
        private const val HOST_ADDRESS = "10.1.1.17"
        private const val HOST_PORT = 80
        private const val HOST_URL = "/"
    }
    // following on ConversationServer log
//    Thu Jul  8 09:41:52 2021 - uwsgi_response_write_body_do(): Broken pipe [core/writer.c line 429] during GET /ws/console?subscribe-broadcast&publish-broadcast&echo (0.0.0.0)
//    IOError: write error
//    companion object {
//        private const val HOST_ADDRESS = "10.20.15.99"
//        private const val HOST_PORT = 443
//        private const val HOST_URL = "/ws/console?subscribe-broadcast&publish-broadcast&echo"
//    }

    private val job = Job()

    private val gson = Gson()

    override val coroutineContext = Dispatchers.IO + job

    private val client = HttpClient(CIO) {
        install(WebSockets)
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    private val sendChannel = ConflatedBroadcastChannel<String>()

    init {
        connect()
    }

    private fun connect() = launch {
        client.ws(host = HOST_ADDRESS, port = HOST_PORT, path = HOST_URL) {
            listener.onConnectionEstablished()
            val sendData = sendChannel.openSubscription()
            try {
                while (true) {

                    sendData.poll()?.let {
                        Log.v(this@SignallingClient.javaClass.simpleName, "Sending: $it")
                        outgoing.send(Frame.Text(it))
                    }
                    incoming.poll()?.let { frame ->
                        if (frame is Frame.Text) {
                            val data = frame.readText()
                            Log.v(this@SignallingClient.javaClass.simpleName, "Received: $data")
                            val jsonObject = gson.fromJson(data, JsonObject::class.java)
                            withContext(Dispatchers.Main) {
                                // TODO send lowercase, Kotlin lib sending UPPERCASE, Cirrus lowercase
                                if (jsonObject.has("type") && jsonObject.get("type").asString == "iceCandidate") {
                                    listener.onIceCandidateReceived(gson.fromJson(jsonObject, IceCandidate::class.java))
                                } else if (jsonObject.has("type") && jsonObject.get("type").asString == "offer") {
                                    listener.onOfferReceived(gson.fromJson(jsonObject, SessionDescription::class.java))
                                } else if (jsonObject.has("type") && jsonObject.get("type").asString == "answer") {
//                                    Log.v(".M.",data)
                                    listener.onAnswerReceived(gson.fromJson(jsonObject, SessionDescription::class.java))
                                }
                            }
                        }
                    }
                }
            } catch (exception: Throwable) {
                Log.e("asd","asd",exception)
            }
        }
    }

    fun send(dataObject: Any?) = runBlocking {
        sendChannel.send(gson.toJson(dataObject))
    }

    fun destroy() {
        client.close()
        job.complete()
    }
}