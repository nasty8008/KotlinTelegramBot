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

        val chatIdMatchResult: MatchResult = chatIdRegex.find(updates) ?: continue
        println(chatIdMatchResult.groups[1]?.value?.toInt())

        val messageTextMatchResult: MatchResult? = messageTextRegex.find(updates)
        val groups = messageTextMatchResult?.groups
        val text = groups?.get(1)?.value
        println(text)
    }
}

fun getUpdates(botToken: String, updateId: Int): String {
    val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"
    val client: HttpClient = HttpClient.newBuilder().build()
    val requestGetUpdates: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val responseGetUpdates: HttpResponse<String> = client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

    return responseGetUpdates.body()
}