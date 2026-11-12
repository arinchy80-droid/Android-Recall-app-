# RECALL — Project Development Record

**Platform:** Recall — Personal Memory, External Brain & Life Intelligence Platform  
**Package / Application ID:** `com.aistudio.recall.memai` (Namespace: `com.example`)  
**Current Authorised Phase:** Phase 2 — Provenance-Aware AI Extraction & Fact Verification Workflow (Completed)  
**Next Phase (Awaiting Approval):** Phase 3 — First-Class Places, Projects & Commitments Engine  

---

## 1. Existing Architecture

### 1.1 Programming Language & Framework
- **Language:** Kotlin 2.0+ (100% Kotlin codebase)
- **UI Framework:** Jetpack Compose with Material Design 3 (`androidx.compose.material3`), custom glassmorphic / neural theme (`NeuralAppBackground`, `Glossy3DOrbIcon`, `AppVisualPreferences`), and responsive layouts.
- **Concurrency & Reactive Streams:** Kotlin Coroutines (`Dispatchers.IO`, `Dispatchers.Main`, `SupervisorJob`) and `StateFlow` / `SharedFlow` with `collectAsStateWithLifecycle`.
- **Build System:** Gradle (Kotlin DSL `.gradle.kts`) with KSP (`com.google.devtools.ksp`) and Secrets Gradle Plugin (`com.google.android.libraries.mapsplatform.secrets-gradle-plugin`).

### 1.2 Architectural Pattern
- **MVVM + Repository Pattern + Domain Intelligence Helpers:**
  - **Presentation Layer (`com.example.ui`):** Single-Activity Compose architecture (`MainActivity.kt`) backed by a shared `RecallViewModel` (`RecallViewModel.kt`) and screen-level composables (`StreamScreen`, `RemindersScreen`, `PeopleScreen`, `GalleryScreen`, `DailyDebriefScreen`, `ChatScreen`, `SettingsScreen`, `HelpAndFaqScreen`, `NoteEditorSheet`, `IntelligenceAndGraphModal`).
  - **Domain / Intelligence Layer (`com.example.util` & `com.example.data.search`):**
    - `MemoryReconciliationEngine`: Stateful "Living Notes" reconciliation engine that detects financial/balance updates (e.g. debt payments/additions) and factual/status updates, rewriting existing notes and person facts in-place while maintaining an append-only `📝 Update History` log.
    - `MemoryIntelligenceHelper`: Related-memory similarity scoring, topic clustering (`TopicCluster`), location clustering (`LocationCluster`), `MeetingPrepBriefing` generator, `WeeklyIntelligenceDigest` generator, reverse-geocoded place detection, and habit streak tracker.
    - `SmartSearchEngine`: Local zero-latency NLU query parser (`parseQueryLocal`) and multi-dimensional memory scorer (`filterAndRankMemories`).
  - **Data Layer (`com.example.data`):**
    - `RecallRepository`: Single source of truth mediating between Room DAOs (`NoteDao`, `PersonDao`, `MediaDao`, `ActionItemDao`, `ReminderDao`, `PlaceDao`, `ProjectDao`, `CommitmentDao`, `MemoryLinkDao`, `NoteRevisionDao`) and remote AI (`GeminiService`).
    - `RecallDatabase`: Local SQLite database managed via Room (`recall_memory.db`, version 5 with non-destructive `MIGRATION_4_5`).

---

## 2. Database Schema & Relationships (Current Baseline — Version 5)

