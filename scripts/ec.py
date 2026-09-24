#!/usr/bin/env python3
"""Everybody Codes API helpers.

  scripts/ec.py submit <year> <quest> <part>   post the saved answer, after checks and a y/N
  scripts/ec.py check  <year> <quest>          compare saved answers with the accepted ones

Verified endpoints on api.everybody.codes, authenticated with cookie everybody-codes=<EC_TOKEN>:
  GET  /event/<year>/quest/<quest>                 keyN, answerN once solved, penaltyLeftMs
  POST /event/<year>/quest/<quest>/part/<p>/answer body {"answer": "..."} -> {"correct": bool, ...}

Saved answers come from answers/<year>/quest<NN>.txt, which `make run` writes.
Requests identify themselves with EC_USER_AGENT from .env, or a generic default.
"""
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

API = "https://api.everybody.codes"
DEFAULT_USER_AGENT = "everybody-codes-kotlin"
USAGE = "usage: ec.py submit <year> <quest> <part>  |  ec.py check <year> <quest>"


def die(message: str, code: int = 1):
    print(message, file=sys.stderr)
    sys.exit(code)


def load_env() -> dict:
    """KEY=value pairs from .env, overridden by any EC_* variables already in the environment."""
    env = {}
    path = Path(".env")
    if path.is_file():
        for line in path.read_text().splitlines():
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                env[key.strip()] = value.strip().strip("\"'")
    env.update({k: v for k, v in os.environ.items() if k.startswith("EC_")})
    return env


def call(env: dict, method: str, path: str, body: dict | None = None) -> dict:
    token = env.get("EC_TOKEN") or die("Set EC_TOKEN in .env")
    data = json.dumps(body).encode() if body is not None else None
    headers = {
        "Cookie": f"{env.get('EC_COOKIE', 'everybody-codes')}={token}",
        "User-Agent": env.get("EC_USER_AGENT", DEFAULT_USER_AGENT),
        "Accept": "application/json",
    }
    if data is not None:
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(API + path, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            raw = response.read().decode()
    except urllib.error.HTTPError as e:
        die(f"{method} {path}: HTTP {e.code} {e.reason}")
    except urllib.error.URLError as e:
        die(f"{method} {path}: {e.reason}")
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        die(f"{method} {path}: response is not JSON (starts with {raw.strip()[:1]!r}). Is EC_TOKEN current?")


def saved_answers(year: int, qq: str) -> dict:
    """{part: answer} parsed from lines like 'Part2: Selkrex'."""
    path = Path(f"answers/{year}/quest{qq}.txt")
    answers = {}
    if path.is_file():
        for line in path.read_text().splitlines():
            label, sep, value = line.partition(": ")
            if sep and label.startswith("Part") and label[4:].isdigit():
                answers[int(label[4:])] = value
    return answers


def duration(ms) -> str:
    seconds = int(ms) // 1000
    hours, rest = divmod(seconds, 3600)
    minutes, secs = divmod(rest, 60)
    return f"{hours}h {minutes:02d}m {secs:02d}s" if hours else f"{minutes}m {secs:02d}s"


def submit(env: dict, year: int, quest: int, part: int) -> int:
    qq = f"{quest:02d}"
    answer = saved_answers(year, qq).get(part) or die(
        f"No saved answer for quest {qq} part {part}. Run: make run Q={qq} P={part}")

    state = call(env, "GET", f"/event/{year}/quest/{quest}")
    accepted = state.get(f"answer{part}")
    if accepted is not None:
        verdict = "matches your saved answer" if accepted == answer else f"your saved answer is '{answer}'"
        print(f"Part {part} is already solved with '{accepted}' ({verdict}). Nothing sent.")
        return 0
    if not state.get(f"key{part}"):
        die(f"Part {part} is still locked. Solve part {part - 1} first.")
    wait = int(state.get("penaltyLeftMs") or 0)
    if wait > 0:
        die(f"Wrong-answer lockout: wait {duration(wait)} before submitting again.")

    if os.environ.get("YES") != "1":
        reply = input(f"Submit '{answer}' for quest {qq} part {part}? [y/N] ").strip().lower()
        if reply != "y":
            print("Not submitted.")
            return 1

    result = call(env, "POST", f"/event/{year}/quest/{quest}/part/{part}/answer", {"answer": answer})
    if result.get("correct") is True:
        print(f"Correct! Global place {result.get('globalPlace', '?')}, "
              f"local time {duration(result.get('localTime', 0))}.")
        if part < 3:
            subprocess.run(["scripts/fetch.sh", str(year), qq, str(part + 1)], check=False)
        return 0
    print(f"Incorrect. The site replied: {json.dumps(result)}")
    return 1


def check(env: dict, year: int, quest: int) -> int:
    qq = f"{quest:02d}"
    state = call(env, "GET", f"/event/{year}/quest/{quest}")
    mine = saved_answers(year, qq)
    consistent = True
    for part in (1, 2, 3):
        accepted, saved = state.get(f"answer{part}"), mine.get(part)
        if accepted is None:
            status = "not solved on the site yet"
        elif saved is None:
            status = f"accepted '{accepted}', nothing saved locally"
        elif saved == accepted:
            status = f"ok  '{saved}'"
        else:
            status = f"MISMATCH  saved '{saved}', accepted '{accepted}'"
            consistent = False
        print(f"Part {part}: {status}")
    return 0 if consistent else 1


def main(argv: list) -> int:
    if len(argv) < 3:
        die(USAGE, 2)
    command, *raw = argv
    try:
        numbers = [int(n, 10) for n in raw]
    except ValueError:
        die(USAGE, 2)
    env = load_env()
    if command == "submit" and len(numbers) == 3 and numbers[2] in (1, 2, 3):
        return submit(env, *numbers)
    if command == "check" and len(numbers) == 2:
        return check(env, *numbers)
    die(USAGE, 2)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
