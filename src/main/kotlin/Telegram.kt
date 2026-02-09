package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val CALLBACK_LEARN_WORDS = "learn_words_clicked"
const val CALLBACK_STATISTICS = "statistics_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

class TelegramBotService(private val botToken: String) {
    val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Int): String {
        val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"
        val requestGetUpdates: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val responseGetUpdates: HttpResponse<String> =
            client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

        return responseGetUpdates.body()
    }

    fun sendMessage(chatId: Int, text: String): String {
        val encodedText = URLEncoder.encode(
            text,
            StandardCharsets.UTF_8
        )
        println(encodedText)
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage?chat_id=$chatId&text=$encodedText"
        val requestSendMessage: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
        val responseSendMessage: HttpResponse<String> =
            client.send(requestSendMessage, HttpResponse.BodyHandlers.ofString())

        return responseSendMessage.body()
    }

    fun sendMenu(chatId: Int): String {
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
            					"callback_data": "$CALLBACK_LEARN_WORDS"
            				},
            				{
            					"text": "Статистика",
            					"callback_data": "$CALLBACK_STATISTICS"
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
        val responseSendMessage: HttpResponse<String> =
            client.send(requestSendMessage, HttpResponse.BodyHandlers.ofString())

        return responseSendMessage.body()
    }

    fun sendQuestion(chatId: Int, question: Question): String {
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage"
        val variants = question.variants.mapIndexed { index, word ->
            """ {
                    "text": "${word.translation}",
                    "callback_data": "$CALLBACK_DATA_ANSWER_PREFIX$index"
                }
            """.trimIndent()
        }
        val sendQuestionBody = """
            {
            	"chat_id": $chatId,
            	"text": "${question.correctAnswer.original}",
            	"reply_markup": {
            		"inline_keyboard": [
                        [
                            ${variants.joinToString()}
                        ]
            		]
            	}
            }  
        """.trimIndent()

        val requestSendQuestion: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendQuestionBody))
            .build()
        val responseSendQuestion: HttpResponse<String> =
            client.send(requestSendQuestion, HttpResponse.BodyHandlers.ofString())

        return responseSendQuestion.body()
    }
}

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0

    val botService = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer()

    val updateIdRegex: Regex = "\"update_id\":\\s?(\\d+)".toRegex()
    val messageTextRegex: Regex = "\"text\":\\s?\"(.+?)\"".toRegex()
    val chatIdRegex: Regex = "\"chat\":\\{\"id\":\\s?(\\d+)".toRegex()
    val dataRegex: Regex = "\"data\":\\s?\"(.+?)\"".toRegex()

    while (true) {
        Thread.sleep(2000)
        val updates = botService.getUpdates(updateId)

        updateId = updateIdRegex.find(updates)?.groups[1]?.value?.toInt()?.plus(1) ?: continue
        println(updates)

        val message = messageTextRegex.find(updates)?.groups[1]?.value ?: continue
        println(message)

        val chatId = chatIdRegex.find(updates)?.groups[1]?.value?.toInt() ?: continue
        val data = dataRegex.find(updates)?.groups[1]?.value

        if (message == "/start") {
            botService.sendMenu(chatId)
        }
        if (data == CALLBACK_STATISTICS) {
            val statistics = trainer.getStatistics()
            val statText = """
                Всего слов в словаре: ${statistics.totalCount}
                Выучено слов: ${statistics.learnedCount}
                Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}
            """.trimIndent()
            botService.sendMessage(chatId, statText)
        }
        if (data == CALLBACK_LEARN_WORDS) {
            checkNextQuestionAndSend(trainer, botService, chatId)
        }
    }
}

fun checkNextQuestionAndSend(
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    chatId: Int
) {
    val question = trainer.getNextQuestion()
    if (question == null) {
        telegramBotService.sendMessage(chatId, "Все слова в словаре выучены!")
    } else {
        println(telegramBotService.sendQuestion(chatId, question))
    }
}