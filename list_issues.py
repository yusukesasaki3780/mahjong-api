from pathlib import Path
import re
roots = [Path('src/main/kotlin/com/example/usecase'), Path('src/main/kotlin/com/example/presentation/routes')]
issue_files = []
patterns = [
    re.compile(r'\?\?'),
    re.compile(r'[ãÃÂ]'),
]
for root in roots:
    for path in root.rglob('*.kt'):
        text = path.read_text(encoding='utf-8', errors='ignore')
        if any(p.search(text) for p in patterns):
            issue_files.append(path)
print(len(issue_files))
for p in issue_files:
    print(p)
