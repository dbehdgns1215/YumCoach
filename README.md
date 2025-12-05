# 🍽️ YumCoach

> 얌코치

## 📋 목차
- [빠른 시작](#-빠른-시작)
- [기술 스택](#-기술-스택)
- [프로젝트 구조](#-프로젝트-구조)
- [주요 명령어](#-주요-명령어)
- [트러블슈팅](#-트러블슈팅)

---

## 🚀 빠른 시작

### 1. 환경변수 설정

```bash
# 루트 .env 파일 생성
cp .env.example .env
```

`.env` 파일 내용 확인/수정:
```dotenv
# MySQL 설정
MYSQL_ROOT_PASSWORD=루트 계정 비밀번호
MYSQL_DATABASE=DB 이름
MYSQL_USER=유저 계정(당장 필요하진 않음)
MYSQL_PASSWORD=유저 비밀번호

# 포트 설정
MYSQL_PORT_LOCAL=3307
BACKEND_PORT=8080
FRONTEND_PORT=3000
```

### 2. Docker Compose 실행

```bash
# 전체 서비스 시작 (최초 1회 및 도커파일 수정시) 
docker-compose build --no-cache
docker-compose up -d

# 전체 서비스 시작
docker-compose up -d

# 볼륨 삭제
docker-compose down -v

# 로그 확인
docker-compose logs -f
```

### 3. 접속

- **프론트엔드**: http://localhost:3000
- **백엔드 API**: http://localhost:8080

---

## 🛠️ 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| **Backend** | Spring Boot | |
| | MyBatis | |
| | Java | |
| | MySQL | |
| **Frontend** | Vue.js | |
| | Vite | |
| **Infra** | Docker | - |
| | Docker Compose | |

---

## 📁 프로젝트 구조

```
YumCoach/
├── compose.yaml              # Docker Compose 설정
├── .env                      # 환경변수 (생성 필요)
├── .env.example              # 환경변수 템플릿
│
├── yumcoach/                 # Spring Boot 백엔드 (로컬 개발용)
│   ├── pom.xml               # Maven 설정
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ssafy/yumcoach/
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   └── mvnw                  # Maven Wrapper
│
├── backend/                  # Docker 배포용 백엔드
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│
├── frontend_v1/              # Vue.js 프론트엔드
│   ├── Dockerfile
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── components/
│       ├── pages/
│       └── router/
│
└── docs/                     # 문서
```

## 📝 주요 명령어

### Docker Compose

```bash
# 시작
docker-compose up -d

# 리빌드
docker-compose build --no-cache
docker-compose up -d

# 중지
docker-compose down

# 볼륨 포함 삭제
docker-compose down -v

# 로그 확인
docker-compose logs -f
docker-compose logs -f backend   # Backend만
docker-compose logs -f frontend  # Frontend만

# 상태 확인
docker-compose ps

# 재시작
docker-compose restart

# 특정 서비스 재빌드
docker-compose build --no-cache backend
```

### Backend (Maven)

```bash
cd yumcoach

# 실행
./mvnw spring-boot:run

# 빌드
./mvnw clean package

# 테스트
./mvnw test

# 테스트 제외 빌드
./mvnw clean package -DskipTests
```

### Frontend (npm)

```bash
cd frontend_v1

# 의존성 설치
npm install

# 개발 서버
npm run dev

# 프로덕션 빌드
npm run build

# 빌드 미리보기
npm run preview
```

---

## 🐛 트러블슈팅


### Maven 빌드 캐시 문제

**해결**
```bash
# Maven 캐시 볼륨 삭제
docker-compose down
docker volume rm yumcoach_maven-cache
docker-compose up -d --build backend
```

### Frontend 빌드 오류

**해결**
```bash
cd frontend_v1

# node_modules 재설치
rm -rf node_modules package-lock.json
npm install

# Docker 재빌드
docker-compose build --no-cache frontend
docker-compose up -d frontend
```

---

## 🔧 환경변수 설명

### 루트 `.env` (Docker Compose용)

```dotenv
# MySQL 설정
MYSQL_ROOT_PASSWORD=yumcoach_root    # MySQL root 비밀번호
MYSQL_DATABASE=yumcoach_db           # 데이터베이스 이름
MYSQL_USER=yumcoach                  # 애플리케이션 사용자
MYSQL_PASSWORD=yumcoach              # 사용자 비밀번호

# 포트 설정
MYSQL_PORT_LOCAL=3306                # MySQL 포트
BACKEND_PORT=8080                    # Spring Boot 포트
FRONTEND_PORT=3000                   # Vue 포트

# API URL
VITE_API_BASE_URL=http://localhost:8080  # Frontend → Backend

# Spring 프로파일
SPRING_PROFILES_ACTIVE=docker        # docker | local
```

---

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

<br/>

**커밋 메시지 규칙**
- `feat`: 새 기능
- `fix`: 버그 수정
- `docs`: 문서 수정
- `style`: 코드 포맷팅
- `refactor`: 리팩토링
- `test`: 테스트 추가
- `chore`: 빌드 설정 등

<br/>

**커밋 메시지 양식**
```scss
<타입>: <제목>
<본문>

<변경 사항 목록>
```

<br/>

**커밋 메시지 예시**
```scss
feat: 로그인 기능 구현
로그인 시 JWT 토큰을 반환하고, 이를 로컬 스토리지에 저장하여 인증을 처리합니다.

- 로그인 화면 추가
- 이메일과 비밀번호 필드 추가
- 서버 API와 연결하여 로그인 처리
```

---

## 📞 문의

프로젝트 링크: [https://github.com/dbehdgns1215/YumCoach](https://github.com/dbehdgns1215/YumCoach)