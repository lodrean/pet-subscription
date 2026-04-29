# Безопасность API ключей в Android

> Практическое руководство по защите API ключей в мобильных приложениях.

---

## ❌ Можно ли использовать один API key для всех клиентов?

**Технически — да. Практически — нет.**

### Почему это плохая идея:

| Проблема | Последствие |
|----------|-------------|
| Невозможно отозвать ключ у одного клиента | Если ключ слит — меняешь его для всех, ломаешь приложение у честных пользователей |
| Невозможно отслеживать лимиты по клиентам | Один злоумышленник может исчерпать квоту для всех |
| Нет аудита | Не понятно, кто и когда делал запросы |
| Нет rate limiting по клиенту | DDoS одного пользователя = downtime для всех |

**Вывод:** каждый клиент должен аутентифицироваться индивидуально (Firebase Auth, OAuth, API key per user).

---

## 🔐 Можно ли зашифровать API key в приложении?

**Краткий ответ: нет. Полностью защитить ключ в клиенте невозможно.**

### Почему:

1. **Decompiling APK** — любой может распаковать `.apk`, достать `.dex`, сконвертировать в `.jar` и прочитать код
2. **Strings в BuildConfig** — даже если ключ в `local.properties` → `BuildConfig`, он всё равно остаётся строкой в скомпилированном коде
3. **Memory dump** — ключ расшифровывается в RAM во время работы. Профессиональный reverse engineer достанет его оттуда
4. **Man-in-the-Middle** — если ключ передаётся по сети без pinning'а, его перехватят

### Цепочка доверия:

```
APK файл → Decompile → Strings / BuildConfig → API key
RAM dump → Heap analysis → Расшифрованный ключ
Network traffic → Proxy (Charles/Fiddler) → API key
```

---

## ✅ Как правильно защищать API ключи

### Уровень 1: Не хранить ключ в коде (minimal)

```kotlin
// ❌ Плохо: ключ прямо в коде
val apiKey = "sk-1234567890abcdef"

// ✅ Лучше: вынести в local.properties → BuildConfig
// Файл local.properties НЕ коммится в git
val apiKey = BuildConfig.API_KEY
```

**Что это даёт:** защита от casual inspection (кто-то открыл код и не увидел ключ).

**Что НЕ даёт:** защиты от decompiling.

---

### Уровень 2: Android Keystore + шифрование (этот проект)

```kotlin
// Ключ генерируется в hardware-backed хранилище
// API key шифруется AES-256-GCM и сохраняется в SharedPreferences
val apiKey = SecureApiKeyProvider(context).getApiKey()
```

**Что это даёт:**
- Ключ не лежит открытым текстом в APK
- Для извлечения нужен root-доступ + reverse engineering
- Если приложение удалено и переустановлено — старый encrypted key не расшифруется (новый Keystore key)

**Что НЕ даёт:**
- Полной защиты от опытного reverse engineer
- Защиты от runtime memory dump

---

### Уровень 3: Native код (JNI/C++)

```cpp
// Ключ хранится в нативной библиотеке .so
// Java/Kotlin дергает JNI метод который возвращает ключ
extern "C" JNIEXPORT jstring JNICALL
Java_com_example_app_NativeLib_getApiKey(JNIEnv* env, jobject thiz) {
    return env->NewStringUTF("encrypted_or_obfuscated_key");
}
```

**Что это даёт:** усложняет reverse engineering (нужно уметь читать ARM/x86 assembly).

**Что НЕ даёт:** полной защиты (`.so` тоже можно декомпилировать через Ghidra/IDA).

---

### Уровень 4: Обфускация + Runtime decryption

```kotlin
// Ключ разбит на части, обфусцирован, собирается в runtime
val part1 = "sk" + reverse("0987654321")
val part2 = xor("encrypted_string", "runtime_salt")
```

**Что это даёт:** защита от автоматических сканеров.

**Что НЕ даёт:** защиты от ручного анализа.

---

### Уровень 5: Серверный прокси (production standard) ⭐

```
┌─────────┐         ┌──────────────┐         ┌─────────────┐
│ Android │ ──────► │ Твой сервер  │ ──────► │ Gemini API  │
│ (без    │         │ (API key     │         │ (ключ       │
│  ключа) │ ◄────── │  на сервере) │ ◄────── │  на сервере)│
└─────────┘         └──────────────┘         └─────────────┘
```

**Архитектура:**
1. Клиент аутентифицируется через Firebase Auth / JWT
2. Клиент шлёт запрос на свой backend: `POST /api/analyze-image` (без API key)
3. Backend проверяет auth, достаёт API key из переменных окружения
4. Backend шлёт запрос в Gemini / OpenAI
5. Backend возвращает результат клиенту

