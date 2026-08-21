from __future__ import annotations

# ECHOTRANSLATE_PRICING_RESOLVER_V1
# LLM_LAB_FAST_NAV_V1
# QWENCLOUD_MARKETPLACE_PRICING_V2
# QWENCLOUD_NEXT_PAYLOAD_PRICING_V3

import asyncio
import json
import logging
import re
from dataclasses import dataclass, field, asdict
from datetime import datetime, timezone, timedelta
from html import unescape as html_unescape
from html.parser import HTMLParser
from typing import Any
from urllib.parse import quote, urlparse

import httpx

from .db import connect

logger = logging.getLogger(__name__)

PRICING_SCHEMA_VERSION = 1
DEFAULT_CACHE_HOURS = 12
NETWORK_TIMEOUT_SECONDS = 4.0


def _ensure_pricing_cache_schema() -> None:
    with connect() as con:
        con.executescript("""
        CREATE TABLE IF NOT EXISTS llm_pricing_cache (
            provider_id TEXT NOT NULL,
            model TEXT NOT NULL,
            billing_mode TEXT NOT NULL DEFAULT 'payg',
            currency TEXT NOT NULL DEFAULT 'USD',
            input_price_per_m REAL,
            output_price_per_m REAL,
            cache_read_price_per_m REAL,
            cache_write_price_per_m REAL,
            context_length INTEGER,
            max_output_tokens INTEGER,
            tiers_json TEXT NOT NULL DEFAULT '[]',
            source TEXT NOT NULL DEFAULT 'unknown',
            source_url TEXT,
            checked_at TEXT NOT NULL,
            confidence TEXT NOT NULL DEFAULT 'unknown',
            incremental_cost REAL,
            requires_exact_pricing INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(provider_id, model)
        );
        """)
        # Normalize legacy resolver rows and enrich llm_model_metadata.
        con.execute("UPDATE llm_pricing_cache SET billing_mode='payg' WHERE billing_mode='token'")
        metadata_columns = {row[1] for row in con.execute("PRAGMA table_info(llm_model_metadata)")}
        additions = {
            "billing_mode": "TEXT",
            "currency": "TEXT",
            "cache_read_price_per_m": "REAL",
            "cache_write_price_per_m": "REAL",
            "max_output_tokens": "INTEGER",
            "tiers_json": "TEXT",
            "source": "TEXT",
            "source_url": "TEXT",
            "checked_at": "TEXT",
            "confidence": "TEXT",
            "metadata_origin": "TEXT",
        }
        for column, sql_type in additions.items():
            if column not in metadata_columns:
                con.execute(f"ALTER TABLE llm_model_metadata ADD COLUMN {column} {sql_type}")


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def safe_float(value: Any) -> float | None:
    if value is None or isinstance(value, bool):
        return None
    try:
        result = float(str(value).strip().replace(",", ""))
    except (TypeError, ValueError):
        return None
    return result if result >= 0 else None


def safe_int(value: Any) -> int | None:
    if value is None or isinstance(value, bool):
        return None
    try:
        result = int(float(str(value).strip().replace(",", "")))
    except (TypeError, ValueError):
        return None
    return result if result > 0 else None


def _nested(raw: dict, *path: str) -> Any:
    value: Any = raw
    for key in path:
        if not isinstance(value, dict) or key not in value:
            return None
        value = value[key]
    return value


def _per_m_from_per_token(value: Any) -> float | None:
    parsed = safe_float(value)
    return parsed * 1_000_000.0 if parsed is not None else None


def _token_number(text: str) -> int | None:
    text = str(text or "").strip().upper().replace(",", "")
    m = re.search(r"(\d+(?:\.\d+)?)\s*([KMB])?", text)
    if not m:
        return None
    value = float(m.group(1))
    unit = m.group(2) or ""
    mult = {"": 1, "K": 1_000, "M": 1_000_000, "B": 1_000_000_000}[unit]
    return int(value * mult)


@dataclass
class PricingTier:
    input_tokens_from: int
    input_tokens_to: int | None
    input: float
    output: float
    cache_read: float | None = None
    cache_write: float | None = None
    thinking_input: float | None = None
    thinking_output: float | None = None
    thinking_cache_read: float | None = None
    thinking_cache_write: float | None = None

    def to_dict(self) -> dict:
        return asdict(self)


@dataclass
class PricingInfo:
    model: str
    billing_mode: str = "payg"
    currency: str = "USD"
    input_price_per_m: float | None = None
    output_price_per_m: float | None = None
    cache_read_price_per_m: float | None = None
    cache_write_price_per_m: float | None = None
    context_length: int | None = None
    max_output_tokens: int | None = None
    tiers: list[PricingTier] = field(default_factory=list)
    source: str = "unknown"
    source_url: str | None = None
    checked_at: str = field(default_factory=_now_iso)
    confidence: str = "unknown"
    incremental_cost: float | None = None
    requires_exact_pricing: bool = False

    def to_dict(self) -> dict:
        data = asdict(self)
        data["tiers"] = [tier.to_dict() for tier in self.tiers]
        data["id"] = self.model
        data["price_source"] = self.source
        data["context_source"] = self.source if self.context_length is not None else "unknown"
        return data

    @classmethod
    def from_dict(cls, raw: dict) -> "PricingInfo":
        tiers = []
        for item in raw.get("tiers") or []:
            if isinstance(item, dict):
                try:
                    tiers.append(PricingTier(
                        input_tokens_from=int(item.get("input_tokens_from") or 0),
                        input_tokens_to=int(item["input_tokens_to"]) if item.get("input_tokens_to") is not None else None,
                        input=float(item["input"]),
                        output=float(item["output"]),
                        cache_read=safe_float(item.get("cache_read")),
                        cache_write=safe_float(item.get("cache_write")),
                        thinking_input=safe_float(item.get("thinking_input")),
                        thinking_output=safe_float(item.get("thinking_output")),
                        thinking_cache_read=safe_float(item.get("thinking_cache_read")),
                        thinking_cache_write=safe_float(item.get("thinking_cache_write")),
                    ))
                except Exception:
                    continue
        return cls(
            model=str(raw.get("model") or raw.get("id") or ""),
            billing_mode=("payg" if str(raw.get("billing_mode") or "payg") == "token" else str(raw.get("billing_mode") or "payg")),
            currency=str(raw.get("currency") or "USD"),
            input_price_per_m=safe_float(raw.get("input_price_per_m")),
            output_price_per_m=safe_float(raw.get("output_price_per_m")),
            cache_read_price_per_m=safe_float(raw.get("cache_read_price_per_m")),
            cache_write_price_per_m=safe_float(raw.get("cache_write_price_per_m")),
            context_length=safe_int(raw.get("context_length")),
            max_output_tokens=safe_int(raw.get("max_output_tokens")),
            tiers=tiers,
            source=str(raw.get("source") or raw.get("price_source") or "unknown"),
            source_url=str(raw.get("source_url") or "") or None,
            checked_at=str(raw.get("checked_at") or _now_iso()),
            confidence=str(raw.get("confidence") or "unknown"),
            incremental_cost=safe_float(raw.get("incremental_cost")),
            requires_exact_pricing=bool(raw.get("requires_exact_pricing", False)),
        )


@dataclass
class UsageInfo:
    prompt_tokens: int | None = None
    completion_tokens: int | None = None
    total_tokens: int | None = None
    cached_read_tokens: int = 0
    cache_write_tokens: int = 0
    cache_miss_tokens: int | None = None
    source: str = "estimated"

    def to_dict(self) -> dict:
        return asdict(self)


def normalize_usage(payload: dict | None) -> UsageInfo:
    usage = payload if isinstance(payload, dict) else {}
    prompt = safe_int(usage.get("prompt_tokens")) or safe_int(usage.get("input_tokens"))
    completion = safe_int(usage.get("completion_tokens")) or safe_int(usage.get("output_tokens"))
    total = safe_int(usage.get("total_tokens"))
    prompt_details = usage.get("prompt_tokens_details") if isinstance(usage.get("prompt_tokens_details"), dict) else {}
    input_details = usage.get("input_tokens_details") if isinstance(usage.get("input_tokens_details"), dict) else {}
    cached = (
        safe_int(usage.get("prompt_cache_hit_tokens"))
        or safe_int(prompt_details.get("cached_tokens"))
        or safe_int(input_details.get("cached_tokens"))
        or safe_int(usage.get("cache_read_input_tokens"))
        or 0
    )
    cache_write = (
        safe_int(usage.get("cache_creation_input_tokens"))
        or safe_int(prompt_details.get("cache_creation_input_tokens"))
        or safe_int(input_details.get("cache_creation_input_tokens"))
        or 0
    )
    miss = safe_int(usage.get("prompt_cache_miss_tokens"))
    if miss is None and prompt is not None:
        miss = max(0, prompt - cached - cache_write)
    source = "provider" if usage else "estimated"
    return UsageInfo(prompt, completion, total, cached, cache_write, miss, source)


def _selected_tier(info: PricingInfo | dict, input_tokens: int) -> PricingTier | None:
    obj = info if isinstance(info, PricingInfo) else PricingInfo.from_dict(info)
    if not obj.tiers:
        return None
    for tier in sorted(obj.tiers, key=lambda t: t.input_tokens_from):
        if input_tokens < tier.input_tokens_from:
            continue
        if tier.input_tokens_to is None or input_tokens <= tier.input_tokens_to:
            return tier
    return None


