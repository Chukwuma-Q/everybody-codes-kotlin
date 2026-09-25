SHELL := /bin/bash
.PHONY: help guard-Q new fetch input run test watch bench answers list clean

YEAR ?= 2025
Q    ?=
P    ?=

QN = $(shell echo '$(Q)' | sed 's/^0*\([0-9]\)/\1/')
QQ = $(shell printf '%02d' '$(QN)' 2>/dev/null)

SRC      = src/main/kotlin/ec/y$(YEAR)
TEST     = src/test/kotlin/ec/y$(YEAR)
REGISTRY = src/main/kotlin/ec/Registry.kt

help:
	@echo "Everybody Codes — $(YEAR)"
	@echo ""
	@echo "  make new Q=01              scaffold quest + test, register it"
	@echo "  make input Q=01 P=1        create an empty input file to paste into"
	@echo "  make fetch Q=01            download all three inputs (see scripts/fetch.sh)"
	@echo "  make fetch Q=01 P=2        download one part"
	@echo "  make key Q=01 P=1          save a key by hand (fallback if the API fails)"
	@echo "  make submit Q=01 P=1       submit the saved answer (asks first)"
	@echo "  make check Q=01            compare saved answers with the accepted ones"
	@echo "  make run Q=01              run every part that has input"
	@echo "  make run Q=01 P=2          run one part"
	@echo "  make test Q=01             run that quest's sample tests"
	@echo "  make test                  run every test"
	@echo "  make watch Q=01            re-run on every save"
	@echo "  make bench Q=01 P=3        hyperfine the installed binary"
	@echo "  make answers               show everything solved so far"
	@echo "  make list                  quests scaffolded this year"
	@echo "  make clean                 gradle clean"
	@echo ""
	@echo "  YEAR defaults to $(YEAR); override with YEAR=2024"

guard-Q:
	@test -n "$(Q)" || { echo "Specify a quest: make $(MAKECMDGOALS) Q=01"; exit 1; }

new: guard-Q
	@test ! -f "$(SRC)/Quest$(QQ).kt" || { echo "$(SRC)/Quest$(QQ).kt exists"; exit 1; }
	@mkdir -p "$(SRC)" "$(TEST)" "inputs/$(YEAR)"
	@sed -e 's/{{YEAR}}/$(YEAR)/g' -e 's/{{NN}}/$(QQ)/g' templates/Quest.kt.tmpl     > "$(SRC)/Quest$(QQ).kt"
	@sed -e 's/{{YEAR}}/$(YEAR)/g' -e 's/{{NN}}/$(QQ)/g' templates/QuestTest.kt.tmpl > "$(TEST)/Quest$(QQ)Test.kt"
	@test -f "$(SRC)/Quests.kt" || sed 's/{{YEAR}}/$(YEAR)/g' templates/Quests.kt.tmpl > "$(SRC)/Quests.kt"
	@grep -q '$(YEAR) to ec.y$(YEAR).quests' $(REGISTRY) || \
	  sed -i 's|^    // make new: years|    $(YEAR) to ec.y$(YEAR).quests,\n&|' $(REGISTRY)
	@sed -i 's|^    // make new: quests|    $(QN) to ::Quest$(QQ),\n&|' "$(SRC)/Quests.kt"
	@for p in 1 2 3; do touch "inputs/$(YEAR)/quest$(QQ)_part$$p.txt"; done
	@echo "created  $(SRC)/Quest$(QQ).kt"
	@echo "created  $(TEST)/Quest$(QQ)Test.kt"
	@echo "added    $(QN) to ::Quest$(QQ) in $(SRC)/Quests.kt"
	@echo "inputs   inputs/$(YEAR)/quest$(QQ)_part{1,2,3}.txt (empty)"
	@echo ""
	@echo "https://everybody.codes/event/$(YEAR)/quests/$(QN)"

input: guard-Q
	@test -n "$(P)" || { echo "Specify a part: make input Q=$(Q) P=1"; exit 1; }
	@mkdir -p "inputs/$(YEAR)"
	@touch "inputs/$(YEAR)/quest$(QQ)_part$(P).txt"
	@echo "paste your part $(P) input into: inputs/$(YEAR)/quest$(QQ)_part$(P).txt"

fetch: guard-Q
	@if [ -n "$(P)" ]; then \
	  scripts/fetch.sh "$(YEAR)" "$(QQ)" "$(P)"; \
	else \
	  for p in 1 2 3; do scripts/fetch.sh "$(YEAR)" "$(QQ)" $$p || true; done; \
	fi

run: guard-Q
	@./gradlew -q --console=plain run --args="$(YEAR) $(QQ) $(P)"

test:
	@if [ -n "$(Q)" ]; then \
	  ./gradlew -q --console=plain test --tests "ec.y$(YEAR).Quest$(QQ)Test"; \
	else \
	  ./gradlew -q --console=plain test; \
	fi

watch: guard-Q
	@./gradlew -t -q --console=plain run --args="$(YEAR) $(QQ) $(P)"

bench: guard-Q
	@command -v hyperfine >/dev/null || { echo "hyperfine not installed"; exit 1; }
	@test -n "$(P)" || { echo "Specify a part: make bench Q=$(Q) P=1"; exit 1; }
	@./gradlew -q installDist
	@mkdir -p benchmark/$(YEAR)
	@hyperfine --warmup 3 --runs 10 \
	  --export-markdown "benchmark/$(YEAR)/quest$(QQ)_part$(P).md" \
	  "build/install/ec/bin/ec $(YEAR) $(QQ) $(P)"

answers:
	@for f in answers/$(YEAR)/*.txt; do \
	  [ -e "$$f" ] || { echo "nothing solved yet"; break; }; \
	  echo "== $$(basename $$f .txt)"; sed 's/^/   /' "$$f"; \
	done

list:
	@ls -1 $(SRC)/Quest[0-9][0-9].kt 2>/dev/null | sed 's|.*/||; s|\.kt$$||' || echo "none yet"

clean:
	@./gradlew -q clean

.PHONY: key
key: guard-Q
	@test -n "$(P)" || { echo "Specify a part: make key Q=$(Q) P=1"; exit 1; }
	@python3 scripts/ec.py key "$(YEAR)" "$(QQ)" "$(P)"

.PHONY: submit check
submit: guard-Q
	@test -n "$(P)" || { echo "Specify a part: make submit Q=$(Q) P=1"; exit 1; }
	@python3 scripts/ec.py submit "$(YEAR)" "$(QQ)" "$(P)"

check: guard-Q
	@python3 scripts/ec.py check "$(YEAR)" "$(QQ)"

.PHONY: lint
lint:
	@mypy --enable-error-code possibly-undefined scripts/ec.py
