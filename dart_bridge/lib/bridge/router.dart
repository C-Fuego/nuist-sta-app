// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
//
// 方法表：把参考实现里已有的 Dart 能力暴露给 Compose 前端。
// 这里只做「调用 + 转 JSON」，不实现任何校园网协议逻辑。

import 'package:flutter/foundation.dart';

import '../core/auth/passkey_bundle.dart';
import '../core/auth/passkey_store.dart';
import '../core/auth/passkey_transfer.dart';
import '../core/auth/student_id_mask.dart';
import '../core/auth/portal_session.dart';
import '../core/config/campus_config.dart';
import '../core/network/vpn_gateway.dart';
import '../core/network/vpn_native.dart';
import '../core/logging/diagnostic_error.dart';
import '../mini_apps/academic/academic_api.dart';
import '../mini_apps/bulletin/bulletin_api.dart';
import '../mini_apps/bulletin/bulletin_models.dart';
import '../mini_apps/electricity/electricity_api.dart';
import '../mini_apps/electricity/electricity_models.dart';
import '../mini_apps/free_classroom/free_classroom_api.dart';
import '../mini_apps/innovation_credit/innovation_credit_api.dart';
import '../mini_apps/labor_score/labor_score_api.dart';
import '../mini_apps/score/score_api.dart';
import '../mini_apps/student_info/student_info_api.dart';
import '../mini_apps/timetable/timetable_api.dart';
import '../mini_apps/timetable/timetable_change_format.dart';
import '../mini_apps/timetable/timetable_controller.dart';
import '../mini_apps/timetable/timetable_date_edits.dart';
import '../mini_apps/timetable/timetable_sync_diff.dart';
import '../mini_apps/timetable/timetable_export_api.dart';
import '../mini_apps/timetable/timetable_export_models.dart';
import '../mini_apps/timetable/timetable_colors.dart';
import '../mini_apps/timetable/timetable_models.dart';
import '../mini_apps/timetable/timetable_overrides.dart';
import '../mini_apps/timetable/timetable_store.dart';
import '../mini_apps/timetable/timetable_week_format.dart';
import '../mini_apps/timetable/timetable_schedule_event.dart';

/// 处理一次桥调用；异常统一转成 `{ok: false, error: {...}}`，不往通道里抛。
Future<Map<String, Object?>> handle(
  String method,
  Map<String, dynamic> args,
) async {
  try {
    return <String, Object?>{'ok': true, 'data': await _dispatch(method, args)};
  } catch (error, stack) {
    return <String, Object?>{'ok': false, 'error': _describe(error, stack)};
  }
}