1. **`notes` (`NoteEntity`)**
   - Core Columns: `id` (PK, UUID String), `title`, `content`, `rawTranscription`, `category` (default `"General"`), `tagsJson` (serialized `List<String>`, including `loc:<PlaceName>` tags), `createdAt` (epoch ms), `updatedAt` (epoch ms), `isPinned` (Boolean), `aiSummary`.
   - Unified Memory Metadata Columns (Added in v5):
     - `memoryType` (`String`, default `"PERSONAL_OBSERVATIONS"`, mapping across the 12 canonical categories: `PEOPLE`, `PLACES`, `EVENTS`, `CONVERSATIONS`, `IDEAS`, `TASKS`, `COMMITMENTS`, `PROJECTS`, `EXPERIENCES`, `PREFERENCES`, `IMPORTANT_DATES`, `PERSONAL_OBSERVATIONS`).
     - `occurredAt` (`Long`, original occurrence timestamp, initialized to `createdAt`).
     - `sourceType` (`String`, default `"USER_EXPLICIT"`: `USER_EXPLICIT` | `AUDIO_TRANSCRIPT` | `IMAGE_OCR` | `AI_INTERPRETATION`).
     - `confidenceScore` (`Float`, default `1.0f`).
     - `verificationStatus` (`String`, default `"VERIFIED_FACT"`: `VERIFIED_FACT` | `AI_INFERRED_PENDING` | `ARCHIVED`).
     - `projectId` (`String?`, nullable link to `projects.id`).
     - `placeId` (`String?`, nullable link to `places.id`).
2. **`people` (`PersonEntity`)**
   - Columns: `id` (PK, UUID String), `name`, `relationship`, `avatarUri`, `factsJson` (serialized `List<String>`), `keyDatesJson` (serialized `Map<String, String>`, including `"CatchUpCadenceDays"`), `lastContactedAt` (epoch ms), `createdAt` (epoch ms).
3. **`note_person_cross_ref` (`NotePersonCrossRef`)**
   - Many-to-many join table between `notes.id` and `people.id` with `CASCADE` delete and indices on `personId` and `noteId`.
4. **`media_attachments` (`MediaEntity`)**
   - Columns: `id` (PK), `noteId` (FK -> `notes.id`, nullable, `CASCADE`), `mediaType` (`IMAGE` | `AUDIO`), `localUri`, `ocrText`, `aiDescription`, `labelsJson`, `capturedAt`.
5. **`action_items` (`ActionItemEntity`)**
   - Columns: `id` (PK), `noteId` (FK -> `notes.id`, nullable, `CASCADE`), `task`, `dueDate` (nullable epoch ms), `isCompleted`, `createdAt`.
6. **`reminders` (`ReminderEntity`)**
   - Columns: `id` (PK), `noteId` (FK -> `notes.id`, nullable, `CASCADE`), `title`, `details`, `reminderTimeMillis`, `toneType`, `vibratePattern`, `isCompleted`, `createdAt`.
7. **`places` (`PlaceEntity` — Added in v5)**
   - Columns: `id` (PK), `name` (indexed), `category`, `address`, `latitude`, `longitude`, `notes`, `visitCount`, `lastVisitedAt`, `createdAt`. Automatically synced from `loc:<PlaceName>` tags.
8. **`projects` (`ProjectEntity` — Added in v5)**
   - Columns: `id` (PK), `name` (indexed), `description`, `status` (`ACTIVE` | `PAUSED` | `COMPLETED` | `ARCHIVED`), `tagsJson`, `createdAt`, `updatedAt`.
9. **`commitments` (`CommitmentEntity` — Added in v5)**
   - Columns: `id` (PK), `noteId` (indexed), `personId` (indexed), `projectId` (indexed), `description`, `direction` (`BY_ME` | `TO_ME`), `dueDate`, `isCompleted`, `sourceType`, `confidenceScore`, `verificationStatus`, `createdAt`.
10. **`memory_links` (`MemoryLinkEntity` — Added in v5)**
    - Columns: `id` (PK), `sourceNoteId` (indexed), `targetNoteId` (indexed), `relationType` (`CONTINUES` | `CONTRADICTS` | `EXPANDS` | `CAUSED_BY` | `RELATED_TO`), `strengthScore`, `createdAt`.
11. **`note_revisions` (`NoteRevisionEntity` — Added in v5)**
    - Columns: `id` (PK), `noteId` (indexed), `previousTitle`, `previousContent`, `changeSummary`, `sourceType`, `createdAt`. Automatically snapshots historical state before any note edit or Living Notes reconciliation update.
12. **Composite Relation (`NoteWithDetails`)**
    - Combines `@Embedded NoteEntity` with `people: List<PersonEntity>`, `media: List<MediaEntity>`, `actionItems: List<ActionItemEntity>`, `revisions: List<NoteRevisionEntity>`, and `commitments: List<CommitmentEntity>`.

---

## 3. API & AI Integrations

