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
    assertEquals("Hermann Memo", appName)
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

  @Test
  fun `test study session preparation and card progression`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repo = com.example.data.repository.StudyRepository(db.deckDao(), db.flashcardDao(), db.dailyQuotaDao())
    repo.initializePreloadIfEmpty()
    val session = repo.prepareStudySession(1L)
    assertTrue("Cards should not be empty", session.cards.isNotEmpty())
    if (session.cards.size > 1) {
      org.junit.Assert.assertNotEquals(session.cards[0].question, session.cards[1].question)
    }

    val vm = com.example.ui.viewmodel.MainViewModel(context as android.app.Application)
    vm.startStudySession(1L, "English")
    kotlinx.coroutines.delay(200)

    val card0 = vm.currentCard()
    println("CARD 0: id=${card0?.id}, q=${card0?.question}")
    val q0 = card0?.question

    vm.submitCardRating(ReviewGrade.NORMAL)
    kotlinx.coroutines.delay(200)

    val card1 = vm.currentCard()
    println("CARD 1: id=${card1?.id}, q=${card1?.question}")
    val q1 = card1?.question

    vm.submitCardRating(ReviewGrade.NORMAL)
    kotlinx.coroutines.delay(200)

    val card2 = vm.currentCard()
    println("CARD 2: id=${card2?.id}, q=${card2?.question}")
    val q2 = card2?.question

    org.junit.Assert.assertNotEquals("Card 0 and Card 1 question must be different", q0, q1)
    org.junit.Assert.assertNotEquals("Card 1 and Card 2 question must be different", q1, q2)
  }
}
