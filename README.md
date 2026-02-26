# 🍽️ YumCoach
> 얌코치 — AI 기반 식단 관리 & 건강 코칭 서비스

---

![메인 데모](docs/main_gif.gif)

![프로젝트 소개](docs/Pasted%20image%2020260226004327.png)

## 📋 목차
- [빠른 시작](#-빠른-시작)
- [아키텍처](#-아키텍처)
- [기술 스택](#-기술-스택)
- [프로젝트 구조](#-프로젝트-구조)
- [주요 기능](#-주요-기능)
- [환경변수](#-환경변수)
- [주요 명령어](#-주요-명령어)
- [트러블슈팅](#-트러블슈팅)

---

## 🚀 빠른 시작

### 1. 환경변수 설정

```bash
# 루트 .env 파일 생성
cp .env.example .env
```

`.env` 파일 주요 항목:
```dotenv
# MySQL
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=yumcoach_db
MYSQL_USER=yumcoach_user
MYSQL_PASSWORD=yumcoach_pass123
MYSQL_ROOT_PASSWORD=root1234

# 서버
BACKEND_PORT=8282
FRONTEND_PORT=3000

# JWT
JWT_SECRET_BASE64=<base64 시크릿>
JWT_ACCESS_TOKEN_VALIDITY=3600000
JWT_REFRESH_TOKEN_VALIDITY=604800000

# 프로파일
SPRING_PROFILES_ACTIVE=local
```

### 2. Docker Compose 실행

```bash
# 전체 서비스 빌드 & 시작
docker compose up -d --build

# 로그 확인
docker compose logs -f
```

### 3. 접속

| 서비스 | URL |
|--------|-----|
| 프론트엔드 | http://localhost:3000 |
| 백엔드 API | http://localhost:8282 |
| Swagger UI | http://localhost:8282/swagger-ui/index.html |

---

![아키텍처 다이어그램](docs/Pasted%20image%2020260225095219.png)

## 🏗️ 아키텍처


![시스템 아키텍처](docs/Pasted%20image%2020260225001753.png)


### Docker 서비스 구성

| 서비스 | 컨테이너 | 포트 | 설명 |
|--------|----------|------|------|
| `redis` | yumcoach-redis | 6379 | 세션/인증코드 캐시 |
| `mysql` | mysql-db | 3306 | 메인 데이터베이스 |
| `backend` | spring-backend | 8282 | Spring Boot API 서버 |
| `frontend` | vue-frontend | 3000 | Vue.js 개발 서버 |

> **프론트엔드 → 백엔드 통신**: Vite 프록시를 통해 `/api/*` 요청이 `spring-backend:8282`로 전달됩니다. 프론트엔드에서 백엔드를 직접 호출하지 않습니다.

---

## 🛠️ 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| **Backend** | Spring Boot | 4.0.0 |
| | MyBatis | 4.0.0 |
| | Java | 17 |
| | Spring Security | - |
| | JWT (jjwt) | 0.11.5 |
| **Database** | MySQL | 8.0+ |
| | Redis | 7.0+ |
| **Frontend** | Vue.js | 3.x |
| | Vite | 7.x |
| | Pinia | - |
| | Axios | - |
| | Vue Router | - |
| **AI** | FastAPI (Python) | - |
| **Infra** | Docker + Compose | - |

---

## 📁 프로젝트 구조

```
YumCoach/
├── compose.yaml                # Docker Compose 설정
├── .env                        # 환경변수 (생성 필요)
├── .env.example                # 환경변수 템플릿
│
├── backend/                    # Spring Boot 백엔드
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ssafy/yumcoach/
│       │   ├── auth/           # 인증 (JWT, 카카오 OAuth, 이메일)
│       │   ├── user/           # 회원 관리
│       │   ├── meal/           # 식단 기록
│       │   ├── food/           # 음식 데이터
│       │   ├── report/         # 일간/주간 리포트
│       │   ├── challenge/      # 챌린지
│       │   ├── community/      # 커뮤니티
│       │   ├── chatbot/        # AI 챗봇
│       │   ├── payments/       # Toss 결제
│       │   └── config/         # Security, CORS, Redis 등
│       └── resources/
│           ├── application.yml           # 공통 설정 (메일, Redis)
│           ├── application-local.yml     # 로컬 프로필 (DB, JWT, CORS 등)
│           └── mapper/                   # MyBatis XML 매퍼
│
├── frontend/                   # Vue.js 프론트엔드 (현재 사용)
│   ├── Dockerfile
│   ├── package.json
│   ├── vite.config.js          # 프록시 설정 포함
│   ├── .env                    # Vite 환경변수
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── lib/api.js          # Axios 인스턴스 (baseURL: /api)
│       ├── stores/auth.js      # Pinia 인증 스토어
│       ├── pages/              # 페이지 컴포넌트
│       ├── components/         # 재사용 컴포넌트
│       ├── api/                # API 호출 모듈
│       ├── composables/        # Vue Composables
│       └── router/             # 라우팅 설정
│
├── ai/                         # AI 서비스 (FastAPI)
├── init-db/                    # DB 초기화 스크립트
├── frontend_v1/                # 초기 프로토타입 (사용 안 함)
├── yumcoach_legacy/            # 레거시 코드 (사용 안 함)
└── docs/                       # 문서 및 이미지
```

---

## ✨ 주요 기능

| 기능            | 설명                                               |
| ------------- | ------------------------------------------------ |
| **회원가입/로그인**  | 이메일 + 카카오 OAuth                                  |
| **JWT 인증**    | AccessToken(메모리) + RefreshToken(HttpOnly Cookie) |
| **식단 기록**     | 음식 검색, 식사 로그, AI 식단 이미지 분석                       |
| **식단 분석 리포트** | AI 기반 일일 리포트 및 주간 리포트 생성                         |
| **챌린지**       | 식단 목표 트래킹                                        |
| **AI 코치 챗봇**  | 식단 상담, 리포트 기반 코칭                                 |
| **마이페이지**     | 건강 정보 관리, 식이 제한 설정                               |
| **커뮤니티**      | 게시글/댓글 CRUD                                      |
| **결제**        | Toss Payments 연동                                 |

--- 
### OAuth

![OAuth 로그인](docs/Pasted%20image%2020260226123508.png)

---
### 식단 기록

![식단 기록](docs/Pasted%20image%2020260226123545.png)

#### 식단 메뉴 추가 - 검색

![식단 메뉴 검색](docs/Pasted%20image%2020260226124945.png)

#### 식단 메뉴 추가 - OCR

![식단 OCR](docs/식단OCR.gif)

#### 메뉴 검색

![메뉴 검색](docs/식단검색.gif)

---
#### 식단 내역

![식단 내역](docs/Pasted%20image%2020260226125555.png)


### 식단 분석 & 분석 리포트

![식단 분석](docs/Pasted%20image%2020260226124153.png)

##### 일일 리포트 생성

![일일 리포트 생성](docs/일일리포트생성.gif)
- 일일 리포트
	- 일반 유저: 1회
	- 어드밴스드 유저: 2회
- 주간 리포트
	- 일반 유저: 5회
	- 어드밴스드 유저: 10회

##### 분석 리포트 - AI 한마디

![AI 한마디](docs/Pasted%20image%2020260226131759.png)

##### 분석 리포트 - 목표 추천

![목표 추천](docs/Pasted%20image%2020260226131809.png)

##### 분석 리포트 - 식단 총평

![식단 총평](docs/Pasted%20image%2020260226131859.png)

---
### 챌린지

#### 챌린지 생성

![챌린지 생성](docs/Pasted%20image%2020260226143435.png)

#### 챌린지 조회

![챌린지 조회](docs/Pasted%20image%2020260226143358.png)

---
### 어드밴스드 유저 - 간편 결제

![어드밴스드 결제](docs/어드밴스드결제.gif)

#### 어드밴스드 유저 - 리포트 메일 발송 (스케줄러)

![리포트 메일 발송](docs/Pasted%20image%2020260226133540.png)
- 카카오톡 채널톡(카카오톡 비즈) 발송 기능은 사업자 관련 이슈로 도입하지 못했고 SMTP를 이용해 메일 발송 기능으로 구현
	- `매일 새벽 1시` 전날 기록한 식단을 바탕으로 일일 리포트를 생성해서 메일로 전송
	- `매주 월요일 새벽 1시` 지난주에 기록한 식단들을 바탕으로 주간 리포트를 생성해서 메일로 전송
- 현재 `새벽 1시`부터 스케줄링 작업 진행시 `새벽 6시`까지 최대 150명의 사용자 수용 가능 (주간 리포트를 생성하는 월요일은 최대 100명)
	- 해당 근거는 하나의 리포트가 생성되는 시간을 바탕으로 추정
		- ${스케줄링에 할당된 시간: 300분} / {일일 리포트 생성 시간: 2분} = 150명$
		- ${스케줄링에 할당된 시간: 300분} / {주간 리포트 생성 시간: 3분} = 100명$
- 사용자 증가시 배치 작업 도입 고려

---
### AI 코치 챗봇

![AI 챗봇](docs/Pasted%20image%2020260226142529.png)

#### 해시태그 기반 질문

![해시태그 질문](docs/Pasted%20image%2020260226142630.png)
- `#식단`: 식사에 관한 조언
- `#상담`: 식단과 생활 패턴 전반에 관한 이야기
- `#일일리포트`: 만들어진 일일 리포트를 기반으로 대화 (24시간)
- `#주간리포트`: 만들어진 주간 리포트를 기반으로 대화 (최근 7일, 168시간)

---
### 커뮤니티

![커뮤니티](docs/Pasted%20image%2020260226123428.png)

---
## 🔧 환경변수

### 루트 `.env` (Docker Compose용)

```dotenv
# MySQL
MYSQL_HOST=localhost          # Docker 내에서는 compose가 'mysql'로 오버라이드
MYSQL_PORT=3306
MYSQL_DATABASE=yumcoach_db
MYSQL_USER=yumcoach_user
MYSQL_PASSWORD=yumcoach_pass123
MYSQL_ROOT_PASSWORD=root1234

# 서버 포트
BACKEND_PORT=8282
FRONTEND_PORT=3000

# JWT
JWT_SECRET_BASE64=<base64 인코딩된 시크릿>
JWT_ACCESS_TOKEN_VALIDITY=3600000       # 1시간
JWT_REFRESH_TOKEN_VALIDITY=604800000    # 7일

# Spring 프로파일
SPRING_PROFILES_ACTIVE=local

# Toss 결제
TOSS_SECRET_KEY=test_gsk_docs_...

# 프론트엔드 (Vite proxy 사용)
VITE_API_BASE_URL=/api
API_BASE_URL_DOCKER=http://spring-backend:8282
```

### 백엔드 `application-local.yml`

로컬/Docker 개발 환경에서 사용되는 프로필별 설정 파일입니다.
- DataSource (MySQL 연결)
- MyBatis 매퍼 경로
- JWT, 카카오 OAuth, CORS
- 메일 (SMTP)
- Toss 결제 키

---

## 📝 주요 명령어

### Docker Compose

```bash
# 전체 빌드 & 시작
docker compose up -d --build

# 백엔드만 재빌드
docker compose up -d --build backend

# 프론트엔드만 재빌드
docker compose up -d --build frontend

# 중지
docker compose down

# 볼륨 포함 삭제 (DB 초기화)
docker compose down -v

# 로그 확인
docker compose logs -f backend
docker compose logs -f frontend

# 상태 확인
docker compose ps
```

### 로컬 개발 (Docker 없이)

```bash
# Backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Frontend
cd frontend
npm install
npm run dev
```

---

## 🐛 트러블슈팅

### Maven 빌드 캐시 문제

```bash
docker compose down
docker volume rm yumcoach_maven-cache
docker compose up -d --build backend
```

### CORS 에러

프론트엔드에서 백엔드를 직접 호출(`http://localhost:8282/...`)하면 CORS 에러가 발생합니다.
- 반드시 **Vite 프록시**(`/api/...`)를 통해 호출해야 합니다
- `frontend/src/lib/api.js`의 `baseUrl`이 `/api`인지 확인하세요