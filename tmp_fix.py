from pathlib import Path
path = Path('src/main/kotlin/com/example/presentation/routes/RankingRoutes.kt')
text = path.read_text(encoding='utf-8')
out = bytearray()
for ch in text:
    code = ord(ch)
    if code <= 0x00FF:
        out.append(code)
    else:
        out.extend(ch.encode('utf-8'))
fixed = out.decode('utf-8')
print(fixed.splitlines()[3])