def _select_tier(info: PricingInfo | dict, input_tokens: int) -> tuple[float | None, float | None]:
    obj = info if isinstance(info, PricingInfo) else PricingInfo.from_dict(info)
    tier = _selected_tier(obj, input_tokens)
    if obj.tiers:
        return (tier.input, tier.output) if tier is not None else (None, None)
    return obj.input_price_per_m, obj.output_price_per_m


def worst_case_request_cost(
    meta: PricingInfo | dict,
    input_tokens: int,
    max_output_tokens: int,
    input_safety: float = 1.25,
) -> float | None:
    info = meta if isinstance(meta, PricingInfo) else PricingInfo.from_dict(meta)
    if info.billing_mode != "payg":
        return 0.0
    safe_input = int(max(1, input_tokens) * max(1.0, float(input_safety))) + 16
    input_price, output_price = _select_tier(info, safe_input)
    if input_price is None or output_price is None:
        return None
    return safe_input / 1_000_000.0 * input_price + max(0, max_output_tokens) / 1_000_000.0 * output_price


def actual_cost_from_usage(
    meta: PricingInfo | dict,
    usage: UsageInfo | dict | None,
    fallback_input_tokens: int = 0,
    fallback_output_tokens: int = 0,
) -> tuple[float, str]:
    info = meta if isinstance(meta, PricingInfo) else PricingInfo.from_dict(meta)
    if info.billing_mode != "payg":
        return 0.0, info.billing_mode
    u = usage if isinstance(usage, UsageInfo) else UsageInfo(**usage) if isinstance(usage, dict) else UsageInfo()
    prompt = u.prompt_tokens if u.prompt_tokens is not None else max(0, fallback_input_tokens)
    output = u.completion_tokens if u.completion_tokens is not None else max(0, fallback_output_tokens)
    selected_tier = _selected_tier(info, prompt)
    input_price, output_price = _select_tier(info, prompt)
    if input_price is None or output_price is None:
        return 0.0, "unknown"

    cached = max(0, int(u.cached_read_tokens or 0))
    cache_write = max(0, int(u.cache_write_tokens or 0))
    miss = u.cache_miss_tokens
    if miss is None:
        miss = max(0, prompt - cached - cache_write)

    cost = max(0, miss) / 1_000_000.0 * input_price
    if cached:
        cached_price = (selected_tier.cache_read if selected_tier and selected_tier.cache_read is not None else info.cache_read_price_per_m)
        cached_price = cached_price if cached_price is not None else input_price
        cost += cached / 1_000_000.0 * cached_price
    if cache_write:
        write_price = (selected_tier.cache_write if selected_tier and selected_tier.cache_write is not None else info.cache_write_price_per_m)
        write_price = write_price if write_price is not None else input_price
        cost += cache_write / 1_000_000.0 * write_price
    cost += max(0, output) / 1_000_000.0 * output_price
    return cost, "provider_usage" if u.source == "provider" else "local_estimate"


class _TableParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.rows: list[list[str]] = []
        self._row: list[str] | None = None
        self._cell: list[str] | None = None

    def handle_starttag(self, tag: str, attrs):
        tag = tag.lower()
        if tag == "tr":
            self._row = []
        elif tag in {"td", "th"} and self._row is not None:
            self._cell = []

    def handle_data(self, data: str):
        if self._cell is not None:
            self._cell.append(data)

    def handle_endtag(self, tag: str):
        tag = tag.lower()
        if tag in {"td", "th"} and self._cell is not None and self._row is not None:
            text = re.sub(r"\s+", " ", " ".join(self._cell)).strip()
            self._row.append(text)
            self._cell = None
        elif tag == "tr" and self._row is not None:
            if any(cell for cell in self._row):
                self.rows.append(self._row)
            self._row = None


def _table_rows(html: str) -> list[list[str]]:
    parser = _TableParser()
    try:
        parser.feed(html)
    except Exception:
        return []
    return parser.rows


def _dollars(text: str) -> list[float]:
    return [float(v.replace(",", "")) for v in re.findall(r"\$\s*(\d+(?:\.\d+)?)", str(text or ""))]


def _parse_range(text: str, previous_to: int | None = None) -> tuple[int, int | None]:
    value = str(text or "").upper().replace(",", "").replace("TOKENS", "").strip()
    nums = [_token_number(part) for part in re.findall(r"\d+(?:\.\d+)?\s*[KMB]?", value)]
    nums = [n for n in nums if n is not None]
    if "≤" in value or "<=" in value:
        return 0, nums[0] if nums else None
    if len(nums) >= 2:
        return nums[0], nums[1]
    if len(nums) == 1:
        return (previous_to + 1 if previous_to is not None else 0), nums[0]
    return (previous_to + 1 if previous_to is not None else 0), None


def _normalize_billing_mode(value: str | None) -> str:
    mode = str(value or "payg").strip().lower()
    if mode == "token":
        return "payg"
    return mode if mode in {"payg", "credits", "subscription"} else "payg"


def _qwencloud_billing_mode(base_url: str) -> str:
    host = (urlparse(str(base_url or "")).hostname or "").lower()
    if "token-plan" in host:
        return "credits"
    if "coding-intl.dashscope.aliyuncs.com" in host:
        return "subscription"
    # QwenCloud documents the general dashscope-intl compatible endpoint as PAYG.
    if "dashscope-intl.aliyuncs.com" in host or host == "dashscope.aliyuncs.com":
        return "payg"
    return "payg"



# QWENCLOUD_OFFICIAL_DISCOVERY_V4
# Conservative PAYG fallback from QwenCloud's official pricing documentation.
# It exists because the OpenAI-compatible endpoint is not required to expose a
# usable GET /models catalogue. Live provider/Marketplace data still takes
# priority whenever it is available.
_QWENCLOUD_OFFICIAL_DISCOVERY_IDS = (
    "qwen3.7-flash",
    "qwen3.7-plus",
    "qwen3.7-max",
)


def qwencloud_discovery_fallback_models(base_url: str) -> list[dict]:
    if _provider_kind(base_url) != "qwencloud":
        return []
    if _qwencloud_billing_mode(base_url) != "payg":
        return []
    return [
        {"id": model, "discovery_source": "qwencloud_official_pricing_catalog"}
        for model in _QWENCLOUD_OFFICIAL_DISCOVERY_IDS
    ]


def _qwencloud_official_snapshot(model: str) -> PricingInfo | None:
    """Official list-price safety fallback, checked 2026-08-20.

    Live Marketplace/provider metadata wins. These list prices are deliberately
    used instead of temporary promotional discounts, making the money guard
    conservative if QwenCloud's public HTML cannot be fetched by the server.
    """
    source_url = "https://docs.qwencloud.com/developer-guides/getting-started/pricing"
    if model == "qwen3.7-flash":
        tiers = [
            PricingTier(0, 32_000, 0.03, 0.13),
            PricingTier(32_001, 256_000, 0.10, 0.40),
            PricingTier(256_001, 1_000_000, 0.20, 0.80),
        ]
        return PricingInfo(
            model=model,
            billing_mode="payg",
            currency="USD",
            input_price_per_m=0.03,
            output_price_per_m=0.13,
            context_length=1_000_000,
            max_output_tokens=65_536,
            tiers=tiers,
            source="official_pricing_snapshot",
            source_url=source_url,
            confidence="official_snapshot",
            requires_exact_pricing=False,
        )
    if model == "qwen3.7-plus":
        tiers = [
            PricingTier(0, 256_000, 0.40, 1.60),
            PricingTier(256_001, 1_000_000, 1.20, 4.80),
        ]
        return PricingInfo(
            model=model,
            billing_mode="payg",
            currency="USD",
            input_price_per_m=0.40,
            output_price_per_m=1.60,
            context_length=1_000_000,
            max_output_tokens=65_536,
            tiers=tiers,
            source="official_pricing_snapshot",
            source_url=source_url,
            confidence="official_snapshot",
            requires_exact_pricing=False,
        )
    if model == "qwen3.7-max":
        return PricingInfo(
            model=model,
            billing_mode="payg",
            currency="USD",
            input_price_per_m=2.50,
            output_price_per_m=7.50,
            context_length=1_000_000,
            max_output_tokens=131_072,
            tiers=[PricingTier(0, 991_000, 2.50, 7.50)],
            source="official_pricing_snapshot",
            source_url=source_url,
            confidence="official_snapshot",
            requires_exact_pricing=False,
        )
    return None

class _VisibleTextParser(HTMLParser):
    _BREAK_TAGS = {"br", "p", "div", "li", "section", "article", "tr", "h1", "h2", "h3", "h4", "h5", "h6"}

    def __init__(self):
        super().__init__()
        self.parts: list[str] = []
        self.skip_depth = 0

    def handle_starttag(self, tag: str, attrs):
        tag = tag.lower()
        if tag in {"script", "style", "noscript"}:
            self.skip_depth += 1
            return
        if not self.skip_depth and tag in self._BREAK_TAGS:
            self.parts.append(chr(10))

    def handle_endtag(self, tag: str):
        tag = tag.lower()
        if tag in {"script", "style", "noscript"}:
            self.skip_depth = max(0, self.skip_depth - 1)
            return
        if not self.skip_depth and tag in self._BREAK_TAGS:
            self.parts.append(chr(10))

    def handle_data(self, data: str):
        if not self.skip_depth:
            value = re.sub(r"\s+", " ", data).strip()
            if value:
                self.parts.append(value)
                self.parts.append(chr(10))


