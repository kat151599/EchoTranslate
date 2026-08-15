ПАТЧ RAPIDOCR + DIRECTML ДЛЯ AMD GPU (WINDOWS 11)
==================================================

Что заменяет:
- app/ocr_engine.py
- app/config.py
- app/main.py
- app/templates/admin.html

Что добавляет:
- requirements-directml.txt
- INSTALL_RAPIDOCR_DIRECTML.bat
- VERIFY_DIRECTML.bat

Установка:
1. Остановить сервер (STOP_SERVER.bat).
2. Скопировать содержимое этого патча в корень сервера с заменой файлов.
3. Запустить INSTALL_RAPIDOCR_DIRECTML.bat.
4. Запустить VERIFY_DIRECTML.bat. В Providers должен быть DmlExecutionProvider.
5. Запустить START_SERVER.bat.
6. Открыть http://127.0.0.1:8765/admin
7. OCR backend -> RapidOCR PP-OCRv6 + DirectML (AMD GPU)
8. Сохранить.
9. Проверить картинку в блоке "Тест OCR".

Диагностика:
http://127.0.0.1:8765/api/ocr/status

Примечания:
- Android API /v1/screen/translate не изменён.
- История, token budget и LLM-код не изменены.
- Используется RapidOCR 3.9.2 + PP-OCRv6 SMALL через ONNX Runtime DirectML.
- PP-OCRv6 SMALL поддерживает китайский и японский одной multilingual-моделью.
- Классификатор поворота 0/180 отключён ради скорости: для текста с экрана телефона он обычно не нужен.
- Если включена галочка fallback, при проблеме DirectML сервер автоматически использует старый PaddleOCR CPU.
- В первый запуск RapidOCR может подготовить/скачать модели; первый OCR поэтому может быть заметно дольше последующих.
