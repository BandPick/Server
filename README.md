# Server

## 🎸 BandPick — Server

> 밴드 동아리 공연 매칭 시스템 **백엔드**  
> 부원 데이터 관리, 자동 팀 매칭 알고리즘, 합주 스케줄 최적화를 담당하는 REST API 서버
> 
<br/>

## 📌 프로젝트 개요

정기공연을 준비할 때마다 기획자 1명이 30명 가까운 부원들의 희망 곡, 포지션, 스케줄을 수작업으로 정리하던 문제를 해결하기 위해 시작된 프로젝트

- 부원이 직접 희망 곡·포지션·합주 가능 시간을 입력
- 수집된 데이터를 기반으로 자동 팀 매칭 및 합주 스케줄 생성
- 기획자가 시각적으로 확인하고 수동으로 조정 가능

<br/>

## 🛠 기술 스택

BACKEND

<img src="https://img.shields.io/badge/Java 17-007396?style=for-the-badge&logo=openjdk&logoColor=white"> <img src="https://img.shields.io/badge/Spring Boot 3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Spring Data JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white"> <img src="https://img.shields.io/badge/REST API-009688?style=for-the-badge&logo=jsonwebtokens&logoColor=white">

DATABASE

<img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white">
TOOLS & DOCS



RELEASE

<img src="https://img.shields.io/badge/Render-000000?style=for-the-badge&logo=render&logoColor=white">

<br/>

## 📁 프로젝트 구조

```
작성 예정
```

<br/>

## 🧩 알고리즘 설계

### 팀 매칭 알고리즘 (`MatchingService`)

1. 전체 부원의 희망 곡·포지션 데이터를 우선순위 순으로 정렬
2. 드럼 포지션 부원을 기준으로 1차 그룹 구성 (드럼 없이 합주 불가)
3. 1순위 희망 곡이 겹치는 부원들 중 포지션 공백을 채우도록 배정
4. 합주 가능 시간 교집합이 존재하는 조합을 우선 선택
5. 배정되지 못한 부원은 2순위 → 3순위 순으로 재시도

### 스케줄 최적화 알고리즘

**배정 우선순위**

| 순위 | 조건 |
|------|------|
| 1순위 | 팀 전원 합주 가능 시간대 |
| 2순위 | 보컬 제외 세션 전체 합주 가능 시간대 |
| 3순위 | 1~2명 빠진 상태로 합주 가능 시간대 |

**스케줄 생성 규칙**

- 합주 횟수: 주 2~3회
- 합주 시간 단위: 30분 / 45분 / 60분 (설정 가능)
- 팀 간 시간 충돌 자동 감지 및 조정

<br/>

## 🗄 데이터베이스 스키마

```
작성 예정
```

<br/>

## 📖 API 시트

https://www.notion.so/API-3257f216435b80bb82f8d448f3f50ddb?source=copy_link

<br/>

## 🤝 기여 가이드

### 브랜치 전략

```
main          # 프로덕션 배포 브랜치
└── dev       # 개발 통합 브랜치
    ├── feat/member-api
    ├── feat/matching-algorithm
    └── fix/...
```

### 커밋 메시지 컨벤션

```
feat: 새로운 기능 추가
fix: 버그 수정
refactor: 코드 리팩토링
test: 테스트 코드 추가/수정
chore: 빌드 설정, 패키지 업데이트 등
docs: 문서 수정
```

### PR 규칙

- `dev` 브랜치로 PR 생성
- 최소 1명 이상 코드 리뷰 후 머지
- PR 제목은 커밋 컨벤션과 동일한 형식 사용
