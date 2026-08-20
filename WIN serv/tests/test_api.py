from __future__ import annotations

import json
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import AsyncMock, patch

import cv2
import numpy as np
from fastapi.testclient import TestClient

from app import config
from app.main import app
from app.ocr_engine import OcrBlock


def png_bytes() -> bytes:
    ok, encoded = cv2.imencode(".png", np.zeros((40, 80, 3), dtype=np.uint8))
    assert ok
    return encoded.tobytes()


class ApiTests(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_openapi_declares_bearer_auth_for_android_endpoint(self):
        schema = self.client.get("/openapi.json").json()
        bearer = schema["components"]["securitySchemes"]["HTTPBearer"]
        self.assertEqual({"type": "http", "scheme": "bearer"}, {k: bearer[k] for k in ("type", "scheme")})
        self.assertIn({"HTTPBearer": []}, schema["paths"]["/v1/screen/translate"]["post"]["security"])

    def test_bearer_required_only_when_server_key_is_configured(self):
        with patch("app.main.load_config", return_value={"server_api_key": "phone-secret"}):
            response = self.client.post("/v1/screen/translate")
            self.assertEqual(401, response.status_code)
            self.assertEqual("Bearer", response.headers["www-authenticate"])
            response = self.client.post(
                "/v1/screen/translate",
                headers={"Authorization": "Bearer phone-secret"},
            )
            self.assertEqual(422, response.status_code)  # Authentication passed; image is missing.
        with patch("app.main.load_config", return_value={"server_api_key": ""}):
            self.assertEqual(422, self.client.post("/v1/screen/translate").status_code)

    def test_admin_ocr_tool_returns_blocks_without_changing_translate_contract(self):
        block = OcrBlock("Hello", 0.98765, [2, 3, 30, 20], "auto")
        cfg = {
            "ocr_min_confidence": 0.45,
            "llm_api_key": "key",
            "llm_base_url": "https://example.test/v1",
            "llm_model": "model",
            "max_output_tokens": 100,
            "system_prompt": "translate",
            "glossary_enabled": False,
        }
        with (
            patch("app.main.load_config", return_value=cfg),
            patch("app.main.ocr_engine.recognize", return_value=[block]),
            patch("app.main.translate_openai_compatible", new=AsyncMock(return_value={0: "Привет"})),
        ):
            response = self.client.post(
                "/admin/ocr-test",
                files={"image": ("test.png", png_bytes(), "image/png")},
                data={"source_lang": "auto", "target_lang": "ru", "translate": "true"},
            )
        self.assertEqual(200, response.status_code)
        payload = response.json()
        self.assertEqual([2, 3, 30, 20], payload["blocks"][0]["box"])
        self.assertEqual("Привет", payload["blocks"][0]["translation"])
        self.assertEqual({"width": 80, "height": 40}, {k: payload["meta"][k] for k in ("width", "height")})

    def test_admin_empty_key_fields_preserve_keys_and_clear_is_explicit(self):
        cfg = {
            "server_api_key": "saved-server",
            "llm_api_key": "saved-llm",
            "llm_base_url": "https://example.test/v1",
            "llm_model": "model",
            "target_lang": "ru",
            "ocr_min_confidence": 0.45,
            "history_token_limit": 3000,
            "max_output_tokens": 1200,
            "tokenizer_encoding": "o200k_base",
            "system_prompt": "translate",
        }
        base_form = {
            "llm_base_url": cfg["llm_base_url"],
            "llm_model": cfg["llm_model"],
            "target_lang": "uk",
            "ocr_min_confidence": "0.45",
            "history_token_limit": "3000",
            "max_output_tokens": "1200",
            "tokenizer_encoding": "o200k_base",
            "system_prompt": "translate",
            "llm_api_key": "",
            "server_api_key": "",
        }
        with patch("app.main.load_config", return_value=cfg), patch("app.main.save_config") as save:
            self.assertEqual(200, self.client.post("/admin/settings", data=base_form).status_code)
            updates = save.call_args.args[0]
            self.assertNotIn("llm_api_key", updates)
            self.assertNotIn("server_api_key", updates)
            self.client.post("/admin/settings", data={**base_form, "clear_server_api_key": "on"})
            self.assertEqual("", save.call_args.args[0]["server_api_key"])


class ConfigTests(unittest.TestCase):
    def test_saved_key_survives_other_updates_and_can_be_cleared(self):
        initial = {"server_api_key": "saved", "llm_api_key": "llm", "target_lang": "ru"}
        with TemporaryDirectory() as directory:
            path = Path(directory) / "config.json"
            path.write_text(json.dumps(initial), encoding="utf-8")
            with patch.object(config, "CONFIG_PATH", path):
                config.save_config({"target_lang": "uk"})
                self.assertEqual("saved", json.loads(path.read_text(encoding="utf-8"))["server_api_key"])
                config.save_config({"server_api_key": ""})
                self.assertEqual("", json.loads(path.read_text(encoding="utf-8"))["server_api_key"])
                self.assertFalse(path.with_suffix(".json.tmp").exists())


if __name__ == "__main__":
    unittest.main()
