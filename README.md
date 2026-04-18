# kmp-ai-novel-ignite

Kotlin Multiplatform 기반의 AI 소설 작성 앱 프로젝트입니다.

이 프로젝트는 `Android + Desktop`을 우선 대상으로 하며, 다음 흐름을 핵심 가치로 둡니다.

- 로컬 우선 집필 경험
- AI를 활용한 문서형 + 대화형 하이브리드 작성
- 재사용 가능한 소설 템플릿 제작
- 템플릿 게시판을 통한 공유와 remix
- 로컬 개발에서는 `Ollama`, 프로덕션에서는 `OpenRouter relay` 사용

## MVP 방향

초기 MVP는 아래 범위를 목표로 합니다.

- KMP + Compose Multiplatform 기반 클라이언트
- SQLDelight + SQLite 기반 로컬 저장
- `InferenceEngine` 추상화를 통한 AI 연결 계층 분리
- `LocalOllamaEngine`과 `RemoteOpenRouterEngine` 우선 구현
- Android 온디바이스 LLM은 이번 단계에서 구현하지 않고 아키텍처 슬롯만 확보
- 선택 로그인 기반 템플릿 게시/탐색/복제

## 문서

현재 제품 설계 스펙은 아래 문서에 정리되어 있습니다.

- [KMP AI Novel App Design](docs/superpowers/specs/2026-04-18-kmp-ai-novel-app-design.md)

## 구현 원칙

- 제품 차별화와 직접 관련 없는 영역은 검증된 안정적인 라이브러리를 우선 사용합니다.
- 로컬 집필 데이터는 기본적으로 기기 내 저장을 우선합니다.
- AI 제공자 전환이 가능하도록 UI와 도메인 로직은 모델 벤더에 직접 의존하지 않게 설계합니다.
- MVP에서는 소셜 기능보다 작가 중심의 집필 흐름과 템플릿 자산화를 우선합니다.

## 다음 단계

다음 구현 계획에서는 아래 순서로 진행하는 것을 기준으로 합니다.

1. KMP 프로젝트 골격 구성
2. SQLDelight 스키마와 로컬 저장 계층 정리
3. 기본 집필 화면과 프로젝트 모델 구성
4. Ollama 연동 기반 생성 흐름 연결
5. 템플릿 작성/적용 기능 추가
6. 게시판과 OpenRouter relay 연동 설계
