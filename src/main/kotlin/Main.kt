package org.example

import java.io.File

data class Word(
    val original: String,
    val translation: String,
    var correctAnswersCount: Int = 0
)

fun main() {
    val wordsFile = File("words.txt")
    val dictionary = loadDictionary(wordsFile)
    val totalCount = dictionary.size
    val learnedCount = dictionary.filter {it.correctAnswersCount >= 3}.size
    val percent = if (totalCount == 0) "Словарь пуст!" else "${100 * learnedCount / totalCount}%"

    while (true) {
        println("Меню: \n" +
                "1 – Учить слова\n" +
                "2 – Статистика\n" +
                "0 – Выход")
        print("Введите номер пункта меню: ")
        val choice: Int? = readln().toIntOrNull()
        when (choice) {
            0 -> {
                println("Выходим из программы...")
                return
            }
            1 -> {
                println("Выбран пункт \"Учить слова\"")

                val notLearnedList = dictionary.filter { it.correctAnswersCount < 3}

                while (notLearnedList.isNotEmpty()) {
                    val questionWords: List<Word> = if (notLearnedList.size < 4) {
                        notLearnedList.shuffled().take(minOf(4, notLearnedList.size))
                    } else {
                        notLearnedList.shuffled().take(4)
                    }
                    val correctAnswer = questionWords.random()

                    println("${correctAnswer.original}:")
                    questionWords.forEachIndexed { index, word ->
                        println(" ${index+1} - ${word.translation}")
                    }
                    println(" ----------\n" +
                            " 0 - Меню")
                    print("Введите число от 0 до ${questionWords.size}: ")
                    val userAnswerInput = readln().toIntOrNull()
                    val correctAnswerId = questionWords.indexOf(correctAnswer)

                    when (userAnswerInput) {
                        null -> println("Пожалуйста, введите число.")
                        0 -> break
                        in 1..questionWords.size -> if (userAnswerInput == correctAnswerId + 1) {
                            println("Правильно!")
                            dictionary[dictionary.indexOf(correctAnswer)].correctAnswersCount++

                            fun saveDictionary(dictionary: MutableList<Word>) {
                                wordsFile.writeText("")
                                dictionary.forEach {
                                    wordsFile.appendText("${it.original}|${it.translation}|${it.correctAnswersCount}\n")
                                }
                            }

                            saveDictionary(dictionary)
                        } else {
                            println("Неправильно! ${correctAnswer.original} - это ${correctAnswer.translation}")
                        }
                        else -> continue
                    }
                }
                println("Все слова в словаре выучены!")
            }
            2 -> {
                println("Выбран пункт \"Статистика\"")
                println("""
                    Всего слов в словаре: $totalCount
                    Выучено слов: $learnedCount
                    Выучено $learnedCount из $totalCount слов | $percent
                    
                """.trimIndent())
            }
            else -> println("Введите число 1, 2 или 0")
        }
    }
}

fun loadDictionary(file: File): MutableList<Word> {
    val dictionary = mutableListOf<Word>()
    file.readLines().forEach { line: String ->
        val parts = line.split("|")
        val word = Word(
            original = parts[0],
            translation = parts[1],
            correctAnswersCount = parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
        dictionary.add(word)
    }
    return dictionary
}