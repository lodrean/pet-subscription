# Gitflow в Pet Subscription

> Практическое руководство по организации веток для Android-проекта с подписками.

## 🌳 Структура веток

```
main    ───●────────────────●─────────────●───────
           ↑                ↑             ↑
           v1.0.0           v1.1.0         v1.1.1

develop ───●───●───●───●───●───●───●───●───●───●────────
              ↑       ↑       ↑       ↑
feature/     ●───────●       ●───────●
subscription              rustore

release/             ●─────────●
v1.1.0

hotfix/                                    ●────●
v1.1.1
```

| Ветка | Назначение | Откуда | Куда | Жизненный цикл |
|-------|-----------|--------|------|----------------|
| `main` | Production. Каждый коммит = тег версии в Google Play | — | — | Бессрочная |
| `develop` | Интеграция всех фич. Сборка для internal testing | `main` | — | Бессрочная |
| `feature/*` | Новая функциональность | `develop` | `develop` | Удаляется после merge |
| `release/*` | Подготовка релиза: версия, финальное тестирование | `develop` | `main` + `develop` | Удаляется после merge |
| `hotfix/*` | Критический баг в production | `main` | `main` + `develop` | Удаляется после merge |

---

## 🚀 Быстрые команды (алиасы)

Добавь в `~/.gitconfig`:

```ini
[alias]
    # Feature workflow
    feat-start = "!f() { git checkout develop && git pull origin develop && git checkout -b feature/$1; }; f"
    feat-finish = "!f() { git checkout develop && git merge --no-ff feature/$1 && git branch -d feature/$1 && git push origin --delete feature/$1; }; f"
    
    # Release workflow
    release-start = "!f() { git checkout develop && git pull origin develop && git checkout -b release/v$1; }; f"
    release-finish = "!f() { git checkout main && git merge --no-ff release/v$1 && git tag -a v$1 -m \"Release v$1\" && git push origin main --tags && git checkout develop && git merge --no-ff release/v$1 && git push origin develop && git branch -d release/v$1; }; f"
    
    # Hotfix workflow
    hotfix-start = "!f() { git checkout main && git pull origin main && git checkout -b hotfix/v$1; }; f"
    hotfix-finish = "!f() { git checkout main && git merge --no-ff hotfix/v$1 && git tag -a v$1 -m \"Hotfix v$1\" && git push origin main --tags && git checkout develop && git merge --no-ff hotfix/v$1 && git push origin develop && git branch -d hotfix/v$1; }; f"
```

---

## 📋 Пошаговые сценарии

### 1. Начать новую фичу

```bash
# Ручной способ
git checkout develop
git pull origin develop
git checkout -b feature/server-verification

# Или через алиас
git feat-start server-verification
```

### 2. Закончить фичу (через PR)

```bash
# Обнови свою ветку
git checkout develop
git pull origin develop
git checkout feature/server-verification
git rebase develop

# Отправь на GitHub и создай PR
git push -u origin feature/server-verification

# После approve в GitHub — мерж локально
git checkout develop
git merge --no-ff feature/server-verification
git push origin develop
git branch -d feature/server-verification
```

### 3. Подготовить релиз

```bash
# Создать релизную ветку от develop
git checkout develop
git pull origin develop
git checkout -b release/v1.1.0

# Обновить versionCode и versionName в app/build.gradle.kts
git add app/build.gradle.kts
git commit -m "chore(release): bump version to 1.1.0"

# QA тестирует, фиксятся баги...
git commit -m "fix(billing): correct token parsing"

# Завершить релиз
git checkout main
git merge --no-ff release/v1.1.0
git tag -a v1.1.0 -m "Release v1.1.0: subscription sync"
git push origin main --tags

git checkout develop
git merge --no-ff release/v1.1.0
git push origin develop

git branch -d release/v1.1.0
```

### 4. Экстренный hotfix

```bash
# Только от main!
git checkout main
git pull origin main
git checkout -b hotfix/v1.1.1

# Исправляем критический баг
git commit -m "fix(billing): add null-check for purchase token"

# Срочно в production
git checkout main
git merge --no-ff hotfix/v1.1.1
git tag -a v1.1.1 -m "Hotfix v1.1.1: fix purchase token NPE"
git push origin main --tags

# Обязательно вернуть в develop
git checkout develop
git merge --no-ff hotfix/v1.1.1
git push origin develop

git branch -d hotfix/v1.1.1
```

---

## 📝 Conventional Commits

Формат:
```
<type>(<scope>): <subject>

<body>

<footer>
```

### Типы для подписочного приложения

| Тип | Когда использовать | Пример |
|-----|-------------------|--------|
| `feat` | Новая функциональность | `feat(billing): add queryPurchasesAsync on app start` |
| `fix` | Исправление бага | `fix(ui): correct premium badge visibility after rotation` |
| `refactor` | Рефакторинг без изменения поведения | `refactor(repository): extract BillingRepository interface` |
| `chore` | Обновление Gradle, зависимостей | `chore(gradle): update Billing Library to 8.3.0` |
| `docs` | Документация | `docs(readme): add subscription setup instructions` |
| `test` | Тесты | `test(billing): add unit tests for purchase validation` |
| `ci` | CI/CD изменения | `ci(github): add release workflow` |

### Скоупы в проекте

- `billing` — работа с BillingClient
- `ui` — Compose экраны
- `repository` — data layer
- `firebase` — Analytics / Firestore
- `gradle` — сборка и зависимости
- `readme` — документация

---

## 🛡 Branch Protection (GitHub)

Настрой в репозитории: **Settings → Branches → Add rule**

### Для `main`:
- ✅ Require a pull request before merging
- ✅ Require approvals (1)
- ✅ Require status checks to pass (CI)
- ✅ Restrict pushes that create files larger than 10MB

### Для `develop`:
- ✅ Require a pull request before merging
- ✅ Require approvals (1)
- ✅ Require status checks to pass (CI)

---

## ❓ Частые вопросы

**Q: Зачем `--no-ff` при merge?**  
A: Создаёт merge-коммит, сохраняя историю ветки как отдельную линию. Без этого история выглядит как линейный хаос, и невозможно понять, где начиналась и заканчивалась фича.

**Q: Можно ли делать rebase в develop?**  
A: Только внутри своей `feature/*` ветки **перед** созданием PR. Никогда не делай rebase веток, которые уже запушены и используются другими.

**Q: В release-ветку прилетел новый баг, но разработчик запушил в develop. Что делать?**  
A: Сделать `git cherry-pick <commit-hash>` из develop в release-ветку. После релиза этот фикс окажется в main через merge.

**Q: Зачем мержить release обратно в develop?**  
A: В release-ветке могли быть багфиксы, которых нет в develop. Если не смержить обратно — следующий релиз потеряет эти исправления.
