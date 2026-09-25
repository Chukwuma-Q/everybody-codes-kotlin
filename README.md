# Everybody Codes — Kotlin

Twenty quests on weekdays from early November, three parts each, and every part
has its own input.

## Once

```bash
cp .env.example .env
```

Fill in `EC_TOKEN` (the `everybody-codes` cookie value from devtools →
Application → Cookies) and `EC_SEED`. The token expires about weekly; when
`make fetch` says so, paste a fresh one.

## Each quest

```bash
make new Q=02            # Quest02.kt + Quest02Test.kt, registered, empty inputs
make fetch Q=02 P=1      # download + decrypt part 1
# paste the puzzle's sample into the test, solve part1
make test Q=02
make run Q=02 P=1        # submit the printed answer on the site
make fetch Q=02 P=2      # key2 exists only after part 1 is accepted
```

`make run Q=02` runs every part with input, prints timings, and merges results
into `answers/2025/quest02.txt`. `make help` lists everything.

## How fetching works

Inputs come from `everybody.codes/assets/<year>/<quest>/input/<seed>.json`: hex
ciphertext for all three parts, locked or not. The site withholds the *keys*
instead, served by `api.everybody.codes/event/<year>/quest/<quest>` once you
unlock each part. Decryption is AES-256-CBC with the 32-character key as-is and
its first 16 characters as the IV. If the API ever fails, `make key Q=02 P=1`
saves a key copied from the browser and `make fetch` uses it.

`inputs/`, `answers/`, `keys/` and `.env` are gitignored: inputs are
per-account, and the token is a login credential.

## Design

- Every part returns `Answer` (a number or text), never `Any`, so a forgotten
  return value is a compile error rather than a wrong submission.
- `Part` is an enum of exactly three; the runner rejects `P=4` at the boundary.
- Quests are registered in `y2025/Quests.kt`, not found by reflection, so a
  missing or renamed quest breaks the build instead of a run.
- Solutions parse raw text into quest-specific types first, then solve on those.

## Layout

```
src/main/kotlin/ec/
  Quest.kt      Quest, Part, Answer, parsing helpers
  Runner.kt     argument parsing, timing, answer recording
  Registry.kt   years → quests
  y2025/        QuestNN.kt solutions + Quests.kt registry
src/test/kotlin/ec/y2025/QuestNNTest.kt
scripts/fetch.sh
templates/      what `make new` copies from
```
