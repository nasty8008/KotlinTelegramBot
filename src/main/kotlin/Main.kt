package org.example

import java.io.File

data class Word(
    val original: String,
    val translation: String,
    var correctAnswersCount: Int = 0
)

fun main() {
    val wordsFile = File("words.txt")
    val dictionary = mutableListOf<Word>()
    wordsFile.readLines().forEach { line: String ->
        val parts = line.split("|")
        val word = Word(
            original = parts[0],
            translation = parts[1],
            correctAnswersCount = parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
        dictionary.add(word)
    }
    dictionary.forEach { word ->
        println(word)
    }
}