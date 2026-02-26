# Report 저장 및 AI 파이프라인 문서

이 문서는 `report` 생성부터 AI 분석 결과가 DB에 저장되는 전체 흐름을 설명합니다.

개요
- 리포트 생성 엔드포인트: `POST /api/reports/daily`, `POST /api/reports/weekly`
- 생성 시점: `ReportServiceImpl.createDailyReport` / `createWeeklyReport`에서 `report` 레코드가 먼저 생성(status=PROGRESS)
- 생성 로그: 각 생성 시도는 `report_generation_log`에 기록
- AI 분석: `OpenAiService.analyzeReport`가 FastAPI(또는 외부 AI 서비스)를 호출하여 원문(JSON)을 받아 `report.ai_response`에 저장
- 분석 결과 저장:
  - `report_insight` 테이블에 인사이트(`good/warn/keep`) 및 `coach`/`action` 항목 삽입
  - `report` 테이블의 `score`, `hero_title`, `hero_line`, `coach_message`, `next_action` 컬럼을 업데이트

테이블 요약
- `report` : 리포트 요약 (id, user_id, type, date/fromDate/toDate, status, score, hero_title, hero_line, coach_message, next_action, ai_response 등)
- `report_insight` : AI가 생성한 개별 인사이트(종류(kind), 제목, 본문)
- `report_generation_log` : 생성 시도 로그 (사용자/시스템 트리거, 결과코드 등)
- `user_generation_count` : 배치 동기화를 위한 사용자별 일/주 단위 요약(옵션)

저장 흐름 상세
1. 클라이언트가 생성 요청을 보냅니다 (`/api/reports/daily` 등).
2. `ReportServiceImpl`에서 `report` 레코드를 `status=PROGRESS`로 먼저 삽입합니다.
3. `meal` 데이터를 집계해 `report_meal`에 요약을 저장하고, 식사가 없으면 `report_generation_log`에 `NO_DATA` 기록 후 예외 반환.
4. AI 분석 시도:
   - `OpenAiService.analyzeReport(report)` 호출
   - AI(또는 FastAPI) 응답(원문 JSON)을 `report.ai_response` 컬럼에 저장합니다 (`reportMapper.updateReportAiResponse`).
   - 응답을 직렬화하여 `ReportAnalysisResult` DTO로 파싱에 성공하면:
     - `report_insight`에 `coach`/`action` 인사이트로 삽입
     - `report_insight`에 `good/warn/keep` 타입의 인사이트를 삽입
     - `report`의 `score`, `hero_title`, `hero_line`을 업데이트 (`updateReportScore`, `updateReportHero`)
     - (추가) `report`의 `coach_message`, `next_action` 컬럼을 즉시 업데이트하도록 구현되어 있습니다.
   - DTO 파싱 실패 시 폴백으로 JSON 노드를 읽어 인사이트를 삽입합니다.
5. AI 분석 성공 여부와 상관없이 `report_generation_log`에 결과 코드(`CREATED_WITH_AI` 또는 `CREATED_NO_AI`)를 기록합니다.
6. 클라이언트가 `GET /api/reports/daily?date=...` 등으로 조회하면, `ReportServiceImpl.getDailyReport`에서 `ai_response`가 존재하면 추가 파싱을 시도하여 누락 필드를 보정하고 DB 컬럼을 업데이트합니다(안정성 보정).

실무 권장사항
- AI 응답 스키마 통일: `coachMessage`, `nextAction`, `insights[]`, `score`, `heroTitle`, `heroLine` 같은 키가 일관되게 포함되도록 FastAPI의 출력 포맷을 고정하세요.
- 원본 보관: `ai_response` 원본을 항상 저장해 두면 재처리(파싱 개선, 버그 수정 후 재처리)가 가능합니다.
- 트랜잭션/성능: AI 호출은 외부 네트워크 작업이므로 리포트 생성 자체와 분리하거나 비동기화(예: 작업 큐)를 고려하세요. 현재는 동기 호출 후 결과를 저장하지만, 대규모 환경에서는 비동기화 권장.
- Null 값 문제: 만약 `report` 컬럼이 자주 NULL로 남는다면 다음을 확인하세요:
  1. AI 서비스의 응답이 실패하거나 빈값을 반환하는 경우
  2. `OpenAiService`에서 `report.getId()`가 null이거나 `updateReportAiResponse` 호출이 실패하는 경우
  3. 파싱 로직이 DTO 필드를 찾지 못해 `updateReportCoachMessage`/`updateReportNextAction` 등을 호출하지 못하는 경우

재처리(Repair) 방법
- 이미 저장된 `ai_response`가 있다면 `ReportServiceImpl.getDailyReport`/`getWeeklyReport`를 통해 재파싱 후 DB컬럼을 보정할 수 있습니다.
- 또는 간단한 재처리 스크립트를 만들어 `report` 테이블을 순회하며 `ai_response`를 파싱하고 누락 필드를 채우게 할 수 있습니다.

문의 및 다음 단계 제안
- 원하시면 재처리 스크립트(간단한 Java/Shell) 예시를 추가해 드리겠습니다.
- 또는 AI 응답 스키마를 강제하는 FastAPI 쪽 예시(스키마/유효성 검사)를 함께 만들어 드릴 수 있습니다.
