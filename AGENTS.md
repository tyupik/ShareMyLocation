```markdown
# AGENTS.md

**ВАЖНО: отвечай только на русском языке. Это обязательное требование, не рекомендация.**

Проект: Android-приложение (локатор).
Архитектура: MVI (State, Intent, Effect) + Clean Architecture.
DI: Hilt (KSP, не kapt).
UI: Jetpack Compose + Material 3.
Auth: Firebase Auth + Credential Manager (не GoogleSignInClient).
Сеть: Retrofit + kotlinx-serialization (если понадобится).
Navigation: Navigation 3 (androidx.navigation3).

---

# Структура проекта

Пакет: `ru.tyupik.sharemylocation`. Слои Clean Architecture:

- `business/` — интеракторы, бизнес-модели (BusinessModel), интерфейсы репозиториев. ViewModel здесь НЕ живут.
- `data/` — реализации репозиториев, источники данных (Firebase, Credential Manager, кэш), Hilt-модули привязок data-слоя.
- `ui/` — Compose-экраны и их UI-элементы, ViewModel с MVI-контрактом (UiState/Intent/Effect), конвертеры BusinessModel → UiModel, корневая навигация.

Внутри каждого слоя — отдельная директория на экран/фичу: `ui.auth`, `business.auth`, `data.auth`.
В директории экрана лежат только модели и классы, относящиеся к этому экрану.

Общие для нескольких экранов классы и модели — в `common`-директории слоя: `ui.common`, `business.common`, `data.common`.

# Поток данных

Data → Repository (интерфейс в business) → Interactor (business, возвращает BusinessModel)
→ ViewModel (ui) → Converter → UiModel → UiState.

- Интерактор возвращает бизнес-модели (например, `AuthResult`, `User`), а не исключения и не DTO источника данных.
- ViewModel не знает про data-слой: получает BusinessModel от интерактора и передаёт в конвертер (`<Фича>UiConverter`).
- Конвертер выдаёт UiModel; ViewModel кладёт её в UiState (или рассылает Effect).
- UI не знает про business-модели напрямую — только через UiModel/UiState.
- Экран перечитывает данные при каждом показе: `LaunchedEffect(Unit) { viewModel.onIntent(<Фича>Intent.ScreenShown) }` — ViewModel может переживать повторный вход на экран, полагаться только на её init нельзя.

# UI-элементы экрана

Приватные UI-элементы экрана — в `ui.<фича>.composable`, один файл — один элемент (например, `AuthEmailField.kt`, `AuthErrorBanner.kt`).
`<Фича>Screen.kt` только собирает элементы, считает производные флаги и маппит состояние в их параметры. Не держать несколько разнородных composable в одном файле.

Пример структуры фичи auth:

```text
ru.tyupik.sharemylocation/
├── business/
│   ├── auth/     # AuthInteractor (+Impl, Module), AuthRepository (интерфейс), AuthResult, AuthError
│   ├── common/   # User — общие бизнес-модели
│   └── ...
├── data/
│   ├── auth/     # AuthRepositoryImpl, FirebaseBackend.java, AuthModule
│   ├── common/   # общие helpers data-слоя (Result-обёртки и т.п.)
│   └── ...
└── ui/
    ├── auth/
    │   ├── composable/  # AuthModeSwitcher, AuthErrorBanner, AuthEmailField, AuthPasswordField, ...
    │   ├── AuthScreen.kt
    │   ├── AuthViewModel.kt
    │   ├── AuthContract.kt   # AuthUiState, AuthIntent, AuthEffect, AuthMode
    │   └── AuthUiConverter.kt # конвертер + AuthUiModel, AuthUiError
    ├── common/   # AppRoot (корневая навигация), AppViewModel, AppUiConverter
    ├── main/     # MainScreen, MainViewModel, MainContract, MainUiConverter
    └── theme/    # тема приложения
