package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Random

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val BOT_FILE_URL = "https://api.telegram.org/file/bot"
const val CALLBACK_LEARN_WORDS = "learn_words_clicked"
const val CALLBACK_STATISTICS = "statistics_clicked"
const val CALLBACK_RESET_PROGRESS = "reset_progress_clicked"
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
data class Document(
    @SerialName("file_name")
    val fileName: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
)

@Serializable
data class Message(
    @SerialName("message_id")
    val messageId: Long? = null,
    @SerialName("text")
    val text: String? = null,
    @SerialName("chat")
    val chat: Chat,
    @SerialName("document")
    val document: Document? = null,
    @SerialName("photo")
    val photo: List<PhotoSize>? = null
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
data class GetFileRequest(
    @SerialName("file_id")
    val fileId: String
)

@Serializable
data class GetFileResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("result")
    val result: TelegramFile? = null,
)

@Serializable
data class TelegramFile(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
    @SerialName("file_path")
    val filePath: String,
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

@Serializable
data class PhotoSize(
    @SerialName("file_id")
    val fileId: String
)

@Serializable
data class SendPhotoResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("result")
    val result: Message
)

@Serializable
data class SendMessageResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("result")
    val result: Message? = null,
)

@Serializable
data class EditMessageTextRequest(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("message_id")
    val messageId: Long,
    @SerialName("text")
    val text: String,
)

class DynamicMessage {
    private val messageIdByChatId = mutableMapOf<Long, Long>()

    fun setMessageId(chatId: Long, messageId: Long) {
        messageIdByChatId[chatId] = messageId
    }

    fun getMessageId(chatId: Long): Long? = messageIdByChatId[chatId]
}

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

    fun editMessage(json: Json, chatId: Long, messageId: Long, text: String): Boolean {
        return try {
            val url = "$TELEGRAM_BASE_URL$botToken/editMessageText"
            val requestBody = EditMessageTextRequest(
                chatId = chatId,
                messageId = messageId,
                text = text,
            )
            val requestBodyString = json.encodeToString(requestBody)
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val jsonResponse = json.decodeFromString<SendMessageResponse>(response.body())
            jsonResponse.ok
        } catch (e: Exception) {
            when {
                e.message?.contains("MESSAGE_NOT_MODIFIED") == true -> {
                    println("Текст не изменился")
                    true
                }
                e.message?.contains("MESSAGE_EDIT_TIME_EXPIRED") == true -> {
                    println("Время редактирования истекло")
                    false
                }
                else -> {
                    println("Ошибка редактирования: ${e.message}")
                    false
                }
            }
        }
    }

    fun sendMenu(json: Json, chatId: Long): String {
        val urlSendMessage = "$TELEGRAM_BASE_URL$botToken/sendMessage"

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                inlineKeyboard = listOf(
                    listOf(
                        InlineKeyboard(text = "Изучить слова", callbackData = CALLBACK_LEARN_WORDS),
                        InlineKeyboard(text = "Статистика", callbackData = CALLBACK_STATISTICS)
                    ),
                    listOf(
                        InlineKeyboard(text = "Сбросить статистику", callbackData = CALLBACK_RESET_PROGRESS)
                    )
                )
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
                listOf(
                    question.variants
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

    fun getFile(fileId: String, json: Json): String {
        val urlGetFile = "$TELEGRAM_BASE_URL$botToken/getFile"
        val requestBody = GetFileRequest(fileId = fileId)
        val requestBodyString = json.encodeToString(requestBody)
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(urlGetFile))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        )
        return response.body()
    }

    fun downloadFile(filePath: String, fileName: String) {
        val urlGetFile = "$BOT_FILE_URL$botToken/$filePath"
        println(urlGetFile)
        val request = HttpRequest
            .newBuilder()
            .uri(URI.create(urlGetFile))
            .GET()
            .build()

        val response: HttpResponse<InputStream> = HttpClient
            .newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofInputStream())

        println("status code: " + response.statusCode())

        response.body().use { inputStream ->
            File(fileName).outputStream().use { outputStream ->
                inputStream.copyTo(outputStream, 16 * 1024)
            }
        }
    }

    fun sendPhoto(file: File, chatId: Long, json: Json, hasSpoiler: Boolean = true): String? {
        val data: MutableMap<String, Any> = LinkedHashMap()
        data["chat_id"] = chatId.toString()
        data["photo"] = file
        data["has_spoiler"] = hasSpoiler
        val boundary: String = BigInteger(35, Random()).toString()

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$TELEGRAM_BASE_URL$botToken/sendPhoto"))
            .postMultipartFormData(boundary, data)
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val parsed = json.decodeFromString<SendPhotoResponse>(response.body())

        return parsed.result.photo?.lastOrNull()?.fileId
    }

    fun sendPhotoByFileId(chatId: Long, fileId: String, hasSpoiler: Boolean = true): String {
        val data: MutableMap<String, Any> = LinkedHashMap()
        data["chat_id"] = chatId.toString()
        data["photo"] = fileId
        data["has_spoiler"] = hasSpoiler
        val boundary: String = BigInteger(35, Random()).toString()

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$TELEGRAM_BASE_URL$botToken/sendPhoto"))
            .postMultipartFormData(boundary, data)
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
}

