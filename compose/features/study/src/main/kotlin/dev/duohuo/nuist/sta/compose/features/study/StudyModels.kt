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

object StudyMock {
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