def _visible_lines(html: str) -> list[str]:
    parser = _VisibleTextParser()
    try:
        parser.feed(html)
    except Exception:
        return []
    text = "".join(parser.parts)
    return [re.sub(r"\s+", " ", line).strip() for line in text.splitlines() if line.strip()]


def _decode_js_string_fragment(value: str) -> str:
    """Best-effort decode for strings embedded in Next/React flight payloads."""
    raw = str(value or "")
    try:
        return json.loads('"' + raw + '"')
    except Exception:
        pass
    replacements = {
        r"\n": "\n",
        r"\r": "\n",
        r"\t": " ",
        r"\/": "/",
        r"\"": '"',
        r"\'": "'",
        r"\u003c": "<",
        r"\u003e": ">",
        r"\u0026": "&",
        r"\u0024": "$",
    }
    for old, new in replacements.items():
        raw = raw.replace(old, new)
    def repl(match: re.Match) -> str:
        try:
            return chr(int(match.group(1), 16))
        except Exception:
            return match.group(0)
    return re.sub(r"\\u([0-9a-fA-F]{4})", repl, raw)


def _qwen_embedded_text(html: str) -> str:
    """Extract text-like values from Next/React script payloads without JS execution."""
    chunks: list[str] = []
    for script in re.findall(r"(?is)<script\b[^>]*>(.*?)</script>", str(html or "")):
        script = html_unescape(script)
        # Flight payloads are usually strings passed to self.__next_f.push(...).
        for quoted in re.findall(r'"((?:\\.|[^"\\])*)"', script):
            decoded = _decode_js_string_fragment(quoted)
            if decoded:
                chunks.append(decoded)
        # Keep a decoded copy too: some builds serialize useful labels outside
        # a standalone JSON string.
        chunks.append(_decode_js_string_fragment(script))
    value = "\n".join(chunks)
    value = html_unescape(value)
    value = re.sub(r"(?is)<[^>]+>", "\n", value)
    # Turn common JSON/React punctuation into separators but preserve pricing
    # symbols, parentheses, inequality signs and model IDs.
    value = re.sub(r'[\{\}\[\]",]+', "\n", value)
    value = value.replace("\\n", "\n").replace("\\r", "\n").replace("\\t", " ")
    return re.sub(r"[ \f\v]+", " ", value)


_QWEN_PRICE_TOKEN_RE = re.compile(
    r"(Explicit\s+Cache\s+Creation(?:\s*\(Thinking\))?"
    r"|Explicit\s+Cache\s+Read(?:\s*\(Thinking\))?"
    r"|Input\s*\([^)]*\)"
    r"|Output\s*\([^)]*\)"
    r"|(?<![A-Za-z])Input(?![A-Za-z])"
    r"|(?<![A-Za-z])Output(?![A-Za-z])"
    r"|\$\s*\d+(?:\.\d+)?)",
    re.I,
)


def _qwen_pricing_fields_flat(text: str) -> dict[str, float | None]:
    """Parse ordered label/$price pairs from flattened rendered or Flight text."""
    fields: dict[str, float | None] = {
        "input": None,
        "output": None,
        "cache_read": None,
        "cache_write": None,
        "thinking_input": None,
        "thinking_output": None,
        "thinking_cache_read": None,
        "thinking_cache_write": None,
    }
    tokens = [m.group(0) for m in _QWEN_PRICE_TOKEN_RE.finditer(str(text or ""))]
    for i, token in enumerate(tokens):
        if token.lstrip().startswith("$"):
            continue
        price = None
        for candidate in tokens[i + 1:i + 4]:
            if candidate.lstrip().startswith("$"):
                price = safe_float(candidate.replace("$", "").strip())
                break
            # Another label before a price means this label had no own price.
            if not candidate.lstrip().startswith("$"):
                break
        if price is None:
            continue
        label = re.sub(r"\s+", "", token).lower()
        if label == "input" and fields["input"] is None:
            fields["input"] = price
        elif label == "output" and fields["output"] is None:
            fields["output"] = price
        elif label in {"input(implicitcache)", "explicitcacheread"} and fields["cache_read"] is None:
            fields["cache_read"] = price
        elif label == "explicitcachecreation" and fields["cache_write"] is None:
            fields["cache_write"] = price
        elif label == "input(thinking)" and fields["thinking_input"] is None:
            fields["thinking_input"] = price
        elif label == "output(thinking)" and fields["thinking_output"] is None:
            fields["thinking_output"] = price
        elif label in {"input(thinkingimplicitcache)", "explicitcacheread(thinking)"} and fields["thinking_cache_read"] is None:
            fields["thinking_cache_read"] = price
        elif label == "explicitcachecreation(thinking)" and fields["thinking_cache_write"] is None:
            fields["thinking_cache_write"] = price
    return fields


def _qwen_context_fields_flat(text: str) -> tuple[int | None, int | None]:
    value = str(text or "")
    context = None
    max_output = None
    context_match = re.search(
        r"(?is)(?:^|[^A-Za-z])Context(?:\s*[:=\-])?\s*(\d+(?:\.\d+)?\s*[KMB])",
        value,
    )
    if context_match:
        context = _token_number(context_match.group(1))
    output_match = re.search(
        r"(?is)Max\s*Output(?!\s*\(Thinking\))(?:\s*[:=\-])?\s*(\d+(?:\.\d+)?\s*[KMB])",
        value,
    )
    if output_match:
        max_output = _token_number(output_match.group(1))
    return context, max_output


def _parse_qwen_flat_marketplace_text(model: str, text: str) -> PricingInfo | None:
    value = str(text or "")
    if "pricing" not in value.lower():
        return None

    # Ignore unrelated script payload before the model's pricing section where
    # possible; keep context/rate-limit tail for context extraction.
    pricing_pos = value.lower().find("pricing")
    pricing_tail = value[pricing_pos:]
    context, max_output = _qwen_context_fields_flat(pricing_tail)

    matches = list(_QWEN_RANGE_RE.finditer(pricing_tail))
    declared: list[tuple[int, int | None]] = []
    for match in matches:
        rng = _qwen_range(match.group(0))
        if rng is not None and rng not in declared:
            declared.append(rng)

    panels: list[tuple[tuple[int, int | None], dict[str, float | None]]] = []
    for index, match in enumerate(matches):
        rng = _qwen_range(match.group(0))
        if rng is None:
            continue
        # A navigation/header occurrence is followed almost immediately by
        # another range and has no Input/Output prices; it will be discarded.
        end = matches[index + 1].start() if index + 1 < len(matches) else len(pricing_tail)
        segment = pricing_tail[match.end():end]
        fields = _qwen_pricing_fields_flat(segment)
        if fields["input"] is not None and fields["output"] is not None:
            if not any(existing[0] == rng for existing in panels):
                panels.append((rng, fields))

    if panels:
        tiers = [
            PricingTier(
                input_tokens_from=rng[0],
                input_tokens_to=rng[1],
                input=float(fields["input"]),
                output=float(fields["output"]),
                cache_read=safe_float(fields["cache_read"]),
                cache_write=safe_float(fields["cache_write"]),
                thinking_input=safe_float(fields["thinking_input"]),
                thinking_output=safe_float(fields["thinking_output"]),
                thinking_cache_read=safe_float(fields["thinking_cache_read"]),
                thinking_cache_write=safe_float(fields["thinking_cache_write"]),
            )
            for rng, fields in sorted(panels, key=lambda item: item[0][0])
        ]
        parsed_ranges = {item[0] for item in panels}
        complete = not declared or all(rng in parsed_ranges for rng in declared)
        first = tiers[0]
        return PricingInfo(
            model=model,
            billing_mode="payg",
            input_price_per_m=first.input,
            output_price_per_m=first.output,
            cache_read_price_per_m=first.cache_read,
            cache_write_price_per_m=first.cache_write,
            context_length=context,
            max_output_tokens=max_output,
            tiers=tiers,
            source="qwen_marketplace",
            confidence="official",
            requires_exact_pricing=not complete,
        )

    # Single-tier pages have no range selector.
    # Stop before API examples if possible so request code doesn't confuse the
    # label/price parser.
    end_match = re.search(r"(?is)Rate\s+Limits|API\s+Reference", pricing_tail)
    single = pricing_tail[:end_match.start()] if end_match else pricing_tail
    fields = _qwen_pricing_fields_flat(single)
    if fields["input"] is None or fields["output"] is None:
        return None
    return PricingInfo(
        model=model,
        billing_mode="payg",
        input_price_per_m=float(fields["input"]),
        output_price_per_m=float(fields["output"]),
        cache_read_price_per_m=safe_float(fields["cache_read"]),
        cache_write_price_per_m=safe_float(fields["cache_write"]),
        context_length=context,
        max_output_tokens=max_output,
        source="qwen_marketplace",
        confidence="official",
    )


_QWEN_RANGE_RE = re.compile(
    r"(?:Input\s*(?:<=|≤)\s*\d+(?:\.\d+)?\s*[KMB]?|"
    r"\d+(?:\.\d+)?\s*[KMB]?\s*<\s*Input\s*(?:<=|≤)\s*\d+(?:\.\d+)?\s*[KMB]?)",
    re.I,
)