Future<Object?> _dispatch(String method, Map<String, dynamic> args) async {
  switch (method) {
    case 'ping':
      return <String, Object?>{'pkg': 'nuist_bridge'};

    case 'campus':
      return <String, Object?>{
        'rootDomain': kCampusRootDomain,
        'bulletinBase': 'https://${CampusHosts.bulletin}/',
        'authserver': CampusHosts.authserver,
        'jwxt': CampusHosts.jwxt,
        'icard': CampusHosts.icard,
        'iportal': CampusHosts.iportal,
        'labor': CampusHosts.labor,
        'cxcyjy': CampusHosts.cxcyjy,
        'vpnController': CampusHosts.vpnController,
      };

    case 'vpn.prepare':
      {
        await VpnGateway.instance.prepare();
        return <String, Object?>{'sessionId': VpnGateway.instance.sessionId};
      }

    case 'vpn.reset':
      {
        await VpnGateway.instance.reset();
        return <String, Object?>{'sessionId': VpnGateway.instance.sessionId};
      }

    case 'vpn.networkChanged':
      {
        await VpnGateway.instance.networkChanged();
        return <String, Object?>{'sessionId': VpnGateway.instance.sessionId};
      }

    case 'vpn.status':
      {
        final id = VpnGateway.instance.sessionId;
        if (id == null) {
          return <String, Object?>{'sessionId': null, 'status': null};
        }
        final status = await const VpnNative().call(<String, Object?>{
          'method': 'status',
          'id': id,
        });
        return <String, Object?>{'sessionId': id, 'status': status};
      }

    case 'portal.status':
      final bundle = await PasskeyStore.read();
      return <String, Object?>{
        'bound': bundle != null,
        if (bundle != null) 'bundle': bundle.toJson(),
        if (bundle != null) 'studentId': bundle.studentId,
        if (bundle != null)
          'maskedStudentId': maskStudentId(bundle.studentId ?? ''),
      };

    case 'portal.preview':
      {
        final text = args['text'] as String? ?? '';
        final pin = args['pin'] as String? ?? '';
        if (!PasskeyTransfer.looksLikeExport(text)) {
          return <String, Object?>{'ok': false, 'reason': 'wrong_format'};
        }
        final bundle = await PasskeyTransfer.decrypt(text, pin);
        final existing = await PasskeyStore.read();
        return <String, Object?>{
          'ok': true,
          'studentId': bundle.studentId,
          'maskedStudentId': maskStudentId(bundle.studentId ?? ''),
          'deviceName': bundle.deviceName,
          'createdAt': bundle.createdAt.toIso8601String(),
          'existing': existing != null,
        };
      }

    case 'portal.import':
      final text = args['text'] as String? ?? '';
      final pin = args['pin'] as String? ?? '';
      if (!PasskeyTransfer.looksLikeExport(text)) {
        return <String, Object?>{'ok': false, 'reason': 'wrong_format'};
      }
      final bundle = await PasskeyTransfer.decrypt(text, pin);
      await PasskeyStore.save(bundle);
      await PortalSession.instance.clear();
      return <String, Object?>{'ok': true, 'studentId': bundle.studentId};

    case 'portal.export':
      final bundle = await PasskeyStore.read();
      if (bundle == null) {
        return <String, Object?>{'ok': false, 'reason': 'not_bound'};
      }
      final text = await PasskeyTransfer.encrypt(
        bundle,
        args['pin'] as String? ?? '',
      );
      return <String, Object?>{'ok': true, 'text': text};

    case 'portal.bind':
      {
        final bundle = PasskeyBundle.fromJson(
          Map<String, dynamic>.from(args['bundle'] as Map? ?? const {}),
        );
        await PasskeyStore.save(bundle);
        await PortalSession.instance.clear();
        return <String, Object?>{'ok': true, 'studentId': bundle.studentId};
      }

    case 'portal.forget':
      await PasskeyStore.delete();
      await PortalSession.instance.clear();
      return <String, Object?>{'ok': true};

    case 'portal.verify':
      final landing = await PortalSession.instance.verifyCredential();
      return <String, Object?>{'ok': true, 'landing': landing};

    case 'timetable.snapshot':
      final termCode =
          args['termCode'] as String? ??
          await TimetableApi.fetchCurrentTermCode();
      final snapshot = await TimetableApi.fetchTermSnapshot(termCode);
      return <String, Object?>{
        'termCode': termCode,
        'snapshot': snapshot.toJson(),
      };

    case 'timetable.display':
      return _timetableDisplayJson(await _loadDisplaySnapshot(args));

    case 'timetable.state':
      return await _timetableStateJson();

    case 'timetable.week':
      return await _timetableWeekJson((args['week'] as num?)?.toInt());

    // 状态与当周一次取回：进课表页时少一次桥往返（桥是单通道串行队列，
    // 每次往返都要一次 JSON 编解码），首屏更快。
    case 'timetable.overview':
      {
        final state = await _timetableStateJson();
        final requested = (args['week'] as num?)?.toInt();
        final week = await _timetableWeekJson(
          requested ?? (state['viewingWeek'] as num?)?.toInt(),
        );
        return <String, Object?>{'state': state, 'week': week};
      }

    case 'timetable.setWeek':
      TimetableController.instance.setViewingWeek(
        (args['week'] as num?)?.toInt() ?? 1,
      );
      return await _timetableStateJson();

    case 'timetable.sync':
      await TimetableController.instance.manualSync();
      return await _timetableStateJson();

    case 'timetable.updateSettings':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final patch =
            (args['patch'] as Map?)?.cast<String, dynamic>() ?? const {};
        final merged = <String, dynamic>{...c.settings.toJson(), ...patch};
        await c.updateSettings(TimetableSettings.fromJson(merged));
        return await _timetableStateJson();
      }

    case 'timetable.notifyPlan':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final horizon = ((args['horizonDays'] as num?)?.toInt() ?? 7).clamp(
          1,
          30,
        );
        return <String, Object?>{
          'reminders': [
            for (final reminder in c.classReminderPlan(horizonDays: horizon))
              reminder.toJson(),
          ],
        };
      }

    case 'timetable.exams':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        return <String, Object?>{
          'exams': <Map<String, Object?>>[
            for (final event in c.scheduleEvents)
              if (event.isExam)
                _scheduleEventJson(
                  c,
                  event,
                  _scheduleNodeRange(c.periods, event.start, event.end),
                ),
          ],
        };
      }

    case 'timetable.event':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final event = _findEvent(c, args);
        if (event == null) throw StateError('日程不存在');
        return _eventDetailJson(c, event);
      }

    case 'timetable.saveEvent':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final event = _findEvent(c, args);
        if (event == null) return <String, Object?>{'ok': false};
        final next = event.copyWith(
          title: args['title'] is String ? args['title'] as String : null,
          date: _argDate(args['date']),
          start: TimeOfDayMinutes.tryParse(args['start']),
          end: TimeOfDayMinutes.tryParse(args['end']),
          location: args['location'] is String
              ? args['location'] as String
              : null,
          note: args['note'] is String ? args['note'] as String : null,
        );
        final ok = await c.updateScheduleEvent(next);
        return <String, Object?>{'ok': ok};
      }

    // 新增手动日程（考试 / 其他）：source 一律 manual，id 由控制器生成。
    case 'timetable.addEvent':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final kind = args['kind']?.toString() == 'exam'
            ? ScheduleKind.exam
            : ScheduleKind.other;
        final title = (args['title'] as String? ?? '').trim();
        final date = _argDate(args['date']);
        final start = TimeOfDayMinutes.tryParse(args['start']);
        final end = TimeOfDayMinutes.tryParse(args['end']);
        if (title.isEmpty || date == null || start == null || end == null) {
          return <String, Object?>{'ok': false, 'message': '请填写名称与时间'};
        }
        final saved = await c.addScheduleEvent(
          ScheduleEvent(
            id: '',
            kind: kind,
            source: ScheduleSource.manual,
            title: title,
            date: date,
            start: start,
            end: end,
            location: (args['location'] as String? ?? '').trim(),
            note: (args['note'] as String? ?? '').trim(),
            courseCode: kind == ScheduleKind.exam
                ? _blankToNull(args['courseCode'])
                : null,
          ),
        );
        return <String, Object?>{'ok': true, 'id': saved.id};
      }

    case 'timetable.deleteEvent':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final event = _findEvent(c, args);
        if (event == null) return <String, Object?>{'ok': false};
        return <String, Object?>{'ok': await c.deleteScheduleEvent(event.id)};
      }

    case 'timetable.restoreEvent':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final event = _findEvent(c, args);
        if (event == null) return <String, Object?>{'ok': false};
        final key = c.jwxtExamKeyOf(event.id);
        if (key == null) return <String, Object?>{'ok': false};
        return <String, Object?>{'ok': await c.restoreJwxtExam(key)};
      }

    case 'timetable.moveEvent':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final event = _findEvent(c, args);
        if (event == null) return <String, Object?>{'ok': false};
        final week = (args['week'] as num?)?.toInt();
        final day = (args['day'] as num?)?.toInt();
        final startNode = (args['startNode'] as num?)?.toInt();
        final span = ((args['span'] as num?)?.toInt() ?? 1).clamp(1, 20);
        if (week == null || day == null || startNode == null) {
          return <String, Object?>{'ok': false};
        }
        final periods = c.periods;
        final first = periods.where((p) => p.node == startNode).firstOrNull;
        final last = periods
            .where((p) => p.node == startNode + span - 1)
            .firstOrNull;
        final start = TimeOfDayMinutes.tryParse(first?.start);
        final end = TimeOfDayMinutes.tryParse(last?.end ?? first?.end);
        if (start == null || end == null) return <String, Object?>{'ok': false};
        final next = event.copyWith(
          date: c.dateOfColumn(week, day),
          start: start,
          end: end,
        );
        return <String, Object?>{'ok': await c.updateScheduleEvent(next)};
      }

    case 'timetable.exportPreview':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        return _exportPreviewJson(c);
      }

    case 'timetable.exportWakeup':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        return await _exportWakeupJson(c);
      }

    case 'timetable.cell':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final cell = _findCell(c, args);
        if (cell != null) return _cellDetailJson(c, cell);
        // 「其他」栏的课没有固定节次，格子里根本没有它；这时给课程级详情，
        // 前端照样能显示名称/教师/周次，只是没有颜色和位置可改。
        final courseId = args['courseId'] as String?;
        final course = courseId == null
            ? null
            : c.courses.where((item) => item.id == courseId).firstOrNull;
        if (course == null) throw StateError('课表格子不存在');
        return _courseDetailJson(c, course);
      }

    case 'timetable.saveCourse':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final cell = _findCell(c, args);
        final week = (args['week'] as num?)?.toInt();
        final scope = args['scope'] as String? ?? 'all';
        final weeks = <int>[
          for (final e in (args['weeks'] as List?) ?? const [])
            if (e is num) e.toInt(),
        ];
        final argb = (args['userColor'] as num?)?.toInt();
        final saved = await c.saveCourseForm(
          cell: cell,
          // 「其他」栏的课没有格子：把原课程带上，控制器才知道在改哪一门，
          // 否则会被当成新建一门手动课。
          initial: cell == null ? _findCourse(c, args['courseId']) : null,
          name: args['name'] as String? ?? '',
          teacher: args['teacher'] as String? ?? '',
          location: args['location'] as String? ?? '',
          note: args['note'] as String? ?? '',
          kind: parseCourseKind(args['kind']) ?? CourseKind.standard,
          day: (args['day'] as num?)?.toInt() ?? 1,
          startNode: (args['startNode'] as num?)?.toInt() ?? 1,
          endNode: (args['endNode'] as num?)?.toInt() ?? 1,
          courseWeeks: weeks,
          applyWeeks:
              _applyWeeksArg(args) ??
              (scope == 'thisWeek' && week != null ? <int>[week] : null),
          userArgb: argb,
          colorChanged: args['colorChanged'] == true,
        );
        return <String, Object?>{'changed': saved != null};
      }

    case 'timetable.conflicts':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final day = (args['day'] as num?)?.toInt() ?? 1;
        final start = (args['startNode'] as num?)?.toInt() ?? 1;
        final end = (args['endNode'] as num?)?.toInt() ?? start;
        final weeks = <int>[
          for (final e in (args['weeks'] as List?) ?? const [])
            if (e is num) e.toInt(),
        ]..sort();
        if (weeks.isEmpty || day < 1) {
          return <String, Object?>{'text': null};
        }
        final bits = _weeksToBits(weeks);
        final candidate = CourseSlot(
          day: day,
          startNode: start,
          endNode: end,
          weeks: bits,
        );
        final conflicts = courseConflicts(
          c.courses,
          candidate,
          excludeId: args['courseId'] as String?,
        );
        return <String, Object?>{
          'text': _describeConflicts(conflicts, candidate, weeks),
        };
      }

    case 'timetable.deleteCourse':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final cell = _findCell(c, args);
        final scope = args['scope'] as String? ?? 'all';
        final courseId = args['courseId'] as String? ?? '';
        var changed = false;
        if (cell != null && scope == 'thisWeek') {
          changed = await c.deleteOccurrence(cell) != null;
        } else if (cell != null && scope == 'weekdayMatch') {
          changed = await c.deleteWeekdayMatch(cell) != null;
        } else if (cell?.course.isManual ?? false) {
          changed = await c.deleteManualCourse(courseId);
        } else {
          changed = await c.deleteJwxtCourse(courseId);
        }
        return <String, Object?>{'changed': changed};
      }

    case 'timetable.setCourseColor':
      {
        // 课程颜色是课程级的（和自动配色一致），不区分是哪一格点的。
        final c = TimetableController.instance;
        await c.ensureStarted();
        final courseId = args['courseId'] as String?;
        if (courseId == null || courseId.isEmpty) {
          throw StateError('课程不存在');
        }
        final course = _findCourse(c, courseId);
        if (course == null) throw StateError('课程不存在');
        final argb = (args['userColor'] as num?)?.toInt();
        await c.setUserCourseColor(courseId, argb);
        return <String, Object?>{'changed': true};
      }

    case 'timetable.moveCell':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final cell = _findCell(c, args);
        if (cell == null) throw StateError('课表格子不存在');
        final index = _slotIndexOf(cell.course, cell.slot);
        final result = await c.moveSlot(
          cell.course,
          index,
          toDay: (args['toDay'] as num?)?.toInt() ?? cell.slot.day,
          toStartPeriod:
              (args['toStart'] as num?)?.toInt() ?? cell.slot.startNode,
          week: (args['week'] as num?)?.toInt() ?? 1,
          scope: switch (args['scope']) {
            'thisWeek' => MoveScope.thisWeek,
            'fromThisWeek' => MoveScope.fromThisWeek,
            _ => MoveScope.allWeeks,
          },
        );
        return <String, Object?>{'ok': result.ok, 'error': result.error?.name};
      }

    case 'timetable.restoreCourse':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final courseId = args['courseId'] as String? ?? '';
        final restored = await c.restoreJwxtCourse(courseId);
        return <String, Object?>{'changed': restored};
      }

    // 课程详情里的分范围恢复：单日/按周修改（occurrence）、调课记录（position）。
    // 「恢复原课程」走 timetable.restoreCourse（整门课回到教务数据）。
    case 'timetable.restoreChanges':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final cell = _findCell(c, args);
        final scope = args['scope']?.toString() ?? '';
        switch (scope) {
          case 'occurrence':
            final edit = cell?.dateEdit;
            final id = edit?.id ?? '';
            final ok = id.isEmpty ? false : await c.restoreDateEdit(id);
            return <String, Object?>{
              'ok': ok,
              'message': ok
                  ? (edit!.isWeeksScoped ? '已恢复按周修改' : '已恢复这一天')
                  : '恢复失败',
            };
          case 'position':
            if (cell == null) throw StateError('课表格子不存在');
            final ok = await c.restoreOverride(
              cell.course,
              _slotIndexOf(cell.course, cell.slot),
            );
            return <String, Object?>{
              'ok': ok,
              'message': ok ? '已恢复原位置' : '恢复失败',
            };
          default:
            throw StateError('未知的恢复范围');
        }
      }

    case 'score.fetch':
      final report = await ScoreApi.fetch();
      return report.toJson();

    case 'academic.fetch':
      final summary = await AcademicApi.fetchSummary();
      return summary.toJson();

    case 'student.fetch':
      final info = await StudentInfoApi.fetch();
      return info.toJson();

    case 'exam.fetch':
      {
        final termCode =
            args['termCode'] as String? ??
            await TimetableApi.fetchCurrentTermCode();
        final events = await TimetableApi.fetchExams(termCode);
        return <String, Object?>{
          'termCode': termCode,
          'exams': [
            for (final event in events)
              <String, Object?>{
                'id': event.id,
                'title': event.title,
                'date': _dateKey(event.date),
                'start': _timeKey(event.start),
                'end': _timeKey(event.end),
                'location': event.location,
                'note': event.note,
                if (event.courseCode != null) 'courseCode': event.courseCode,
              },
          ],
        };
      }

    case 'bulletin.list':
      {
        final path = args['category'] as String? ?? '';
        final pageNumber = (args['page'] as num?)?.toInt() ?? 1;
        final uri = Uri.parse('https://${CampusHosts.bulletin}/$path');
        final page = await BulletinApi().fetchPage(uri, pageNumber: pageNumber);
        return <String, Object?>{
          'pageNumber': page.pageNumber,
          'totalCount': page.totalCount,
          'nextPath': _relativePath(page.nextUrl),
          'items': [for (final b in page.items) _bulletinJson(b)],
        };
      }

    case 'bulletin.detail':
      {
        final url = args['url'] as String? ?? '';
        final bulletin = Bulletin(
          id: args['id'] as String? ?? url,
          title: args['title'] as String? ?? '',
          url: url,
          officialCategory: args['category'] as String? ?? '',
          publisher: args['publisher'] as String? ?? '',
          publishedAt: args['publishedAt'] as String? ?? '',
        );
        final document = await BulletinApi().fetchDetail(bulletin);
        if (document == null) return <String, Object?>{'found': false};
        return <String, Object?>{
          'found': true,
          'blocks': [
            for (final block in document.blocks)
              <String, Object?>{
                'kind': block.kind.name,
                'text': block.text,
                'items': block.items,
                if (block.url != null) 'url': block.url,
                'altText': block.altText,
                'level': block.level,
                'centered': block.centered,
              },
          ],
          'attachments': [
            for (final a in document.attachments)
              <String, Object?>{'name': a.name, 'url': a.url},
          ],
        };
      }

    case 'electricity.campuses':
      return <String, Object?>{
        'options': _optionsJson(await ElectricityApi.listCampuses()),
      };

    case 'electricity.buildings':
      {
        final campus = _optionOf(args['campus']);
        return <String, Object?>{
          'options': _optionsJson(await ElectricityApi.listBuildings(campus)),
        };
      }

    case 'electricity.rooms':
      {
        final campus = _optionOf(args['campus']);
        final building = _optionOf(args['building']);
        return <String, Object?>{
          'options': _optionsJson(
            await ElectricityApi.listRooms(campus, building),
          ),
        };
      }

    case 'electricity.balance':
      {
        final room = ElecRoom(
          campus: _optionOf(args['campus']),
          building: _optionOf(args['building']),
          room: _optionOf(args['room']),
        );
        final kwh = await ElectricityApi.queryBalance(room);
        return <String, Object?>{'kwh': kwh};
      }

    case 'classroom.buildings':
      return <String, Object?>{
        'buildings': [
          for (final b in await FreeClassroomApi.listBuildings())
            <String, Object?>{'code': b.code, 'name': b.name},
        ],
      };

    case 'classroom.day':
      {
        final code = args['code'] as String? ?? '';
        final buildings = await FreeClassroomApi.listBuildings();
        if (buildings.isEmpty) throw StateError('教务没有返回教学楼列表');
        final building = buildings.firstWhere(
          (b) => b.code == code,
          orElse: () => buildings.first,
        );
        final date =
            DateTime.tryParse(args['date'] as String? ?? '') ?? DateTime.now();
        final calendar = await FreeClassroomApi.calendar(date);
        final rooms = await FreeClassroomApi.classrooms(
          date: date,
          calendar: calendar,
          building: building,
        );
        return <String, Object?>{
          'date': _dateKey(date),
          'building': <String, Object?>{
            'code': building.code,
            'name': building.name,
          },
          'calendar': <String, Object?>{
            'termCode': calendar.termCode,
            'week': calendar.week,
            'weekday': calendar.weekday,
            'maxPeriod': calendar.maxPeriod,
            'groups': [
              for (final g in calendar.groups)
                <String, Object?>{
                  'start': g.start,
                  'end': g.end,
                  'startTime': g.startTime,
                  'endTime': g.endTime,
                },
            ],
          },
          'rooms': [
            for (final room in rooms)
              <String, Object?>{
                'name': room.name,
                'type': room.type,
                'seats': room.seats,
                'floor': room.floor,
                if (room.zone != null) 'zone': room.zone,
                'occupancy': room.occupancy,
              },
          ],
        };
      }

    case 'innovation.fetch':
      {
        final credit = await InnovationCreditApi.fetch();
        return <String, Object?>{
          'total': credit.total,
          'grade': credit.grade,
          'fetchedAt': credit.fetchedAt.toIso8601String(),
          'items': [
            for (final item in credit.items)
              <String, Object?>{
                'name': item.name,
                'date': item.date,
                'category1': item.category1,
                'category2': item.category2,
                'level': item.level,
                'standardScore': item.standardScore,
                'score': item.score,
                'batch': item.batch,
                'status': item.status,
              },
          ],
        };
      }

    case 'labor.fetch':
      {
        final score = await LaborScoreApi.fetch();
        return <String, Object?>{
          'fetchedAt': score.fetchedAt.toIso8601String(),
          'liveLife': score.liveLife,
          'liveService': score.liveService,
          if (score.official != null)
            'official': <String, Object?>{
              'theory': score.official!.theory,
              'life': score.official!.life,
              'service': score.official!.service,
              'majorCourse': score.official!.majorCourse,
              'majorContest': score.official!.majorContest,
              'majorTotal': score.official!.majorTotal,
              'total': score.official!.total,
              'confirmed': score.official!.confirmed,
              'updatedAt': score.official!.updatedAt,
              'filed': score.official!.filed,
              'filedAt': score.official!.filedAt,
            },
          if (score.liveMajor != null)
            'liveMajor': <String, Object?>{
              'course': score.liveMajor!.course,
              'contest': score.liveMajor!.contest,
              'total': score.liveMajor!.total,
            },
        };
      }

    // ===== 课表管理：整天调课 / 变动记录 / 恢复教务课程 =====
    // 业务规则都在 Dart 控制器里，这里只做参数解析与结构化输出；
    // 面向用户的一句话结果由 Dart 给出，前端不重算、不改写。

    case 'timetable.daySwaps':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        return _daySwapsJson(c);
      }

    case 'timetable.addDaySwap':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        final a = _argDate(args['dateA']);
        final b = _argDate(args['dateB']);
        if (a == null || b == null) {
          return <String, Object?>{
            'ok': false,
            'code': 'missingDate',
            'message': '请选择原日期和目标日期',
            'swaps': _daySwapListJson(c),
          };
        }
        final invalid = _daySwapErrorFor(c, a, b);
        if (invalid != null) {
          return <String, Object?>{
            'ok': false,
            'code': invalid.code,
            'message': invalid.message,
            'swaps': _daySwapListJson(c),
          };
        }
        final swap = await c.addDaySwap(a, b);
        return <String, Object?>{
          'ok': swap != null,
          'message': swap == null ? '调课失败' : '已与${_dayLabel(b)}互换',
          'swaps': _daySwapListJson(c),
        };
      }

    case 'timetable.removeDaySwap':
      {
        final c = TimetableController.instance;
        final id = args['id'];
        final removed = id is String && id.isNotEmpty
            ? await c.removeDaySwap(id)
            : false;
        return <String, Object?>{
          'ok': removed,
          'message': removed ? '已恢复' : '恢复失败',
          'swaps': _daySwapListJson(c),
        };
      }

    case 'timetable.changes':
      {
        final records = await TimetableChangeLogStore.readNewestFirst();
        return _changeLogJson(records);
      }

    case 'timetable.restoreAllJwxt':
      {
        final c = TimetableController.instance;
        await c.ensureStarted();
        return <String, Object?>{'changed': await c.restoreAllJwxt()};
      }

    default:
      throw UnsupportedError('unknown bridge method: $method');
  }
}

