package org.example

import java.io.File

data class Word(
    val original: String,
    val translation: String,
    var correctAnswersCount: Int? = 0
)

fun main() {
    val wordsFile = File("words.txt")
    val dictionary = mutableListOf<Word>()
    wordsFile.readLines().forEach { line: String ->
        val line = line.split("|")
        val word = Word(
            original = line[0],
            translation = line[1],
            correctAnswersCount = line.getOrNull(2)?.toIntOrNull() ?: 0
        )
        dictionary.add(word)
    }
    dictionary.forEach { word ->
        println(word)
    }
}