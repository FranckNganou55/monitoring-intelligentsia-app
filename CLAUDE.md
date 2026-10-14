# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Contexte du Projet

Refonte complète de la plateforme de surveillance d'un réseau monétique (GABs/ATMs, TPE/POS, transactions, fraude, flotte). L'ancien projet (Java 8 + JSF + Oracle + Tomcat) est analysé dans `ANALYSE_ANCIEN_PROJET.md`. Ce dépôt est le **nouveau projet**.

---

## Stack Technique

| Couche | Technologie |
|--------|-------------|
| Backend | Spring Boot 3.x, Java 21 |
| Frontend | React 18 + Vite + TypeScript |
| Base de données principale | PostgreSQL 16 |
| Base de données SVFE (lecture seule) | Oracle (datasource secondaire) |
| Conteneurisation | Docker + Docker Compose |
| Auth | JWT (access token 15 min + refresh token 7 jours) |
| Notifications temps réel | Server-Sent Events (SSE) |
| Emails | Spring Mail + Thymeleaf HTML templates |
| ORM | Spring Data JPA + Hibernate 6 |
| Migrations DB | Flyway |
| Documentation API | SpringDoc OpenAPI 3 / Swagger UI |
| Mapping | MapStruct |
| Réduction boilerplate | Lombok |
| Tests backend | JUnit 5 + Mockito + Testcontainers |
| Tests frontend | Vitest + Testing Library |

---

## Arborescence Complète du Projet