private fun HttpRequest.Builder.postMultipartFormData(boundary: String, data: Map<String, Any>): HttpRequest.Builder {
    val byteArrays = ArrayList<ByteArray>()
    val separator = "--$boundary\r\nContent-Disposition: form-data; name=".toByteArray(StandardCharsets.UTF_8)

    for (entry in data.entries) {
        byteArrays.add(separator)
        when (entry.value) {
            is File -> {
                val file = entry.value as File
                val path = Path.of(file.toURI())
                val mimeType = Files.probeContentType(path)
                byteArrays.add(
                    "\"${entry.key}\"; filename=\"${path.fileName}\"\r\nContent-Type: $mimeType\r\n\r\n".toByteArray(
                        StandardCharsets.UTF_8
                    )
                )
                byteArrays.add(Files.readAllBytes(path))
                byteArrays.add("\r\n".toByteArray(StandardCharsets.UTF_8))
            }

            else -> byteArrays.add("\"${entry.key}\"\r\n\r\n${entry.value}\r\n".toByteArray(StandardCharsets.UTF_8))
        }
    }
    byteArrays.add("--$boundary--".toByteArray(StandardCharsets.UTF_8))

    this.header("Content-Type", "multipart/form-data;boundary=$boundary")
        .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))
    return this
}

fun main(args: Array<String>) {

    val botToken = args[0]
    var lastUpdateId = 0L

    val botService = TelegramBotService(botToken)
    val trainers = HashMap<Long, LearnWordsTrainer>()
    val statisticsMessages = DynamicMessage()

    val json = Json {
        ignoreUnknownKeys = true
    }

    while (true) {
        Thread.sleep(2000)
        val responseString = botService.getUpdates(lastUpdateId)
        println(responseString)

        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdate(it, json, botService, trainers, statisticsMessages) }
        lastUpdateId = sortedUpdates.last().updateId + 1
    }
}

fun formatStatisticsText(trainer: LearnWordsTrainer): String {
    val statistics = trainer.getStatistics()
    val percent = if (statistics.totalCount == 0) 0 else 100 * statistics.learnedCount / statistics.totalCount
    val bar = "█".repeat(percent / 10) + "▒".repeat(10 - percent / 10)
    return """
                Всего слов в словаре: ${statistics.totalCount}
                Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}
                Прогресс изучения: $percent%
                [$bar]
            """.trimIndent()
}

fun handleUpdate(
    update: Update,
    json: Json,
    botService: TelegramBotService,
    trainers: HashMap<Long, LearnWordsTrainer>,
    statisticsMessages: DynamicMessage,
) {

    val message = update.message?.text
    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
    val data = update.callbackQuery?.data
    val document = update.message?.document

    val trainer = trainers.getOrPut(chatId) { LearnWordsTrainer("$chatId.txt") }

    if (message == "/start") {
        botService.sendMenu(json, chatId)
    }

    if (document != null) {
        val jsonResponse = botService.getFile(document.fileId, json)
        val response: GetFileResponse = json.decodeFromString(jsonResponse)
        response.result?.let {
            val fileName = document.fileUniqueId
            val localFile = File(fileName)

            if (!localFile.exists()) {
                botService.downloadFile(it.filePath, fileName)
            }

            trainer.loadWordsFromFile(localFile)
        }
    }

    if (data == CALLBACK_STATISTICS) {
        val statText = formatStatisticsText(trainer)
        val responseBody = botService.sendMessage(json, chatId, statText)
        val parsed = runCatching { json.decodeFromString<SendMessageResponse>(responseBody) }.getOrNull()
        if (parsed?.ok == true) {
            parsed.result?.messageId?.let { statisticsMessages.setMessageId(chatId, it) }
        }
    }

    if (data == CALLBACK_RESET_PROGRESS) {
        trainer.resetProgress()
        botService.sendMessage(json, chatId, "Прогресс сброшен")
    }

    if (data == CALLBACK_LEARN_WORDS) {
        checkNextQuestionAndSend(json, trainer, botService, chatId)
    }

    if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) ?: return) {
        val index = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
        if (trainer.checkAnswer(index)) {
            botService.sendMessage(json, chatId, "Правильно!")
            statisticsMessages.getMessageId(chatId)?.let { messageId ->
                botService.editMessage(json, chatId, messageId, formatStatisticsText(trainer))
            }
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

fun checkNextQuestionAndSend(
    json: Json,
    trainer: LearnWordsTrainer,
    telegramBotService: TelegramBotService,
    chatId: Long
) {
    val question = trainer.getNextQuestion()

    if (question == null) {
        telegramBotService.sendMessage(json, chatId, "Все слова в словаре выучены!")
        return
    }

    if (!question.correctAnswer.fileId.isNullOrEmpty()) {
        telegramBotService.sendPhotoByFileId(chatId, question.correctAnswer.fileId ?: return)
    } else {
        val imagePath = question.correctAnswer.imagePath

        if (imagePath != null) {
            val file = File(imagePath)
            if (file.exists()) {
                val fileId = telegramBotService.sendPhoto(file, chatId, json)
                question.correctAnswer.fileId = fileId
            }
        }
    }

    telegramBotService.sendQuestion(json, chatId, question)
}