Map<String, Object?> _describe(Object error, StackTrace stack) {
  final map = <String, Object?>{
    'message': error.toString(),
    'type': error.runtimeType.toString(),
  };
  if (error is DiagnosticError) {
    final context = error.diagnosticContext;
    if (context.isNotEmpty) {
      map['context'] = <String, Object?>{
        for (final entry in context.entries) entry.key: '${entry.value}',
      };
    }
    final cause = error.cause;
    if (cause != null) map['cause'] = cause.toString();
  }
  if (error is DiagnosticException) {
    map['code'] = error.code;
    map['stage'] = error.stage;
  }
  final frames = stack.toString().split('\n').take(12).join('\n');
  map['stack'] = frames;
  debugPrint('[bridge] $error\n$frames');
  return map;
}

String _dateKey(DateTime date) =>
    '${date.year.toString().padLeft(4, '0')}-'
    '${date.month.toString().padLeft(2, '0')}-'
    '${date.day.toString().padLeft(2, '0')}';

String _timeKey(TimeOfDayMinutes time) => time.format();

Map<String, Object?> _bulletinJson(Bulletin b) => <String, Object?>{
  'id': b.id,
  'title': b.title,
  'url': b.url,
  'officialCategory': b.officialCategory,
  'publisher': b.publisher,
  'publishedAt': b.publishedAt,
  'topics': [for (final t in b.topics) t.label],
};

