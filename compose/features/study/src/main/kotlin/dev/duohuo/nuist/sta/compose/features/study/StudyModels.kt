package dev.duohuo.nuist.sta.compose.features.study

/** 学业概览（对应 Flutter `AcademicSummary` 的展示子集）。 */
data class AcademicSummary(
    val averageGradePoint: String,
    val gpa: String,
    val averageScore: String,
    val earnedCredits: String,
    val requiredCredits: String,
    val classRank: String,
    val majorRank: String,
    val passRate: String,
    val weightedAverageScore: String,
) {
    /** 学分进度 0..1。 */
    val creditProgress: Float
        get() {
            val earned = earnedCredits.toDoubleOrNull() ?: 0.0
            val required = requiredCredits.toDoubleOrNull() ?: 0.0
            if (required <= 0) return 0f
            return (earned / required).toFloat().coerceIn(0f, 1f)
        }
}

/** 学生身份 + 学期信息（对应 Flutter `PortalUser` 子集）。 */
data class StudentInfo(
    val name: String,
    val studentId: String,
    val orgLine: String,
    val semester: String,
    val week: Int,
    val totalWeeks: Int,
)

/** 一个学期：学期名 + 该学期学业概览 + 绩点走势。 */
data class StudyTerm(
    val label: String,
    val academic: AcademicSummary,
    val trendLabels: List<String>,
    val trendValues: List<Float>,
)

object StudyMock {
    /** 可选学期（新→旧），第一项为当前学期。 */
    val terms = listOf(
        StudyTerm(
            label = "2026-2027 秋",
            academic = AcademicSummary(
                averageGradePoint = "3.86", gpa = "3.92", averageScore = "88.4",
                earnedCredits = "128", requiredCredits = "160",
                classRank = "3 / 42", majorRank = "12 / 210",
                passRate = "98.6%", weightedAverageScore = "89.2",
            ),
            trendLabels = listOf("2025 秋", "2026 春", "2026 秋"),
            trendValues = listOf(3.71f, 3.84f, 3.92f),
        ),
        StudyTerm(
            label = "2025-2026 春",
            academic = AcademicSummary(
                averageGradePoint = "3.78", gpa = "3.84", averageScore = "87.1",
                earnedCredits = "104", requiredCredits = "160",
                classRank = "5 / 42", majorRank = "18 / 210",
                passRate = "97.9%", weightedAverageScore = "88.0",
            ),
            trendLabels = listOf("2025 秋", "2026 春"),
            trendValues = listOf(3.71f, 3.84f),
        ),
        StudyTerm(
            label = "2025-2026 秋",
            academic = AcademicSummary(
                averageGradePoint = "3.62", gpa = "3.71", averageScore = "85.6",
                earnedCredits = "72", requiredCredits = "160",
                classRank = "9 / 42", majorRank = "31 / 210",
                passRate = "96.8%", weightedAverageScore = "86.4",
            ),
            trendLabels = listOf("2025 秋"),
            trendValues = listOf(3.71f),
        ),
    )

    val academic = AcademicSummary(
        averageGradePoint = "3.86",
        gpa = "3.92",
        averageScore = "88.4",
        earnedCredits = "128",
        requiredCredits = "160",
        classRank = "3 / 42",
        majorRank = "12 / 210",
        passRate = "98.6%",
        weightedAverageScore = "89.2",
    )
    val student = StudentInfo(
        name = "张同学",
        studentId = "2025****0001",
        orgLine = "计算机学院 · 软件工程 2025 级 1 班",
        semester = "2026-2027 学年 秋季学期",
        week = 12,
        totalWeeks = 18,
    )
}