```
application-monitoring/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/org/intelligentsia/monitoring/
│   │   │   │   ├── MonitoringApplication.java
│   │   │   │   │
│   │   │   │   ├── config/
│   │   │   │   │   ├── SecurityConfig.java          # Spring Security + JWT filter chain
│   │   │   │   │   ├── JwtConfig.java               # JWT secret, expiry config
│   │   │   │   │   ├── OpenApiConfig.java           # Swagger / SpringDoc config
│   │   │   │   │   ├── MailConfig.java              # JavaMailSender config
│   │   │   │   │   ├── SseConfig.java               # SSE async config
│   │   │   │   │   ├── SchedulingConfig.java        # @EnableScheduling + ThreadPoolTaskScheduler
│   │   │   │   │   ├── SvfeDataSourceConfig.java    # Oracle datasource secondaire (SVFE)
│   │   │   │   │   └── AuditConfig.java             # Spring Data Auditing + AuditorAware
│   │   │   │   │
│   │   │   │   ├── domain/
│   │   │   │   │   │
│   │   │   │   │   ├── auth/
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   ├── User.java
│   │   │   │   │   │   │   ├── Role.java
│   │   │   │   │   │   │   ├── Permission.java
│   │   │   │   │   │   │   ├── RefreshToken.java
│   │   │   │   │   │   │   └── PasswordHistory.java
│   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   │   │   ├── AuthResponse.java
│   │   │   │   │   │   │   ├── RefreshTokenRequest.java
│   │   │   │   │   │   │   ├── UserDto.java
│   │   │   │   │   │   │   ├── UserCreateRequest.java
│   │   │   │   │   │   │   └── PasswordResetRequest.java
│   │   │   │   │   │   ├── mapper/
│   │   │   │   │   │   │   └── UserMapper.java
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   │   │   ├── RoleRepository.java
│   │   │   │   │   │   │   ├── PermissionRepository.java
│   │   │   │   │   │   │   ├── RefreshTokenRepository.java
│   │   │   │   │   │   │   └── PasswordHistoryRepository.java
│   │   │   │   │   │   ├── service/
│   │   │   │   │   │   │   ├── AuthService.java
│   │   │   │   │   │   │   ├── UserService.java
│   │   │   │   │   │   │   └── TokenService.java
│   │   │   │   │   │   └── controller/
│   │   │   │   │   │       ├── AuthController.java  # /api/auth/*
│   │   │   │   │   │       └── UserController.java  # /api/users/*
│   │   │   │   │   │
│   │   │   │   │   ├── monitoring/
│   │   │   │   │   │   ├── atm/
│   │   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   │   ├── Atm.java
│   │   │   │   │   │   │   │   └── AtmSnapshot.java
│   │   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   │   │   ├── AtmDto.java
│   │   │   │   │   │   │   │   ├── AtmSnapshotDto.java
│   │   │   │   │   │   │   │   └── AtmFilterRequest.java
│   │   │   │   │   │   │   ├── mapper/AtmMapper.java
│   │   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   │   │   ├── AtmRepository.java
│   │   │   │   │   │   │   │   └── AtmSnapshotRepository.java
│   │   │   │   │   │   │   ├── service/AtmService.java
│   │   │   │   │   │   │   └── controller/AtmController.java  # /api/atms/*
│   │   │   │   │   │   │
│   │   │   │   │   │   ├── network/
│   │   │   │   │   │   │   ├── entity/NetworkSnapshot.java
│   │   │   │   │   │   │   ├── dto/NetworkSnapshotDto.java
│   │   │   │   │   │   │   ├── repository/NetworkSnapshotRepository.java
│   │   │   │   │   │   │   ├── service/NetworkService.java
│   │   │   │   │   │   │   └── controller/NetworkController.java
│   │   │   │   │   │   │
│   │   │   │   │   │   ├── host/
│   │   │   │   │   │   │   ├── entity/HostSnapshot.java
│   │   │   │   │   │   │   ├── dto/HostSnapshotDto.java
│   │   │   │   │   │   │   ├── repository/HostSnapshotRepository.java
│   │   │   │   │   │   │   ├── service/HostService.java
│   │   │   │   │   │   │   └── controller/HostController.java
│   │   │   │   │   │   │
│   │   │   │   │   │   ├── transaction/
│   │   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   │   ├── TransactionRate.java
│   │   │   │   │   │   │   │   └── TransactionError.java
│   │   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   │   ├── service/TransactionService.java
│   │   │   │   │   │   │   └── controller/TransactionController.java
│   │   │   │   │   │   │
│   │   │   │   │   │   ├── fraud/
│   │   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   │   ├── FraudRate.java
│   │   │   │   │   │   │   │   └── FraudDetail.java
│   │   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   │   ├── service/FraudService.java
│   │   │   │   │   │   │   └── controller/FraudController.java
│   │   │   │   │   │   │
│   │   │   │   │   │   ├── fleet/
│   │   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   │   ├── FleetAccount.java
│   │   │   │   │   │   │   │   └── CardOverLimit.java
│   │   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   │   ├── service/FleetService.java
│   │   │   │   │   │   │   └── controller/FleetController.java
│   │   │   │   │   │   │
│   │   │   │   │   │   ├── pos/
│   │   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   │   ├── PosTerminal.java
│   │   │   │   │   │   │   │   ├── Technician.java
│   │   │   │   │   │   │   │   └── PosTelecollectSnapshot.java
│   │   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   │   ├── mapper/
│   │   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   │   ├── service/PosService.java
│   │   │   │   │   │   │   └── controller/
│   │   │   │   │   │   │       ├── PosController.java
│   │   │   │   │   │   │       └── TechnicianController.java
│   │   │   │   │   │   │
│   │   │   │   │   │   ├── extraction/
│   │   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   │   ├── ExtractionBatch.java
│   │   │   │   │   │   │   │   ├── ExtractionTerminal.java
│   │   │   │   │   │   │   │   └── ExtractionTransaction.java
│   │   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   │   ├── service/ExtractionService.java
│   │   │   │   │   │   │   └── controller/ExtractionController.java
│   │   │   │   │   │   │
│   │   │   │   │   │   └── dashboard/
│   │   │   │   │   │       ├── dto/DashboardSummaryDto.java
│   │   │   │   │   │       ├── service/DashboardService.java
│   │   │   │   │   │       └── controller/DashboardController.java  # /api/dashboard/*
│   │   │   │   │   │
│   │   │   │   │   ├── configuration/
│   │   │   │   │   │   ├── institution/
│   │   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   │   ├── Institution.java
│   │   │   │   │   │   │   │   ├── Currency.java
│   │   │   │   │   │   │   │   └── InstitutionCurrency.java
│   │   │   │   │   │   │   ├── dto/
│   │   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   │   ├── service/InstitutionService.java
│   │   │   │   │   │   │   └── controller/InstitutionController.java
│   │   │   │   │   │   │
│   │   │   │   │   │   └── scheduler/
│   │   │   │   │   │       ├── entity/SchedulerTask.java
│   │   │   │   │   │       ├── dto/SchedulerTaskDto.java
│   │   │   │   │   │       ├── repository/SchedulerTaskRepository.java
│   │   │   │   │   │       ├── service/SchedulerManagementService.java
│   │   │   │   │   │       └── controller/SchedulerController.java
│   │   │   │   │   │
│   │   │   │   │   ├── notification/
│   │   │   │   │   │   ├── email/
│   │   │   │   │   │   │   ├── EmailService.java
│   │   │   │   │   │   │   └── EmailType.java  (enum: WELCOME, RESET_PASSWORD, ALERT)
│   │   │   │   │   │   └── inapp/
│   │   │   │   │   │       ├── entity/Notification.java
│   │   │   │   │   │       ├── dto/NotificationDto.java
│   │   │   │   │   │       ├── repository/NotificationRepository.java
│   │   │   │   │   │       ├── service/NotificationService.java
│   │   │   │   │   │       ├── service/SseEmitterService.java
│   │   │   │   │   │       └── controller/NotificationController.java  # /api/notifications/*, /api/sse
│   │   │   │   │   │
│   │   │   │   │   └── audit/
│   │   │   │   │       ├── entity/AuditLog.java
│   │   │   │   │       ├── dto/AuditLogDto.java
│   │   │   │   │       ├── repository/AuditLogRepository.java
│   │   │   │   │       ├── service/AuditService.java
│   │   │   │   │       └── controller/AuditController.java  # /api/audit/*
│   │   │   │   │
│   │   │   │   ├── infrastructure/
│   │   │   │   │   ├── security/
│   │   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   │   │   └── CustomUserDetailsService.java
│   │   │   │   │   │
│   │   │   │   │   ├── exception/
│   │   │   │   │   │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   │   │   │   │   │   ├── BusinessException.java
│   │   │   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   │   │   └── ErrorResponse.java
│   │   │   │   │   │
│   │   │   │   │   ├── scheduler/
│   │   │   │   │   │   ├── MonitoringScheduler.java     # @Scheduled tasks, reads SchedulerTask config
│   │   │   │   │   │   └── tasks/
│   │   │   │   │   │       ├── AtmQueryTask.java
│   │   │   │   │   │       ├── NetworkQueryTask.java
│   │   │   │   │   │       ├── HostQueryTask.java
│   │   │   │   │   │       ├── TransactionQueryTask.java
│   │   │   │   │   │       ├── FraudQueryTask.java
│   │   │   │   │   │       ├── FleetQueryTask.java
│   │   │   │   │   │       └── TeleCollectQueryTask.java
│   │   │   │   │   │
│   │   │   │   │   └── svfe/                            # Anti-corruption layer — Oracle SVFE
│   │   │   │   │       ├── entity/
│   │   │   │   │       │   ├── CmsAtm.java
│   │   │   │   │       │   ├── CmsNetwork.java
│   │   │   │   │       │   ├── CmsHost.java
│   │   │   │   │       │   ├── CmsTransaction.java
│   │   │   │   │       │   ├── CmsFraud.java
│   │   │   │   │       │   ├── CmsAccount.java
│   │   │   │   │       │   ├── CmsCardLimit.java
│   │   │   │   │       │   ├── CmsPos.java
│   │   │   │   │       │   ├── CmsRule.java
│   │   │   │   │       │   └── SSAtm.java
│   │   │   │   │       └── repository/
│   │   │   │   │           ├── CmsAtmRepository.java
│   │   │   │   │           ├── CmsNetworkRepository.java
│   │   │   │   │           └── ...
│   │   │   │   │
│   │   │   │   └── shared/
│   │   │   │       ├── dto/
│   │   │   │       │   ├── PageResponse.java        # Generic paginated response wrapper
│   │   │   │       │   ├── ApiResponse.java         # { success, message, data }
│   │   │   │       │   └── FilterRequest.java       # { page, size, sortBy, direction, search }
│   │   │   │       ├── annotation/
│   │   │   │       │   └── Auditable.java           # Method-level annotation for audit logging
│   │   │   │       └── util/
│   │   │   │           ├── DateUtils.java
│   │   │   │           └── SecurityUtils.java       # Current user context helpers
│   │   │   │
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       ├── application-prod.yml
│   │   │       ├── db/
│   │   │       │   └── migration/
│   │   │       │       ├── V1__init_auth_tables.sql
│   │   │       │       ├── V2__monitoring_config_tables.sql
│   │   │       │       ├── V3__monitoring_snapshot_tables.sql
│   │   │       │       ├── V4__extraction_tables.sql
│   │   │       │       ├── V5__notification_audit_tables.sql
│   │   │       │       └── V6__seed_data.sql
│   │   │       └── templates/
│   │   │           └── email/
│   │   │               ├── welcome.html
│   │   │               ├── reset-password.html
│   │   │               ├── alert-atm.html
│   │   │               └── alert-fraud.html
│   │   │
│   │   └── test/
│   │       └── java/org/intelligentsia/monitoring/
│   │           ├── auth/
│   │           │   ├── AuthServiceTest.java
│   │           │   └── AuthControllerTest.java
│   │           ├── monitoring/
│   │           │   ├── AtmServiceTest.java
│   │           │   └── DashboardServiceTest.java
│   │           └── infrastructure/
│   │               └── scheduler/MonitoringSchedulerTest.java
│   │
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/
│   ├── public/
│   │   └── favicon.ico
│   ├── src/
│   │   ├── main.tsx
│   │   ├── App.tsx
│   │   ├── vite-env.d.ts
│   │   │
│   │   ├── app/
│   │   │   ├── router.tsx               # React Router v6 routes
│   │   │   ├── providers.tsx            # QueryClientProvider, AuthProvider, ThemeProvider
│   │   │   └── queryClient.ts
│   │   │
│   │   ├── pages/                       # Route-level components (thin shells)
│   │   │   ├── auth/
│   │   │   │   ├── LoginPage.tsx
│   │   │   │   ├── ForgotPasswordPage.tsx
│   │   │   │   └── ResetPasswordPage.tsx
│   │   │   ├── DashboardPage.tsx
│   │   │   ├── atm/
│   │   │   │   ├── AtmListPage.tsx
│   │   │   │   └── AtmDetailPage.tsx
│   │   │   ├── pos/
│   │   │   │   ├── PosListPage.tsx
│   │   │   │   └── TechnicianListPage.tsx
│   │   │   ├── TransactionPage.tsx
│   │   │   ├── FraudPage.tsx
│   │   │   ├── FleetPage.tsx
│   │   │   ├── extraction/
│   │   │   │   ├── ExtractionAtmPage.tsx
│   │   │   │   └── ExtractionPosPage.tsx
│   │   │   ├── admin/
│   │   │   │   ├── UserManagementPage.tsx
│   │   │   │   ├── InstitutionPage.tsx
│   │   │   │   ├── RolePage.tsx
│   │   │   │   └── SchedulerPage.tsx
│   │   │   └── AuditLogPage.tsx
│   │   │
│   │   ├── features/                    # Domain feature modules
│   │   │   ├── auth/
│   │   │   │   ├── components/
│   │   │   │   │   ├── LoginForm.tsx
│   │   │   │   │   └── ProtectedRoute.tsx
│   │   │   │   ├── hooks/
│   │   │   │   │   └── useLogin.ts
│   │   │   │   └── api/
│   │   │   │       └── authApi.ts
│   │   │   ├── dashboard/
│   │   │   │   ├── components/
│   │   │   │   │   ├── KpiCard.tsx
│   │   │   │   │   ├── AtmStatusWidget.tsx
│   │   │   │   │   ├── TransactionRateChart.tsx
│   │   │   │   │   ├── FraudRateChart.tsx
│   │   │   │   │   ├── FleetStatusWidget.tsx
│   │   │   │   │   ├── HostStatusList.tsx
│   │   │   │   │   └── NetworkStatusList.tsx
│   │   │   │   ├── hooks/
│   │   │   │   │   └── useDashboard.ts
│   │   │   │   └── api/
│   │   │   │       └── dashboardApi.ts
│   │   │   ├── atm/
│   │   │   │   ├── components/
│   │   │   │   │   ├── AtmTable.tsx
│   │   │   │   │   ├── AtmFilterBar.tsx
│   │   │   │   │   ├── AtmDetailPanel.tsx
│   │   │   │   │   └── CassetteGauge.tsx
│   │   │   │   ├── hooks/
│   │   │   │   └── api/
│   │   │   ├── pos/
│   │   │   ├── transactions/
│   │   │   ├── fraud/
│   │   │   ├── fleet/
│   │   │   ├── extraction/
│   │   │   ├── users/
│   │   │   └── notifications/
│   │   │       ├── NotificationPanel.tsx
│   │   │       ├── NotificationBell.tsx
│   │   │       └── hooks/
│   │   │           └── useSse.ts        # SSE subscription hook
│   │   │
│   │   ├── components/                  # Shared reusable UI components
│   │   │   ├── ui/
│   │   │   │   ├── Button.tsx
│   │   │   │   ├── Input.tsx
│   │   │   │   ├── Select.tsx
│   │   │   │   ├── Table.tsx            # Generic sortable/paginated table
│   │   │   │   ├── Modal.tsx
│   │   │   │   ├── Badge.tsx
│   │   │   │   ├── Card.tsx
│   │   │   │   ├── Pagination.tsx
│   │   │   │   ├── SearchBar.tsx
│   │   │   │   ├── DateRangePicker.tsx
│   │   │   │   ├── StatusBadge.tsx      # Color-coded status indicator
│   │   │   │   └── Tooltip.tsx
│   │   │   ├── charts/
│   │   │   │   ├── LineChart.tsx        # Recharts wrapper
│   │   │   │   ├── BarChart.tsx
│   │   │   │   ├── DonutChart.tsx
│   │   │   │   └── GaugeChart.tsx       # ATM cassette fill level
│   │   │   ├── layout/
│   │   │   │   ├── AppLayout.tsx        # Sidebar + Header + main content
│   │   │   │   ├── Sidebar.tsx
│   │   │   │   ├── Header.tsx
│   │   │   │   └── AuthLayout.tsx       # Centered card for login
│   │   │   └── feedback/
│   │   │       ├── LoadingSpinner.tsx
│   │   │       ├── ErrorBoundary.tsx
│   │   │       ├── EmptyState.tsx
│   │   │       └── PageLoader.tsx
│   │   │
│   │   ├── hooks/
│   │   │   ├── useAuth.ts
│   │   │   ├── usePermissions.ts        # RBAC checks
│   │   │   ├── usePagination.ts
│   │   │   ├── useDebounce.ts
│   │   │   └── useInstitutionFilter.ts  # Current user institution context
│   │   │
│   │   ├── services/
│   │   │   ├── api.ts                   # Axios instance + interceptors (auth header, 401 refresh)
│   │   │   ├── authService.ts
│   │   │   ├── atmService.ts
│   │   │   ├── posService.ts
│   │   │   ├── transactionService.ts
│   │   │   ├── fraudService.ts
│   │   │   ├── fleetService.ts
│   │   │   ├── extractionService.ts
│   │   │   ├── notificationService.ts
│   │   │   ├── dashboardService.ts
│   │   │   └── auditService.ts
│   │   │
│   │   ├── store/
│   │   │   ├── authStore.ts             # Zustand: user, tokens, login/logout
│   │   │   └── notificationStore.ts     # Zustand: unread count, notification list
│   │   │
│   │   ├── types/
│   │   │   ├── auth.ts
│   │   │   ├── monitoring.ts
│   │   │   ├── api.ts                   # PageResponse<T>, ApiResponse<T>, FilterRequest
│   │   │   └── notifications.ts
│   │   │
│   │   └── utils/
│   │       ├── formatters.ts            # Date, currency, percentage formatters
│   │       ├── colorMap.ts              # Status → color mapping (no hex in components)
│   │       └── cn.ts                    # clsx + tailwind-merge helper
│   │
│   ├── index.html
│   ├── vite.config.ts
│   ├── tailwind.config.ts
│   ├── tsconfig.json
│   ├── tsconfig.node.json
│   ├── .env.example
│   ├── Dockerfile
│   └── package.json
│
├── docker-compose.yml
├── docker-compose.dev.yml
├── .env.example
├── ANALYSE_ANCIEN_PROJET.md
└── CLAUDE.md
```

