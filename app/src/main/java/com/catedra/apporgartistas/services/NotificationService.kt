package com.catedra.apporgartistas.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NotificationService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    private val notificationUrl =
        "https://messaging-service-lfyh.onrender.com/send-notification"

    fun enviarNotificacion(
        token: String,
        title: String,
        body: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonBody = JSONObject().apply {
                    put("token", token)
                    put("title", title)
                    put("body", body)
                }

                val requestBody = jsonBody
                    .toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(notificationUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Error al enviar notificación: ${response.code}")
                }

                response.close()

            } catch (exception: Exception) {
                onError(exception.message ?: "Error desconocido al enviar notificación")
            }
        }
    }

    fun notificarNuevoMusicoEnSetlist(
        tokenDirector: String,
        nombreInvitado: String,
        nombreSetlist: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        enviarNotificacion(
            token = tokenDirector,
            title = "¡Nuevo músico en tu setlist!",
            body = "$nombreInvitado se unió a: $nombreSetlist",
            onSuccess = onSuccess,
            onError = onError
        )
    }
}