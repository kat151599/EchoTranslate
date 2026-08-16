# Remote PC Pipeline protocol

The Android app is a thin client. It does not run OCR or translation when **PC server** is selected.

## Request

`POST /v1/screen/translate` using `multipart/form-data`:

- `image`: JPEG binary (`image/jpeg`)
- `source_lang`: BCP-47 language tag or `auto`
- `target_lang`: BCP-47 language tag
- `session_id`: server-side context/memory session
- optional header `Authorization: Bearer <token>`

The server should decode the uploaded bytes directly, e.g. in Python:

```python
raw = await image.read()
arr = np.frombuffer(raw, np.uint8)
frame = cv2.imdecode(arr, cv2.IMREAD_COLOR)
result = paddle_ocr.predict(frame)
```

## Response

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
  ]
}
```

`box` is `[left, top, right, bottom]` in pixels relative to the uploaded image.

## Health check

`GET /health` should return HTTP 200.
