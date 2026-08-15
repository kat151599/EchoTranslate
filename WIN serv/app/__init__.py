"""Overlay translation server package."""

# This is deliberately the first application import. PaddleX captures its cache
# environment variable at module import time.
from .bootstrap import PADDLEX_CACHE_HOME as PADDLEX_CACHE_HOME
