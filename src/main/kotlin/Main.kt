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
                return}
            1 -> println("Выбран пункт \"Учить слова\"")
            2 -> println("Выбран пункт \"Статистика\"")
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