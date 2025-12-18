/**
 * 날짜 관련 유틸리티 함수
 */

/**
 * 주의 시작일(월요일) 계산
 * @param {Date} date - 기준 날짜
 * @returns {Date} 해당 주의 월요일 00:00:00
 */
export function startOfWeek(date) {
  const d = new Date(date);
  const day = d.getDay(); // 0 Sun .. 6 Sat
  const diff = day === 0 ? -6 : 1 - day; // 월요일 기준
  d.setDate(d.getDate() + diff);
  d.setHours(0, 0, 0, 0);
  return d;
}

/**
 * 날짜를 YYYY-MM-DD 형식으로 포맷
 * @param {Date} date - 날짜 객체
 * @returns {string} YYYY-MM-DD 형식 문자열
 */
export function formatDate(date) {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

/**
 * 날짜를 YYYY.MM.DD 형식으로 포맷
 * @param {Date} date - 날짜 객체
 * @returns {string} YYYY.MM.DD 형식 문자열
 */
export function formatDateDot(date) {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}.${mm}.${dd}`;
}

/**
 * 날짜에 일수를 더함
 * @param {Date} date - 기준 날짜
 * @param {number} days - 더할 일수
 * @returns {Date} 새로운 날짜 객체
 */
export function addDays(date, days) {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

/**
 * 오늘 날짜 (00:00:00)
 * @returns {Date}
 */
export function today() {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
}
