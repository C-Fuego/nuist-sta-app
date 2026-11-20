package dev.duohuo.nuist.sta.compose.ui.page.nuist.score

/** 一条课程成绩。UI 阶段字段与 Flutter `ScoreRecord` 对齐（截取展示所需）。 */
data class ScoreRecord(
    val courseName: String,
    val courseCode: String,
    val score: String,
    val credit: String,
    val gradePoint: String,
    val passStatus: String,
    val examType: String,
    val usualScore: String,
    val finalScore: String,
    val retakeType: String,
    val term: String = "",
) {
    val numericScore: Double?
        get() = score.replace("分", "").trim().toDoubleOrNull()
    val passed: Boolean
        get() = when {
            numericScore != null -> numericScore!! >= 60
            // 等级制成绩：优秀/良好/中等/合格/及格 均视为通过
            PASS_GRADE_PREFIXES.any { score.contains(it) } -> true
            else -> passStatus == "通过"
        }
    val failed: Boolean
        get() = when {
            numericScore != null -> numericScore!! < 60
            score.contains("不及格") || score.contains("不合格") -> true
            else -> false
        }
}

/** 等级制成绩里表示"通过"的字样。 */
private val PASS_GRADE_PREFIXES = listOf("优秀", "良好", "中等", "合格", "及格")

/** 一个学期的汇总。 */
data class ScoreTermSummary(
    val total: Int,
    val passed: Int,
    val averageScore: Double?,
    val credits: Double,
)

/** 一份成绩单（某学期）。 */
data class ScoreReport(
    val term: String,
    val records: List<ScoreRecord>,
) {
    val summary: ScoreTermSummary
        get() {
            val avg = records.mapNotNull { it.numericScore }.takeIf { it.isNotEmpty() }?.average()
            return ScoreTermSummary(
                total = records.size,
                passed = records.count { it.passed },
                averageScore = avg,
                credits = records.sumOf { it.credit.toDoubleOrNull() ?: 0.0 },
            )
        }
}

object ScoreMock {
    val terms = listOf("2026-2027 秋", "2025-2026 春", "2025-2026 秋")

    private val autumn = listOf(
        rec("高等数学 A", "MATH1001", "92", "5", "4.2", "考试"),
        rec("大学物理", "PHYS1002", "87", "4", "3.7", "考试"),
        rec("程序设计基础", "CS1001", "95", "4", "4.5", "考试", usual = "96", final = "94"),
        rec("体育", "PE1001", "合格", "1", "", "考查"),
        rec("思想道德与法治", "MARX1001", "78", "3", "2.8", "考试"),
        rec("线性代数", "MATH1002", "54", "3", "0", "考试", retake = "初修"),
    )

    fun report(term: String): ScoreReport = ScoreReport(
        term = term,
        records = when (term) {
            terms[0] -> autumn
            else -> autumn.take(4)
        },
    )

    private fun rec(
        name: String, code: String, score: String, credit: String, gp: String,
        exam: String, usual: String = "", final: String = "", retake: String = "",
    ) = ScoreRecord(
        courseName = name, courseCode = code, score = score, credit = credit,
        gradePoint = gp, passStatus = if ((score.toDoubleOrNull() ?: 100.0) < 60) "不及格" else "通过",
        examType = exam, usualScore = usual, finalScore = final, retakeType = retake,
    )
}
