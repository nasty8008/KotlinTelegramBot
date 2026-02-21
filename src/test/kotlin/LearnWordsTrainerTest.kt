import org.example.LearnWordsTrainer
import org.example.Statistics
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LearnWordsTrainerTest {

    @Test
    fun `test statistics with 4 words of 7`() {
        val trainer = LearnWordsTrainer("src/test/4_words_of_7.txt")
        assertEquals(
            Statistics(totalCount = 7, learnedCount = 4, percent = "57%"),
            trainer.getStatistics()
        )
    }

    @Test
    fun `test statistics with corrupted file`() {
        val trainer = LearnWordsTrainer("src/test/corrupt_file.txt")
        assertEquals(
            Statistics(totalCount = 0, learnedCount = 0, percent = "Словарь пуст!"),
            trainer.getStatistics()
        )
    }

    @Test
    fun `test getNextQuestion() with 5 unlearned words`() {
        val trainer = LearnWordsTrainer("src/test/2_words_of_7.txt")
        val question = trainer.getNextQuestion()

        assertNotNull(question)
        assertEquals(4, question.variants.size)

        assertTrue(question.variants.contains(question.correctAnswer))
    }

    @Test
    fun `test getNextQuestion() with 1 unlearned word`() {
        val trainer = LearnWordsTrainer("src/test/1_unlearned.txt")

        val question = trainer.getNextQuestion()
        assertNotNull(question)
        assertEquals(4, question.variants.size)

        val notLearned = question.variants.filter { it.correctAnswersCount < 3 }
        assertEquals(1, notLearned.size)

        assertTrue(question.variants.contains(question.correctAnswer))
    }

    @Test
    fun `test getNextQuestion() with all words learned`() {
        val trainer = LearnWordsTrainer("src/test/all_words_learned.txt")
        assertEquals(
            null,
            trainer.getNextQuestion()
        )
    }

    @Test
    fun `test checkAnswer() with true`() {
        val trainer = LearnWordsTrainer("src/test/4_words_of_7.txt")

        val question = trainer.getNextQuestion()
        assertNotNull(question)

        val correctIndex = question.variants.indexOf(question.correctAnswer)

        val result = trainer.checkAnswer(correctIndex)

        assertTrue(result)
        assertEquals(1, question.correctAnswer.correctAnswersCount)
    }

    @Test
    fun `test checkAnswer() with false`() {
        val trainer = LearnWordsTrainer("src/test/4_words_of_7.txt")

        val question = trainer.getNextQuestion()
        assertNotNull(question)

        val correctIndex = question.variants.indexOf(question.correctAnswer)

        val wrongIndex = question.variants.indices.first { it != correctIndex }

        val result = trainer.checkAnswer(wrongIndex)

        assertFalse(result)
        assertEquals(0, question.correctAnswer.correctAnswersCount)
    }

    @Test
        fun `test resetProgress() with 2 words in dictionary`() {
        val trainer = LearnWordsTrainer("src/test/reset_test.txt")

        val statsBefore = trainer.getStatistics()
        assertTrue(statsBefore.learnedCount > 0)

        trainer.resetProgress()

        val statsAfter = trainer.getStatistics()
        assertEquals(0, statsAfter.learnedCount)
        assertEquals(statsBefore.totalCount, statsAfter.totalCount)
    }
}