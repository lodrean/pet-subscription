# Pet Subscription — Android Billing + Firebase Demo

Минимальный пет-проект для демонстрации работы с **Google Play Billing Library 8.3.0** на Kotlin + Jetpack Compose + Koin + **Firebase**.

## Стек

- **Kotlin** 1.9.22
- **Jetpack Compose** (BOM 2024.02.00)
- **Google Play Billing Library** 8.3.0 (`billing` core, без ktx — используем `suspendCancellableCoroutine`)
- **Firebase** (Analytics + Firestore)
- **Koin** 3.5.3 (DI)
- **Coroutines + StateFlow**
- **MVVM**

## Что реализовано

### Billing
1. **BillingRepository** — обёртка над `BillingClient`:
   - Подключение к Google Play (`startConnection`)
   - Запрос продуктов (`queryProductDetailsAsync`)
   - Запуск покупки (`launchBillingFlow`)
   - Подтверждение покупки (`acknowledgePurchase`)
   - Проверка активных подписок при старте (`queryPurchasesAsync`)

### Firebase
2. **Firebase Analytics** — логирование ключевых событий:
   - `billing_query_products_start` — начало загрузки продуктов
   - `billing_products_loaded` — продукты загружены
   - `billing_purchase_start` — пользователь нажал "Купить"
   - `billing_purchase_success` — покупка подтверждена
   - `billing_purchase_error` — ошибка на любом этапе

3. **Firestore** — серверное хранение статуса подписки:
   - Коллекция `subscriptions`
   - Документ по `deviceId` (генерируется при первом запуске и сохраняется в SharedPreferences)
   - Поля: `deviceId`, `productId`, `isPremium`, `purchaseToken`, `updatedAt`

## 🔑 Ключевой файл: `google-services.json`

Для работы Firebase **обязателен** реальный конфигурационный файл:

### Как получить:

1. Перейди на [Firebase Console](https://console.firebase.google.com/)
2. Нажми **"Add project"** (или используй существующий)
3. Введи название проекта, например `pet-subscription`
4. Отключи Google Analytics если не нужен (или оставь — мы используем его)
5. Дождись создания проекта
6. Нажми **Android icon** (добавить приложение)
7. В поле **Android package name** введи: `com.example.subscription`
8. В поле **App nickname** введи: `Pet Subscription`
9. Нажми **Register app**
10. Скачай **`google-services.json`**
11. **Положи его в `pet-subscription/app/google-services.json`** (замени placeholder)
12. В консоли Firebase нажми **"Next"** и заверши настройку

> ⚠️ **Важно:** Без реального `google-services.json` приложение не соберётся (падает на плагине `com.google.gms.google-services`).

### Что внутри `google-services.json` (не редактируй руками):

- `project_number` и `project_id` — идентификаторы проекта
- `mobilesdk_app_id` — ID приложения в Firebase
- `api_key.current_key` — публичный API ключ (можно ограничить в Google Cloud Console)

## Как запустить

### 1. Создай подписки в Google Play Console

1. Загрузи APK/AAB во **внутреннее тестирование**
2. Перейди в **Monetize → Products → Subscriptions**
3. Создай 2 подписки с ID:
   - `premium_monthly`
   - `premium_yearly`
4. Для каждой подписки создай **Base Plan** (месяц / год)
5. Добавь тестовый аккаунт Gmail в **License Testing**

### 2. Настрой Firebase

- Выполни шаги из раздела **"Как получить google-services.json"** выше
- Замени placeholder файл на реальный

### 3. Настрой Firestore Security Rules (для теста)

В Firebase Console перейди в **Firestore Database → Rules** и временно установи:

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

> ⚠️ Для production используй auth-based rules!

### 4. Собери и запусти

```bash
./gradlew :app:assembleDebug
```

Или открой в Android Studio и нажми **Run**.

> **Важно:** BillingClient работает только на реальном устройстве или эмуляторе с Google Play services.

## Архитектура

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

## Ключевые моменты для собеседования

- **Acknowledge** — Google требует подтвердить покупку в течение 3 дней, иначе она вернётся
- **queryPurchasesAsync** — используем при старте, чтобы восстановить подписку после переустановки
- **Pending purchases** — в PBL 8+ `enablePendingPurchases(PendingPurchasesParams)` обязателен
- **ProductDetailsResponseListener** в PBL 8.3.0 возвращает `QueryProductDetailsResult`, а не `List<ProductDetails>` (API изменился)
- **Firestores sync** — статус подписки дублируется в облако для аналитики и кросс-девайс синхронизации

## Что почитать

- [Официальная документация PBL](https://developer.android.com/google/play/billing)
- [Релиз-ноты PBL](https://developer.android.com/google/play/billing/release-notes)
- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [Firebase Analytics Events](https://firebase.google.com/docs/analytics/events?platform=android)

## Лицензия

MIT — используй как шаблон для своих проектов.
