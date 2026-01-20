package org.example

import java.io.File

fun main() {
    val wordsFile = File("words.txt")
    wordsFile.writeText(
        "hello привет\n" +
                "dog собака\n" +
                "cat кошка"
    )
    wordsFile.readLines().forEach {
        println(it)
    }
}