String? _relativePath(String? url) {
  if (url == null || url.isEmpty) return null;
  final uri = Uri.tryParse(url);
  if (uri == null) return url;
  final path = uri.path.startsWith('/') ? uri.path.substring(1) : uri.path;
  return uri.hasQuery ? '$path?${uri.query}' : path;
}

List<Map<String, Object?>> _optionsJson(List<ElecOption> options) => [
  for (final o in options) <String, Object?>{'name': o.name, 'value': o.value},
];

ElecOption _optionOf(Object? raw) {
  final map = raw is Map ? raw : const <String, Object?>{};
  return ElecOption(
    name: '${map['name'] ?? ''}',
    value: '${map['value'] ?? ''}',
  );
}

/// 最近一次展示快照（同一学期不重复拉取）。课表页下拉刷新时传 `refresh: true` 清空。
TimetableSnapshot? _displayCache;
String? _displayCacheTerm;

Future<TimetableSnapshot> _loadDisplaySnapshot(
  Map<String, dynamic> args,
) async {
  final stored = await TimetableStore.readSettings();
  var termCode = (args['termCode'] as String? ?? '').trim();
  if (termCode.isEmpty) termCode = stored.termCode.trim();
  if (termCode.isEmpty) termCode = await TimetableApi.fetchCurrentTermCode();
  final refresh = args['refresh'] == true;
  final cached = _displayCache;
  if (!refresh && cached != null && _displayCacheTerm == termCode) {
    return cached;
  }
  final snapshot = await TimetableApi.fetchTermSnapshot(
    termCode,
    baseSettings: stored.termCode == termCode ? stored : null,
  );
  _displayCache = snapshot;
  _displayCacheTerm = termCode;
  return snapshot;
}