- **`GeminiService` (`com.example.data.remote.GeminiService`):**
  - Direct HTTPS REST integration via `OkHttpClient` against `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`.
  - **Multi-Model Failover Chain:** `gemini-3.5-flash` -> `gemini-flash-latest` -> `gemini-3.1-flash-lite-preview` -> `gemini-3.1-pro-preview`.
  - **API Key Management:** Read from `BuildConfig.GEMINI_API_KEY` (via Secrets Gradle Plugin / `.env`).
  - **Graceful Local Fallback:** Every AI operation (`processDailyDebrief`, `processDailyDebriefAudio`, `queryMemoryConcierge`, `analyzeImageMemory`, `extractSmartTagsAndSummary`, `interpretNaturalLanguageQuery`, `enrichPersonProfile`) includes a deterministic on-device fallback when offline, unconfigured, or rate-limited (HTTP 429/503).

---

## 4. Completed Phases

- **Pre-Audit Enhancements (Completed):**
  - Fixed startup initialization and safeguarded receivers/database access.
  - Implemented Stateful Memory Reconciliation ("Living Notes") via `MemoryReconciliationEngine` so financial and factual updates (e.g., "I owe Jason $600" followed by "I paid Jason $200" -> "$400") update older notes and linked person facts in-place with an audit trail.
  - Streamlined UI, removed blank header spacing on Reminders screen, fixed button text clipping on Daily Debrief screen, and updated Help & FAQ documentation.
  - Hardened background & lock-screen alarms (`ReminderReceiver` and `DailyDebriefReceiver`) with `AlarmManager.setAlarmClock`, `WAKE_LOCK`, `goAsync()`, and boot/time-change rescheduling.
  - Updated welcome popup attribution to `"Made by Aaron Lissa\nPerth S and M AI Innovations"`, removed example text from "How Living Notes Works", and added full inline Edit & Delete functionality for facts in Person Dossiers (`PeopleScreen.kt`).
- **Phase 0 — Project Audit and Development Plan (Completed):**
  - Inspected all packages, Room entities/DAOs, AI pipelines, speech capture, search engine, intelligence helpers, reminders, and UI screens.
  - Documented baseline architecture, identified technical gaps, and produced the 8-phase Recall evolution roadmap.
- **Phase 1 — Unified Memory Core & Non-Destructive Database Evolution (Room v4 -> v5) (Completed):**
  - Created `UnifiedMemoryModels.kt` defining the 12 canonical memory categories (`UnifiedMemoryType`), provenance source types (`MemorySourceType`), verification statuses (`MemoryVerificationStatus`), commitment directions (`CommitmentDirection`), and graph relation types (`MemoryRelationType`).
  - Extended `NoteEntity` with backward-compatible columns (`memoryType`, `occurredAt`, `sourceType`, `confidenceScore`, `verificationStatus`, `projectId`, `placeId`).
  - Created 5 new Room entities (`PlaceEntity`, `ProjectEntity`, `CommitmentEntity`, `MemoryLinkEntity`, `NoteRevisionEntity`) and their corresponding DAOs (`PlaceDao`, `ProjectDao`, `CommitmentDao`, `MemoryLinkDao`, `NoteRevisionDao`).
  - Upgraded `RecallDatabase` to `version = 5` with explicit non-destructive `MIGRATION_4_5`.
  - Updated `RecallRepository` to automatically snapshot `NoteRevisionEntity` records before note edits/reconciliations, automatically sync `loc:<PlaceName>` tags into `PlaceEntity` records, backfill legacy v4 notes on startup, and export/import all v5 tables in JSON backups.