**Что это даёт:**
- ✅ API key вообще не покидает сервер
- ✅ Можно отключить конкретного пользователя
- ✅ Можно логировать и лимитировать запросы
- ✅ Можно кэшировать ответы
- ✅ Можно менять AI-провайдера без обновления приложения

**Что НЕ даёт:** защиты от MITM без HTTPS + certificate pinning.

---

## 📊 Сравнение методов

| Метод | Защита от decompiling | Защита от MITM | Сложность | Production-ready |
|-------|----------------------|----------------|-----------|-----------------|
| Hardcoded в коде | ❌ Нет | ❌ Нет | 🟢 Просто | ❌ Нет |
| BuildConfig + `.gitignore` | ❌ Нет | ❌ Нет | 🟢 Просто | ❌ Нет |
| Android Keystore | 🟡 Средняя | ❌ Нет | 🟡 Средне | ❌ Нет |
| Native код (JNI) | 🟡 Средняя | ❌ Нет | 🔴 Сложно | ❌ Нет |
| Серверный прокси | ✅ Да | 🟡 HTTPS | 🟡 Средне | ✅ Да |
| Сервер + Certificate Pinning | ✅ Да | ✅ Да | 🔴 Сложно | ✅ Да |

---

## 🛡 Что реализовано в этом проекте

### 1. `local.properties` → `BuildConfig`

Ключ вынесен из кода в `local.properties`:
```properties
GEMINI_API_KEY=YOUR_GEMINI_API_KEY_HERE
```

`local.properties` добавлен в `.gitignore` и **никогда не попадёт в репозиторий**.

### 2. `SecureApiKeyProvider` (Android Keystore)

```kotlin
class SecureApiKeyProvider(context: Context) {
    fun getApiKey(): String {
        // При первом запуске: берёт из BuildConfig → шифрует AES-256-GCM → сохраняет в SharedPrefs
        // При последующих: расшифровывает из SharedPrefs через hardware-backed ключ
    }
}
```

**Алгоритм:**
- `AES/GCM/NoPadding`
- Ключ 256-bit, генерируется в `AndroidKeyStore`
- IV (initialization vector) сохраняется рядом с зашифрованными данными
- `setRandomizedEncryptionRequired(true)` — каждое шифрование даёт новый IV

### 3. Pre-commit hook

В `.githooks/pre-commit` проверка на случайный коммит `google-services.json` или hardcoded ключей:
```bash
if git diff --cached --name-only | grep -q "google-services.json"; then
    echo "ERROR: google-services.json detected!"
    exit 1
fi
```

---

## 🚀 Как добавить серверный прокси (production)

Если ты хочешь довести безопасность до production-уровня, добавь `SubscriptionVerifyService.kt` как backend endpoint:

```kotlin
interface SubscriptionVerifyService {
    @POST("api/analyze-image")
    suspend fun analyzeImage(
        @Body request: ImageAnalysisRequest
    ): ImageAnalysisResponse
}
```

Backend (Node.js / Python / Kotlin Spring):
```javascript
app.post('/api/analyze-image', async (req, res) => {
    // 1. Проверить Firebase Auth token
    const user = await verifyAuthToken(req.headers.authorization);
    
    // 2. Rate limiting: max 10 запросов в минуту на пользователя
    if (await isRateLimited(user.uid)) {
        return res.status(429).json({ error: 'Rate limit exceeded' });
    }
    
    // 3. Отправить в Gemini со своим ключом
    const geminiResponse = await fetch('https://generativelanguage.googleapis.com/...', {
        headers: { 'x-goog-api-key': process.env.GEMINI_API_KEY }
    });
    
    // 4. Вернуть результат
    res.json(geminiResponse);
});
```

---

## ❓ FAQ

**Q: Почему нельзя просто использовать ProGuard/R8 для защиты?**  
A: ProGuard обфусцирует имена классов/методов, но строковые литералы остаются нетронутыми. API key в `BuildConfig` или в коде всё равно будет виден после decompiling.

**Q: А если использовать Firebase Remote Config для подгрузки ключа?**  
A: Это лучше чем hardcoded ключ (можно отключить/поменять без релиза), но сам Remote Config value всё равно хранится на устройстве и может быть перехвачен.

**Q: Что делать если ключ всё-таки слит?**  
A: Если используется серверный прокси — меняешь ключ на сервере за 5 минут. Если ключ в приложении — выпускаешь обновление, ждёшь пока все обновятся, отключаешь старый ключ.

**Q: А как Google Play защищает свой billing?**  
A: Google Play Billing Library не хранит ключ в приложении. Она использует IPC (inter-process communication) с Google Play Services app, который подписан Google certificate. Это hardware-backed аутентификация.

---

## 📚 Полезные ссылки

- [Android Keystore System](https://developer.android.com/training/articles/keystore)
- [OWASP Mobile Security Testing Guide](https://mas.owasp.org/)
- [Google AI Studio — API Keys](https://aistudio.google.com/app/apikey)
