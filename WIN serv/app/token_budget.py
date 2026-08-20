from __future__ import annotations
from dataclasses import dataclass
import tiktoken


HISTORY_HEADER = "HISTORY (context only; do not output it):\n"
OUTPUT_SCHEMA = '{"translations":[{"id":0,"translation":"..."}]}'


@dataclass
class BudgetResult:
    history: list[dict]
    prompt_tokens: int
    history_tokens: int


class TokenBudget:
    def __init__(self, encoding_name: str = "o200k_base"):
        try:
            self.enc = tiktoken.get_encoding(encoding_name)
        except Exception:
            self.enc = tiktoken.get_encoding("o200k_base")

    def count(self, text: str) -> int:
        return len(self.enc.encode(text or ""))

    @staticmethod
    def _history_chunk(row: dict) -> str:
        return f"SOURCE: {row['source_text']}\nTRANSLATION: {row['translation']}"

    def _history_section_tokens(self, newest_first: list[dict]) -> int:
        if not newest_first:
            return 0
        chronological = reversed(newest_first)
        section = HISTORY_HEADER + "\n\n".join(self._history_chunk(row) for row in chronological)
        return self.count(section)

    def fit_history(
        self,
        *,
        system_prompt: str,
        glossary_text: str,
        target_text: str,
        history_newest_first: list[dict],
        history_token_limit: int,
        fixed_overhead_tokens: int = 120,
    ) -> BudgetResult:
        # Only HISTORY is fitted to history_token_limit. System, glossary,
        # TARGET_BLOCKS and the model response limit are independent.
        history_limit = max(0, int(history_token_limit))
        selected_newest_first: list[dict] = []
        history_tokens = 0

        for row in history_newest_first:
            candidate = selected_newest_first + [row]
            candidate_tokens = self._history_section_tokens(candidate)
            if candidate_tokens > history_limit:
                break
            selected_newest_first.append(row)
            history_tokens = candidate_tokens

        selected = list(reversed(selected_newest_first))
        base_prompt_tokens = (
            self.count(system_prompt)
            + self.count(glossary_text)
            + self.count(target_text)
            + self.count("OUTPUT SCHEMA: " + OUTPUT_SCHEMA)
            + fixed_overhead_tokens
        )
        return BudgetResult(
            history=selected,
            prompt_tokens=base_prompt_tokens + history_tokens,
            history_tokens=history_tokens,
        )