- **Phase 2 — Provenance-Aware AI Extraction & Fact Verification Workflow (Completed):**
  - Upgraded `DailyDebriefResult` (`DebriefParsedNote`, `DebriefNoteUpdate`, `DebriefPersonUpdate`, and new `DebriefCommitmentItem`) and `GeminiService` (both cloud Gemini JSON schema and on-device deterministic fallback) to classify memories across all 12 canonical `UnifiedMemoryType` categories, extract structured `commitments` (`BY_ME` / `TO_ME`), and separate explicitly stated facts (`VERIFIED_FACT`, `confidenceScore >= 0.80f`) from tentative AI interpretations or speculative phrasing (`AI_INFERRED_PENDING`, `sourceType = AI_INTERPRETATION`).
  - Updated `RecallRepository.commitDailyDebrief()` to persist provenance metadata (`memoryType`, `sourceType`, `confidenceScore`, `verificationStatus`), prefix unverified person facts with `[AI Inferred - Unverified]` until confirmed, and save extracted `CommitmentEntity` records.
  - Added an interactive **"🤖 AI Interpretations Awaiting Review"** banner on `StreamScreen` allowing 1-tap fact confirmation (`✓ Confirm Fact`), editing (`✎ Edit`), or dismissal (`✕ Dismiss`), plus provenance badges on memory cards and in `DailyDebriefScreen`'s `TriageConfirmationModal`.
  - Added a **"🛡️ Memory Provenance & Verification"** card and an interactive **"📜 Revision & Reconciliation History"** viewer with 1-tap non-destructive snapshot restoration (`↺ Restore`) inside `NoteEditorScreen`.
- **Phase 3 — First-Class Places, Projects & Commitments Engine (Completed):**
  - Created `MemoryEntitiesViews.kt` and evolved `PeopleScreen.kt` into a unified **Memory Entities Hub** (`👥 People | 📍 Places | 🚀 Projects | 🤝 Commitments`) while preserving 100% of existing People features (contacts import, avatar upload, voice notes, facts inline edit/delete, AI profile summaries, relationship health badges, and person reminders).
  - Implemented **Place Dossiers (`PlaceDossierView`)**: displays place category, address/hint, visit count, associated people who visited or met there, chronological memories at that location, 1-tap `+ Memory Here` creation, and full place create/edit/delete dialogs.
  - Implemented **Project Dossiers (`ProjectDossierView`)**: displays project status (`ACTIVE`, `PAUSED`, `COMPLETED`), description, project-specific commitments, chronological project memories, 1-tap `+ Project Note` creation, and full project create/edit/delete dialogs.
  - Implemented **Commitments Tracker (`CommitmentsTrackerContent` & `PersonCommitmentsSection`)**: tracks promises and obligations owed by the user (`BY_ME`) or owed to the user (`TO_ME`), linked to people and projects, with completion toggles, filter chips (`OPEN`, `BY_ME`, `TO_ME`, `COMPLETED`), and 1-tap `🔔 Remind` alarm scheduling.
  - Wired **Place & Project selectors** directly into `NoteEditorScreen.kt` (`📍 Place` dropdown now includes saved `PlaceEntity` records; new `🚀 Project` dropdown links/unlinks `ProjectEntity` records or creates a new project inline, plus renders an active `🚀 Project` badge in the note header strip).
- **Phase 4 — Hybrid Semantic + Structured Retrieval & Graph Traversal (Completed):**
  - Upgraded `SmartSearchModels.kt` and `SmartSearchEngine.kt` with **BM25/TF-IDF inverse document frequency lexical weighting**, structured entity scoring (`PLACE`, `PROJECT`, `COMMITMENT`), provenance-aware ranking (`VERIFIED_FACT` prioritized above `AI_INFERRED_PENDING`), and **1-Hop Relational Graph Traversal** across `MemoryLinkEntity` edges (`CONTINUES`, `EXPANDS`, `RELATED_TO`), shared `projectId` associations, and person-place-project links.
  - Added automatic `MemoryLinkEntity` graph edge discovery (`autoDiscoverMemoryLinks`) in `RecallRepository.saveNote()` so related notes sharing projects, people + places, or people + topic tags are linked automatically.
  - Added **Connected Memories (`🕸️ Connected Memories`)** viewer and inline **`+ Link Memory`** selector inside `NoteEditorScreen.kt` with 1-tap navigation between linked memories.
  - Upgraded `SmartSearchView.kt` and `StreamScreen.kt` to display `SmartPlaceResultCard`, `SmartProjectResultCard`, and `SmartCommitmentResultCard` (with 1-tap commitment completion toggle), plus `📍 Place`, `🚀 Project`, and `🕸️ X Graph Hop(s)` chips in `SmartSearchInterpretationCard`.
  - Upgraded `GeminiService.queryMemoryConcierge()`, `fallbackLocalRAG()`, and `AskAiScreen.kt` to ground the AI Concierge across all 6 Unified Memory Graph dimensions (People, Places, Projects, Commitments, Notes, and Memory Links).
