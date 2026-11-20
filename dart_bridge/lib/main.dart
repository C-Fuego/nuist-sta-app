// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
//
// 无界面入口：Compose 前端通过 MethodChannel 调这里，校园网逻辑全部来自
// 机械同步的 Dart 实现（见 tools/vendor.py），不在 Kotlin 侧重写。

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'bridge/router.dart';

@pragma('vm:entry-point')
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('nuist.bridge');
  channel.setMethodCallHandler((MethodCall call) async {
    final args = call.arguments is Map
        ? Map<String, dynamic>.from(call.arguments as Map)
        : <String, dynamic>{};
    return handle(call.method, args);
  });
}