/// 把课表快照转成「前端直接能画」的形状：课程按课段展开，周次是课段自己的集合。
Map<String, Object?> _timetableDisplayJson(TimetableSnapshot snapshot) {
  final settings = snapshot.settings;
  final termStart = settings.termStartDate;
  final today = TimetableApi.shanghaiToday();
  // 参考实现把学期长度定为 20 周；实际课表里出现更晚的周次就顺延。
  var totalWeeks = 20;
  final rawWeek = termStart == null ? 1 : teachingWeekOf(termStart, today);
  final currentWeek = rawWeek.clamp(1, totalWeeks);

  final periods = <Map<String, Object?>>[
    for (final p in snapshot.periods)
      <String, Object?>{'node': p.node, 'start': p.start, 'end': p.end},
  ];

  final courses = <Map<String, Object?>>[];
  final unscheduled = <Map<String, Object?>>[];
  for (final course in snapshot.courses) {
    if (course.hidden) continue;
    final slots = course.slots.where((s) => s.hasFixedTime).toList();
    if (slots.isEmpty) {
      unscheduled.add(<String, Object?>{
        'id': course.id,
        'name': course.displayName,
        'location': course.buildingDisplay,
        'teacher': '',
        'colorIndex': _displayColor(course),
      });
      continue;
    }
    for (final slot in slots) {
      final weeks = <int>[
        for (var i = 0; i < slot.weeks.length; i++)
          if (slot.weeks[i] == '1') i + 1,
      ];
      for (final w in weeks) {
        if (w > totalWeeks) totalWeeks = w;
      }
      courses.add(<String, Object?>{
        'id': course.id,
        'name': (slot.nameOverride ?? '').trim().isEmpty
            ? course.displayName
            : slot.nameOverride!.trim(),
        'day': slot.day,
        'startNode': slot.startNode,
        'endNode': slot.endNode,
        'location': slot.location,
        'teacher': slot.teacher,
        'colorIndex': _displayColor(course),
        'weeks': weeks,
        'weeksLabel': slot.weeksLabel,
        'isLab': slot.isLab,
      });
    }
  }

  return <String, Object?>{
    'termCode': settings.termCode,
    'termLabel': _termLabel(settings.termCode),
    'currentWeek': currentWeek,
    'currentDay': today.weekday,
    'totalWeeks': totalWeeks,
    'weekStartsOnMonday': settings.weekStartsOnMonday,
    'showNonCurrentWeek': settings.showNonCurrentWeek,
    'periods': periods,
    'courses': courses,
    'unscheduled': unscheduled,
    'syncedAt': snapshot.syncedAt?.toIso8601String(),
  };
}