- **Phase 5 — Proactive AI Assistant & Anticipatory Briefing Engine (Completed):**
  - Upgraded `MemoryIntelligenceHelper.kt` with `ProactiveInsightType` (`OPEN_COMMITMENT`, `UPCOMING_DATE`, `DORMANT_RELATIONSHIP`, `PROJECT_MOMENTUM`, `PENDING_TASK`), `ProactiveInsightItem`, `AnticipatoryDailyBriefing`, and `buildAnticipatoryBriefing(...)` to proactively synthesize time-of-day aware briefings across open promises (`BY_ME` / `TO_ME`), upcoming person milestones & `IMPORTANT_DATES` memories, overdue relationship catch-ups, active project momentum, and pending tasks.
  - Upgraded `MeetingPrepBriefing` and `MeetingPrepBriefingDialog` (`IntelligenceAndGraphModal.kt`, `PeopleScreen.kt`) to automatically surface open commitments (`openCommitments`) and shared projects (`sharedProjects`) alongside talking points and action items when preparing for a meeting with any person.
  - Added `NotificationHelper.showProactiveBriefingNotification(...)` to dispatch high-priority lock-screen & heads-up notifications summarizing the user's anticipatory daily briefing with physical acoustic chime and vibration feedback.
  - Added the collapsible **"⚡ Proactive Assistant • Anticipatory Briefing"** hub card on `StreamScreen.kt` with metric pills (`Promises`, `Dates`, `Catch-Ups`, `Active Projects`), 1-tap lock-screen notification trigger (`🔔 Alert`), and 1-tap actionable insight cards (`✓ Done`, `🔔 Remind`, `🔔 Set Alarm`, `💬 Catch-Up Note`, `+ Project Note`, `👤 Dossier`).
  - Added the **"⚡ Proactive Alarm Suggestions"** card on `RemindersScreen.kt` that detects unscheduled open commitments and lets users schedule a lock-screen exact alarm for any promise in 1 tap.
- **Phase 6 — Personal Life Intelligence, Timeline & Pattern Discovery (Completed):**
  - Extended `MemoryIntelligenceHelper.kt` with `LifePatternInsight`, `TimelineEventType`, `LifeTimelineEntry`, `TimelineEpochGroup`, `LifeIntelligenceReport`, and `buildLifeIntelligenceReport(...)`.
  - Automatically discovers cross-category life patterns across **Relational Synergy** (person × project × location co-occurrence), **Project Velocity** (active project notes & commitment completion), **Spatial Habits** (top anchor places & frequent companions), **Commitment Balance** (fulfillment rate & open obligations), and **Knowledge Graph Density** (`MemoryLinkEntity` directional edges).
  - Implemented **"✨ Rediscover Past Memories & Ideas"** to surface high-value ideas, experiences, observations, and pinned memories so important thoughts are never overlooked.
  - Implemented the **Unified Chronological Life Timeline** grouping memories, commitments, and place visits into chronological epochs (`Today`, `Last 7 Days`, `Earlier This Month`, `Personal History Archive`) with interactive filters (`All`, `Memories`, `Promises`, `Places`).
  - Upgraded `IntelligenceHubDialog` (`IntelligenceAndGraphModal.kt`) into a 4-tab intelligence center (`📊 Digest`, `🧬 Patterns`, `🕸️ Graph`, `📍 Places`), adding the **Neural Memory Graph Edges Explorer (`MemoryLinkEntity`)** and the **12-Category Unified Memory Spectrum** visualization.
