# Учёт ТО автобусов парка — Telegram Bot

Spring Boot приложение с Telegram-ботом для учёта технического обслуживания автобусов.

## Запуск

### 1. Токен бота

Задайте переменную окружения `BOT_TOKEN` (или укажите в `application.properties`):

```bash
# Windows (PowerShell)
$env:BOT_TOKEN="your_token_here"

# Linux/Mac
export BOT_TOKEN=your_token_here
```

### 2. Запуск приложения

```bash
mvn spring-boot:run
```

Или из IntelliJ IDEA: запустите `BusFleetApplication`.

## Команды бота

| Команда | Описание |
|---------|----------|
| `/start` | Приветствие и главное меню с кнопками |
| `/addbus` | Добавить автобус (пошаговый ввод) |
| `/removebus` | Удалить автобус по госномеру |
| `/updatemileage` | Обновить пробег (выбор автобуса и режима) |
| `/list` | Список парка с группировкой по моделям |
| `/stats` | Статистика по моделям |
| `/search <номер>` | Поиск автобуса по госномеру |

## Структура проекта

```
src/main/java/com/busfleet/
├── BusFleetApplication.java    # Spring Boot
├── model/
│   ├── Bus.java
│   └── BusModel.java
├── repository/
│   ├── BusRepository.java
│   └── FileBusRepository.java  # сохранение в buspark.dat
├── service/
│   ├── BusService.java
│   ├── MaintenanceCalculator.java
│   └── MaintenanceInfo.java
└── bot/
    ├── BusFleetBot.java
    └── UserSession.java
```

## Данные

Данные сохраняются в файл `buspark.dat` в корне проекта.
