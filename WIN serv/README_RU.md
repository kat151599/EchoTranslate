# Overlay Translation Server — Windows 11

Сервер для Android-клиента `overlay-translator-remote-pc`.

## Архитектура

`Android screenshot → FastAPI → PaddleOCR → история сессии → token budget → OpenAI-compatible LLM → перевод + координаты → Android overlay`

Телефон не выполняет OCR и не вызывает LLM.

## Быстрый запуск на Windows 11

1. Установите **Python 3.12 x64** (3.11 тоже поддерживается установщиком).
2. Распакуйте ZIP в обычную папку, например `C:\OverlayTranslatorServer`.
3. Запустите `install_windows.bat`. Дождитесь именно сообщения `Installation completed successfully`.
4. При желании запустите `VERIFY_INSTALL.bat`.
5. После успешной установки запустите `START_SERVER.bat`.
6. Откроется панель `http://127.0.0.1:8765/admin`.
7. Укажите LLM Base URL, API key, модель и ограничение токенов истории.
8. На телефоне в PC server URL укажите `http://IP_ПК:8765`, например `http://192.168.1.50:8765`.
9. Если `server_api_key` задан, API key телефона должен с ним совпадать. Пустой ключ отключает серверную авторизацию.

Windows Firewall может запросить разрешение Python принимать соединения — разрешите для частной сети.

## Важно про токены

`history_token_limit` ограничивает **только HISTORY**. По умолчанию это 3000 токенов. Сервер берёт самые свежие пары `source + translation` текущего приложения/сессии и добавляет их целиком, пока следующая более старая пара уже не помещается. System prompt, glossary и текущие TARGET_BLOCKS этот бюджет не уменьшают.

Общего лимита токенов входного prompt на сервере нет. `max_output_tokens` задаёт только максимальный размер ответа модели.

Tokenizer задаётся `tokenizer_encoding`, по умолчанию `o200k_base`. Для OpenAI это хороший вариант. Для LM Studio/Ollama с другой моделью реальное количество токенов её tokenizer может отличаться; если нужна строгая граница именно конкретной локальной модели, следует добавить её нативный tokenizer.

## Контекст

SQLite хранит пары:

- `source_text`
- `translation`
- OCR confidence
- box
- session_id
- timestamp

Каждый `session_id` имеет отдельную историю. В `/admin` её можно очистить.

## OCR

Установщик ставит официальный релиз `paddleocr==3.7.0` как обычный Python-пакет внутрь `.venv`. Исходники из GitHub ZIP намеренно не устанавливаются через `pip -e`: архив без `.git` не содержит метаданных, необходимых `setuptools-scm`. При первом реальном OCR-запросе PaddleOCR может загрузить веса моделей, если их ещё нет в кэше компьютера.

Используются поля результата PaddleOCR:

- `rec_texts`
- `rec_scores`
- `rec_polys`

`rec_polys` переводятся в `[left, top, right, bottom]`, совместимый с Android-клиентом.

## API

### GET /health

Возвращает 200, если FastAPI работает.

### POST /v1/screen/translate

`multipart/form-data`:

- `image`: JPEG
- `source_lang`: `auto`, `ja`, `zh-CN`, ...
- `target_lang`: `ru`, ...
- `session_id`: имя контекстной сессии
- Header `Authorization: Bearer <server_api_key>`

Swagger UI доступен на `/docs`: кнопка **Authorize** отправляет этот Bearer-заголовок. Android-контракт и формат ответа не изменены.

В `/admin` есть отдельный тест PaddleOCR: он показывает распознанные блоки, confidence и координаты поверх изображения, а при включённой опции переводит их через настроенный LLM. Тест не пишет данные в историю Android-сессий и экранный кэш.

Ответ:

```json
{
  "blocks": [
    {
      "source": "今度は私があなたを守る",
      "translation": "Теперь я буду защищать тебя.",
      "confidence": 0.99,
      "box": [120, 300, 610, 365],
      "language": "ja"
    }
  ],
  "meta": {
    "history_items_used": 24,
    "history_tokens_used": 2874,
    "history_token_limit": 3000,
    "estimated_prompt_tokens": 5432,
    "response_token_limit": 1200
  }
}
```

## LLM

Первая версия использует OpenAI-compatible `/chat/completions`. Это подходит для OpenAI и для многих локальных серверов с совместимым API (например LM Studio). Если backend не поддерживает `response_format`, сервер автоматически повторяет запрос без этого параметра.