def _qwen_range(text: str) -> tuple[int, int | None] | None:
    value = re.sub(r"\s+", "", str(text or "")).upper()
    m = re.fullmatch(r"INPUT(?:<=|≤)(\d+(?:\.\d+)?[KMB]?)", value)
    if m:
        return 0, _token_number(m.group(1))
    m = re.fullmatch(r"(\d+(?:\.\d+)?[KMB]?)<INPUT(?:<=|≤)(\d+(?:\.\d+)?[KMB]?)", value)
    if m:
        low = _token_number(m.group(1))
        high = _token_number(m.group(2))
        if low is not None and high is not None:
            return low + 1, high
    return None


def _next_price(lines: list[str], start: int) -> float | None:
    """Read a Qwen Marketplace price directly following a known pricing label.

    Marketplace server-rendered text can split ``$ 0.03 Per 1M tokens``
    across three DOM text nodes.  A bare number is accepted only when its
    short label-local window contains the unit marker; this prevents context
    sizes, ranges, and rate limits from becoming prices.
    """
    window = [str(item or "").strip() for item in lines[start:start + 6]]
    for offset, item in enumerate(window):
        values = _dollars(item)
        if values:
            return values[0]

        # A separate currency node is common in QwenCloud's rendered page.
        if item in {"$", "USD", "US$"} and offset + 1 < len(window):
            number = re.fullmatch(r"\d+(?:\.\d+)?", window[offset + 1].replace(",", ""))
            if number:
                return float(number.group(0))

        # Some rendered variants omit the currency glyph but retain the unit.
        # Restrict this to a numeric node immediately associated with a
        # ``Per 1M tokens`` marker in the same label-local window.
        number = re.fullmatch(r"\d+(?:\.\d+)?", item.replace(",", ""))
        if number and any(re.fullmatch(r"per\s*1m\s*tokens?", value, re.I) for value in window[offset + 1:offset + 3]):
            return float(number.group(0))
    return None


def _qwen_pricing_fields(lines: list[str]) -> dict[str, float | None]:
    fields: dict[str, float | None] = {
        "input": None,
        "output": None,
        "cache_read": None,
        "cache_write": None,
        "thinking_input": None,
        "thinking_output": None,
        "thinking_cache_read": None,
        "thinking_cache_write": None,
    }
    for i, line in enumerate(lines):
        label = re.sub(r"\s+", "", line).lower()
        price = _next_price(lines, i + 1)
        if price is None:
            continue
        if label == "input":
            fields["input"] = price
        elif label == "output":
            fields["output"] = price
        elif label == "input(implicitcache)":
            fields["cache_read"] = price
        elif label == "explicitcachecreation":
            fields["cache_write"] = price
        elif label == "explicitcacheread" and fields["cache_read"] is None:
            fields["cache_read"] = price
        elif label == "input(thinking)":
            fields["thinking_input"] = price
        elif label == "output(thinking)":
            fields["thinking_output"] = price
        elif label == "input(thinkingimplicitcache)":
            fields["thinking_cache_read"] = price
        elif label == "explicitcachecreation(thinking)":
            fields["thinking_cache_write"] = price
    return fields


def _qwen_context_fields(lines: list[str]) -> tuple[int | None, int | None]:
    context = None
    max_output = None
    for i, line in enumerate(lines):
        label = re.sub(r"\s+", "", line).lower()
        if label == "context" and i + 1 < len(lines):
            context = _token_number(lines[i + 1]) or context
        elif label == "maxoutput" and i + 1 < len(lines):
            max_output = _token_number(lines[i + 1]) or max_output
    return context, max_output


def _parse_qwen_marketplace_page(model: str, html: str) -> PricingInfo | None:
    # First try ordinary server-rendered text.
    lines = _visible_lines(html)
    if lines:
        pricing_start = -1
        for candidate in (i for i, line in enumerate(lines) if line.strip().lower() == "pricing"):
            candidate_end = next(
                (i for i in range(candidate + 1, len(lines)) if "rate limits" in lines[i].lower()),
                len(lines),
            )
            candidate_lines = lines[candidate + 1:candidate_end]
            # The first occurrences are Marketplace navigation links.  Select
            # the pricing section only when it contains both price labels and
            # the explicit per-million unit used by QwenCloud.
            labels = {re.sub(r"\s+", "", value).lower() for value in candidate_lines}
            if "input" in labels and "output" in labels and any(
                re.fullmatch(r"per\s*1m\s*tokens?", value.strip(), re.I)
                for value in candidate_lines
            ):
                pricing_start = candidate
                break
        if pricing_start >= 0:
            rate_start = next(
                (i for i in range(pricing_start + 1, len(lines)) if "rate limits" in lines[i].lower()),
                len(lines),
            )
            pricing_lines = lines[pricing_start + 1:rate_start]
            context_lines = lines[rate_start:]
            context, max_output = _qwen_context_fields(context_lines)

            declared: list[tuple[int, int | None]] = []
            range_occurrences: list[tuple[int, tuple[int, int | None]]] = []
            for index, line in enumerate(pricing_lines):
                for match in _QWEN_RANGE_RE.findall(line):
                    parsed = _qwen_range(match)
                    if parsed is not None and parsed not in declared:
                        declared.append(parsed)
                    if parsed is not None:
                        range_occurrences.append((index, parsed))

            # QwenCloud sometimes renders a row of tier-selector buttons
            # before the actual pricing panel.  They are consecutive range
            # labels with no price fields; never attach the first visible
            # price to one of those selector labels.
            first_price_label = next(
                (i for i, line in enumerate(pricing_lines)
                 if re.sub(r"\s+", "", line).lower() in {"input", "output"}),
                len(pricing_lines),
            )
            selector_indexes = {
                index for index, _parsed in range_occurrences if index < first_price_label
            }
            if len(selector_indexes) < 2:
                selector_indexes.clear()

            panels: list[tuple[tuple[int, int | None], dict[str, float | None]]] = []
            for i, line in enumerate(pricing_lines):
                if i in selector_indexes:
                    continue
                matches = _QWEN_RANGE_RE.findall(line)
                if len(matches) != 1:
                    continue
                parsed_range = _qwen_range(matches[0])
                if parsed_range is None:
                    continue
                end = len(pricing_lines)
                for j in range(i + 1, len(pricing_lines)):
                    next_matches = _QWEN_RANGE_RE.findall(pricing_lines[j])
                    if len(next_matches) == 1:
                        end = j
                        break
                fields = _qwen_pricing_fields(pricing_lines[i + 1:end])
                if fields["input"] is not None and fields["output"] is not None:
                    if not any(existing[0] == parsed_range for existing in panels):
                        panels.append((parsed_range, fields))

            if panels:
                tiers = [
                    PricingTier(
                        input_tokens_from=rng[0],
                        input_tokens_to=rng[1],
                        input=float(fields["input"]),
                        output=float(fields["output"]),
                        cache_read=safe_float(fields["cache_read"]),
                        cache_write=safe_float(fields["cache_write"]),
                        thinking_input=safe_float(fields["thinking_input"]),
                        thinking_output=safe_float(fields["thinking_output"]),
                        thinking_cache_read=safe_float(fields["thinking_cache_read"]),
                        thinking_cache_write=safe_float(fields["thinking_cache_write"]),
                    )
                    for rng, fields in sorted(panels, key=lambda item: item[0][0])
                ]
                first = tiers[0]
                parsed_ranges = {item[0] for item in panels}
                complete = not declared or all(rng in parsed_ranges for rng in declared)
                return PricingInfo(
                    model=model,
                    billing_mode="payg",
                    input_price_per_m=first.input,
                    output_price_per_m=first.output,
                    cache_read_price_per_m=first.cache_read,
                    cache_write_price_per_m=first.cache_write,
                    context_length=context,
                    max_output_tokens=max_output,
                    tiers=tiers,
                    source="qwen_marketplace",
                    confidence="official",
                    requires_exact_pricing=not complete,
                )

            fields = _qwen_pricing_fields(pricing_lines)
            if fields["input"] is not None and fields["output"] is not None:
                return PricingInfo(
                    model=model,
                    billing_mode="payg",
                    input_price_per_m=float(fields["input"]),
                    output_price_per_m=float(fields["output"]),
                    cache_read_price_per_m=safe_float(fields["cache_read"]),
                    cache_write_price_per_m=safe_float(fields["cache_write"]),
                    context_length=context,
                    max_output_tokens=max_output,
                    source="qwen_marketplace",
                    confidence="official",
                )

    # QwenCloud's Marketplace may ship pricing only in a Next/React Flight
    # payload. Parse that embedded payload directly; no browser/JS/LLM needed.
    embedded = _qwen_embedded_text(html)
    return _parse_qwen_flat_marketplace_text(model, embedded)




