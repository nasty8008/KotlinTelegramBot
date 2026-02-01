package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"

fun main(args: Array<String>) {

    val botToken = args[0]
    val urlGetMe = "$TELEGRAM_BASE_URL$botToken/getMe"
    val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates"

    val client: HttpClient = HttpClient.newBuilder().build()

    val requestGetMe: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetMe)).build()
    val requestGetUpdates: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()

    val responseGetMe: HttpResponse<String> = client.send(requestGetMe, HttpResponse.BodyHandlers.ofString())
    val responseGetUpdates: HttpResponse<String> = client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

    println(responseGetMe.body())
    println()
    println(responseGetUpdates.body())
}