export function parseNumberedList(text) {
  // 간단한 정규식 기반 파서: "1) 바나나 먹기\n2) 물 1L" 같은 포맷을 항목 배열로 반환
  if (!text) return []
  // 줄바꿈을 기준으로 먼저 분리
  const lines = text.split(/\r?\n/)
  const items = []
  const numberedRegex = /^\s*\d+\)\s*(.+)$/
  for (const line of lines) {
    const m = line.match(numberedRegex)
    if (m && m[1].trim()) items.push(m[1].trim())
    else if (line.trim()) items.push(line.trim())
  }
  return items
}
