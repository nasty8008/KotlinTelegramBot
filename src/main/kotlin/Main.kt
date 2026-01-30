package org.example

data class Word(
    val original: String,
    val translation: String,
    var correctAnswersCount: Int = 0,
)

fun Question.asConsoleString(): String {
    val variants = this.variants.mapIndexed { index, word ->
            " ${index + 1} - ${word.translation}"
        }.joinToString(separator = "\n")
    return this.correctAnswer.original + "\n" + variants + "\n ----------\n" + " 0 - Меню"
}

fun main() {

    val trainer = try {
        LearnWordsTrainer()
    } catch (e: Exception) {
        println("Невозможно загрузить файл")
        return
    }

    while (true) {
        println(
            "Меню: \n" + "1 – Учить слова\n" + "2 – Статистика\n" + "0 – Выход"
        )
        print("Введите номер пункта меню: ")
        val choice: Int? = readln().toIntOrNull()
        when (choice) {
            0 -> {
                println("Выходим из программы...")
                return
            }

            1 -> {
                println("Выбран пункт \"Учить слова\"")
                while (true) {
                    val question = trainer.getNextQuestion()

                    if (question != null) {

                        println(question.asConsoleString())
                        print("Введите число от 0 до ${question.variants.size}: ")

                        when (val userAnswerInput = readln().toIntOrNull()) {
                            null -> println("Пожалуйста, введите число.")
                            0 -> break
                            in 1..question.variants.size -> if (trainer.checkAnswer(userAnswerInput.minus(1))) {
                                println("Правильно!")
                            } else {
                                println("Неправильно! ${question.correctAnswer.original} - это ${question.correctAnswer.translation}")
                            }

                            else -> continue
                        }

                    } else {
                        println("Все слова в словаре выучены!")
                        break
                    }
                }
            }

            2 -> {
                val statistics = trainer.getStatistics()
                println("Выбран пункт \"Статистика\"")
                println(
                    "Всего слов в словаре: ${statistics.totalCount}\n"
                            + "Выучено слов: ${statistics.learnedCount}\n"
                            + "Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}"
                )
            }

            else -> println("Введите число 1, 2 или 0")
        }
    }
}