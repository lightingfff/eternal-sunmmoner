#!/usr/bin/env python3
"""Package a complete APK in a classic ZIP and verify bytes, CRCs and hashes."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

APK = 'Eternal-Summoner-debug.apk'
PORTABLE = 'Eternal-Summoner-Android.zip'

def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()

def valid_apk(path):
    with zipfile.ZipFile(path) as archive:
        assert archive.testzip() is None, 'APK CRC failure'
        names = set(archive.namelist())
        assert {'AndroidManifest.xml', 'classes.dex', 'resources.arsc'} <= names, 'Incomplete APK'
        assert len(names) == len(archive.infolist()), 'Duplicate APK entries'
    assert path.stat().st_size > 0, 'Empty APK'

def verify(folder):
    apk = folder / APK
    valid_apk(apk)
    lines = (folder / 'SHA256SUMS').read_text().splitlines()
    assert len(lines) == 2 and {line.split('  ', 1)[1] for line in lines} == {APK, PORTABLE}, 'Incomplete checksum manifest'
    for line in lines:
        expected, name = line.split('  ', 1)
        assert name in (APK, PORTABLE), 'Unexpected checksum path'
        assert digest(folder / name) == expected, f'Hash mismatch: {name}'
    with zipfile.ZipFile(folder / PORTABLE) as archive:
        assert archive.testzip() is None, 'Distribution ZIP CRC failure'
        assert set(archive.namelist()) == {APK, 'APK-SHA256.txt'}, 'Unexpected ZIP contents'
        entry = archive.getinfo(APK)
        assert entry.file_size == apk.stat().st_size, 'Truncated packaged APK'
        assert hashlib.sha256(archive.read(APK)).hexdigest() == digest(apk), 'Packaged bytes differ'
        assert archive.read('APK-SHA256.txt').decode() == f'{digest(apk)}  {APK}\n'
        assert all(i.extract_version <= 20 for i in archive.infolist()), 'Requires ZIP64 extraction'
    print(json.dumps({'apk': APK, 'bytes': apk.stat().st_size, 'sha256': digest(apk),
                      'portable_zip': PORTABLE, 'integrity': 'passed'}, indent=2))

def package(source, folder):
    valid_apk(source)
    folder.mkdir(parents=True, exist_ok=True)
    apk = folder / APK
    shutil.copyfile(source, apk)
    assert digest(source) == digest(apk), 'Copy failed'
    with zipfile.ZipFile(folder / PORTABLE, 'w', compression=zipfile.ZIP_STORED, allowZip64=False) as archive:
        archive.write(apk, APK)
        archive.writestr('APK-SHA256.txt', f'{digest(apk)}  {APK}\n')
    (folder / 'SHA256SUMS').write_text(''.join(f'{digest(folder / name)}  {name}\n' for name in (APK, PORTABLE)))
    verify(folder)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', type=Path)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--verify', type=Path)
    args = parser.parse_args()
    if args.verify:
        verify(args.verify)
    else:
        parser.error('--apk and --output are required') if not args.apk or not args.output else package(args.apk, args.output)
