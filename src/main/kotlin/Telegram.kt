package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0

    while (true) {
        Thread.sleep(2000)
        val updates = getUpdates(botToken, updateId)

        val updateIdRegex: Regex = "\"update_id\":(.+?),".toRegex()
        val updateIdMatchResult: MatchResult = updateIdRegex.find(updates) ?: continue
        updateId = updateIdMatchResult.groups[1]?.value!!.toInt() + 1
        println(updates)

        val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
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