/// 按「课程 + 星期 + 起止节」定位当前这一周的那一格。
GridCell? _findCell(TimetableController c, Map<String, dynamic> args) {
  final courseId = args['courseId'] as String?;
  final day = (args['day'] as num?)?.toInt();
  final start = (args['startNode'] as num?)?.toInt();
  final end = (args['endNode'] as num?)?.toInt();
  final week = (args['week'] as num?)?.toInt();
  if (courseId == null || day == null || start == null || end == null) {
    return null;
  }
  for (final cell in c.cellsForDay(week ?? c.viewingWeek, day)) {
    if (cell.course.id == courseId &&
        cell.slot.startNode == start &&
        cell.slot.endNode == end) {
      return cell;
    }
  }
  return null;
}

int _slotIndexOf(Course course, CourseSlot slot) {
  for (var i = 0; i < course.slots.length; i++) {
    final s = course.slots[i];
    if (s.day == slot.day &&
        s.startNode == slot.startNode &&
        s.endNode == slot.endNode &&
        s.weeks == slot.weeks &&
        s.location == slot.location &&
        s.teacher == slot.teacher) {
      return i;
    }
  }
  return -1;
}

/// 课程详情的完整数据：合并后的格子 + 本地改动的差异分组（供「恢复」用）。
Map<String, Object?> _cellDetailJson(TimetableController c, GridCell cell) {
  final json = Map<String, Object?>.of(_weekCellJson(c, cell));
  json['hasOverride'] = c.hasOverride(
    cell.course,
    _slotIndexOf(cell.course, cell.slot),
  );
  json['changes'] = <Map<String, Object?>>[
    for (final group in c.courseDetailChanges(cell))
      <String, Object?>{
        'scope': group.scope.name,
        'title': group.title,
        'affectedScope': group.affectedScope,
        'changes': <Map<String, Object?>>[
          for (final change in group.changes)
            <String, Object?>{
              'field': change.field,
              'before': change.before,
              'after': change.after,
            },
        ],
      },
  ];
  return json;
}

/// 该时段对应教务原始时段的「有课周次」；没套覆盖记录时就用它自己的周次。
Iterable<int> _weeksOfOrigin(CourseSlot slot) {
  final weeks = (slotOriginOf(slot)?.original ?? slot).weeks;
  return <int>[
    for (var i = 0; i < weeks.length; i++)
      if (weeks[i] == '1') i + 1,
  ];
}

/// 按 id 找课程；课表里没有这一门时返回 null。
Course? _findCourse(TimetableController c, Object? id) {
  if (id is! String || id.isEmpty) return null;
  return c.courses.where((course) => course.id == id).firstOrNull;
}

/// 课程级详情：给没有固定节次的课（「其他」栏）兜底，字段与格子详情保持一致。
Map<String, Object?> _courseDetailJson(TimetableController c, Course course) {
  final slot = course.slots.isEmpty ? null : course.slots.first;
  return <String, Object?>{
    'courseId': course.id,
    'name': course.displayName,
    'teacher': slot?.teacher ?? '',
    'location': slot?.location ?? '',
    'note': slot?.note ?? '',
    'day': 0,
    'startNode': 0,
    'endNode': 0,
    'start': '',
    'end': '',
    'weeks': const <int>[],
    'weeksLabel': _otherCourseWeeksLabel(course),
    'activeThisWeek': false,
    'conflictCount': 0,
    'colorIndex': c.colorIndexOf(course),
    'userColor': c.userColorOf(course),
    'occurrenceColor': course.occurrenceColor,
    'useDefaultColor': course.useDefaultColor,
    'isLab': false,
    'kind': 'other',
    'isManual': course.isManual,
    'date': null,
    'editedThisDay': false,
    'movedAway': false,
    'moved': false,
    'hasOverride': false,
    'changes': const <Map<String, Object?>>[],
  };
}

/// 课表状态的唯一数据源是 Dart 侧的 `TimetableController`：学期、周次、同步态、
/// 设置都由它算好，前端只负责画。前端不要在 Kotlin 里重算这些规则。
Future<Map<String, Object?>> _timetableStateJson() async {
  final c = TimetableController.instance;
  await c.ensureStarted();
  // 打开周课表时对齐到本周：跨周或回前台后浏览周仍停在旧的周次。
  c.followCurrentWeekIfChanged();
  final s = c.settings;
  return <String, Object?>{
    'termCode': s.termCode,
    'termLabel': _termLabel(s.termCode),
    'termStartDate': s.termStartDate == null
        ? null
        : _dateKey(s.termStartDate!),
    'currentWeek': c.currentWeek,
    'viewingWeek': c.viewingWeek,
    'totalWeeks': c.totalWeeks,
    'currentDay': TimetableApi.shanghaiToday().weekday,
    'lastPeriod': c.lastPeriod,
    'portalBound': c.portalBound,
    'loading': c.loading,
    'hasCourses': c.hasCourses,
    'weekPending': c.weekPending,
    'syncStatus': c.syncStatus.name,
    'syncFailure': c.syncFailure.name,
    'statusMessage': c.statusMessage,
    'syncedAt': c.syncedAt?.toIso8601String(),
    'settings': s.toJson(),
    // 整天调课记录随状态一起下发：表头要标出「这一天被换过」，也避免再来一次往返。
    'daySwaps': _daySwapListJson(c),
    'periods': <Map<String, Object?>>[
      for (final p in c.periods)
        <String, Object?>{'node': p.node, 'start': p.start, 'end': p.end},
    ],
  };
}

/// 某一周的渲染数据：合并完调课/单日修改后的格子、该周的日程与考试、
/// 以及「不在任何节次内」的安排（前端画在日期表头下）。
Future<Map<String, Object?>> _timetableWeekJson(int? week) async {
  final c = TimetableController.instance;
  await c.ensureStarted();
  final w = (week ?? c.viewingWeek).clamp(1, c.totalWeeks);
  final days = c.visibleDays(w);
  final cells = <Map<String, Object?>>[];
  final events = <Map<String, Object?>>[];
  final unplaced = <Map<String, Object?>>[];
  final dateOfDay = <String, String>{};
  for (final day in days) {
    final date = c.dateOfColumn(w, day);
    dateOfDay['$day'] = _dateKey(date);
    for (final cell in c.cellsForDay(w, day)) {
      cells.add(_weekCellJson(c, cell));
    }
    for (final event in c.scheduleEventsOn(date)) {
      final range = _scheduleNodeRange(c.periods, event.start, event.end);
      final json = _scheduleEventJson(c, event, range);
      if (range == null) {
        unplaced.add(json);
      } else {
        events.add(json);
      }
    }
  }
  return <String, Object?>{
    'week': w,
    'days': days,
    'dateOfDay': dateOfDay,
    'monthLabel': c.monthLabelOf(w),
    'cells': cells,
    'events': events,
    'unplaced': unplaced,
    // 「其他」栏跟着周走：已经上完的课不再显示（与周视图灰格同一条规则）。
    // 这类课没有固定节次，前端只展示教师与周次。
    'unscheduled': <Map<String, Object?>>[
      for (final course in otherCoursesForWeek(c.otherCourses(), w))
        <String, Object?>{
          'courseId': course.id,
          'name': course.displayName,
          'teacher': _firstTeacher(course),
          'weeksLabel': _otherCourseWeeksLabel(course),
        },
    ],
  };
}

