package com.example.flashcards.utils

import com.example.flashcards.model.Flashcard
import com.example.flashcards.model.StudySet
import java.util.UUID

object QuizGenerator {
    fun generateQuizSet(quizId: String): StudySet {
        val title = when (quizId) {
            "QUIZ_JA_N5" -> "Trắc nghiệm Tiếng Nhật N5"
            "QUIZ_JA_N4" -> "Trắc nghiệm Tiếng Nhật N4"
            "QUIZ_JA_N3" -> "Trắc nghiệm Tiếng Nhật N3"
            "QUIZ_JA_N2" -> "Trắc nghiệm Tiếng Nhật N2"
            "QUIZ_JA_N1" -> "Trắc nghiệm Tiếng Nhật N1"
            "QUIZ_TOEIC_450" -> "Trắc nghiệm TOEIC 450+"
            "QUIZ_TOEIC_650" -> "Trắc nghiệm TOEIC 650+"
            "QUIZ_TOEIC_800" -> "Trắc nghiệm TOEIC 800+"
            "QUIZ_IELTS_55" -> "Trắc nghiệm IELTS Band 5.5"
            "QUIZ_IELTS_65" -> "Trắc nghiệm IELTS Band 6.5"
            "QUIZ_IELTS_75" -> "Trắc nghiệm IELTS Band 7.5+"
            "QUIZ_ZH_BASIC" -> "Trắc nghiệm HSK Trung Cơ Bản"
            "QUIZ_PA_INTRO" -> "Trắc nghiệm Pali Sơ Cấp"
            else -> "Trắc nghiệm Tổng Hợp"
        }

        val cards = when {
            quizId.startsWith("QUIZ_JA") -> getJapaneseQuestions(quizId)
            quizId.startsWith("QUIZ_TOEIC") -> getToeicQuestions(quizId)
            quizId.startsWith("QUIZ_IELTS") -> getIeltsQuestions(quizId)
            quizId == "QUIZ_ZH_BASIC" -> getChineseQuestions()
            quizId == "QUIZ_PA_INTRO" -> getPaliQuestions()
            else -> getDefaultQuestions()
        }

        return StudySet(
            id = quizId,
            title = title,
            description = "Bộ câu hỏi luyện thi trắc nghiệm thông minh.",
            cards = cards
        )
    }

    private fun getJapaneseQuestions(quizId: String): List<Flashcard> {
        return when (quizId) {
            "QUIZ_JA_N5" -> listOf(
                Flashcard(id = UUID.randomUUID().toString(), question = "食べる (taberu)", answer = "Ăn", explanation = "Động từ nhóm 2 nghĩa là ăn."),
                Flashcard(id = UUID.randomUUID().toString(), question = "飲む (nomu)", answer = "Uống", explanation = "Động từ nhóm 1 nghĩa là uống."),
                Flashcard(id = UUID.randomUUID().toString(), question = "行く (iku)", answer = "Đi", explanation = "Động từ nhóm 1 nghĩa là đi."),
                Flashcard(id = UUID.randomUUID().toString(), question = "見る (miru)", answer = "Xem / Nhìn", explanation = "Động từ nhóm 2 nghĩa là xem, nhìn, quan sát."),
                Flashcard(id = UUID.randomUUID().toString(), question = "先生 (sensei)", answer = "Giáo viên", explanation = "Danh từ chỉ giáo viên, thầy cô giáo.")
            )
            "QUIZ_JA_N4" -> listOf(
                Flashcard(id = UUID.randomUUID().toString(), question = "覚える (oboyeru)", answer = "Nhớ / Ghi nhớ", explanation = "Động từ nhóm 2 nghĩa là nhớ, học thuộc."),
                Flashcard(id = UUID.randomUUID().toString(), question = "簡単 (kantan)", answer = "Đơn giản / Dễ dàng", explanation = "Tính từ đuôi na nghĩa là đơn giản."),
                Flashcard(id = UUID.randomUUID().toString(), question = "重い (omoi)", answer = "Nặng", explanation = "Tính từ đuôi i nghĩa là nặng (trọng lượng)."),
                Flashcard(id = UUID.randomUUID().toString(), question = "軽い (karui)", answer = "Nhẹ", explanation = "Tính từ đuôi i nghĩa là nhẹ.")
            )
            else -> listOf(
                Flashcard(id = UUID.randomUUID().toString(), question = "一生懸命 (isshoukenmei)", answer = "Nỗ lực hết sức", explanation = "Cụm từ chỉ sự cố gắng, nỗ lực hết mình."),
                Flashcard(id = UUID.randomUUID().toString(), question = "調査 (chousa)", answer = "Điều tra / Khảo sát", explanation = "Danh từ chỉ cuộc khảo sát, nghiên cứu."),
                Flashcard(id = UUID.randomUUID().toString(), question = "緊張 (kinchou)", answer = "Căng thẳng / Hồi hộp", explanation = "Trạng thái tâm lý lo lắng, hồi hộp.")
            )
        }
    }

