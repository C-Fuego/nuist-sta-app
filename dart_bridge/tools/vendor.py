#!/usr/bin/env python3
"""把参考实现里的校园网 Dart 代码机械同步到 bridge 包（不手改逻辑）。

只复制「从入口可达」的文件，避免把 UI 与重型插件拖进来：
  * 入口：core/config、core/auth、core/network 全部，mini_apps 的 *_api/_models/_parser/_tagger/_store
  * 被 copy 的文件里 `package:nuist_sta_app/` 前缀改写为 `package:nuist_bridge/`
  * 命中 BLOCKED 插件的文件整体跳过并打印，方便手工决策
"""
from __future__ import annotations
import pathlib, re, shutil, sys

ROOT = pathlib.Path(__file__).resolve().parents[2]      # nuist-sta-app/
SRC = ROOT / 'lib'
DST = ROOT / 'dart_bridge' / 'lib'
PKG = 'nuist_sta_app'
NEW = 'nuist_bridge'

BLOCKED = [
    'maplibre_gl', 'geolocator', 'mobile_scanner', 'qr_flutter',
    'fl_chart', 'share_plus', 'file_picker', 'sensors_plus', 'connectivity_plus',
    'flutter_markdown_plus',
]

PKG_IMPORT = re.compile(r"import 'package:%s/([^']+)'" % PKG)
REL_IMPORT = re.compile(r"import '([^':]+\.dart)'")


def discover() -> list[pathlib.Path]:
    roots: list[pathlib.Path] = []
    for sub in ('config', 'auth', 'network'):
        roots += sorted((SRC / 'core' / sub).glob('*.dart'))
    for d in sorted((SRC / 'mini_apps').iterdir()):
        if not d.is_dir():
            continue
        for f in sorted(d.glob('*.dart')):
            if any(k in f.name for k in ('_api', '_models', '_parser', '_tagger', '_store')):
                roots.append(f)
    return roots


def resolve(base: pathlib.Path, target: str) -> pathlib.Path | None:
    p = (base.parent / target).resolve()
    return p if p.is_file() and SRC in p.parents else None


def main() -> int:
    seen: set[pathlib.Path] = set()
    queue = discover()
    skipped: list[tuple[pathlib.Path, str]] = []
    while queue:
        f = queue.pop()
        rel = f.relative_to(SRC)
        if rel in seen:
            continue
        seen.add(rel)
        text = f.read_text(encoding='utf-8')
        hit = next((b for b in BLOCKED if re.search(r"package:%s/" % re.escape(b), text)), None)
        if hit:
            skipped.append((rel, hit))
            continue
        for m in PKG_IMPORT.finditer(text):
            nxt = (SRC / m.group(1))
            if nxt.is_file():
                queue.append(nxt)
        for m in REL_IMPORT.finditer(text):
            nxt = resolve(f, m.group(1))
            if nxt is not None:
                queue.append(nxt)
    if '--dry-run' in sys.argv:
        print(f'would copy {len(seen)} files, skip {len(skipped)}')
        for rel, why in skipped:
            print(f'  skip {rel}  ({why})')
        return 0
    # 只清理机械同步的目录，保留 bridge 自有的 main.dart 与 bridge/ 方法表。
    for owned in (DST / 'core', DST / 'mini_apps'):
        if owned.exists():
            shutil.rmtree(owned)
    for rel in sorted(seen):
        out = DST / rel
        out.parent.mkdir(parents=True, exist_ok=True)
        text = (SRC / rel).read_text(encoding='utf-8')
        out.write_text(PKG_IMPORT.sub(f"import 'package:{NEW}/\\1'", text), encoding='utf-8')
    print(f'copied {len(seen)} files -> {DST.relative_to(ROOT)}')
    for rel, why in skipped:
        print(f'  skip {rel}  ({why})')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