def _generic_raw_info(model: str, raw: dict) -> PricingInfo | None:
    context = (
        safe_int(raw.get("context_length"))
        or safe_int(raw.get("context_window"))
        or safe_int(raw.get("max_context_length"))
        or safe_int(raw.get("max_context_tokens"))
        or safe_int(_nested(raw, "model_spec", "availableContextTokens"))
        or safe_int(_nested(raw, "top_provider", "context_length"))
    )
    max_output = (
        safe_int(raw.get("max_completion_tokens"))
        or safe_int(raw.get("max_output_tokens"))
        or safe_int(_nested(raw, "model_spec", "maxOutputTokens"))
    )

    venice_in = safe_float(_nested(raw, "model_spec", "pricing", "input", "usd"))
    venice_out = safe_float(_nested(raw, "model_spec", "pricing", "output", "usd"))
    if venice_in is not None and venice_out is not None:
        return PricingInfo(
            model=model, input_price_per_m=venice_in, output_price_per_m=venice_out,
            context_length=context, max_output_tokens=max_output,
            source="provider_api", confidence="official",
        )

    pricing = raw.get("pricing")
    if isinstance(pricing, dict):
        prompt = pricing.get("prompt", pricing.get("input"))
        completion = pricing.get("completion", pricing.get("output"))
        if prompt is not None and completion is not None:
            input_price = _per_m_from_per_token(prompt)
            output_price = _per_m_from_per_token(completion)
            if input_price is not None and output_price is not None:
                return PricingInfo(
                    model=model, input_price_per_m=input_price, output_price_per_m=output_price,
                    context_length=context, max_output_tokens=max_output,
                    source="provider_api", confidence="official",
                )

    direct_in = (
        safe_float(raw.get("input_price_per_million"))
        or safe_float(raw.get("input_price_per_1m"))
        or safe_float(raw.get("input_cost_per_million"))
    )
    direct_out = (
        safe_float(raw.get("output_price_per_million"))
        or safe_float(raw.get("output_price_per_1m"))
        or safe_float(raw.get("output_cost_per_million"))
    )
    if direct_in is not None and direct_out is not None:
        return PricingInfo(
            model=model, input_price_per_m=direct_in, output_price_per_m=direct_out,
            context_length=context, max_output_tokens=max_output,
            source="provider_api", confidence="official",
        )
    if context is not None or max_output is not None:
        return PricingInfo(model=model, context_length=context, max_output_tokens=max_output)
    return None


def _provider_kind(base_url: str) -> str:
    host = (urlparse(str(base_url or "")).hostname or "").lower()
    if "venice.ai" in host:
        return "venice"
    if "featherless.ai" in host:
        return "featherless"
    if "haimaker.ai" in host:
        return "haimaker"
    if "deepseek.com" in host:
        return "deepseek"
    if "qwencloud.com" in host or "dashscope" in host or "aliyuncs.com" in host:
        return "qwencloud"
    if "arliai.com" in host:
        return "arliai"
    return "generic"


def _merge_info(primary: PricingInfo | None, fallback: PricingInfo | None) -> PricingInfo | None:
    if primary is None:
        return fallback
    if fallback is None:
        return primary
    if _normalize_billing_mode(primary.billing_mode) == "payg" and _normalize_billing_mode(fallback.billing_mode) != "payg":
        primary.billing_mode = fallback.billing_mode
    if primary.input_price_per_m is None:
        primary.input_price_per_m = fallback.input_price_per_m
    if primary.output_price_per_m is None:
        primary.output_price_per_m = fallback.output_price_per_m
    if primary.cache_read_price_per_m is None:
        primary.cache_read_price_per_m = fallback.cache_read_price_per_m
    if primary.cache_write_price_per_m is None:
        primary.cache_write_price_per_m = fallback.cache_write_price_per_m
    if primary.context_length is None:
        primary.context_length = fallback.context_length
    if primary.max_output_tokens is None:
        primary.max_output_tokens = fallback.max_output_tokens
    if not primary.tiers and fallback.tiers:
        primary.tiers = fallback.tiers
    if primary.source == "unknown" and fallback.source != "unknown":
        primary.source = fallback.source
        primary.source_url = fallback.source_url
        primary.confidence = fallback.confidence
    return primary