    private fun getToeicQuestions(quizId: String): List<Flashcard> {
        return when (quizId) {
            "QUIZ_TOEIC_450" -> listOf(
                Flashcard(id = UUID.randomUUID().toString(), question = "Confirm", answer = "Xác nhận", explanation = "To verify or make sure of something."),
                Flashcard(id = UUID.randomUUID().toString(), question = "Submit", answer = "Nộp / Trình", explanation = "To hand in a document or proposal."),
                Flashcard(id = UUID.randomUUID().toString(), question = "Delay", answer = "Trì hoãn", explanation = "To postpone or make late.")
            )
            else -> listOf(
                Flashcard(id = UUID.randomUUID().toString(), question = "Negotiate", answer = "Thương lượng / Đàm phán", explanation = "To discuss to reach an agreement."),
                Flashcard(id = UUID.randomUUID().toString(), question = "Implement", answer = "Thi hành / Thực hiện", explanation = "To put a decision or plan into effect."),
                Flashcard(id = UUID.randomUUID().toString(), question = "Collaborate", answer = "Hợp tác", explanation = "To work together with others.")
            )
        }
    }

    private fun getIeltsQuestions(quizId: String): List<Flashcard> {
        return listOf(
            Flashcard(id = UUID.randomUUID().toString(), question = "Analyze", answer = "Phân tích", explanation = "To examine in detail."),
            Flashcard(id = UUID.randomUUID().toString(), question = "Synthesize", answer = "Tổng hợp", explanation = "To combine various elements into a whole."),
            Flashcard(id = UUID.randomUUID().toString(), question = "Hypothesis", answer = "Giả thuyết", explanation = "A proposed explanation based on limited evidence.")
        )
    }

    private fun getChineseQuestions(): List<Flashcard> {
        return listOf(
            Flashcard(id = UUID.randomUUID().toString(), question = "你好 (nǐ hǎo)", answer = "Xin chào", explanation = "Lời chào cơ bản trong tiếng Trung."),
            Flashcard(id = UUID.randomUUID().toString(), question = "谢谢 (xièxie)", answer = "Cảm ơn", explanation = "Lời cảm ơn."),
            Flashcard(id = UUID.randomUUID().toString(), question = "再见 (zàijiàn)", answer = "Tạm biệt", explanation = "Lời chào tạm biệt.")
        )
    }

    private fun getPaliQuestions(): List<Flashcard> {
        return listOf(
            Flashcard(id = UUID.randomUUID().toString(), question = "Buddha", answer = "Đức Phật / Bậc Giác Ngộ", explanation = "Bậc đã tự mình giác ngộ hoàn toàn."),
            Flashcard(id = UUID.randomUUID().toString(), question = "Dhamma", answer = "Giáo Pháp", explanation = "Lời dạy của Đức Phật hoặc chân lý vũ trụ."),
            Flashcard(id = UUID.randomUUID().toString(), question = "Sangha", answer = "Tăng Đoàn", explanation = "Cộng đồng những bậc tu hành xuất gia.")
        )
    }

    private fun getDefaultQuestions(): List<Flashcard> {
        return listOf(
            Flashcard(id = UUID.randomUUID().toString(), question = "Hello", answer = "Xin chào", explanation = "English greeting.")
        )
    }
}