- **Phase 7 — Privacy, Audio Retention Controls, Consent Workflows & Security (Completed):**
  - Created `PrivacySecurityManager.kt` (`AudioRetentionPolicy`, `PrivacyAuditEntry`, `AudioStorageStats`, `PrivacySecurityManager`) providing persistent `SharedPreferences`-backed security and privacy controls.
  - Implemented **Salted SHA-256 PIN Vault Lock**: users can enable the PIN Security Vault Lock in `SettingsScreen.kt`, customize their 4-to-8 digit PIN, lock the vault on demand (`🔒 Lock Vault Now`), and authenticate via the full-screen `RecallVaultLockOverlay` in `MainActivity.kt`.
  - Implemented **Configurable Raw Audio Retention Policies** (`DELETE_AFTER_TRANSCRIPTION`, `KEEP_7_DAYS`, `KEEP_30_DAYS`, `KEEP_INDEFINITELY`), automatic raw `.m4a` cleanup on startup and after `commitDebriefToMemory()` when `DELETE_AFTER_TRANSCRIPTION` is active, live on-device audio storage stats, and a 1-tap **`🧹 Purge Audio Now`** control that deletes raw audio files while preserving structured text transcripts.
  - Implemented **Explicit Recording Consent Workflows & Indicators**: added the `🛡️ Push-to-Record Only • Audio: <Policy>` banner and optional pre-recording **Participant Consent Confirmation Dialog** in `DailyDebriefScreen.kt`, plus a persistent **Privacy & Security Audit Log** in `SettingsScreen.kt`.
- **Phase 8 — Comprehensive QA, Robolectric CUJ Verification & Performance Optimization (Completed):**
  - Optimized `RoomConverters.kt` with pre-compiled static `Regex` patterns (`LIST_ITEM_REGEX`, `MAP_PAIR_REGEX`) and thread-safe bounded `ConcurrentHashMap` caches (`listParseCache`, `mapParseCache`) for O(1) repeated tag, fact, and date lookups across `SmartSearchEngine`, `MemoryIntelligenceHelper`, and UI recompositions.
  - Expanded `ExampleRobolectricTest.kt` with end-to-end Critical User Journey (CUJ) tests against Room SQLite and Robolectric Android `Context` covering:
    1. `testPhase1To4_UnifiedMemoryGraphRevisionAndBackupCUJ`: Auto-synced `PlaceEntity` visit counts, auto-discovered `MemoryLinkEntity` graph edges, non-destructive `NoteRevisionEntity` snapshot restoration, and portable v5 JSON backup export & restore.
    2. `testPhase2_ProvenanceGateSeparatesVerifiedFactsFromAiInterpretationsCUJ`: Provenance separation (`AI_INFERRED_PENDING` vs `VERIFIED_FACT`) and 1-tap user confirmation (`confirmNoteAsVerifiedFact`).
    3. `testPhase5And6_ProactiveBriefingLifeIntelligenceAndNotificationsCUJ`: End-to-end anticipatory daily briefing synthesis (`buildAnticipatoryBriefing`) and lock-screen notification dispatch (`NotificationHelper.showProactiveBriefingNotification`).
    4. `testPhase7_PrivacySecurityManagerContextPersistenceAndAudioPurgeCUJ`: Context-backed PIN lock persistence, session unlock verification, `cacheDir` raw `.m4a` audio file purging (`purgeAllRawAudioFiles`), and audit log verification.

---

## 5. Known Bugs & Technical Weaknesses Remaining for Subsequent Phases

1. ~~**Destructive Room Migration Flag**~~ *(Resolved in Phase 1 via `MIGRATION_4_5`)*
2. ~~**Implicit Places Representation & Missing Unified Memory Columns**~~ *(Schema & auto-sync resolved in Phase 1)*
3. ~~**Provenance-Aware AI Extraction & Verification UI**~~ *(Resolved in Phase 2)*
4. ~~**First-Class Places, Projects & Commitments UI Integration**~~ *(Resolved in Phase 3)*
5. ~~**Hybrid Semantic + Structured Relational Graph Retrieval**~~ *(Resolved in Phase 4)*
6. ~~**Proactive AI Assistant & Anticipatory Briefing Engine**~~ *(Resolved in Phase 5)*
7. ~~**Personal Life Intelligence, Timeline & Pattern Discovery**~~ *(Resolved in Phase 6)*
8. ~~**Biometric / PIN Lock Toggle Persistence & Enforcement**~~ *(Resolved in Phase 7)*
9. ~~**Audio Retention & Consent Controls**~~ *(Resolved in Phase 7)*

---

## 6. Phased Development Roadmap (All 8 Phases Completed)