class PricingResolver:
    def __init__(self, cfg: dict, provider_id: str | None = None):
        self.cfg = dict(cfg)
        self.base_url = str(cfg.get("llm_base_url") or "").strip().rstrip("/")
        self.api_key = str(cfg.get("llm_api_key") or "").strip()
        self.kind = _provider_kind(self.base_url)
        self.provider_id = provider_id or f"base_url:{self.base_url}"
        self.cache_hours = max(1, int(cfg.get("pricing_cache_hours", DEFAULT_CACHE_HOURS)))
        self._page_cache: dict[str, str] = {}
        self._ensure_schema()

    def _ensure_schema(self) -> None:
        _ensure_pricing_cache_schema()

    def _manual_override(self, model: str) -> dict | None:
        try:
            with connect() as con:
                row = con.execute(
                    "SELECT model, input_price_per_m, output_price_per_m, context_length, updated_at, metadata_origin "
                    "FROM llm_model_metadata WHERE provider_id=? AND model=?",
                    (self.provider_id, model),
                ).fetchone()
            if row is None:
                return None
            item = dict(row)
            origin = str(item.get("metadata_origin") or "manual").lower()
            return item if origin == "manual" else None
        except Exception:
            return None

    def _cached(self, model: str) -> PricingInfo | None:
        with connect() as con:
            row = con.execute(
                "SELECT * FROM llm_pricing_cache WHERE provider_id=? AND model=?",
                (self.provider_id, model),
            ).fetchone()
        if row is None:
            return None
        raw = dict(row)
        # CHATGPT_PRICING_AGENT_NONEXPIRING_CACHE_V1
        # User-requested Pricing Agent snapshots remain authoritative until the
        # user explicitly refreshes them again. Legacy resolver rows keep TTL.
        if str(raw.get("source") or "") != "chatgpt_pricing_agent":
            try:
                checked = datetime.fromisoformat(str(raw["checked_at"]).replace("Z", "+00:00"))
                if datetime.now(timezone.utc) - checked > timedelta(hours=self.cache_hours):
                    return None
            except Exception:
                return None
        raw["tiers"] = json.loads(raw.pop("tiers_json") or "[]")
        raw["billing_mode"] = _normalize_billing_mode(raw.get("billing_mode"))
        raw["requires_exact_pricing"] = bool(raw.get("requires_exact_pricing"))
        return PricingInfo.from_dict(raw)

    def _save(self, info: PricingInfo) -> PricingInfo:
        with connect() as con:
            con.execute(
                "INSERT INTO llm_pricing_cache("
                "provider_id,model,billing_mode,currency,input_price_per_m,output_price_per_m,"
                "cache_read_price_per_m,cache_write_price_per_m,context_length,max_output_tokens,"
                "tiers_json,source,source_url,checked_at,confidence,incremental_cost,requires_exact_pricing"
                ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                "ON CONFLICT(provider_id,model) DO UPDATE SET "
                "billing_mode=excluded.billing_mode,currency=excluded.currency,"
                "input_price_per_m=excluded.input_price_per_m,output_price_per_m=excluded.output_price_per_m,"
                "cache_read_price_per_m=excluded.cache_read_price_per_m,cache_write_price_per_m=excluded.cache_write_price_per_m,"
                "context_length=excluded.context_length,max_output_tokens=excluded.max_output_tokens,"
                "tiers_json=excluded.tiers_json,source=excluded.source,source_url=excluded.source_url,"
                "checked_at=excluded.checked_at,confidence=excluded.confidence,incremental_cost=excluded.incremental_cost,"
                "requires_exact_pricing=excluded.requires_exact_pricing",
                (
                    self.provider_id, info.model, info.billing_mode, info.currency,
                    info.input_price_per_m, info.output_price_per_m,
                    info.cache_read_price_per_m, info.cache_write_price_per_m,
                    info.context_length, info.max_output_tokens,
                    json.dumps([t.to_dict() for t in info.tiers], ensure_ascii=False),
                    info.source, info.source_url, info.checked_at, info.confidence,
                    info.incremental_cost, 1 if info.requires_exact_pricing else 0,
                ),
            )
        # Also persist standardized metadata in llm_model_metadata.
        # Automatic resolver data never overwrites an existing manual override row.
        try:
            with connect() as con:
                existing = con.execute(
                    "SELECT metadata_origin FROM llm_model_metadata WHERE provider_id=? AND model=?",
                    (self.provider_id, info.model),
                ).fetchone()
                origin = str(existing["metadata_origin"] or "manual").lower() if existing is not None else ""
                if existing is None or origin != "manual":
                    con.execute(
                        "INSERT INTO llm_model_metadata("
                        "provider_id,model,input_price_per_m,output_price_per_m,context_length,updated_at,"
                        "billing_mode,currency,cache_read_price_per_m,cache_write_price_per_m,max_output_tokens,"
                        "tiers_json,source,source_url,checked_at,confidence,metadata_origin"
                        ") VALUES(?,?,?,?,?,datetime('now'),?,?,?,?,?,?,?,?,?,?,?) "
                        "ON CONFLICT(provider_id,model) DO UPDATE SET "
                        "input_price_per_m=excluded.input_price_per_m,output_price_per_m=excluded.output_price_per_m,"
                        "context_length=excluded.context_length,updated_at=datetime('now'),billing_mode=excluded.billing_mode,"
                        "currency=excluded.currency,cache_read_price_per_m=excluded.cache_read_price_per_m,"
                        "cache_write_price_per_m=excluded.cache_write_price_per_m,max_output_tokens=excluded.max_output_tokens,"
                        "tiers_json=excluded.tiers_json,source=excluded.source,source_url=excluded.source_url,"
                        "checked_at=excluded.checked_at,confidence=excluded.confidence,metadata_origin='resolver'",
                        (
                            self.provider_id, info.model, info.input_price_per_m, info.output_price_per_m,
                            info.context_length, _normalize_billing_mode(info.billing_mode), info.currency,
                            info.cache_read_price_per_m, info.cache_write_price_per_m, info.max_output_tokens,
                            json.dumps([t.to_dict() for t in info.tiers], ensure_ascii=False),
                            info.source, info.source_url, info.checked_at, info.confidence, "resolver",
                        ),
                    )
        except Exception:
            logger.exception("Failed to persist standardized pricing metadata model=%s", info.model)
        return info

    async def _get_text(self, url: str, headers: dict | None = None) -> str:
        if url in self._page_cache:
            return self._page_cache[url]
        try:
            async with httpx.AsyncClient(timeout=NETWORK_TIMEOUT_SECONDS, follow_redirects=True) as client:
                response = await client.get(url, headers=headers or {})
            response.raise_for_status()
            text = response.text
        except Exception as exc:
            logger.info("PRICING PAGE FAILED url=%s reason=%s", url, exc)
            text = ""
        self._page_cache[url] = text
        return text

    async def _featherless_detail(self, model: str) -> PricingInfo | None:
        url = f"https://api.featherless.ai/v1/models/{quote(model, safe='')}"
        headers = {"Authorization": f"Bearer {self.api_key}"} if self.api_key else {}
        try:
            async with httpx.AsyncClient(timeout=NETWORK_TIMEOUT_SECONDS, follow_redirects=True) as client:
                response = await client.get(url, headers=headers)
            response.raise_for_status()
            raw = response.json()
        except Exception as exc:
            logger.info("FEATHERLESS PRICING DETAIL FAILED model=%s reason=%s", model, exc)
            return None
        info = _generic_raw_info(model, raw)
        if info and info.input_price_per_m is not None and info.output_price_per_m is not None:
            info.source = "provider_catalog"
            info.source_url = url
            info.confidence = "official"
            info.requires_exact_pricing = False
            return info
        return None

    async def _featherless_catalog(self, raw_models: list[tuple[str, dict]]) -> dict[str, PricingInfo]:
        """Resolve all Featherless models from one official model-class pricing table.

        Class prices are useful for cheap pre-filtering, but the exact model detail
        endpoint remains authoritative because Featherless documents model-specific
        exceptions. `requires_exact_pricing` keeps those class-only rows out of paid
        tests until an exact detail request succeeds.
        """
        url = "https://featherless.ai/docs/request-pricing-and-credits"
        html = await self._get_text(url)
        rows = await asyncio.to_thread(_table_rows, html)
        if not rows:
            return {}

        result: dict[str, PricingInfo] = {}
        for model, raw in raw_models:
            model_class = str(raw.get("model_class") or "").strip()
            exact_row = next(
                (row for row in rows if model.lower() in " | ".join(row).lower()),
                None,
            )
            row = exact_row
            if row is None and model_class:
                row = next(
                    (
                        candidate
                        for candidate in rows
                        if any(cell.strip().lower() == model_class.lower() for cell in candidate)
                    ),
                    None,
                )
            if row is None:
                continue
            prices = _dollars(" | ".join(row))
            if len(prices) < 2:
                continue
            cache_price = prices[1] if len(prices) >= 3 else None
            result[model] = PricingInfo(
                model=model,
                input_price_per_m=prices[0],
                output_price_per_m=prices[-1],
                cache_read_price_per_m=cache_price,
                context_length=safe_int(raw.get("context_length")),
                max_output_tokens=safe_int(raw.get("max_completion_tokens")),
                source="provider_catalog",
                source_url=url,
                confidence="official",
                requires_exact_pricing=exact_row is None,
            )
        return result

    async def _haimaker_catalog(self, models: list[str]) -> dict[str, PricingInfo]:
        url = "https://haimaker.ai/models"
        html = await self._get_text(url)
        if not html:
            return {}
        rows = await asyncio.to_thread(_table_rows, html)
        result: dict[str, PricingInfo] = {}
        for model in models:
            normalized = model.lower()
            row = next((r for r in rows if normalized in " ".join(r).lower()), None)
            if not row:
                continue
            joined = " | ".join(row)
            prices = _dollars(joined)
            if len(prices) < 2:
                continue
            context = None
            for cell in row:
                if "$" not in cell:
                    maybe = _token_number(cell)
                    if maybe and maybe >= 1024:
                        context = maybe
            result[model] = PricingInfo(
                model=model, input_price_per_m=prices[-2], output_price_per_m=prices[-1],
                context_length=context, source="provider_catalog", source_url=url,
                confidence="official",
            )
        return result

    async def _deepseek_catalog(self, models: list[str]) -> dict[str, PricingInfo]:
        url = "https://api-docs.deepseek.com/quick_start/pricing"
        html = await self._get_text(url)
        rows = await asyncio.to_thread(_table_rows, html)
        result: dict[str, PricingInfo] = {}
        for model in models:
            row = next((r for r in rows if model.lower() in " ".join(r).lower()), None)
            if not row:
                continue
            joined = " | ".join(row)
            prices = _dollars(joined)
            if len(prices) < 3:
                continue
            # Official table order: cache hit, cache miss, output.
            context = next((_token_number(cell) for cell in row if re.search(r"\b\d+\s*[KM]\b", cell, re.I)), None)
            result[model] = PricingInfo(
                model=model,
                input_price_per_m=prices[-2],
                output_price_per_m=prices[-1],
                cache_read_price_per_m=prices[-3],
                context_length=context,
                source="official_pricing_page",
                source_url=url,
                confidence="official",
            )
        return result

    async def _qwen_catalog(self, models: list[str]) -> dict[str, PricingInfo]:
        url = "https://docs.qwencloud.com/developer-guides/getting-started/pricing"
        html = await self._get_text(url)
        rows = await asyncio.to_thread(_table_rows, html)
        tiers_by_model: dict[str, list[PricingTier]] = {}
        current = ""
        previous_to: dict[str, int | None] = {}
        known = {m.lower(): m for m in models}
        for row in rows:
            if len(row) < 3:
                continue
            first = row[0].strip()
            match_model = None
            for lower, original in known.items():
                if lower in first.lower():
                    match_model = original
                    break
            if match_model:
                current = match_model
            if not current:
                continue
            joined = " | ".join(row)
            prices = _dollars(joined)
            if len(prices) < 2:
                continue
            range_text = row[1] if len(row) > 1 else ""
            start, end = _parse_range(range_text, previous_to.get(current))
            tiers_by_model.setdefault(current, []).append(PricingTier(start, end, prices[-2], prices[-1]))
            previous_to[current] = end
        result: dict[str, PricingInfo] = {}
        for model, tiers in tiers_by_model.items():
            first = tiers[0]
            context = max((t.input_tokens_to or 0 for t in tiers), default=0) or None
            result[model] = PricingInfo(
                model=model, input_price_per_m=first.input, output_price_per_m=first.output,
                context_length=context, tiers=tiers, source="official_pricing_page",
                source_url=url, confidence="official",
            )
        # If server-side HTTP cannot see the JS-rendered pricing document, keep
        # representative Qwen PAYG text models usable from the bundled official
        # list-price snapshot. Live parsed data above always wins.
        for model in models:
            if model not in result:
                snapshot = _qwencloud_official_snapshot(model)
                if snapshot is not None:
                    result[model] = snapshot
        return result

    async def _qwen_model_page(self, model: str) -> PricingInfo | None:
        url = f"https://www.qwencloud.com/models/{quote(model, safe='')}"
        html = await self._get_text(
            url,
            headers={
                "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language": "en-US,en;q=0.8",
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) "
                    "Chrome/148.0.0.0 Safari/537.36"
                ),
            },
        )
        if not html:
            logger.info("QWEN MARKETPLACE FETCH FAILED model=%s url=%s", model, url)
            return None
        info = await asyncio.to_thread(_parse_qwen_marketplace_page, model, html)
        if info is None:
            logger.info(
                "QWEN MARKETPLACE PARSE FAILED model=%s bytes=%s has_pricing=%s has_next_payload=%s",
                model,
                len(html),
                "pricing" in html.lower(),
                "__next" in html.lower() or "next_f" in html.lower(),
            )
            return None
        info.source_url = url
        info.billing_mode = "payg"
        logger.info(
            "QWEN MARKETPLACE OK model=%s in=%s out=%s tiers=%s context=%s",
            model,
            info.input_price_per_m,
            info.output_price_per_m,
            len(info.tiers),
            info.context_length,
        )
        return info

    async def _generic_pages(self, models: list[str]) -> dict[str, PricingInfo]:
        parsed = urlparse(self.base_url)
        if not parsed.scheme or not parsed.netloc:
            return {}
        root = f"{parsed.scheme}://{parsed.netloc}"
        result: dict[str, PricingInfo] = {}
        for url in (f"{root}/models", f"{root}/pricing"):
            html = await self._get_text(url)
            if not html or "<" not in html:
                continue
            rows = await asyncio.to_thread(_table_rows, html)
            for model in models:
                if model in result:
                    continue
                row = next((r for r in rows if model.lower() in " ".join(r).lower()), None)
                if not row:
                    continue
                prices = _dollars(" | ".join(row))
                if len(prices) < 2:
                    continue
                result[model] = PricingInfo(
                    model=model, input_price_per_m=prices[-2], output_price_per_m=prices[-1],
                    source="generic_html", source_url=url, confidence="parsed",
                )
        return result

    async def _optional_llm_parse(self, model: str, html: str, source_url: str) -> PricingInfo | None:
        if not bool(self.cfg.get("pricing_llm_parser_enabled", False)):
            return None
        base = str(self.cfg.get("pricing_llm_parser_base_url") or "").strip().rstrip("/")
        parser_model = str(self.cfg.get("pricing_llm_parser_model") or "").strip()
        if not base or not parser_model:
            return None
        key = str(self.cfg.get("pricing_llm_parser_api_key") or "").strip()
        excerpt = re.sub(r"\s+", " ", html)
        pos = excerpt.lower().find(model.lower())
        if pos >= 0:
            excerpt = excerpt[max(0, pos - 2500):pos + 4500]
        else:
            excerpt = excerpt[:7000]
        body = {
            "model": parser_model,
            "messages": [
                {"role": "system", "content": "Extract official model pricing from supplied page text. Return JSON only. Never guess."},
                {"role": "user", "content": (
                    f"MODEL: {model}\nSOURCE: {source_url}\nPAGE:\n{excerpt}\n\n"
                    'SCHEMA: {"input_price_per_m":number|null,"output_price_per_m":number|null,'
                    '"cache_read_price_per_m":number|null,"cache_write_price_per_m":number|null,'
                    '"context_length":integer|null,"max_output_tokens":integer|null}'
                )},
            ],
            "temperature": 0,
            "max_tokens": 300,
            "response_format": {"type": "json_object"},
        }
        headers = {"Content-Type": "application/json"}
        if key:
            headers["Authorization"] = f"Bearer {key}"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.post(f"{base}/chat/completions", headers=headers, json=body)
            response.raise_for_status()
            payload = response.json()
            content = (((payload.get("choices") or [{}])[0].get("message") or {}).get("content") or "").strip()
            raw = json.loads(content)
        except Exception:
            return None
        info = PricingInfo(
            model=model,
            input_price_per_m=safe_float(raw.get("input_price_per_m")),
            output_price_per_m=safe_float(raw.get("output_price_per_m")),
            cache_read_price_per_m=safe_float(raw.get("cache_read_price_per_m")),
            cache_write_price_per_m=safe_float(raw.get("cache_write_price_per_m")),
            context_length=safe_int(raw.get("context_length")),
            max_output_tokens=safe_int(raw.get("max_output_tokens")),
            source="llm_parser",
            source_url=source_url,
            confidence="parsed",
        )
        # Safety: numbers must literally occur in the supplied page excerpt.
        for number in (info.input_price_per_m, info.output_price_per_m):
            if number is not None and str(number) not in excerpt and f"{number:.2f}" not in excerpt:
                return None
        return info if info.input_price_per_m is not None and info.output_price_per_m is not None else None

    async def resolve(
        self,
        model: str,
        raw_model: dict | None = None,
        *,
        force_refresh: bool = False,
        exact: bool = True,
    ) -> PricingInfo:
        model = str(model).strip()
        raw_model = raw_model if isinstance(raw_model, dict) else {"id": model}
        if self.kind == "qwencloud":
            qwen_mode = _qwencloud_billing_mode(self.base_url)
            if qwen_mode != "payg":
                info = _generic_raw_info(model, raw_model) or PricingInfo(model=model)
                info.billing_mode = qwen_mode
                info.input_price_per_m = None
                info.output_price_per_m = None
                info.incremental_cost = 0.0 if qwen_mode == "subscription" else None
                info.source = "qwen_plan"
                info.source_url = self.base_url
                info.confidence = "official"
                return self._save(info)

        if self.kind == "arliai":
            info = _generic_raw_info(model, raw_model) or PricingInfo(model=model)
            info.billing_mode = "subscription"
            info.input_price_per_m = None
            info.output_price_per_m = None
            info.incremental_cost = 0.0
            info.source = "provider_plan"
            info.source_url = self.base_url
            info.confidence = "official"
            return self._apply_manual(info, model)

        if not force_refresh:
            cached = self._cached(model)
            if cached is not None:
                return self._apply_manual(cached, model)

        api_info = _generic_raw_info(model, raw_model)
        if api_info and api_info.input_price_per_m is not None and api_info.output_price_per_m is not None:
            api_info.source_url = f"{self.base_url}/models"
            return self._apply_manual(self._save(api_info), model)

        info = api_info or PricingInfo(model=model)
        if self.kind == "featherless" and exact:
            detail = await self._featherless_detail(model)
            info = _merge_info(detail, info) or info
        elif self.kind == "qwencloud" and exact:
            detail = await self._qwen_model_page(model)
            info = _merge_info(detail, info) or info

        if info.input_price_per_m is not None and info.output_price_per_m is not None:
            return self._apply_manual(self._save(info), model)
        return self._apply_manual(info, model)

    def _apply_manual(self, info: PricingInfo, model: str) -> PricingInfo:
        manual = self._manual_override(model)
        if not manual:
            return info
        if manual.get("input_price_per_m") is not None:
            info.input_price_per_m = safe_float(manual.get("input_price_per_m"))
        if manual.get("output_price_per_m") is not None:
            info.output_price_per_m = safe_float(manual.get("output_price_per_m"))
        if manual.get("context_length") is not None:
            info.context_length = safe_int(manual.get("context_length"))
        if manual.get("input_price_per_m") is not None or manual.get("output_price_per_m") is not None:
            info.source = "manual_override"
            info.confidence = "manual"
            if info.input_price_per_m is not None and info.output_price_per_m is not None:
                info.requires_exact_pricing = False
        return info

    async def resolve_many(
        self,
        raw_models: list[dict],
        *,
        force_refresh: bool = False,
        detail_limit: int = 100,
    ) -> list[dict]:
        unique: list[tuple[str, dict]] = []
        seen = set()
        for raw in raw_models:
            if not isinstance(raw, dict):
                continue
            model = str(raw.get("id") or "").strip()
            if not model or model in seen:
                continue
            seen.add(model)
            unique.append((model, raw))

        if self.kind == "arliai":
            return [
                (await self.resolve(model, raw, force_refresh=force_refresh)).to_dict()
                for model, raw in unique
            ]

        if self.kind == "qwencloud" and _qwencloud_billing_mode(self.base_url) != "payg":
            return [
                (await self.resolve(model, raw, force_refresh=force_refresh)).to_dict()
                for model, raw in unique
            ]

        # Cache is checked before any external pricing page.
        cached_infos: dict[str, PricingInfo] = {}
        unresolved: list[tuple[str, dict]] = []
        for model, raw in unique:
            cached = None if force_refresh else self._cached(model)
            if cached is not None:
                cached_infos[model] = self._apply_manual(cached, model)
            else:
                unresolved.append((model, raw))

        unresolved_models = [model for model, _ in unresolved]
        bulk: dict[str, PricingInfo] = {}
        if self.kind == "featherless":
            bulk = await self._featherless_catalog(unresolved)
        elif self.kind == "haimaker":
            bulk = await self._haimaker_catalog(unresolved_models)
        elif self.kind == "deepseek":
            bulk = await self._deepseek_catalog(unresolved_models)
        elif self.kind == "qwencloud":
            bulk = await self._qwen_catalog(unresolved_models)
        elif self.kind == "generic":
            bulk = await self._generic_pages(unresolved_models)

        # Build cheap preliminary metadata first. Provider API has priority over
        # provider catalog / pricing pages.
        preliminary: dict[str, PricingInfo] = {}
        raw_by_model = {model: raw for model, raw in unresolved}
        for model, raw in unresolved:
            api_info = _generic_raw_info(model, raw)
            preliminary[model] = _merge_info(api_info, bulk.get(model)) or PricingInfo(model=model)

        detail_infos: dict[str, PricingInfo] = {}
        detail_limit = max(0, int(detail_limit))
        if self.kind in {"featherless", "qwencloud"} and detail_limit:
            # For Featherless, resolve exact detail only for the cheapest
            # class-priced candidates rather than thousands of catalogue rows.
            def detail_score(model: str) -> tuple:
                info = preliminary[model]
                price = (
                    (info.input_price_per_m if info.input_price_per_m is not None else 1e12)
                    + (info.output_price_per_m if info.output_price_per_m is not None else 1e12)
                )
                context = -(info.context_length or 0)
                if self.kind == "qwencloud":
                    lower = model.lower()
                    # LLM Lab is a text-translation benchmark. Prioritize likely
                    # chat/text models so a bounded refresh doesn't spend all
                    # detail slots on image/audio/embedding catalogue entries.
                    multimedia = bool(re.search(
                        r"(?:^|[-_/])(vl|vision|image|video|audio|speech|tts|asr|omni|embedding|rerank|moderation)(?:$|[-_/])",
                        lower,
                    ))
                    text_family = 0 if (
                        lower.startswith("qwen")
                        or any(word in lower for word in ("flash", "plus", "turbo", "max", "deepseek", "kimi", "glm"))
                    ) else 1
                    translation_bias = 0 if any(word in lower for word in ("flash", "plus", "turbo", "mt")) else 1
                    known = 0 if info.input_price_per_m is not None and info.output_price_per_m is not None else 1
                    return (1 if multimedia else 0, known, text_family, translation_bias, price, context, lower)
                return (price, context, model.lower())

            if self.kind == "qwencloud":
                # Explicit refresh must populate all benchmark-suitable text
                # models.  Marketplace requests remain bounded by the shared
                # semaphore below; image/audio/etc. endpoints are excluded
                # before fan-out.
                excluded = re.compile(
                    r"(?:embedding|rerank|moderation|image|video|audio|speech|tts|asr|"
                    r"realtime|omni|wan|vl|vision)",
                    re.I,
                )
                detail_candidates = [
                    model for model in sorted(unresolved_models, key=detail_score)
                    if not excluded.search(model)
                ]
            else:
                detail_candidates = sorted(unresolved_models, key=detail_score)[:detail_limit]
            semaphore = asyncio.Semaphore(8)

            async def fetch_detail(model: str):
                async with semaphore:
                    if self.kind == "featherless":
                        return model, await self._featherless_detail(model)
                    return model, await self._qwen_model_page(model)

            pairs = await asyncio.gather(
                *(fetch_detail(model) for model in detail_candidates),
                return_exceptions=True,
            )
            for pair in pairs:
                if isinstance(pair, tuple) and len(pair) == 2 and isinstance(pair[1], PricingInfo):
                    detail_infos[pair[0]] = pair[1]

        resolved_infos: dict[str, PricingInfo] = {}
        llm_fallback_candidates: list[tuple[str, PricingInfo]] = []
        for model, _raw in unresolved:
            info = preliminary[model]
            if self.kind == "featherless":
                exact = detail_infos.get(model)
                if exact is not None:
                    info = _merge_info(exact, info) or info
                    info.requires_exact_pricing = False
                else:
                    # Class prices are useful for ranking but not safe enough
                    # to authorize a paid request because model-specific
                    # exceptions exist.
                    info.requires_exact_pricing = True
            elif self.kind == "qwencloud":
                exact = detail_infos.get(model)
                if exact is not None:
                    # Exact Marketplace metadata is authoritative for the
                    # selected model. Bulk docs are only fallback coverage.
                    info = _merge_info(exact, info) or exact

            if info.input_price_per_m is None or info.output_price_per_m is None:
                llm_fallback_candidates.append((model, info))
            resolved_infos[model] = info

        # Optional and OFF by default. It can only parse text already fetched
        # from an official/provider page and is capped to a small number of
        # unresolved models.
        if bool(self.cfg.get("pricing_llm_parser_enabled", False)) and llm_fallback_candidates:
            max_llm = min(
                max(0, int(self.cfg.get("pricing_llm_parser_max_models", 10))),
                max(0, detail_limit),
            )
            source_urls: list[str] = []
            if self.kind == "haimaker":
                source_urls = ["https://haimaker.ai/models"]
            elif self.kind == "deepseek":
                source_urls = ["https://api-docs.deepseek.com/quick_start/pricing"]
            elif self.kind == "qwencloud":
                source_urls = ["https://docs.qwencloud.com/developer-guides/getting-started/pricing"]
            elif self.kind == "featherless":
                source_urls = ["https://featherless.ai/docs/request-pricing-and-credits"]
            elif self.kind == "generic":
                parsed = urlparse(self.base_url)
                if parsed.scheme and parsed.netloc:
                    root = f"{parsed.scheme}://{parsed.netloc}"
                    source_urls = [f"{root}/models", f"{root}/pricing"]

            for model, info in llm_fallback_candidates[:max_llm]:
                for source_url in source_urls:
                    html = self._page_cache.get(source_url)
                    if not html:
                        html = await self._get_text(source_url)
                    if not html:
                        continue
                    parsed_info = await self._optional_llm_parse(model, html, source_url)
                    if parsed_info is not None:
                        resolved_infos[model] = _merge_info(info, parsed_info) or parsed_info
                        break

        results: list[PricingInfo] = []
        for model, _raw in unique:
            if model in cached_infos:
                results.append(cached_infos[model])
                continue
            info = resolved_infos.get(model) or PricingInfo(model=model)
            if (
                info.input_price_per_m is not None
                and info.output_price_per_m is not None
                and not info.requires_exact_pricing
            ):
                self._save(info)
            results.append(self._apply_manual(info, model))

        return [info.to_dict() for info in results]