---

## Dépendances Backend — `backend/pom.xml`

```xml
<project>
  <groupId>org.intelligentsia</groupId>
  <artifactId>monitoring-backend</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>jar</packaging>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.5</version>
  </parent>

  <properties>
    <java.version>21</java.version>
    <mapstruct.version>1.6.3</mapstruct.version>
    <jjwt.version>0.12.6</jjwt.version>
    <springdoc.version>2.6.0</springdoc.version>
  </properties>

  <dependencies>
    <!-- Spring Boot Web -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Data JPA -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- Spring Security -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Validation (Bean Validation 3 / Jakarta) -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Email -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>

    <!-- Thymeleaf (email templates only) -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>

    <!-- AOP (audit aspect) -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- Actuator -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- PostgreSQL driver -->
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>

    <!-- Oracle JDBC (SVFE read-only datasource) -->
    <dependency>
      <groupId>com.oracle.database.jdbc</groupId>
      <artifactId>ojdbc11</artifactId>
      <version>23.4.0.24.05</version>
    </dependency>

    <!-- Flyway -->
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- JWT (JJWT) -->
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-api</artifactId>
      <version>${jjwt.version}</version>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-impl</artifactId>
      <version>${jjwt.version}</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-jackson</artifactId>
      <version>${jjwt.version}</version>
      <scope>runtime</scope>
    </dependency>

    <!-- OpenAPI / Swagger UI -->
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>${springdoc.version}</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>

    <!-- MapStruct -->
    <dependency>
      <groupId>org.mapstruct</groupId>
      <artifactId>mapstruct</artifactId>
      <version>${mapstruct.version}</version>
    </dependency>
    <dependency>
      <groupId>org.mapstruct</groupId>
      <artifactId>mapstruct-processor</artifactId>
      <version>${mapstruct.version}</version>
      <scope>provided</scope>
    </dependency>

    <!-- Tests -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </exclude>
          </excludes>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <annotationProcessorPaths>
            <path>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </path>
            <path>
              <groupId>org.mapstruct</groupId>
              <artifactId>mapstruct-processor</artifactId>
              <version>${mapstruct.version}</version>
            </path>
          </annotationProcessorPaths>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

---

## Dépendances Frontend — `frontend/package.json`

```json
{
  "name": "monitoring-frontend",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint src --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "test": "vitest",
    "test:ui": "vitest --ui",
    "test:coverage": "vitest run --coverage"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^6.28.0",
    "axios": "^1.7.9",
    "@tanstack/react-query": "^5.62.0",
    "@tanstack/react-query-devtools": "^5.62.0",
    "zustand": "^5.0.2",
    "recharts": "^2.13.3",
    "react-hook-form": "^7.54.0",
    "@hookform/resolvers": "^3.9.1",
    "zod": "^3.23.8",
    "date-fns": "^4.1.0",
    "react-hot-toast": "^2.4.1",
    "lucide-react": "^0.460.0",
    "clsx": "^2.1.1",
    "tailwind-merge": "^2.5.4"
  },
  "devDependencies": {
    "@types/react": "^18.3.12",
    "@types/react-dom": "^18.3.1",
    "@vitejs/plugin-react": "^4.3.3",
    "typescript": "^5.7.2",
    "vite": "^6.0.3",
    "tailwindcss": "^3.4.16",
    "autoprefixer": "^10.4.20",
    "postcss": "^8.4.49",
    "@tailwindcss/forms": "^0.5.9",
    "eslint": "^9.16.0",
    "@typescript-eslint/eslint-plugin": "^8.18.0",
    "@typescript-eslint/parser": "^8.18.0",
    "eslint-plugin-react-hooks": "^5.0.0",
    "eslint-plugin-react-refresh": "^0.4.16",
    "vitest": "^2.1.6",
    "@vitest/ui": "^2.1.6",
    "@vitest/coverage-v8": "^2.1.6",
    "@testing-library/react": "^16.0.1",
    "@testing-library/jest-dom": "^6.6.3",
    "@testing-library/user-event": "^14.5.2"
  }
}
```

---

## Schéma de Base de Données PostgreSQL

### Tables Auth & Sécurité

```sql
-- V1__init_auth_tables.sql

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE permissions (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,   -- e.g. "ATM_READ", "USER_WRITE"
    name        VARCHAR(100) NOT NULL,
    resource    VARCHAR(50)  NOT NULL,          -- e.g. "ATM", "USER"
    action      VARCHAR(20)  NOT NULL           -- READ, WRITE, DELETE, ADMIN
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    login               VARCHAR(100) NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(100) NOT NULL,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    date_birth          DATE,
    phone               VARCHAR(20),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
    language_code       VARCHAR(5)   NOT NULL DEFAULT 'fr',
    institution_id      BIGINT,                -- FK set after institutions table created
    password_expires_at TIMESTAMP,
    account_expires_at  TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_by          BIGINT
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE password_history (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

### Tables Configuration Monitoring

```sql
-- V2__monitoring_config_tables.sql

CREATE TABLE institutions (
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(50)  NOT NULL UNIQUE,
    name           VARCHAR(200) NOT NULL,
    svfe_id        VARCHAR(50),   -- identifiant externe SVFE
    host_pid       VARCHAR(50),   -- référence hôte SVFE
    mail_recharge  VARCHAR(255),  -- email alertes recharge
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by     BIGINT,
    updated_by     BIGINT
);

ALTER TABLE users ADD CONSTRAINT fk_users_institution
    FOREIGN KEY (institution_id) REFERENCES institutions(id);

CREATE TABLE currencies (
    id            BIGSERIAL PRIMARY KEY,
    iso_numeric   VARCHAR(3)   NOT NULL UNIQUE,   -- e.g. "952"
    iso_alpha     VARCHAR(3)   NOT NULL UNIQUE,   -- e.g. "XOF"
    label         VARCHAR(100) NOT NULL            -- e.g. "Franc CFA BCEAO"
);

CREATE TABLE institution_currencies (
    institution_id BIGINT         NOT NULL REFERENCES institutions(id) ON DELETE CASCADE,
    currency_id    BIGINT         NOT NULL REFERENCES currencies(id),
    atm_threshold  NUMERIC(20, 2),    -- seuil d'alerte ATM cassette
    card_pp_limit  NUMERIC(20, 2),    -- limite carte PP
    PRIMARY KEY (institution_id, currency_id)
);

CREATE TABLE atms (
    id             BIGSERIAL PRIMARY KEY,
    institution_id BIGINT       NOT NULL REFERENCES institutions(id),
    terminal_id    VARCHAR(50)  NOT NULL,
    address        TEXT,
    city           VARCHAR(100),
    currency1_id   BIGINT       REFERENCES currencies(id),
    threshold1     NUMERIC(20, 2),
    currency2_id   BIGINT       REFERENCES currencies(id),
    threshold2     NUMERIC(20, 2),
    svfe_pid       BIGINT,           -- PID dans le système SVFE
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by     BIGINT,
    updated_by     BIGINT,
    UNIQUE (institution_id, terminal_id)
);

CREATE TABLE technicians (
    id             BIGSERIAL PRIMARY KEY,
    institution_id BIGINT       NOT NULL REFERENCES institutions(id),
    name           VARCHAR(200) NOT NULL,
    phone          VARCHAR(20),
    cards          TEXT,             -- liste de cartes (format libre)
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by     BIGINT,
    updated_by     BIGINT
);

CREATE TABLE pos_terminals (
    id             BIGSERIAL PRIMARY KEY,
    institution_id BIGINT      NOT NULL REFERENCES institutions(id),
    terminal_id    VARCHAR(50) NOT NULL UNIQUE,
    merchant       VARCHAR(200),
    city           VARCHAR(100),
    district       VARCHAR(100),
    extra_name     VARCHAR(200),
    technician_id  BIGINT      REFERENCES technicians(id) ON DELETE SET NULL,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by     BIGINT,
    updated_by     BIGINT
);

CREATE TABLE scheduler_tasks (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    interval_ms   BIGINT       NOT NULL,
    last_run      TIMESTAMP,
    last_status   VARCHAR(20),   -- SUCCESS, ERROR, RUNNING
    last_error    TEXT
);
```

### Tables Snapshots Monitoring (Séries Temporelles)

```sql
-- V3__monitoring_snapshot_tables.sql

CREATE TABLE atm_snapshots (
    id                  BIGSERIAL PRIMARY KEY,
    atm_id              BIGINT    NOT NULL REFERENCES atms(id),
    snapshot_time       TIMESTAMP NOT NULL,
    currency1_id        BIGINT    REFERENCES currencies(id),
    note1               INTEGER,
    quantity1           INTEGER,
    currency2_id        BIGINT    REFERENCES currencies(id),
    note2               INTEGER,
    quantity2           INTEGER,
    currency3_id        BIGINT    REFERENCES currencies(id),
    note3               INTEGER,
    quantity3           INTEGER,
    currency4_id        BIGINT    REFERENCES currencies(id),
    note4               INTEGER,
    quantity4           INTEGER,
    status_id           VARCHAR(10),
    status_desc         VARCHAR(200),
    all_ok              BOOLEAN,
    status_message      TEXT,
    threshold1_exceeded BOOLEAN   NOT NULL DEFAULT FALSE,
    threshold2_exceeded BOOLEAN   NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_atm_snapshots_atm_time   ON atm_snapshots(atm_id, snapshot_time DESC);
CREATE INDEX idx_atm_snapshots_time       ON atm_snapshots(snapshot_time DESC);

CREATE TABLE network_snapshots (
    id                 BIGSERIAL PRIMARY KEY,
    network_code       VARCHAR(50)  NOT NULL,
    description        VARCHAR(200),
    operating_status   VARCHAR(20),
    processing_status  VARCHAR(20),
    transaction_mode   VARCHAR(20),
    snapshot_time      TIMESTAMP    NOT NULL
);
CREATE INDEX idx_network_snapshots_time ON network_snapshots(snapshot_time DESC);

CREATE TABLE host_snapshots (
    id                BIGSERIAL PRIMARY KEY,
    hostname          VARCHAR(200) NOT NULL,
    ip_address        VARCHAR(50),
    operating_status  VARCHAR(20),
    processing_status VARCHAR(20),
    snapshot_time     TIMESTAMP    NOT NULL
);
CREATE INDEX idx_host_snapshots_time ON host_snapshots(snapshot_time DESC);

CREATE TABLE transaction_rates (
    id             BIGSERIAL PRIMARY KEY,
    network        VARCHAR(50)    NOT NULL,
    acquirer       VARCHAR(50),
    issuer         VARCHAR(50),
    success_count  INTEGER        NOT NULL DEFAULT 0,
    success_rate   NUMERIC(5, 2),
    failure_count  INTEGER        NOT NULL DEFAULT 0,
    failure_rate   NUMERIC(5, 2),
    snapshot_time  TIMESTAMP      NOT NULL
);
CREATE INDEX idx_transaction_rates_time ON transaction_rates(snapshot_time DESC);

CREATE TABLE transaction_errors (
    id             BIGSERIAL PRIMARY KEY,
    network        VARCHAR(50)  NOT NULL,
    response_code  VARCHAR(10),
    response_desc  VARCHAR(200),
    count          INTEGER      NOT NULL DEFAULT 0,
    snapshot_time  TIMESTAMP    NOT NULL
);
CREATE INDEX idx_transaction_errors_time ON transaction_errors(snapshot_time DESC);

CREATE TABLE fraud_rates (
    id             BIGSERIAL PRIMARY KEY,
    institution_id BIGINT    REFERENCES institutions(id),
    bin            VARCHAR(10) NOT NULL,
    suspect_count  INTEGER   NOT NULL DEFAULT 0,
    fraud_count    INTEGER   NOT NULL DEFAULT 0,
    snapshot_time  TIMESTAMP NOT NULL
);
CREATE INDEX idx_fraud_rates_time ON fraud_rates(snapshot_time DESC);

CREATE TABLE fraud_details (
    id             BIGSERIAL PRIMARY KEY,
    institution_id BIGINT      REFERENCES institutions(id),
    bin            VARCHAR(10) NOT NULL,
    rule_code      VARCHAR(50),
    rule_desc      VARCHAR(200),
    suspect_count  INTEGER     NOT NULL DEFAULT 0,
    fraud_count    INTEGER     NOT NULL DEFAULT 0,
    bins           TEXT,
    description    TEXT,
    snapshot_time  TIMESTAMP   NOT NULL
);
CREATE INDEX idx_fraud_details_time ON fraud_details(snapshot_time DESC);

CREATE TABLE fleet_accounts (
    id               BIGSERIAL PRIMARY KEY,
    institution_id   BIGINT         REFERENCES institutions(id),
    account_number   VARCHAR(50)    NOT NULL,
    currency_id      BIGINT         REFERENCES currencies(id),
    balance          NUMERIC(20, 2),
    previous_balance NUMERIC(20, 2),
    delta1           NUMERIC(20, 2),
    delta2           NUMERIC(20, 2),
    delta1_exceeded  BOOLEAN        NOT NULL DEFAULT FALSE,
    delta2_exceeded  BOOLEAN        NOT NULL DEFAULT FALSE,
    snapshot_time    TIMESTAMP      NOT NULL
);
CREATE INDEX idx_fleet_accounts_time ON fleet_accounts(snapshot_time DESC);

CREATE TABLE cards_over_limit (
    id             BIGSERIAL PRIMARY KEY,
    institution_id BIGINT         REFERENCES institutions(id),
    card_number    VARCHAR(30)    NOT NULL,
    currency_id    BIGINT         REFERENCES currencies(id),
    limit_amount   NUMERIC(20, 2),
    current_amount NUMERIC(20, 2),
    exceeded       BOOLEAN        NOT NULL DEFAULT FALSE,
    snapshot_time  TIMESTAMP      NOT NULL
);
CREATE INDEX idx_cards_over_limit_time ON cards_over_limit(snapshot_time DESC);

CREATE TABLE pos_telecollect_snapshots (
    id             BIGSERIAL PRIMARY KEY,
    institution_id BIGINT      REFERENCES institutions(id),
    terminal_id    VARCHAR(50) NOT NULL,
    collect_date   DATE,
    status         VARCHAR(20),
    snapshot_time  TIMESTAMP   NOT NULL
);
CREATE INDEX idx_pos_telecollect_time ON pos_telecollect_snapshots(snapshot_time DESC);
```

### Tables Extraction

```sql
-- V4__extraction_tables.sql

CREATE TABLE extraction_batches (
    id             BIGSERIAL PRIMARY KEY,
    institution_id BIGINT    NOT NULL REFERENCES institutions(id),
    type           VARCHAR(10) NOT NULL CHECK (type IN ('ATM', 'POS')),
    start_date     TIMESTAMP NOT NULL,
    end_date       TIMESTAMP NOT NULL,
    merchant       VARCHAR(200),   -- POS uniquement
    city           VARCHAR(100),   -- POS uniquement
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by     BIGINT    NOT NULL,
    CONSTRAINT chk_dates CHECK (end_date >= start_date)
);

CREATE TABLE extraction_terminals (
    id          BIGSERIAL PRIMARY KEY,
    batch_id    BIGINT      NOT NULL REFERENCES extraction_batches(id) ON DELETE CASCADE,
    terminal_id VARCHAR(50) NOT NULL
);
CREATE INDEX idx_extraction_terminals_batch ON extraction_terminals(batch_id);

CREATE TABLE extraction_transactions (
    id               BIGSERIAL PRIMARY KEY,
    batch_id         BIGINT         NOT NULL REFERENCES extraction_batches(id),
    terminal_id      VARCHAR(50),
    transaction_date TIMESTAMP,
    amount           NUMERIC(20, 2),
    response_code    VARCHAR(10),
    status           VARCHAR(20),
    merchant         VARCHAR(200),
    city             VARCHAR(100)
);
CREATE INDEX idx_extraction_transactions_batch ON extraction_transactions(batch_id);
```

### Tables Notifications & Audit

```sql
-- V5__notification_audit_tables.sql

CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(50)  NOT NULL,    -- ATM_ALERT, FRAUD_ALERT, SYSTEM, etc.
    title       VARCHAR(200) NOT NULL,
    message     TEXT,
    read        BOOLEAN      NOT NULL DEFAULT FALSE,
    entity_type VARCHAR(50),             -- ATM, POS, FRAUD, etc.
    entity_id   BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, read, created_at DESC);

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    user_login  VARCHAR(100),
    action      VARCHAR(50)  NOT NULL,   -- CREATE, UPDATE, DELETE, LOGIN, LOGOUT
    entity_type VARCHAR(100),
    entity_id   VARCHAR(100),
    old_value   JSONB,
    new_value   JSONB,
    ip_address  VARCHAR(50),
    user_agent  VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_logs_user_time   ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity      ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_time        ON audit_logs(created_at DESC);
```

### Politique de Rétention des Données

Les tables de snapshots (`atm_snapshots`, `network_snapshots`, etc.) doivent être purgées périodiquement. Une tâche schedulée supprime les entrées de plus de 90 jours :

```sql
-- Exécutée par RetentionTask (@Scheduled, 1x/jour à 02:00)
DELETE FROM atm_snapshots         WHERE snapshot_time < NOW() - INTERVAL '90 days';
DELETE FROM network_snapshots     WHERE snapshot_time < NOW() - INTERVAL '90 days';
DELETE FROM host_snapshots        WHERE snapshot_time < NOW() - INTERVAL '90 days';
DELETE FROM transaction_rates     WHERE snapshot_time < NOW() - INTERVAL '90 days';
DELETE FROM transaction_errors    WHERE snapshot_time < NOW() - INTERVAL '90 days';
DELETE FROM fraud_rates           WHERE snapshot_time < NOW() - INTERVAL '90 days';
DELETE FROM fraud_details         WHERE snapshot_time < NOW() - INTERVAL '90 days';
DELETE FROM fleet_accounts        WHERE snapshot_time < NOW() - INTERVAL '90 days';
DELETE FROM cards_over_limit      WHERE snapshot_time < NOW() - INTERVAL '90 days';
DELETE FROM pos_telecollect_snapshots WHERE snapshot_time < NOW() - INTERVAL '90 days';
```

---

## Architecture Backend — Principes

### Découpage en couches

```
controller  →  service  →  repository  →  entity (JPA)
                ↓
            mapper (MapStruct)  →  dto
```

- **Controller** : validation input (`@Valid`), délégation au service, retour `ApiResponse<T>` ou `PageResponse<T>`.
- **Service** : logique métier, transactions (`@Transactional`), appels repository, émission de notifications.
- **Repository** : Spring Data JPA ; queries custom en `@Query` JPQL ou SQL natif avec projections DTO.
- **Mapper** : MapStruct uniquement — pas de mapping manuel.
- **DTO** : classes distinctes par opération (XxxDto lecture, XxxCreateRequest création, XxxUpdateRequest mise à jour).

### Sécurité JWT

```
POST /api/auth/login
  → AuthController → AuthService.authenticate()
  → BCrypt verify + User.status check
  → JwtTokenProvider.generateAccessToken()  (15 min)
  → JwtTokenProvider.generateRefreshToken() (7 jours, stocké en DB)
  → AuthResponse { accessToken, refreshToken, user }

POST /api/auth/refresh
  → RefreshToken vérifié en DB (non révoqué, non expiré)
  → Nouveau access token généré

POST /api/auth/logout
  → RefreshToken révoqué (revoked=true)
```

Le filtre `JwtAuthenticationFilter` extrait et valide le Bearer token sur chaque requête `/api/**` sauf `/api/auth/login`, `/api/auth/refresh`, `/api/auth/register`.

### Multi-tenancy par institution

Les utilisateurs avec `institution_id != null` ne voient que les données de leur institution. Ce filtrage est appliqué **dans la couche service** via `SecurityUtils.getCurrentUserInstitutionId()`. Les admins sans institution voient tout.

### Notifications SSE

```
GET /api/sse/stream  (EventSource côté client, header Authorization requis)
  → SseEmitterService enregistre un SseEmitter par userId
  → Emitters stockés dans une ConcurrentHashMap<Long, SseEmitter>
  → NotificationService.sendNotification(userId, notification)
      → sauvegarde en DB
      → pousse via SseEmitter si l'utilisateur est connecté
```

### Tâches de Monitoring (@Scheduled)

Chaque tâche (`AtmQueryTask`, etc.) est un `@Component` Spring avec `@Scheduled(fixedDelay)`. L'intervalle est lu depuis la table `scheduler_tasks` en base (rechargé à chaque exécution pour permettre la reconfiguration à chaud). Toutes les tâches sont `@Transactional` et utilisent des logs SLF4J structurés.

### Couche SVFE (Anti-Corruption)

Le datasource Oracle SVFE est une datasource secondaire (`@Qualifier("svfeDataSource")`). Les entités SVFE (`CmsAtm`, etc.) utilisent `@Entity` mais sont dans un `EntityManagerFactory` séparé. Les `SvfeCmsXxxRepository` ne font que du `findAll` / `findBy...` — jamais de save/delete. Les données SVFE sont transformées via les services métier avant d'être persistées dans PostgreSQL.

### Gestion d'Erreurs

`GlobalExceptionHandler` (`@RestControllerAdvice`) centralise :
- `BusinessException` → 400 avec code métier
- `ResourceNotFoundException` → 404
- `MethodArgumentNotValidException` → 422 avec détail champ par champ
- `AccessDeniedException` → 403
- `Exception` générique → 500 masqué (log interne complet)

Format de réponse d'erreur :
```json
{
  "success": false,
  "code": "ATM_NOT_FOUND",
  "message": "ATM avec l'ID 42 introuvable",
  "timestamp": "2026-04-30T10:00:00Z",
  "errors": []
}
```

### Audit Log

L'annotation `@Auditable(action = "CREATE", entityType = "ATM")` sur les méthodes de service déclenche `AuditAspect` (AOP `@Around`) qui :
1. Exécute la méthode
2. Capture le résultat (new_value) et l'état avant (old_value si applicable)
3. Persiste un `AuditLog` avec `userId`, `action`, `entityType`, `entityId`, `oldValue` (JSONB), `newValue` (JSONB), `ipAddress`.

---

## Architecture Frontend — Principes

### Gestion d'État

- **Zustand** : état global minimal — session utilisateur (`authStore`) + notifications non lues (`notificationStore`).
- **TanStack Query** : tout le state serveur (fetch, cache, invalidation). Pas de Redux.
- **React Hook Form + Zod** : formulaires avec validation côté client.

### Palette de Couleurs (Design System)

```typescript
// tailwind.config.ts — extend.colors
colors: {
  gray: {
    900: '#2C2C2C',   // Texte principal, sidebar foncée
    700: '#4A4A4A',   // Texte secondaire
    50:  '#F5F5F5',   // Fond général
  },
  gold: {
    500: '#C9A84C',   // Accent principal (boutons, liens actifs)
    400: '#E2C07A',   // Hover, surbrillance
    300: '#F5DFA0',   // Fond cards actives
  },
  status: {
    ok:      '#22C55E',   // Vert — opérationnel
    warning: '#F59E0B',   // Orange — avertissement
    error:   '#EF4444',   // Rouge — critique / hors ligne
    unknown: '#6B7280',   // Gris — état inconnu
  }
}
```

### Conventions de Composants

- **Composants UI** (`components/ui/`) : aucune logique métier, props typées, variantes via `clsx`.
- **Feature components** (`features/*/components/`) : consomment les hooks de la feature, pas les services directement.
- **Pages** : assemblage de feature components, définit le layout et le titre de page.
- Pas de prop-drilling au-delà de 2 niveaux — utiliser un hook de context ou Zustand.

### Intercepteur Axios (Token Refresh)

```typescript
// services/api.ts
// Request interceptor : ajoute Authorization: Bearer {accessToken}
// Response interceptor : si 401, tente un refresh silencieux (1 retry),
//   si le refresh échoue → logout automatique + redirect /login
```

### SSE (Server-Sent Events)

```typescript
// features/notifications/hooks/useSse.ts
// EventSource pointant sur GET /api/sse/stream
// Header Authorization envoyé via URL param (SSE ne supporte pas les headers custom)
// ou via cookie HttpOnly si implémenté côté back
// Reconnexion automatique via EventSource natif
```

---

## Commandes Utiles

### Démarrage Complet (Docker)

```bash
# Copier les variables d'environnement
cp .env.example .env

# Démarrer tous les services (PostgreSQL + backend + frontend)
docker compose up -d

# Logs
docker compose logs -f backend
docker compose logs -f frontend
```

### Développement Local

```bash
# Backend (depuis backend/)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend (depuis frontend/)
npm install
npm run dev         # http://localhost:5173

# Base de données (Docker seul)
docker compose -f docker-compose.dev.yml up -d postgres
```

### Backend

```bash
cd backend/

# Build
./mvnw clean package -DskipTests

# Tests unitaires
./mvnw test

# Tests d'un seul fichier
./mvnw test -Dtest=AuthServiceTest

# Migrations Flyway manuelles
./mvnw flyway:info
./mvnw flyway:migrate

# Swagger UI (local)
# http://localhost:8080/swagger-ui.html
```

### Frontend

```bash
cd frontend/

npm run dev          # Serveur de développement
npm run build        # Build de production (dist/)
npm run preview      # Prévisualiser le build
npm run lint         # ESLint
npm test             # Vitest (watch)
npm run test:coverage
```

---

## Conventions de Code

### Backend

- **Packages** : `org.intelligentsia.monitoring.{domain|infrastructure|shared|config}`
- **Nommage entités** : PascalCase, sans suffixe (e.g., `Atm`, `AtmSnapshot`, pas `AtmEntity`)
- **DTOs** : suffixes explicites — `AtmDto`, `AtmCreateRequest`, `AtmUpdateRequest`
- **Réponses API** : toujours wrapper dans `ApiResponse<T>` ou `PageResponse<T>`
- **Logs** : `private static final Logger log = LoggerFactory.getLogger(...)` — jamais `System.out`
- **Transactions** : `@Transactional(readOnly = true)` sur les méthodes de lecture, `@Transactional` sur les écritures

### Frontend

- **Fichiers** : PascalCase pour les composants (`AtmTable.tsx`), camelCase pour les hooks et services (`useAuth.ts`, `atmService.ts`)
- **Imports** : alias `@/` pour `src/` (configuré dans `vite.config.ts` et `tsconfig.json`)
- **Typage** : pas de `any` — toujours typer explicitement les props et retours de fonction
- **Composants** : `function` déclarée, pas de `const Component = () =>` pour les exports
- **Couleurs** : utiliser uniquement les tokens Tailwind définis dans `tailwind.config.ts` — jamais de hex en inline

### Git

- Branches : `feat/nom-feature`, `fix/description-bug`, `chore/nom-tache`
- Commits : format `type(scope): description` — e.g., `feat(auth): add JWT refresh token endpoint`

---

## Variables d'Environnement Clés

### Backend (`application.yml`)

```yaml
spring:
  datasource:
    url: ${DB_URL}           # jdbc:postgresql://localhost:5432/monitoring
    username: ${DB_USER}
    password: ${DB_PASS}
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT}
    username: ${MAIL_USER}
    password: ${MAIL_PASS}

app:
  jwt:
    secret: ${JWT_SECRET}    # Minimum 512 bits
    access-expiry-ms: 900000   # 15 min
    refresh-expiry-ms: 604800000  # 7 jours
  svfe:
    datasource:
      url: ${SVFE_DB_URL}    # jdbc:oracle:thin:@host:1521:sid
      username: ${SVFE_DB_USER}
      password: ${SVFE_DB_PASS}
  cors:
    allowed-origins: ${CORS_ORIGINS}   # http://localhost:5173
```

### Frontend (`.env`)

```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_SSE_URL=http://localhost:8080/api/sse/stream
```
