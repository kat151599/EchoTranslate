"""Process-wide initialization which must run before optional OCR imports."""
from __future__ import annotations

import logging
import os
import tempfile
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent.parent
PADDLEX_CACHE_HOME = PROJECT_ROOT / "data" / "paddlex"


def configure_paddlex_cache(cache_home: Path = PADDLEX_CACHE_HOME) -> Path:
    """Select and validate the application-owned PaddleX cache."""
    cache_home = cache_home.resolve()
    os.environ["PADDLE_PDX_CACHE_HOME"] = str(cache_home)
    try:
        cache_home.mkdir(parents=True, exist_ok=True)
        with tempfile.NamedTemporaryFile(
            mode="wb", prefix=".write-test-", dir=cache_home, delete=False
        ) as probe:
            probe_path = Path(probe.name)
            probe.write(b"ok")
        probe_path.unlink()
    except OSError as exc:
        raise RuntimeError(
            "PaddleX cache is not writable: "
            f"{cache_home}. The application requires write access to its project-local "
            "data/paddlex directory."
        ) from exc

    logging.getLogger(__name__).info("PADDLE_PDX_CACHE_HOME=%s", cache_home)
    return cache_home


configure_paddlex_cache()
