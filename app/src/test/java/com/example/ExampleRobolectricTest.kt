package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.algorithm.EbbinghausEngine
import com.example.data.model.CardStatus
import com.example.data.model.FlashcardEntity
import com.example.data.model.ReviewGrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Эббингауз", appName)
  }

  @Test
  fun `ebbinghaus engine bad rating resets streak and marks heavy`() {
    val card = FlashcardEntity(
      deckId = 1L,
      question = "Тестовый вопрос",
      answer = "Тестовый ответ",
      repetitionCount = 3,
      easeFactor = 2.5,
      intervalDays = 3.0
    )

    val result = EbbinghausEngine.processReview(card, ReviewGrade.BAD)
    assertEquals(0, result.updatedCard.repetitionCount)
    assertTrue(result.updatedCard.isHeavy)
    assertEquals(CardStatus.LEARNING, result.updatedCard.status)
    assertTrue(result.nextIntervalDays < 0.1) // 15 mins
  }

  @Test
  fun `ebbinghaus engine normal rating increases interval`() {
    val card = FlashcardEntity(
      deckId = 1L,
      question = "Вопрос 2",
      answer = "Ответ 2",
      repetitionCount = 1,
      easeFactor = 2.5,
      intervalDays = 0.3
    )

    val result = EbbinghausEngine.processReview(card, ReviewGrade.NORMAL)
    assertEquals(2, result.updatedCard.repetitionCount)
    assertEquals(1.0, result.updatedCard.intervalDays, 0.01)
  }

  @Test
  fun `ebbinghaus engine excellent rating jumps ahead`() {
    val card = FlashcardEntity(
      deckId = 1L,
      question = "Вопрос 3",
      answer = "Ответ 3",
      repetitionCount = 1,
      easeFactor = 2.5,
      intervalDays = 0.3
    )

    val result = EbbinghausEngine.processReview(card, ReviewGrade.EXCELLENT)
    assertEquals(2, result.updatedCard.repetitionCount)
    assertEquals(3.5, result.updatedCard.intervalDays, 0.01)
  }
}