```

Разделение ответственности: контракт экрана (UiState/Intent/Effect) и ViewModel живут в `ui.<фича>`;
`business.<фича>` — только интеракторы и бизнес-модели; `data.<фича>` — только реализации и источники данных.

---

# MVI Architecture Rules

## Тройка MVI

Каждая фича обязана иметь три составляющие:

1. **State (UiState)**
   - Единственный `data class` или `sealed class` на весь экран.
   - Иммутабельный. Обновляется только через `copy()` или новый инстанс.
   - Expose через `StateFlow<UiState>`.
   - Начальное состояние обязательно.

2. **Intent**
   - `sealed class` со всеми возможными действиями пользователя/системы.
   - UI вызывает `viewModel.onIntent(intent)` — единственную публичную функцию.
   - Запрещены публичные методы типа `onEmailChanged()`, `onSubmit()`.

3. **Effect**
   - `sealed class` для one-off событий: навигация, Toast, Snackbar, диалог.
   - Expose через `Channel<Effect>` + `receiveAsFlow()`.
   - **Запрещено** класть Effect в State (иначе повторится при рекомпозиции/повороте).

## ViewModel

- Наследуется от `ViewModel` (AAC), НЕ от `AndroidViewModel`.
- Помечается `@HiltViewModel`.
- Внутри — `MutableStateFlow<UiState>` и `Channel<Effect>`.
- Вся логика обработки Intent — в `viewModelScope.launch`.
- **Никаких прямых вызовов Firebase/Retrofit** — только через Repository.

## Repository

- Интерфейс в `domain`, реализация в `data`.
- Возвращает `Flow<T>` или `suspend fun ...: Result<T>`.
- Repository — единственный, кто знает про источники данных.

## UI (Compose)

- Composable получает `uiState` и лямбду `onIntent: (Intent) -> Unit`.
- **Не знает** про ViewModel напрямую (кроме входной точки).
- Собирает Effect через `LaunchedEffect` + `collect`.

## Single Source of Truth

- `UiState` — SSOT для UI.
- Repository — SSOT для данных.
- **Никогда** не смешивать локальные `remember`-переменные с бизнес-состоянием.

## Навигация

- Навигация — это часть UiState или отдельный слой, а не логика внутри Composable.
- В MVI навигационные события (какой экран показать) должны проходить через Effect, а не вызываться напрямую из лямбды кнопки.

---

# Tech Stack

## Core

- Kotlin 2.1+
- Jetpack Compose (BOM latest stable)
- Coroutines + Flow, StateFlow, SharedFlow
- Hilt 2.51+ (использовать KSP, не kapt)

## Network (если понадобится)

- Retrofit 2.11+ + kotlinx-serialization
- OkHttp 4.12+
- НЕ использовать Ktor (если только не KMP)

## Firebase

- Firebase BOM latest
- Firebase Auth (Credential Manager, НЕ GoogleSignInClient)
- Firestore

## Navigation

- Navigation 3 (`androidx.navigation3`)
- НЕ использовать Navigation 2 (устаревающий подход для новых проектов)
- Back stack управляется через `rememberNavBackStack` / `SnapshotStateList`

## Именование MVI-компонентов

- ViewModel: `AuthViewModel`, `GroupsViewModel`
- UiState: `AuthUiState`
- Intent: `AuthIntent`
- Effect: `AuthEffect`
- Repository: интерфейс `AuthRepository`, реализация `AuthRepositoryImpl`

## Запрещено

- ❌ LiveData — только StateFlow/SharedFlow
- ❌ GlobalScope
- ❌ kapt — использовать KSP
- ❌ GoogleSignInClient (устарел) — только Credential Manager
- ❌ Gson/Moshi — только kotlinx-serialization
- ❌ Прямые вызовы Firebase из ViewModel
- ❌ Material 2 (`androidx.compose.material.*`) — только Material 3
- ❌ Хардкод hex/rgb — только роли `MaterialTheme.colorScheme.*`
- ❌ `ContentAlpha` (удалён в M3)

---

# Design System (Material 3)

## Импорты

- ТОЛЬКО из `androidx.compose.material3.*`
- ❌ НЕ использовать M2 (`androidx.compose.material.*`)

## Цвета

- Только роли: `MaterialTheme.colorScheme.primary`, `.surface`, `.onSurfaceVariant`
- ❌ НЕ хардкодить hex/rgb
- Для разделителей: `outlineVariant` (не `outline`)

## Элевация

- M3 использует тональную элевацию (`tonalElevation`), не тени
- `shadowElevation` — только при необходимости отделить от шумного фона

## Типографика

- `MaterialTheme.typography.*`
- ❌ НЕ использовать `ContentAlpha` (удалён в M3)

---

# Kotlin Conventions

## Coroutines

- `viewModelScope.launch` для всех операций во ViewModel
- `suspend` функции только в Repository/UseCase
- Отмена через `ensureActive()` или `isActive`

## Flow

- `StateFlow` для состояния (всегда `MutableStateFlow` → `asStateFlow()`)
- `Channel` для Effect (capacity = `Channel.BUFFERED`)
- `receiveAsFlow()` для Expose Effect

## Обработка ошибок

- Repository возвращает `Result<T>`
- В ViewModel: `result.fold(onSuccess = ..., onFailure = ...)`
- **НЕ глотать исключения** — логировать и преобразовывать в `UiState.Error`

## UiState

- `sealed interface` / `sealed class` если состояния взаимоисключающие
- `data class` если поля независимые (но всё равно один State)
- Всегда есть `object Loading` или `data class Error(val message: String)`

## Комментарии

- KDoc для публичных функций
- НЕ комментировать очевидное

---

# Общие соглашения

## Именование

- Пакеты: `lowercase`, без подчёркиваний. Пример: `com.example.locationapp.auth`
- Классы, интерфейсы, объекты, enum: `UpperCamelCase`. Пример: `AuthViewModel`, `AuthUiState`
- Функции и свойства: `lowerCamelCase`. Пример: `signInWithEmail`, `isLoading`
- Константы (`const val`, `val` верхнего уровня, `companion object`): `SCREAMING_SNAKE_CASE`. Пример: `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT_MS`
- Файлы: `UpperCamelCase` для классов (`AuthViewModel.kt`), `lowerCamelCase` для утилит (`extensions.kt`)

## Форматирование

- Отступы: **4 пробела**. Не табы.
- Завершающие запятые: **включены** для всех многострочных объявлений (аргументы, параметры, элементы списков, `when`-ветки).
- Максимальная длина строки: 120 символов.
- Пустая строка между логическими блоками внутри функции — приветствуется.
- Импорты: без wildcard (`import foo.*` запрещён), сортировка автоматическая.

## Сохранение технических артефактов

**Никогда не переводить, не переписывать и не «улучшать»:**

- Блоки кода (Kotlin, Gradle, XML, JSON, YAML и т.д.) — как есть.
- CLI-команды (`./gradlew`, `git`, `adb`) — как есть.
- Пути к файлам (`app/src/main/...`) — как есть.
- Стек-трейсы — как есть, включая номера строк и имена классов.
- Логи (Logcat, Firebase, Gradle) — как есть.
- JSON-ключи (`"displayName"`, `"createdAt"`) — как есть.
- Идентификаторы (`applicationId`, `serverClientId`, `projectId`) — как есть.

## Цитаты пользователя

Если пользователь что-то процитировал — сохраняй в кавычках **как есть**, не исправляя опечатки и не переводя.

## Сообщения об ошибках инструментов

Сообщения об ошибках от Gradle, Android Studio, Firebase, компилятора и любых инструментов **оставлять на английском**, как есть. Не переводить и не перефразировать.

## Тестирование

- **Юнит-тесты на данном этапе не пишем.** Не создавай `*Test.kt`, не предлагай тесты, не добавляй test-зависимости.
- Если тесты понадобятся позже — я скажу отдельно.

## Язык ответов агента

**ОБЯЗАТЕЛЬНОЕ ТРЕБОВАНИЕ:** агент отвечает **только на русском языке**.

Это не рекомендация, а жёсткое правило:

- Все объяснения, планы, вопросы, комментарии к коду — на русском.
- Комментарии в коде (KDoc, `//`) — на русском.
- Сообщения коммитов — на русском.
- Если пользователь написал на другом языке — всё равно отвечай на русском.
- Исключение: технические артефакты (см. выше) остаются как есть.

---

# Workflow

## Feature Spec Template

Когда я прошу реализовать фичу, следуй этому шаблону:

### 1. Contract (сначала покажи мне)

- `UiState` (все поля, начальное состояние)
- `Intent` (sealed class, все действия)
- `Effect` (sealed class, one-off события)

### 2. Слои

- Repository: интерфейс + реализация (если нет — создай)
- ViewModel: обработка Intent → State, отправка Effect
- Screen: Composable, рендер State, отправка Intent

### 3. Правила

- **Сначала покажи контракт**, дождись подтверждения
- **Потом пиши код** слоями: Repository → ViewModel → Screen
- **Не трогай** MainActivity и навигацию без явного разрешения
- **Используй** Hilt `@Inject` везде

### 4. Критерии готовности

- Компилируется без ошибок
- UiState не содержит избыточных флагов
- Effect не дублируется при повороте

---

**НАПОМИНАНИЕ: отвечай только на русском языке. Это обязательное требование.**
```