#!/usr/bin/env python3
"""
Convert a Claude Code raw session (JSONL) into OpenAI-chat-compatible
trajectory.json.

Usage:
    python3 convert_ai_session.py <session.json> <trajectory.json>

Preserves:
- user turns (text + document titles + tool results)
- assistant turns (text + tool_use calls as tool_calls)
- system-side subtypes (compact/init) as system messages
- tool results linked to their originating tool_use_id
"""

import json
import sys
from pathlib import Path


def _blocks_to_text(blocks):
    """Flatten a Claude content-block list into a single string."""
    if isinstance(blocks, str):
        return blocks
    parts = []
    if not isinstance(blocks, list):
        return ""
    for b in blocks:
        if not isinstance(b, dict):
            parts.append(str(b))
            continue
        btype = b.get("type")
        if btype == "text":
            parts.append(b.get("text", ""))
        elif btype == "document":
            title = b.get("title") or "document"
            parts.append(f"[attached document: {title}]")
        elif btype == "image":
            parts.append("[attached image]")
        elif btype == "thinking":
            # OpenAI has no direct equivalent; keep as annotated text so the
            # trajectory still captures reasoning without losing information.
            parts.append(f"<thinking>\n{b.get('thinking','')}\n</thinking>")
        elif btype == "tool_result":
            content = b.get("content", "")
            parts.append(_blocks_to_text(content) if isinstance(content, list) else str(content))
        else:
            parts.append(json.dumps(b, ensure_ascii=False))
    return "\n".join(p for p in parts if p)


def _extract_tool_calls(blocks):
    """Return the OpenAI-style tool_calls list from an assistant content list."""
    calls = []
    if not isinstance(blocks, list):
        return calls
    for b in blocks:
        if isinstance(b, dict) and b.get("type") == "tool_use":
            calls.append({
                "id": b.get("id"),
                "type": "function",
                "function": {
                    "name": b.get("name"),
                    "arguments": json.dumps(b.get("input", {}), ensure_ascii=False),
                },
            })
    return calls


def _extract_tool_results(blocks):
    """Return a list of (tool_use_id, text) pairs from a user content list."""
    out = []
    if not isinstance(blocks, list):
        return out
    for b in blocks:
        if isinstance(b, dict) and b.get("type") == "tool_result":
            content = b.get("content", "")
            text = _blocks_to_text(content) if isinstance(content, list) else str(content)
            out.append((b.get("tool_use_id"), text))
    return out


def convert(raw_path: Path):
    messages = []
    meta = {"source": str(raw_path), "events_seen": 0}

    with raw_path.open("r", encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line:
                continue
            try:
                ev = json.loads(line)
            except json.JSONDecodeError:
                continue

            etype = ev.get("type")
            meta["events_seen"] += 1

            if etype == "user":
                msg = ev.get("message", {})
                content = msg.get("content", "")
                tool_results = _extract_tool_results(content) if isinstance(content, list) else []

                # Emit tool results first so they appear before any sibling user text.
                for tool_use_id, text in tool_results:
                    messages.append({
                        "role": "tool",
                        "tool_call_id": tool_use_id,
                        "content": text,
                    })

                # Remaining non-tool_result content becomes a user message.
                if isinstance(content, list):
                    non_tool = [b for b in content if not (isinstance(b, dict) and b.get("type") == "tool_result")]
                    text = _blocks_to_text(non_tool).strip()
                else:
                    text = str(content).strip()

                if text:
                    messages.append({"role": "user", "content": text})

            elif etype == "assistant":
                msg = ev.get("message", {})
                content = msg.get("content", [])
                text = _blocks_to_text(
                    [b for b in content if isinstance(b, dict) and b.get("type") != "tool_use"]
                ).strip()
                tool_calls = _extract_tool_calls(content)

                entry = {"role": "assistant", "content": text or None}
                if tool_calls:
                    entry["tool_calls"] = tool_calls
                messages.append(entry)

            elif etype == "system":
                # Capture compaction / init markers as system messages.
                subtype = ev.get("subtype", "system")
                content = ev.get("content", "")
                text = content if isinstance(content, str) else json.dumps(content, ensure_ascii=False)
                if text:
                    messages.append({
                        "role": "system",
                        "content": f"[{subtype}] {text}",
                    })

            # attachment / file-history-snapshot / queue-operation / last-prompt /
            # ai-title are session bookkeeping and are intentionally skipped.

    return messages, meta


def main():
    if len(sys.argv) != 3:
        print("Usage: python3 convert_ai_session.py <session.json> <trajectory.json>", file=sys.stderr)
        sys.exit(2)

    src = Path(sys.argv[1])
    dst = Path(sys.argv[2])
    if not src.exists():
        print(f"Input not found: {src}", file=sys.stderr)
        sys.exit(1)

    messages, meta = convert(src)
    trajectory = {
        "format": "openai-chat",
        "model": "claude-sonnet-4-6",
        "task_id": "TASK-20260318-393C91",
        "source_events": meta["events_seen"],
        "message_count": len(messages),
        "messages": messages,
    }
    dst.write_text(json.dumps(trajectory, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {len(messages)} messages ({meta['events_seen']} source events) -> {dst}")


if __name__ == "__main__":
    main()
