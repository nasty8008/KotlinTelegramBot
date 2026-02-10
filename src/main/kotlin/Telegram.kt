package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val CALLBACK_LEARN_WORDS = "learn_words_clicked"
const val CALLBACK_STATISTICS = "statistics_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
)

@Serializable
data class Response(
    @SerialName("result")
    val result: List<Update>,
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String,
    @SerialName("chat")
    val chat: Chat,
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
)

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long,
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>,
)

@Serializable
data class InlineKeyboard(
    @SerialName("text")
    val text: String,
    @SerialName("callback_data")
    val callbackData: String,
)

class TelegramBotService(private val botToken: String) {
    val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Long): String {
        val urlGetUpdates = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"
        val requestGetUpdates: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val responseGetUpdates: HttpResponse<String> =
            client.send(requestGetUpdates, HttpResponse.BodyHandlers.ofString())

        return responseGetUpdates.body()
    }

    fun sendMessage(json: Json, chatId: Long, text: String): String {
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage"

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = text
        )

        val requestBodyString = json.encodeToString(requestBody)

        val requestSendMessage: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val responseSendMessage: HttpResponse<String> =
            client.send(requestSendMessage, HttpResponse.BodyHandlers.ofString())

        return responseSendMessage.body()
    }

    fun sendMenu(json: Json, chatId: Long): String {
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage"

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                inlineKeyboard = listOf(listOf(
                    InlineKeyboard(
                        text = "Изучить слова",
                        callbackData = CALLBACK_LEARN_WORDS
                    ),
                    InlineKeyboard(
                        text = "Статистика",
                        callbackData = CALLBACK_STATISTICS
                    )
                ))
            )
        )

        val requestBodyString = json.encodeToString(requestBody)

        val requestSendMessage: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val responseSendMessage: HttpResponse<String> =
            client.send(requestSendMessage, HttpResponse.BodyHandlers.ofString())

        return responseSendMessage.body()
    }

    fun sendQuestion(json: Json, chatId: Long, question: Question): String {
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage"

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = question.correctAnswer.original,
            replyMarkup = ReplyMarkup(
                listOf(question.variants
                    .mapIndexed { index, word ->
                        InlineKeyboard(
                            text = word.translation,
                            callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"
                        )
                    }
                )
            )
        )

        val requestBodyString = json.encodeToString(requestBody)

        val requestSendQuestion: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val responseSendQuestion: HttpResponse<String> =
            client.send(requestSendQuestion, HttpResponse.BodyHandlers.ofString())

        return responseSendQuestion.body()
    }
}

fun main(args: Array<String>) {

    val botToken = args[0]
    var lastUpdateId = 0L

    val botService = TelegramBotService(botToken)
    val trainer = LearnWordsTrainer()

    val json = Json {
        ignoreUnknownKeys = true
    }

    while (true) {
        Thread.sleep(2000)
        val responseString = botService.getUpdates(lastUpdateId)
        println(responseString)

        val response: Response = json.decodeFromString(responseString)
        val updates = response.result
        val firstUpdate = updates.firstOrNull() ?: continue
        val updateId = firstUpdate.updateId
        lastUpdateId = updateId + 1

        val message = firstUpdate.message?.text
        val chatId: Long = (firstUpdate.message?.chat?.id ?: firstUpdate.callbackQuery?.message?.chat?.id)!!
        val data = firstUpdate.callbackQuery?.data

        if (message == "/start") {
            botService.sendMenu(json, chatId)
        }
        if (data == CALLBACK_STATISTICS) {
            val statistics = trainer.getStatistics()
            val statText = """
                Всего слов в словаре: ${statistics.totalCount}
                Выучено слов: ${statistics.learnedCount}
                Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}
            """.trimIndent()
            botService.sendMessage(json, chatId, statText)
        }
        if (data == CALLBACK_LEARN_WORDS) {
            checkNextQuestionAndSend(json,  trainer, botService, chatId)
        }

        if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) ?: continue) {
            val index = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
            if (trainer.checkAnswer(index)) {
                botService.sendMessage(json, chatId, "Правильно!")
            } else {
                botService.sendMessage(
                    json,
                    chatId,
                    "Неправильно! ${trainer.question?.correctAnswer?.original} это ${trainer.question?.correctAnswer?.translation}"
                )
            }
            checkNextQuestionAndSend(json, trainer, botService, chatId)
        }
    }
}

fun checkNextQuestionAndSend(
    json: Json,
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    chatId: Long
) {
    val question = trainer.getNextQuestion()
    if (question == null) {
        telegramBotService.sendMessage(json, chatId, "Все слова в словаре выучены!")
    } else {
        println(telegramBotService.sendQuestion(json, chatId, question))
    }
}