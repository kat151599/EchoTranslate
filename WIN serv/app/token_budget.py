from __future__ import annotations
from dataclasses import dataclass
import tiktoken

@dataclass
class BudgetResult:
    history: list[dict]
    prompt_tokens: int
    total_reserved_tokens: int
    history_tokens: int

class TokenBudget:
    def __init__(self, encoding_name: str = "o200k_base"):
        try:
            self.enc = tiktoken.get_encoding(encoding_name)
        except Exception:
            self.enc = tiktoken.get_encoding("o200k_base")

    def count(self, text: str) -> int:
        return len(self.enc.encode(text or ""))

    def fit_history(
        self,
        *,
        system_prompt: str,
        glossary_text: str,
        target_text: str,
        history_newest_first: list[dict],
        max_request_tokens: int,
        max_output_tokens: int,
        fixed_overhead_tokens: int = 120,
    ) -> BudgetResult:
        # The enforced ceiling is prompt tokens + reserved output tokens.
        base = self.count(system_prompt) + self.count(glossary_text) + self.count(target_text) + fixed_overhead_tokens
        hard_prompt_limit = max(0, int(max_request_tokens) - int(max_output_tokens))
        if base > hard_prompt_limit:
            raise ValueError(
                f"TARGET + system/glossary already require about {base} tokens, exceeding prompt budget {hard_prompt_limit}. "
                "Increase max_request_tokens or reduce max_output_tokens/system/glossary."
            )
        selected: list[dict] = []
        used = base
        hist_tokens = 0
        for row in history_newest_first:
            chunk = f"SOURCE: {row['source_text']}\nTRANSLATION: {row['translation']}\n"
            n = self.count(chunk) + 6
            if used + n > hard_prompt_limit:
                break
            selected.append(row)
            used += n
            hist_tokens += n
        selected.reverse()  # chronological order in prompt
        return BudgetResult(
            history=selected,
            prompt_tokens=used,
            total_reserved_tokens=used + int(max_output_tokens),
            history_tokens=hist_tokens,
        )
