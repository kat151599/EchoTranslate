from __future__ import annotations

import json
import subprocess
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent


class PaddleXCacheBootstrapTests(unittest.TestCase):
    def test_cache_is_configured_before_paddleocr_import(self):
        code = r'''
import builtins
import json
import os
from pathlib import Path

root = Path.cwd().resolve()
expected = (root / "data" / "paddlex").resolve()
os.environ.pop("PADDLE_PDX_CACHE_HOME", None)
early_imports = []
real_import = builtins.__import__

def guarded_import(name, *args, **kwargs):
    if (name == "paddleocr" or name.startswith("paddleocr.") or
            name == "paddlex" or name.startswith("paddlex.")):
        if Path(os.environ.get("PADDLE_PDX_CACHE_HOME", "")).resolve() != expected:
            early_imports.append(name)
            raise AssertionError(f"{name} imported before PaddleX cache configuration")
    return real_import(name, *args, **kwargs)

builtins.__import__ = guarded_import
import app
import paddleocr
from paddlex.utils import cache

print(json.dumps({
    "environment": os.environ["PADDLE_PDX_CACHE_HOME"],
    "paddlex_cache": str(Path(cache.CACHE_DIR).resolve()),
    "lock_dir": str((Path(cache.CACHE_DIR) / "locks" / "official_models").resolve()),
    "early_imports": early_imports,
}))
'''
        completed = subprocess.run(
            [sys.executable, "-c", code],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=True,
        )
        result = json.loads(completed.stdout.strip().splitlines()[-1])
        expected = str((ROOT / "data" / "paddlex").resolve())
        self.assertEqual([], result["early_imports"])
        self.assertEqual(expected, result["environment"])
        self.assertEqual(expected, result["paddlex_cache"])
        self.assertEqual(
            str((ROOT / "data" / "paddlex" / "locks" / "official_models").resolve()),
            result["lock_dir"],
        )
        self.assertNotIn(str(Path.home() / ".paddlex"), result["lock_dir"])


if __name__ == "__main__":
    unittest.main()
