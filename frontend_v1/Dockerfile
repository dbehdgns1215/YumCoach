FROM node:20-alpine

WORKDIR /app

# package.json만 먼저 복사해서 의존성만 설치(빌드 빠르게)
COPY package*.json ./

# npm ci로 정확한 의존성 설치
RUN npm ci

# 나머지 파일은 컨테이너에 복사 (개발 시에는 volume으로 덮어쓰기 가능)
COPY . .

ENV HOST=0.0.0.0
ENV PORT=3000

EXPOSE 3000

# 개발 서버 실행 (hot-reload)
CMD ["npm", "run", "dev"]
