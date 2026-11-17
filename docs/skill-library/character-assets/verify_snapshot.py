#!/usr/bin/env python3
"""Verify the saved references offline without importing/executing upstream code."""
from __future__ import annotations

import ast
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import sys
from urllib.parse import unquote


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
OWN_MARKDOWN = [HERE / name for name in ('README.md', 'READING_NOTES.md', 'CATALOG.md')]
OWN_MARKDOWN += [ROOT / '.agents/skills/character-asset-pipeline/SKILL.md']


def check(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def project_path(relative: str) -> Path:
    p = Path(relative)
    check(not p.is_absolute() and '..' not in p.parts, f'Unsafe stored path: {relative}')
    result = (ROOT / p).resolve()
    check(result.is_relative_to(ROOT), f'Escaping path: {relative}')
    return result


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def local_markdown_links(path: Path):
    # This check covers project-authored Markdown, whose inline links have no nested
    # parentheses. Upstream incomplete supporting-file links are not claimed valid.
    for target in re.findall(r'\]\(([^)\s]+)\)', path.read_text()):
        if target.startswith(('#', 'https://', 'http://', 'mailto:')):
            continue
        yield unquote(target.split('#', 1)[0])


def verify() -> dict:
    data = json.loads((HERE / 'inventory.json').read_text())
    check(data['schema_version'] == 1, 'Unknown inventory schema')
    expected_counts = {'character': 1, 'meowa': 1, 'scenario': 179}
    repos = {r['id']: r for r in data['repositories']}
    check(len(data['repositories']) == len(repos) == 3, 'Repository identities differ')
    check(set(repos) == set(expected_counts), 'Repository set differs')
    for key, repo in repos.items():
        check(re.fullmatch(r'[0-9a-f]{40}', repo['commit']) is not None, f'Invalid pin: {key}')
        check(repo['skill_count'] == expected_counts[key], f'Invalid declared count: {key}')

    files = data['files']
    check(len({f['local'] for f in files}) == len(files), 'Duplicate retained file')
    count_by_repository = Counter(f['repository'] for f in files)
    check(count_by_repository == {'meowa': 37, 'scenario': 27}, 'Snapshot file set changed')
    for entry in files:
        p = project_path(entry['local'])
        check(p.is_file(), f'Missing retained file: {entry["local"]}')
        check(p.stat().st_size == entry['bytes'] and sha256(p) == entry['sha256'],
              f'Source bytes changed: {entry["local"]}')
    retained = {project_path(f['local']) for f in files}
    actual = {p.resolve() for folder in (HERE / 'meowa/upstream', HERE / 'scenario/delta')
              for p in folder.rglob('*') if p.is_file()}
    actual.add(HERE / 'scenario/LICENSE')
    check(actual == retained, 'Uninventoried or missing snapshot files')

    skills = data['skills']
    check(Counter(s['repository'] for s in skills) == expected_counts, 'Skill counts differ')
    check(len({(s['repository'], s['source']) for s in skills}) == 181,
          'Duplicate entrypoint source')
    old_data = json.loads((ROOT / 'docs/skill-library/scenario/inventory.json').read_text())
    old = {s['source']: s for s in old_data['skills']}
    delta = Counter()
    catalog = (HERE / 'CATALOG.md').read_text()
    for skill in skills:
        repo = repos[skill['repository']]
        check(skill['name'] and skill['description'] and skill['headings'],
              f'Missing indexed metadata: {skill["source"]}')
        check(f'| {skill["repository"]} | {skill["name"]} |' in catalog,
              f'Missing catalog row: {skill["name"]}')
        check(f'/blob/{repo["commit"]}/{skill["source"]}' in skill['url'], 'Unpinned URL')
        if skill['local']:
            p = project_path(skill['local'])
            check(p.is_file() and sha256(p) == skill['sha256'],
                  f'Entrypoint hash mismatch: {skill["source"]}')
            check(p.stat().st_size == skill['bytes'], 'Entrypoint size mismatch')
            body = p.read_text()
            check(body.startswith('---\n') and '\n---\n' in body[4:],
                  f'Invalid frontmatter: {skill["source"]}')
        else:
            check(skill['repository'] == 'character' and skill['status'] == 'notes-only',
                  'Unexpected remote-only entrypoint')
        if skill['repository'] == 'scenario':
            prior = old.get(skill['source'])
            state = 'added' if prior is None else 'unchanged' if prior['sha256'] == skill['sha256'] else 'changed'
            check(skill['status'] == state, f'Delta mismatch: {skill["source"]}')
            delta[state] += 1
    check(delta == {'added': 5, 'changed': 11, 'unchanged': 163}, 'Scenario delta differs')
    scenario_paths = {s['source'] for s in skills if s['repository'] == 'scenario'}
    removed = sorted(old.keys() - scenario_paths)
    check(repos['scenario']['removed_since_archive'] == removed == [], 'Unexpected removal')

    character = repos['character']['source_files']
    check(len(character) == len({f['source'] for f in character}) == 16,
          'Character metadata incomplete')
    for entry in character:
        check(re.fullmatch(r'[0-9a-f]{64}', entry['sha256']) is not None and entry['bytes'] > 0,
              'Invalid remote source metadata')
    main = next(f for f in character if f['source'] == 'SKILL.md')
    entry = next(s for s in skills if s['repository'] == 'character')
    check(entry['sha256'] == main['sha256'], 'Character entrypoint metadata differs')

    python_count = json_count = 0
    for p in sorted(retained):
        if p.suffix == '.py':
            ast.parse(p.read_text(), filename=str(p))
            python_count += 1
        if p.suffix == '.json':
            json.loads(p.read_text())
            json_count += 1
    for p in (HERE / 'meowa/upstream/LICENSE', HERE / 'scenario/LICENSE'):
        text = p.read_text()
        check('MIT License' in text and 'Permission is hereby granted' in text
              and 'Copyright (c) 2026' in text, f'Missing license notice: {p}')
    links = 0
    for p in OWN_MARKDOWN:
        for relative in local_markdown_links(p):
            target = (p.parent / relative).resolve()
            # The report is written after this check, including on a first run.
            if target != HERE / 'verification.json':
                check(target.is_file(), f'Broken link {p.name}: {relative}')
            links += 1
    return {
        'status': 'PASS', 'entrypoints_indexed': 181,
        'retained_source_files_hash_verified': len(files),
        'unchanged_scenario_entrypoints_hash_verified': delta['unchanged'],
        'scenario_delta': dict(delta), 'python_files_ast_parsed_without_execution': python_count,
        'retained_json_files_parsed': json_count, 'project_authored_local_links_verified': links,
        'character_remote_metadata_records': len(character),
        'character_source_bytes_retained': False,
        'upstream_full_security_audit': False,
        'upstream_provider_or_script_executed': False,
        'paid_calls_or_remote_uploads': False, 'apk_build_or_device_test': 'NOT RUN: reference-only task',
    }


def main() -> int:
    report_path = HERE / 'verification.json'
    try:
        report = verify()
    except (ValueError, KeyError, TypeError, SyntaxError, OSError) as error:
        report = {'status': 'FAIL', 'error': str(error)}
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if report['status'] == 'PASS' else 1


if __name__ == '__main__':
    sys.exit(main())
