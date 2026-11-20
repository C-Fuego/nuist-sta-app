import 'timetable_models.dart';

/// 一条可直接在原生侧排闹钟的上课提醒。
///
/// [at] 用本机墙钟时间（不带时区）序列化，原生按设备本地时区解释；
/// 课表本身也是按本机自然日计算的，两边不会因为时区换算错开。
class ClassReminder {
  const ClassReminder({
    required this.at,
    required this.title,
    required this.body,
    required this.courseId,
    required this.week,
    required this.day,
    required this.start,
    required this.end,
    required this.location,
  });

  final DateTime at;
  final String title;
  final String body;
  final String courseId;

  /// 课程所在教学周（可能超出学期总周数，由调用方决定是否过滤）。
  final int week;

  /// 星期几，1 = 周一 … 7 = 周日。
  final int day;
  final String start;
  final String end;
  final String location;

  Map<String, Object?> toJson() => {
    'at': at.toIso8601String(),
    'title': title,
    'body': body,
    'courseId': courseId,
    'week': week,
    'day': day,
    'start': start,
    'end': end,
    'location': location,
  };
}

({int hour, int minute})? _parseClock(String? value) {
  if (value == null) return null;
  final parts = value.split(':');
  if (parts.length < 2) return null;
  final hour = int.tryParse(parts[0]);
  final minute = int.tryParse(parts[1]);
  if (hour == null || minute == null) return null;
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
  return (hour: hour, minute: minute);
}

/// 生成未来 [horizonDays] 天（含今天）的上课提醒，按触发时间升序。
///
/// 只返回触发时间仍在本机当前时刻之后的提醒；已经到点的不再补排，
/// 避免 App 长时间没打开后一次性弹出一堆过期通知。
List<ClassReminder> buildClassReminderPlan({
  required DateTime now,
  required int horizonDays,
  required int leadMinutes,
  required int? Function(DateTime date) weekOf,
  required List<TodayCourseItem> Function(DateTime date) coursesOnDate,
  required ({String? start, String? end}) Function(
    TodayCourseItem item,
    DateTime date,
  )
  timeOf,
}) {
  if (horizonDays <= 0) return const [];
  final today = DateTime(now.year, now.month, now.day);
  final lead = leadMinutes.clamp(0, 180);
  final reminders = <ClassReminder>[];

  for (var offset = 0; offset < horizonDays; offset++) {
    final date = DateTime(today.year, today.month, today.day + offset);
    final week = weekOf(date);
    // 没有开学日或不在学期内时不排；与首页「明天有没有课」用同一判定。
    if (week == null || week < 1) continue;
    for (final item in coursesOnDate(date)) {
      final time = timeOf(item, date);
      final startClock = _parseClock(time.start);
      // 缺少作息表就算不出真实上课时刻，宁可漏一条也不要排错时间。
      if (startClock == null) continue;
      final startAt = DateTime(
        date.year,
        date.month,
        date.day,
        startClock.hour,
        startClock.minute,
      );
      final at = startAt.subtract(Duration(minutes: lead));
      if (!at.isAfter(now)) continue;

      final slot = item.slot;
      final nodeLabel = slot.startNode == slot.endNode
          ? '第${slot.startNode}节'
          : '第${slot.startNode}–${slot.endNode}节';
      final endText = time.end ?? '';
      final timeLabel = endText.isEmpty
          ? time.start!
          : '${time.start}–$endText';
      final location = slot.location.trim();
      reminders.add(
        ClassReminder(
          at: at,
          title: '即将上课 · ${courseDisplayName(course: item.course, slot: slot)}',
          body: [
            timeLabel,
            nodeLabel,
            if (location.isNotEmpty) location,
          ].join(' · '),
          courseId: item.course.id,
          week: week,
          day: date.weekday,
          start: time.start!,
          end: endText,
          location: location,
        ),
      );
    }
  }

  reminders.sort((a, b) {
    final byTime = a.at.compareTo(b.at);
    return byTime != 0 ? byTime : a.courseId.compareTo(b.courseId);
  });
  return reminders;
}
