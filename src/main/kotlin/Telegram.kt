package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"

class TelegramBotService {
    val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(botToken: String, updateId: Int): String {
        val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"
        val requestGetUpdates: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val responseGetUpdates: HttpResponse<String> = client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

        return responseGetUpdates.body()
    }

    fun sendMessage(botToken: String, chatId: Int, text: String): String {
        val encodedText = URLEncoder.encode(
            text,
            StandardCharsets.UTF_8)
        println(encodedText)
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage?chat_id=$chatId&text=$encodedText"
        val requestSendMessage: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
        val responseSendMessage: HttpResponse<String> = client.send(requestSendMessage, HttpResponse.BodyHandlers.ofString())

        return responseSendMessage.body()
    }

    fun sendMenu(botToken: String, chatId: Int): String {
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage"
        val sendMenuBody = """
            {
            	"chat_id": $chatId,
            	"text": "Основное меню",
            	"reply_markup": {
            		"inline_keyboard": [
            			[
            				{
            					"text": "Изучить слова",
            					"callback_data": "learn_words_clicked"
            				},
            				{
            					"text": "Статистика",
            					"callback_data": "statistics_clicked"
            				}
            			]
            		]
            	}
            }
        """.trimIndent()

        val requestSendMessage: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendMenuBody))
            .build()
        val responseSendMessage: HttpResponse<String> = client.send(requestSendMessage, HttpResponse.BodyHandlers.ofString())

        return responseSendMessage.body()
    }
}

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0

    val botService = TelegramBotService()
    val trainer = LearnWordsTrainer()

    val updateIdRegex: Regex = "\"update_id\":\\s?(\\d+)".toRegex()
    val messageTextRegex: Regex = "\"text\":\\s?\"(.+?)\"".toRegex()
    val chatIdRegex: Regex = "\"chat\":\\{\"id\":\\s?(\\d+)".toRegex()
    val dataRegex: Regex = "\"data\":\\s?\"(.+?)\"".toRegex()

    while (true) {
        Thread.sleep(2000)
        val updates = botService.getUpdates(botToken, updateId)

        updateId = updateIdRegex.find(updates)?.groups[1]?.value?.toInt()?.plus(1) ?: continue
        println(updates)

        val message = messageTextRegex.find(updates)?.groups[1]?.value ?: continue
        println(message)

        val chatId = chatIdRegex.find(updates)?.groups[1]?.value?.toInt() ?: continue
        val data = dataRegex.find(updates)?.groups[1]?.value

        if (message  == "/start") {
            botService.sendMenu(botToken, chatId)
        }
        if (data?.lowercase() == "statistics_clicked") {
            botService.sendMessage(botToken, chatId, "Выучено 10 из 10 слов | 100%")
        }
    }
}