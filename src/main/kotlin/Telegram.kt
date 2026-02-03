package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0
    val updateIdRegex: Regex = "\"update_id\":\\s?(\\d+)".toRegex()
    val messageTextRegex: Regex = "\"text\":\\s?\"(.+?)\"".toRegex()
    val chatIdRegex: Regex = "\"chat\":\\{\"id\":\\s?(\\d+)".toRegex()

    while (true) {
        Thread.sleep(2000)
        val updates = getUpdates(botToken, updateId)

        val updateIdMatchResult: MatchResult = updateIdRegex.find(updates) ?: continue
        updateId = updateIdMatchResult.groups[1]?.value?.toInt()?.plus(1) ?: continue
        println(updates)

        val messageTextMatchResult: MatchResult? = messageTextRegex.find(updates)
        val groups = messageTextMatchResult?.groups
        val text = groups?.get(1)?.value
        println(text)

        val chatIdMatchResult: MatchResult = chatIdRegex.find(updates) ?: continue
        val chatId = chatIdMatchResult.groups[1]?.value?.toInt() ?: continue
        sendMessage(botToken, chatId, text.toString())
    }
}

fun getUpdates(botToken: String, updateId: Int): String {
    val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"
    val client: HttpClient = HttpClient.newBuilder().build()
    val requestGetUpdates: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val responseGetUpdates: HttpResponse<String> = client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

    return responseGetUpdates.body()
}

fun sendMessage(botToken: String, chatId: Int, text: String): String {
    val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage?chat_id=$chatId&text=$text"
    val client: HttpClient = HttpClient.newBuilder().build()
    val requestSendMessage: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
    val responseSendMessage: HttpResponse<String> = client.send(requestSendMessage, HttpResponse.BodyHandlers.ofString())

    return responseSendMessage.body()
}