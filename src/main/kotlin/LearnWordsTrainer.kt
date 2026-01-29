package org.example

import java.io.File

data class Statistics(
    val totalCount: Int,
    val learnedCount: Int,
    val percent: String,
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)

class LearnWordsTrainer {

    private var question: Question? = null
    private val dictionary = loadDictionary()

    fun getStatistics(): Statistics {
        val totalCount = dictionary.size
        val learnedCount = dictionary.filter { it.correctAnswersCount >= CORRECT_ANSWERS }.size
        val percent = if (totalCount == 0) "Словарь пуст!" else "${100 * learnedCount / totalCount}%"
        return Statistics(totalCount = totalCount, learnedCount = learnedCount, percent = percent)
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < CORRECT_ANSWERS }
        if (notLearnedList.isEmpty()) return null
        val questionWords: List<Word> = if (notLearnedList.size < MAX_OPTIONS) {
            notLearnedList.shuffled().take(minOf(MAX_OPTIONS, notLearnedList.size))
        } else {
            notLearnedList.shuffled().take(MAX_OPTIONS)
        }
        val correctAnswer = questionWords.random()

        question = Question(variants = questionWords, correctAnswer = correctAnswer)
        return question
    }

    fun checkAnswer(userAnswerInput: Int?): Boolean {
        return question?.let {
            val correctAnswerId = it.variants.indexOf(it.correctAnswer)
            if (correctAnswerId == userAnswerInput) {
                it.correctAnswer.correctAnswersCount++
                saveDictionary(dictionary)
                true
            } else {
                false
            }
        } ?: false
    }

    private fun loadDictionary(): MutableList<Word> {
        val dictionary = mutableListOf<Word>()
        val wordsFile = File("words.txt")
        wordsFile.readLines().forEach { line: String ->
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

    private fun saveDictionary(dictionary: MutableList<Word>) {
        val wordsFile = File("words.txt")
        wordsFile.writeText("")
        dictionary.forEach {
            wordsFile.appendText("${it.original}|${it.translation}|${it.correctAnswersCount}\n")
        }
    }
}