/// 「其他」栏的周次摘要：把每个无节次课段的周次拼起来，没有原始标签就按位串格式化。
String _otherCourseWeeksLabel(Course course) {
  final labels = <String>{};
  for (final slot in course.slots) {
    final label = slot.weeksLabel.trim().isNotEmpty
        ? slot.weeksLabel.trim()
        : formatWeekBits(slot.weeks);
    if (label.isNotEmpty) labels.add(label);
  }
  return labels.join('、');
}

Map<String, Object?> _weekCellJson(TimetableController c, GridCell cell) {
  final course = cell.course;
  final slot = cell.slot;
  final kind = courseKindOf(course: course, slot: slot);
  final time = c.timeForSlot(course, slot, date: cell.date);
  final origin = slotOriginOf(slot);
  return <String, Object?>{
    'courseId': course.id,
    'name': courseDisplayName(course: course, slot: slot),
    'teacher': slot.teacher,
    'location': slot.location,
    'note': slot.note,
    'day': slot.day,
    'startNode': slot.startNode,
    'endNode': slot.endNode,
    'start': time.start,
    'end': time.end,
    'weeks': <int>[
      for (var i = 0; i < slot.weeks.length; i++)
        if (slot.weeks[i] == '1') i + 1,
    ],
    'weeksLabel': slot.weeksLabel.isNotEmpty
        ? slot.weeksLabel
        : formatWeekBits(slot.weeks),
    // 教务原始周次：单/双周角标要按原始周次判断，改过本周的时段不能拿裁剪后的周次算。
    'origWeeks': <int>[
      for (final we in _weeksOfOrigin(slot))
        we,
    ],
    'activeThisWeek': cell.activeThisWeek,
    'conflictCount': cell.conflictCount,
    'colorIndex': c.colorIndexOf(course),
    'userColor': c.userColorOf(course),
    'occurrenceColor': course.occurrenceColor,
    'useDefaultColor': course.useDefaultColor,
    'isLab': kind == CourseKind.lab,
    'kind': kind.name,
    'isManual': course.isManual,
    'date': cell.date == null ? null : _dateKey(cell.date!),
    'editedThisDay': cell.dateEdit != null,
    'movedAway': origin != null && !origin.moved,
    'moved': origin?.moved ?? false,
  };
}

Map<String, Object?> _scheduleEventJson(
  TimetableController c,
  ScheduleEvent event,
  ({int startNode, int endNode})? range,
) {
  return <String, Object?>{
    'id': event.id,
    'kind': event.kind.name,
    'isExam': event.isExam,
    'isManual': !event.isFromJwxt,
    'title': event.title,
    'date': _dateKey(event.date),
    'start': event.start.format(),
    'end': event.end.format(),
    'location': event.location,
    'note': event.note,
    'courseCode': event.courseCode,
    'examWeek': c.weekOfDate(event.date),
    'startNode': range?.startNode,
    'endNode': range?.endNode,
  };
}

String _firstTeacher(Course course) {
  for (final slot in course.slots) {
    if (slot.teacher.isNotEmpty) return slot.teacher;
  }
  return '';
}

/// 日程与节次「重叠一分钟即算覆盖」，首尾相接不算；完全不重叠返回 null。
/// 与参考实现同规则，故不把带 Flutter 样式的 layout 文件拖进 bridge。
({int startNode, int endNode})? _scheduleNodeRange(
  List<PeriodTime> periods,
  TimeOfDayMinutes start,
  TimeOfDayMinutes end,
) {
  final st = start.totalMinutes;
  final en = end.totalMinutes < st ? st : end.totalMinutes;
  int? lo;
  int? hi;
  for (final p in periods) {
    final ps = TimeOfDayMinutes.tryParse(p.start)?.totalMinutes;
    final pe = TimeOfDayMinutes.tryParse(p.end)?.totalMinutes;
    if (ps == null || pe == null) continue;
    final overlaps = st == en ? (ps <= st && st < pe) : (ps < en && st < pe);
    if (!overlaps) continue;
    if (lo == null || p.node < lo) lo = p.node;
    if (hi == null || p.node > hi) hi = p.node;
  }
  if (lo == null || hi == null) return null;
  return (startNode: lo, endNode: hi);
}

/// 颜色先交给前端按调色板取；这里只给出课名的稳定色号，便于多处一致。
int _displayColor(Course course) =>
    course.colorIndex ?? (course.name.hashCode.abs() % 12);

String _termLabel(String termCode) {
  final parts = termCode.split('-');
  if (parts.length < 3) return termCode;
  final season = parts[2] == '2' ? '春' : '秋';
  return '${parts[0]}-${parts[1]} $season';
}

/// 桥里收到的是「周次列表」，而控制器内部一律用位串。
String _weeksToBits(Iterable<int> weeks) {
  final max = weeks.isEmpty ? 0 : weeks.reduce((a, b) => a > b ? a : b);
  final chars = List<String>.filled(max, '0');
  for (final w in weeks) {
    if (w >= 1 && w <= max) chars[w - 1] = '1';
  }
  return chars.join();
}

/// 表单里能选的周次：显式传入优先（自选/本周及以后），否则交给 scope。
List<int>? _applyWeeksArg(Map<String, dynamic> args) {
  final raw = args['applyWeeks'];
  if (raw is! List || raw.isEmpty) return null;
  final weeks = <int>[
    for (final e in raw)
      if (e is num) e.toInt(),
  ]..sort();
  return weeks.isEmpty ? null : weeks;
}