def save_manual_override(
    provider_id: str,
    model: str,
    input_price: float | None,
    output_price: float | None,
    context_length: int | None,
) -> dict:
    _ensure_pricing_cache_schema()
    with connect() as con:
        con.execute(
            "INSERT INTO llm_model_metadata(provider_id, model, input_price_per_m, output_price_per_m, context_length, updated_at, metadata_origin) "
            "VALUES(?,?,?,?,?,datetime('now'),'manual') ON CONFLICT(provider_id, model) DO UPDATE SET "
            "input_price_per_m=excluded.input_price_per_m, output_price_per_m=excluded.output_price_per_m, "
            "context_length=excluded.context_length, updated_at=datetime('now'), metadata_origin='manual'",
            (provider_id, model, input_price, output_price, context_length),
        )
        con.execute("DELETE FROM llm_pricing_cache WHERE provider_id=? AND model=?", (provider_id, model))
    return {
        "model": model,
        "input_price_per_m": input_price,
        "output_price_per_m": output_price,
        "context_length": context_length,
        "source": "manual_override",
    }

# CHATGPT_PRICING_AGENT_BATCH_SAVE_V1
def save_pricing_agent_batch(provider_id: str, infos: list[PricingInfo]) -> dict:
    """Atomically replace the last Pricing Agent result for the supplied models.

    Unlike the legacy resolver cache, Pricing Agent rows are explicit user-requested
    snapshots and therefore do not expire. Manual metadata overrides stay authoritative.
    """
    _ensure_pricing_cache_schema()
    if not provider_id:
        raise ValueError("provider_id is required")
    if not infos:
        raise ValueError("Pricing Agent returned no models")
    known = unknown = partial = 0
    with connect() as con:
        con.execute("BEGIN IMMEDIATE")
        for info in infos:
            if not isinstance(info, PricingInfo) or not info.model:
                raise ValueError("Invalid PricingInfo in agent batch")
            info.source = "chatgpt_pricing_agent"
            con.execute(
                "INSERT INTO llm_pricing_cache("
                "provider_id,model,billing_mode,currency,input_price_per_m,output_price_per_m,"
                "cache_read_price_per_m,cache_write_price_per_m,context_length,max_output_tokens,"
                "tiers_json,source,source_url,checked_at,confidence,incremental_cost,requires_exact_pricing"
                ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                "ON CONFLICT(provider_id,model) DO UPDATE SET "
                "billing_mode=excluded.billing_mode,currency=excluded.currency,"
                "input_price_per_m=excluded.input_price_per_m,output_price_per_m=excluded.output_price_per_m,"
                "cache_read_price_per_m=excluded.cache_read_price_per_m,cache_write_price_per_m=excluded.cache_write_price_per_m,"
                "context_length=excluded.context_length,max_output_tokens=excluded.max_output_tokens,"
                "tiers_json=excluded.tiers_json,source=excluded.source,source_url=excluded.source_url,"
                "checked_at=excluded.checked_at,confidence=excluded.confidence,incremental_cost=excluded.incremental_cost,"
                "requires_exact_pricing=excluded.requires_exact_pricing",
                (
                    provider_id, info.model, _normalize_billing_mode(info.billing_mode), info.currency,
                    info.input_price_per_m, info.output_price_per_m,
                    info.cache_read_price_per_m, info.cache_write_price_per_m,
                    info.context_length, info.max_output_tokens,
                    json.dumps([t.to_dict() for t in info.tiers], ensure_ascii=False),
                    "chatgpt_pricing_agent", info.source_url, info.checked_at, info.confidence,
                    info.incremental_cost, 1 if info.requires_exact_pricing else 0,
                ),
            )
            existing = con.execute(
                "SELECT metadata_origin FROM llm_model_metadata WHERE provider_id=? AND model=?",
                (provider_id, info.model),
            ).fetchone()
            origin = str(existing["metadata_origin"] or "manual").lower() if existing is not None else ""
            if existing is None or origin != "manual":
                con.execute(
                    "INSERT INTO llm_model_metadata("
                    "provider_id,model,input_price_per_m,output_price_per_m,context_length,updated_at,"
                    "billing_mode,currency,cache_read_price_per_m,cache_write_price_per_m,max_output_tokens,"
                    "tiers_json,source,source_url,checked_at,confidence,metadata_origin"
                    ") VALUES(?,?,?,?,?,datetime('now'),?,?,?,?,?,?,?,?,?,?,?) "
                    "ON CONFLICT(provider_id,model) DO UPDATE SET "
                    "input_price_per_m=excluded.input_price_per_m,output_price_per_m=excluded.output_price_per_m,"
                    "context_length=excluded.context_length,updated_at=datetime('now'),billing_mode=excluded.billing_mode,"
                    "currency=excluded.currency,cache_read_price_per_m=excluded.cache_read_price_per_m,"
                    "cache_write_price_per_m=excluded.cache_write_price_per_m,max_output_tokens=excluded.max_output_tokens,"
                    "tiers_json=excluded.tiers_json,source=excluded.source,source_url=excluded.source_url,"
                    "checked_at=excluded.checked_at,confidence=excluded.confidence,metadata_origin='resolver'",
                    (
                        provider_id, info.model, info.input_price_per_m, info.output_price_per_m,
                        info.context_length, _normalize_billing_mode(info.billing_mode), info.currency,
                        info.cache_read_price_per_m, info.cache_write_price_per_m, info.max_output_tokens,
                        json.dumps([t.to_dict() for t in info.tiers], ensure_ascii=False),
                        "chatgpt_pricing_agent", info.source_url, info.checked_at, info.confidence, "resolver",
                    ),
                )
            if info.billing_mode == "payg" and (info.input_price_per_m is None or info.output_price_per_m is None):
                unknown += 1
            else:
                known += 1
                if info.requires_exact_pricing:
                    partial += 1
    return {"saved": len(infos), "known_saved": known, "unknown_saved": unknown, "partial_saved": partial}

