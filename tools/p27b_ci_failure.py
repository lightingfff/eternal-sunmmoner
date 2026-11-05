#!/usr/bin/env python3
"""Expose enough Gradle/JUnit failure detail in GitHub check annotations.

The sandbox cannot access Actions log blob hosts; only api.github.com annotations
are reachable. No uploaded log artifact or credentials are used.
"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

log = Path(sys.argv[1]).read_text(errors='replace').splitlines()
selected = [line.strip() for line in log if re.search(
    r'(^e:|error:|FAILED|FAILURE|What went wrong|Unresolved reference|Compilation error)', line, re.I)]
lines = selected[:15] or log[-15:]
results = Path(sys.argv[2])
for xml_path in sorted(results.glob('TEST-*P27B*.xml')):
    try:
        root = ET.parse(xml_path).getroot()
        for case in root.iter('testcase'):
            failed = case.find('failure')
            if failed is not None:
                msg = failed.get('message') or (failed.text or '')
                lines.append(f'{xml_path.name}:{case.get("name")}: {msg[:1100]}')
    except (OSError, ET.ParseError) as ex:
        lines.append(f'{xml_path.name}: XML read error: {ex}')
text = '\n'.join(lines)[:5400]
print('::error title=P27B Gradle/JUnit failure::' + text.replace('%','%25').replace('\r','%0D').replace('\n','%0A'))
