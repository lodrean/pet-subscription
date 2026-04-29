# 🛒 Pet Subscription — Android Billing + Firebase

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blueviolet?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02.00-brightgreen?logo=android)](https://developer.android.com/jetpack/compose)
[![Billing](https://img.shields.io/badge/Google%20Play%20Billing-8.3.0-orange?logo=google-play)](https://developer.android.com/google/play/billing)
[![Firebase](https://img.shields.io/badge/Firebase-Analytics%20%7C%20Firestore-yellow?logo=firebase)](https://firebase.google.com/)

> Минимальный пет-проект для демонстрации организации подписочного сервиса в Android с серверной синхронизацией через Firebase.

---

## 📹 Демо

| Экран подписок | Премиум контент |
| :---: | :---: |
| *(добавь скриншот)* | *(добавь скриншот)* |

---

## 🏗 Архитектура

```
┌─────────────────┐
│  UI (Compose)   │
│  Subscription   │
│     Screen      │
└────────┬────────┘
         │ StateFlow
┌────────▼────────┐
│   ViewModel     │
│ SubscriptionVM  │
└────────┬────────┘
         │
┌────────▼──────────────────┐
│  BillingRepositoryImpl    │
│  (BillingClient wrapper)  │
└────────┬──────────────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐  ┌──▼─────────────┐
│Google │  │  Firebase       │
│ Play  │  │  Analytics      │
│       │  │  Firestore      │
└───────┘  └─────────────────┘
```

---

## 🚀 Стек

| Технология | Версия | Назначение |
|-----------|--------|-----------|
| **Kotlin** | 1.9.22 | Язык |
| **Jetpack Compose** | BOM 2024.02.00 | UI |
| **Google Play Billing** | 8.3.0 (core) | Покупки и подписки |
| **Firebase Analytics** | 33.7.0 | Аналитика событий |
| **Firebase Firestore** | 33.7.0 | Облачное хранение статуса |
| **Koin** | 3.5.3 | DI |
| **Coroutines + StateFlow** | 1.7.3 | Асинхронность и реактивность |

> Почему `billing` core вместо `billing-ktx`?  
> PBL 8.3.0 ktx требует Kotlin 2.x. В пет-проекте используем core API с `suspendCancellableCoroutine` — это демонстрирует более глубокое понимание корутин на собеседовании.

---

## ✨ Что реализовано

### 💳 Billing
- [x] Подключение к Google Play (`startConnection`)
- [x] Запрос доступных подписок (`queryProductDetailsAsync`)
- [x] Запуск покупки (`launchBillingFlow`)
- [x] Подтверждение покупки (`acknowledgePurchase`) — **критично!**
- [x] Проверка активных подписок при старте (`queryPurchasesAsync`)
- [x] Обработка отмены и ошибок (`PurchasesUpdatedListener`)

### 🔥 Firebase
- [x] **Analytics** — логирование ключевых событий:
  - `billing_query_products_start`
  - `billing_products_loaded`
  - `billing_purchase_start`
  - `billing_purchase_success`
  - `billing_purchase_error`
- [x] **Firestore** — серверное хранение статуса подписки:
  - Коллекция `subscriptions`
  - Документ по `deviceId` (генерируется при первом запуске, сохраняется в `SharedPreferences`)
  - Поля: `deviceId`, `productId`, `isPremium`, `purchaseToken`, `updatedAt`

---

## 📁 Структура проекта

```
app/src/main/java/com/example/subscription/
├── MainActivity.kt
├── SubscriptionApp.kt              # Application + Koin
├── di/
│   └── AppModule.kt                # DI-модули (Koin)
├── domain/model/
│   └── SubscriptionTier.kt         # Модель подписки
├── data/
│   ├── billing/
│   │   ├── BillingRepository.kt    # Интерфейс
│   │   └── BillingRepositoryImpl.kt # Реализация PBL 8.3.0
│   └── firebase/
│       └── FirebaseSubscriptionDataSource.kt # Firestore sync
└── presentation/
    ├── SubscriptionViewModel.kt
    └── screens/
        └── SubscriptionScreen.kt   # Compose UI
```

---

## 🔑 Как подключить Firebase

### Шаг 1: Создай проект в Firebase Console
1. Перейди на [Firebase Console](https://console.firebase.google.com/)
2. Нажми **"Add project"** → введи название → **Create project**

### Шаг 2: Добавь Android-приложение
1. На главной странице проекта нажми **Android-иконку** (Add app)
2. Введи:
   - **Package name:** `com.example.subscription`
   - **App nickname:** `Pet Subscription`
3. Нажми **Register app**

### Шаг 3: Скачай конфигурационный файл
- Скачай **`google-services.json`**
- Положи его в `app/google-services.json` (**замени placeholder**)
- В консоли нажми **Next → Next → Continue to console**

> ⚠️ **Важно:** файл `google-services.json` уже добавлен в `.gitignore` и не попадёт в репозиторий. Никогда не коммить его в публичный репозиторий!

### Шаг 4: Настрой Firestore Rules (для теста)
В Firebase Console перейди в **Firestore Database → Rules** и установи:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /subscriptions/{document} {
      allow read, write: if true;
    }
  }
}
```

> Для production используй auth-based rules!

---

## 🧪 Как запустить

### 1. Google Play Console
- Создай приложение и загрузи APK/AAB во **внутреннее тестирование**
- Перейди в **Monetize → Products → Subscriptions**
- Создай 2 подписки с ID:
  - `premium_monthly`
  - `premium_yearly`
- Для каждой подписки создай **Base Plan** (месяц / год)
- Добавь тестовый Gmail в **License Testing**

### 2. Сборка
```bash
./gradlew :app:assembleDebug
```

Или открой в Android Studio и нажми **Run**.

> ⚠️ **BillingClient работает только на реальном устройстве** или эмуляторе с Google Play services.

---

## 📊 Просмотр данных в Firebase

| Сервис | Где смотреть | Что увидишь |
|--------|-------------|-------------|
| **Analytics** | Console → Analytics → Events | `billing_purchase_success`, `billing_purchase_error` и др. |
| **Firestore** | Console → Firestore → Data | Коллекция `subscriptions` с deviceId-документами |

---

## 🎯 Что спросят на собеседовании

| Вопрос | Ответ в коде |
|--------|-------------|
| «Что будет, если не вызвать acknowledgePurchase?» | Покупка автоматически вернётся через 3 дня |
| «Как обработать upgrade/downgrade?» | Нужно использовать `SubscriptionUpdateParams` с `replacementMode` |
| «Почему не ktx?» | PBL 8.3.0 ktx требует Kotlin 2.x; core API + `suspendCancellableCoroutine` даёт полный контроль |
| «Как тестировать подписки?» | Google Play Console → тестовые аккаунты + тестовые SKU |
| «Где хранить статус подписки?» | Локально (StateFlow) + Firestore для кросс-девайс синхронизации |

---

## 📄 Лицензия

MIT — используй как шаблон для своих проектов.
