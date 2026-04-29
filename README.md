# 🛒 Pet Subscription + 🤖 AI Photo Editor

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blueviolet?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02.00-brightgreen?logo=android)](https://developer.android.com/jetpack/compose)
[![Billing](https://img.shields.io/badge/Google%20Play%20Billing-8.3.0-orange?logo=google-play)](https://developer.android.com/google/play/billing)
[![Firebase](https://img.shields.io/badge/Firebase-Analytics%20%7C%20Firestore-yellow?logo=firebase)](https://firebase.google.com/)
[![Gemini](https://img.shields.io/badge/Gemini%20AI-1.5%20Flash-blue?logo=google)](https://ai.google.dev/)

> Android pet-project: подписочная система через Google Play Billing Library + AI фоторедактор на базе Gemini API.  
> Стек: Jetpack Compose, MVVM, Koin, Firebase, Coroutines.

---

## 📱 Скриншоты

| Подписки | AI Photo Editor |
| :---: | :---: |
| *(добавь скриншот)* | *(добавь скриншот)* |

---

## 🏗 Архитектура

```
┌─────────────────────────────────────────────┐
│              UI (Compose)                    │
│  ┌─────────────┐      ┌─────────────────┐   │
│  │ Subscription│      │  AI Photo Editor│   │
│  │    Screen   │      │     Screen      │   │
│  └──────┬──────┘      └────────┬────────┘   │
│         │ StateFlow             │ StateFlow  │
│  ┌──────▼──────┐      ┌────────▼────────┐   │
│  │ Subscription│      │  PhotoEditor    │   │
│  │   ViewModel │      │    ViewModel    │   │
│  └──────┬──────┘      └────────┬────────┘   │
│         │                       │            │
│  ┌──────▼──────┐      ┌────────▼────────┐   │
│  │  Billing    │      │  GeminiService  │   │
│  │ Repository  │      │  ImageFilters   │   │
│  └──────┬──────┘      └─────────────────┘   │
│         │                                    │
│    ┌────┴────┐                               │
│    │         │                               │
│ ┌──▼───┐  ┌──▼─────────────┐                │
│ │Google│  │  Firebase       │                │
│ │ Play │  │  Analytics      │                │
│ │      │  │  Firestore      │                │
│ └──────┘  └─────────────────┘                │
└─────────────────────────────────────────────┘
```

---

## 🚀 Функционал

### 💳 Подписочная система
- Запрос доступных подписок через Google Play Billing Library 8.3.0
- Покупка и подтверждение (acknowledge) подписки
- Проверка активных подписок при старте приложения
- Firebase Analytics событий покупок
- Firestore синхронизация статуса подписки

### 🤖 AI Photo Editor
- Выбор фото из галереи (Photo Picker)
- Локальные фильтры на Bitmap: Grayscale, Sepia, Brightness, Contrast
- Интеграция с **Gemini 1.5 Flash** — анализ изображений по текстовому промпту
- Отправка фото + промпта в Google Generative AI API

---

## 🛠 Стек

| Технология | Версия | Назначение |
|-----------|--------|-----------|
| **Kotlin** | 1.9.22 | Язык |
| **Jetpack Compose** | BOM 2024.02.00 | UI |
| **Compose Navigation** | 2.7.7 | Навигация между экранами |
| **Google Play Billing** | 8.3.0 (core) | Покупки и подписки |
| **Gemini AI** | 0.9.0 | Генеративный AI |
| **Firebase Analytics** | 33.7.0 | Аналитика |
| **Firebase Firestore** | 33.7.0 | Облачное хранение |
| **Koin** | 3.5.3 | DI |
| **Coil** | 2.6.0 | Загрузка изображений |
| **Coroutines + StateFlow** | 1.7.3 | Асинхронность |

---

## 📁 Структура проекта

```
app/src/main/java/com/example/subscription/
├── MainActivity.kt                      # BottomNavigation: Подписки + Фоторедактор
├── SubscriptionApp.kt                   # Application + Koin
├── di/
│   └── AppModule.kt                     # DI-модули
├── domain/model/
│   └── SubscriptionTier.kt
├── data/
│   ├── billing/
│   │   ├── BillingRepository.kt         # Интерфейс
│   │   └── BillingRepositoryImpl.kt     # Реализация PBL 8.3.0
│   ├── firebase/
│   │   └── FirebaseSubscriptionDataSource.kt
│   └── remote/
│       ├── GeminiService.kt             # Google Generative AI SDK
│       └── SubscriptionVerifyService.kt # Placeholder для серверной верификации
├── presentation/
│   ├── SubscriptionViewModel.kt
│   ├── PhotoEditorViewModel.kt          # AI + фильтры
│   └── screens/
│       ├── SubscriptionScreen.kt
│       └── PhotoEditorScreen.kt         # Photo Picker + Gemini
└── utils/
    └── ImageFilters.kt                  # Bitmap фильтры
```

---

## 🔑 Ключи API

### Google Play Billing
- Создай подписки в [Google Play Console](https://play.google.com/console/)
- Добавь тестовый аккаунт в License Testing

### Firebase
1. Создай проект в [Firebase Console](https://console.firebase.google.com/)
2. Зарегистрируй Android-приложение с package `com.example.subscription`
3. Скачай `google-services.json` и положи в `app/`

### Gemini API
1. Получи ключ в [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Замени в `di/AppModule.kt`:
   ```kotlin
   GeminiService(apiKey = "YOUR_GEMINI_API_KEY_HERE")
   ```

> ⚠️ **Никогда не коммить API ключи!** `google-services.json` и ключи уже добавлены в `.gitignore`.

---

## 🧪 Запуск

```bash
./gradlew :app:assembleDebug
```

> BillingClient работает только на реальном устройстве.  
> Gemini API требует интернет-соединения.

---

## 🌳 Git Workflow (Gitflow)

```
main    ───●────────────────●─────────────●───────
           ↑                ↑             ↑
           v1.0.0           v1.1.0         v1.1.1

develop ───●───●───●───●───●───●───●───●───●───●────────
              ↑       ↑       ↑       ↑
feature/     ●───────●       ●───────●

release/             ●─────────●

hotfix/                                    ●────●
```

| Ветка | Назначение |
|-------|-----------|
| `main` | Production-код. Каждый коммит = тег версии |
| `develop` | Интеграция фич. Сборка для internal testing |
| `feature/*` | Новая функциональность |
| `release/*` | Релиз: бамп версии, финальное тестирование |
| `hotfix/*` | Критический баг в production |

Подробное руководство: [`docs/GITFLOW.md`](docs/GITFLOW.md)

---

## 📝 Conventional Commits

```
feat(billing): add queryPurchasesAsync on app start
fix(ui): correct premium badge visibility after rotation
refactor(repository): extract BillingRepository interface
chore(gradle): update Billing Library to 8.3.0
feat(ai): integrate Gemini photo analyzer
test(billing): add unit tests for purchase validation
```

---

## 📄 Лицензия

MIT — используй как шаблон для своих проектов.
