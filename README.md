# Everybody Codes — Kotlin

Twenty quests on weekdays from early November, three parts each, and every part
has its own input. Kotlin 2.4.20 on JDK 21, driven by `make`.

## Once

```bash
cp .env.example .env
```

Fill in `EC_TOKEN` (the `everybody-codes` cookie value from devtools →
Application → Cookies) and `EC_SEED`. The token expires about weekly; the tool
warns in its last day and refuses once it has expired.

## Each quest

```bash
make new Q=03            # Quest03.kt + Quest03Test.kt, registered, empty inputs
make fetch Q=03 P=1      # download + decrypt part 1
# paste the puzzle's sample into the test, solve part1
make test Q=03 && make run Q=03 P=1 && make submit Q=03 P=1
```

A correct submission fetches the next part's input straight away. `make run`
exits non-zero when any part fails or has no input, so a red test or a broken
part stops the chain before anything is submitted. `make help` lists everything.

## Layout

Two Gradle modules. `cli` depends on `puzzles`; Gradle forbids the reverse, so a
solution can never reach the network or the file system.

```
puzzles/                  the quests: pure code, no dependencies
  src/main/kotlin/ec/
    Quest.kt              Quest, Part, Answer, parsing helpers
    Registry.kt           years → quests
    y2025/                QuestNN.kt solutions + Quests.kt registry
  src/test/kotlin/ec/y2025/QuestNNTest.kt

cli/                      the tool: one program, five commands
  src/main/kotlin/ec/cli/
    Main.kt               entry point; failures become one-line messages
    Command.kt            the sealed command type and its parser
    Workspace.kt          every file path, defined once
    Config.kt             .env and EC_* variables; token expiry
    Json.kt               a small strict JSON reader
    Api.kt                the site's three endpoints
    Inputs.kt             fetch: download + AES-256-CBC decryption
    Answers.kt            the answers file, written and read in one place
    Runner.kt             run: timing and recording answers
    Submit.kt             submit, check, key
  src/test/kotlin/ec/cli/ including end-to-end tests against a fake site

templates/                what `make new` copies from
```

The `Makefile` builds `cli/build/install/ec/bin/ec` only when a source or build
file is newer than it, so most commands start in a fraction of a second.

## How fetching works

Inputs come from `everybody.codes/assets/<year>/<quest>/input/<seed>.json`: hex
ciphertext for all three parts, locked or not. The site withholds the keys
instead, served by `api.everybody.codes/event/<year>/quest/<quest>` once each
part is unlocked. Decryption is AES-256-CBC with the 32-character key as-is and
its first 16 characters as the IV. If the API ever fails, `make key Q=03 P=1`
saves a key copied from the browser, and `make fetch` prefers it.

`inputs/`, `answers/`, `keys/` and `.env` are gitignored: inputs are
per-account, and the token is a login credential.

## Design

- Every part returns `Answer` (a number or text), never `Any`.
- `Part` is an enum of exactly three; bad commands, quests and parts are
  rejected by `Command.parse` before anything runs.
- Quests are registered in `y2025/Quests.kt`, not found by reflection, so a
  missing or renamed quest breaks the build instead of a run.
- API responses become typed values in one place; the rest of the tool never
  handles raw JSON.
- No third-party dependencies: the JDK's HTTP client and cryptography, plus a
  small JSON reader sized to the site's three flat responses.
