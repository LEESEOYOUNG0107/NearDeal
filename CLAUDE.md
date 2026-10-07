# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

This is a freshly scaffolded Spring Boot project (generated via start.spring.io) with no
application code yet — only the default `CarrotApplication` entry point and a placeholder
`contextLoads()` test. There are no controllers, entities, repositories, or services. When
adding the first real code, use `kr.hs.study.carrot` as the base package and follow standard
Spring Boot layering (`controller` / `service` / `repository` / `entity` / `dto` subpackages)
since none of that structure exists yet to infer from.

Not a git repository yet (`git init` has not been run).

## Commands

Use the Gradle wrapper (`gradlew.bat` on this Windows environment; PowerShell is the primary shell).

- Build: `./gradlew.bat build`
- Run the app: `./gradlew.bat bootRun`
- Run all tests: `./gradlew.bat test`
- Run a single test class: `./gradlew.bat test --tests "kr.hs.study.carrot.CarrotApplicationTests"`
- Run a single test method: `./gradlew.bat test --tests "kr.hs.study.carrot.CarrotApplicationTests.contextLoads"`

Tests use JUnit 5 (`useJUnitPlatform()` is configured in `build.gradle`).

## Stack and dependencies

- Java 21 (via Gradle toolchain), Spring Boot 4.1.1, Gradle (Groovy DSL, `build.gradle`)
- Web: Spring Web MVC + Thymeleaf templates (`thymeleaf-extras-springsecurity6` is included, so
  templates are expected to use `sec:` attributes once security is wired up)
- Data: Spring Data JPA + Spring JDBC, running against Oracle (`ojdbc17` is the only JDBC driver
  on the classpath — this is an Oracle-backed project, not H2/Postgres/MySQL)
- Security: Spring Security (`spring-boot-starter-security`)
- AI: Spring AI's Google Gemini starter (`spring-ai-starter-model-google-genai`), pinned via the
  Spring AI BOM at version `2.0.1` (see `ext.springAiVersion` in `build.gradle`). Key comes from the
  `GEMINI_API_KEY` env var; code only uses `ChatClient`, so the provider can be swapped without touching Service/Controller
- Lombok is available (`compileOnly` + annotation processor) for both main and test source sets
- Spring Boot DevTools is on the classpath for local development (auto-restart on change)

- Auth: signup/login are implemented (`/api/auth/*`, `Member` entity, BCrypt). JWT access tokens (HS256, Spring Security's
  `spring-security-oauth2-jose`) are issued by `security/JwtProvider`; the key comes from the `JWT_SECRET` env var (32+ bytes),
  falling back to a random per-start key. Spring Security validates the Bearer token on every request (`SecurityConfig`);
  `/api/**` requires login except signup/login and `GET /api/products` (list/search) and `GET /api/products/{id}`.
  Controllers get the logged-in member id with `@CurrentUserId Long userId`; WebSocket authenticates on STOMP CONNECT
  (`Authorization: Bearer ...`). The Thymeleaf pages are still open and use a dev `uid` cookie until a login page exists.

`application.properties` holds a local Oracle datasource (hardcoded dev credentials) and the AI settings;
the Gemini API key is read from the `GEMINI_API_KEY` env var and must never be written into the file.
Move the datasource credentials to env vars or an untracked override before this goes into git.
