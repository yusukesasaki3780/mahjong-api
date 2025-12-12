import subprocess
from pathlib import Path
import os
core_autocrlf = subprocess.check_output(['git', 'config', 'core.autocrlf'], text=True).strip().lower() == 'true'
lines = subprocess.check_output(['git', 'status', '--porcelain'], text=True).splitlines()
paths = []
for line in lines:
    if not line:
        continue
    status = line[:2]
    if status.strip() in {'M', 'MM'} or status == ' M':
        paths.append(line[3:])
for rel in paths:
    data = subprocess.check_output(['git', 'show', f'HEAD:{rel}'])
    if core_autocrlf:
        data = data.replace(b'\n', b'\r\n')
    Path(rel).write_bytes(data)
print('restored', len(paths))