- **Phases 0–8: Completed & Verified** — All phases of the Master Development Specification for **Recall** (*External Brain, Proactive AI Assistant, and Personal Life Intelligence Platform*) are implemented, compiled, and verified against both local JVM unit tests and Robolectric Critical User Journey (CUJ) tests.

---

## 7. Technical Decisions Log

1. **Preserve Room SQLite as Primary Local-First Database:** Evolved `RecallDatabase` from v4 to v5 using an explicit `Migration(4, 5)` so zero existing user notes, people, reminders, action items, or media attachments are lost on upgrade.
2. **Default-Valued Entity Columns for Zero-Regression Compatibility:** Added all 7 Unified Memory columns on `NoteEntity` with sensible defaults so every existing caller and UI screen compiles and operates without breaking changes.
3. **Automatic Non-Destructive Revision History (`NoteRevisionEntity`):** Hooked `NoteRevisionDao` directly into `RecallRepository.saveNote()` and `RecallRepository.applyNoteUpdates()` so every manual edit or Living Notes balance reconciliation preserves the prior title and content snapshot.
4. **Automatic Bidirectional Place Synchronization:** Kept `loc:<PlaceName>` tag helpers intact while automatically resolving and updating `PlaceEntity` rows (`visitCount`, `lastVisitedAt`, `placeId`) inside `RecallRepository`.
5. **Strict Provenance Gate Against Unverified AI Hallucinations (Phase 2):** Any debrief extraction or statement with `confidenceScore < 0.80f` or speculative phrasing (`"maybe"`, `"might"`, `"I think"`, `"not sure"`, `"possibly"`) is persisted with `verificationStatus = AI_INFERRED_PENDING` and `sourceType = AI_INTERPRETATION`, surfaced in a dedicated review banner on `StreamScreen`, and never silently treated as an established fact until confirmed by the user.
6. **Unified Entities Hub Architecture (Phase 3):** Rather than cluttering the main bottom navigation bar with extra tabs, we integrated a segmented sub-navigation bar (`👥 People | 📍 Places | 🚀 Projects | 🤝 Commitments`) inside `PeopleScreen` and wired direct `Place` and `Project` selectors into `NoteEditorScreen`, keeping navigation fast and intuitive while making Places, Projects, and Commitments first-class citizens.
7. **Hybrid BM25/TF-IDF + 1-Hop Relational Graph Retrieval (Phase 4):** Rather than relying solely on substring matching or vector search, `SmartSearchEngine` combines corpus-level IDF keyword weighting with structured entity matching (`PersonEntity`, `PlaceEntity`, `ProjectEntity`, `CommitmentEntity`) and 1-hop relational expansion over `MemoryLinkEntity` edges and shared Project/Place associations.
8. **Deterministic Zero-Cost Anticipatory Briefing Synthesis (Phase 5):** Following Rule 6 (*Control complexity and costs — use deterministic code for straightforward tasks*), `MemoryIntelligenceHelper.buildAnticipatoryBriefing()` and `generateMeetingPrepBriefing()` synthesize open commitments, person dates, relationship cadence health, and project momentum deterministically on-device in sub-millisecond time without incurring background LLM token costs or battery drain.
9. **Cross-Dimensional Pattern Discovery & Epoch Timeline Engine (Phase 6):** `MemoryIntelligenceHelper.buildLifeIntelligenceReport()` correlates People × Projects × Places × Commitments × MemoryLinks to surface high-signal life patterns, memory rediscovery candidates, and a unified chronological timeline without additional network calls.
10. **Salted SHA-256 PIN Gate & Non-Destructive Raw Audio Retention (Phase 7):** `PrivacySecurityManager` hashes user PINs with a local salt via SHA-256 and enforces granular raw `.m4a` voice file retention policies (`DELETE_AFTER_TRANSCRIPTION`, `KEEP_7_DAYS`, `KEEP_30_DAYS`, `KEEP_INDEFINITELY`) while guaranteeing structured text transcripts and memories remain intact.
11. **O(1) Cached JSON TypeConverters & Full-Stack Robolectric CUJ Verification (Phase 8):** Pre-compiled regexes and bounded thread-safe lookup caches in `RoomConverters` eliminate redundant JSON parsing allocations during search and intelligence synthesis, verified across 25 combined unit and Robolectric CUJ tests.