/// 「与『课名』第 1-3 周冲突」，与参考实现同一套措辞。
String? _describeConflicts(
  List<Course> conflicts,
  CourseSlot candidate,
  List<int> candidateWeeks,
) {
  if (conflicts.isEmpty) return null;
  if (conflicts.length > 1) return '与 ${conflicts.length} 门课冲突';
  final course = conflicts.single;
  final common = <int>{};
  for (final slot in course.slots) {
    if (slot.day != candidate.day ||
        slot.startNode > candidate.endNode ||
        candidate.startNode > slot.endNode) {
      continue;
    }
    final slotWeeks = weeksFromBits(slot.weeks).toSet();
    common.addAll(candidateWeeks.where(slotWeeks.contains));
  }
  if (common.isEmpty) return '与「${course.name}」冲突';
  return '与「${course.name}」${formatWeeks(common)}冲突';
}

/// WakeUp 导出的预览：只读现有课表，不产生网络请求。
Map<String, Object?> _exportPreviewJson(TimetableController c) {
  try {
    final snapshot = TimetableExportSnapshot.fromController(c);
    return <String, Object?>{
      'ok': true,
      'name': snapshot.payload['name'],
      'courseCount': snapshot.courseCount,
      'examCount': snapshot.examCount,
      'otherCount': snapshot.otherCount,
      'skipped': <Map<String, Object?>>[
        for (final item in snapshot.skippedItems)
          <String, Object?>{
            'courseName': item.courseName,
            'reason': item.reason.name,
          },
      ],
      'warnings': snapshot.warnings,
    };
  } on TimetableExportException catch (error) {
    // 预览阶段的失败（缺开学日、没有可导出内容等）本身就是给人看的一句话。
    return <String, Object?>{'ok': false, 'message': error.message};
  }
}

/// 生成 WakeUp 口令；失败时同样把可展示的提示放进 `message`，不往通道外抛。
Future<Map<String, Object?>> _exportWakeupJson(TimetableController c) async {
  TimetableExportApi? api;
  try {
    final snapshot = TimetableExportSnapshot.fromController(c);
    api = TimetableExportApi();
    final share = await api.generate(snapshot);
    return <String, Object?>{'ok': true, 'shareText': share.shareText};
  } on TimetableExportException catch (error) {
    return <String, Object?>{'ok': false, 'message': error.message};
  } finally {
    api?.close();
  }
}

ScheduleEvent? _findEvent(TimetableController c, Map<String, Object?> args) {
  final id = args['id'] is String ? args['id'] as String : '';
  if (id.isEmpty) return null;
  for (final e in c.scheduleEvents) {
    if (e.id == id) return e;
  }
  return null;
}

Map<String, Object?> _eventDetailJson(
  TimetableController c,
  ScheduleEvent event,
) {
  final range = _scheduleNodeRange(c.periods, event.start, event.end);
  final key = c.jwxtExamKeyOf(event.id);
  return <String, Object?>{
    'event': _scheduleEventJson(c, event, range),
    'overridden': key != null && c.isJwxtOverridden(examKey: key),
  };
}

DateTime? _argDate(Object? raw) {
  if (raw is! String || raw.isEmpty) return null;
  final parts = raw.split('-');
  if (parts.length != 3) return null;
  final year = int.tryParse(parts[0]);
  final month = int.tryParse(parts[1]);
  final day = int.tryParse(parts[2]);
  if (year == null || month == null || day == null) return null;
  return DateTime(year, month, day);
}

String? _blankToNull(Object? value) {
  final text = value?.toString().trim() ?? '';
  return text.isEmpty ? null : text;
}


/// 「M月D日」，与参考实现的日期标签一致。
String _dayLabel(DateTime date) => '${date.month}月${date.day}日';

/// 整天调课页的初始数据：学期首末日（日期选择器上下界）、今天与全部调课记录。
Map<String, Object?> _daySwapsJson(TimetableController c) {
  final first = c.weekStartDate(1);
  final last = c.weekStartDate(c.totalWeeks).add(const Duration(days: 6));
  return <String, Object?>{
    'semesterFirst': _dateKey(first),
    'semesterLast': _dateKey(last),
    'today': _dateKey(TimetableApi.shanghaiToday()),
    'swaps': _daySwapListJson(c),
  };
}

List<Map<String, Object?>> _daySwapListJson(TimetableController c) => [
  for (final swap in c.daySwaps)
    <String, Object?>{
      'id': swap.id,
      'dateA': _dateKey(swap.dateA),
      'dateB': _dateKey(swap.dateB),
      'description': '${_dayLabel(swap.dateA)}与${_dayLabel(swap.dateB)}的课程互换',
    },
];

/// 新增整天调课前的一次性校验；通过返回 null。
({String code, String message})? _daySwapErrorFor(
  TimetableController c,
  DateTime a,
  DateTime b,
) {
  if (isSameDate(a, b)) {
    return (code: 'sameDay', message: '不能和同一天互换');
  }
  final first = c.weekStartDate(1);
  final last = c.weekStartDate(c.totalWeeks).add(const Duration(days: 6));
  final dayA = DateTime(a.year, a.month, a.day);
  final dayB = DateTime(b.year, b.month, b.day);
  if (dayA.isBefore(first) ||
      dayA.isAfter(last) ||
      dayB.isBefore(first) ||
      dayB.isAfter(last)) {
    return (code: 'outOfSemester', message: '日期不在本学期内');
  }
  if (c.isSwappedDate(a)) {
    return (code: 'alreadySwapped', message: '${_dayLabel(a)}已调过课，请先撤销');
  }
  if (c.isSwappedDate(b)) {
    return (code: 'alreadySwapped', message: '${_dayLabel(b)}已调过课，请先撤销');
  }
  return null;
}

/// 变动记录按 `fingerprint`（同一次同步）分组，组标题是那次的检出时间；
/// 组内保持「最新在前」的原始顺序。展示文案由 Dart 侧算好，前端只排版。
Map<String, Object?> _changeLogJson(List<TimetableChangeRecord> newestFirst) {
  final groups = <Map<String, Object?>>[];
  final indexOf = <String, int>{};
  for (final record in newestFirst) {
    var index = indexOf[record.fingerprint];
    if (index == null) {
      index = groups.length;
      indexOf[record.fingerprint] = index;
      groups.add(<String, Object?>{
        'title': TimetableChangeFormat.groupTitle(record.time),
        'items': <Map<String, Object?>>[],
      });
    }
    final items = groups[index]['items']! as List<Map<String, Object?>>;
    items.add(<String, Object?>{
      'id': record.id,
      'courseName': record.courseName.isEmpty ? '（无课名）' : record.courseName,
      'type': record.type.wireName,
      'typeLabel': record.type.labelZh,
      'scope': TimetableChangeFormat.scopeBodyOf(record),
      'detail': TimetableChangeFormat.detailOf(record),
    });
  }
  return <String, Object?>{'groups': groups, 'total': newestFirst.length};
}
