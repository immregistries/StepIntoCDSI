# cdsi-engine - per-step spec-conformance test workflow

This is the runbook for Phase 21 of the reference-module plan: bringing every executable step class in `cdsi-engine` under a dedicated, spec-conformance JUnit test, one step at a time, tracked in `cdsi-reference/step-tests/status.yaml`. It is a separate effort from `cdsi-fits-tests/AGENTS.md`'s FITS-case investigations - **do not confuse the two**. FITS tests whether the whole engine produces the right end-to-end forecast for a real patient scenario; this workflow tests whether one step's class does what its own specification section says, in isolation, independent of whether that currently changes any FITS outcome. Read `cdsi-reference/AGENTS.md` and `cdsi-fits-tests/AGENTS.md` first regardless - the finding taxonomy, the review gate, and the "don't guess, investigate" discipline are the same everywhere in this repository.

## The unit of work

One unit is one entry in `cdsi-reference/mappings/spec-to-code.yaml` - a numbered spec section (e.g. `"8.6"`) with its step package under `cdsi-reference/logic-spec/versions/<version>/steps/<NN-NN-slug>/index.md`, or an `unmapped_classes` entry (a real, instantiable step class with no numbered section - see that file's own note on each one). `cdsi-reference/step-tests/status.yaml` tracks two independent axes per unit:

- `test_status` - has a dedicated test class been written yet (Role A, below)?
- `fix_status` - has the implementation been brought in line with it, reviewed, and merged (Role B, below)?

Run `python -m cdsi_reference_tools step-tests status --version 4.6` (from `cdsi-reference/`) for a text table, or `step-tests dashboard --version 4.6` for the same information as a static HTML file (`cdsi-reference/step-tests/dashboard.html`, gitignored, open it directly in a browser) - both read pass/fail/error/skipped counts live from `cdsi-engine/target/surefire-reports/`, so run `mvn -pl cdsi-engine test` first for a fresh view. `step-tests sync --version 4.6` adds any unit `spec-to-code.yaml` has that `status.yaml` doesn't yet, with `not_started`/`not_started` defaults - never overwrites an existing entry.

`NoValidDosesCompletableTest` (unit `8.6`, written while investigating `SPEC-4.6-0007`) is the concrete template for what a Role A test class looks like: it drives one scoring condition of `NoValidDoses` in isolation, invoking a private method reflectively rather than routing a whole `ForecastInput` through the full pipeline. It only covers one of that class's several conditions, though - `8.6` is deliberately left `test_status: tests_written` / not fully covered, as a reminder that "a test class exists" and "this unit is fully covered" are different claims; say which one is true in the unit's `notes`.

## Role A - write the spec-conformance tests (touches no production code)

Given one unit, assigned for one session:

1. Read the step's `index.md` in full - Purpose, Business Rules, Decision Tables, State Changes - and the actual implementation class(es) `spec-to-code.yaml` names for it, not a paraphrase of either.
2. Write one JUnit test method per business rule / decision-table row, asserting what the specification says should happen. Isolate the class: construct the minimal domain objects it needs directly, and invoke it directly (reflectively, if the method under test is private - see the precedent above) rather than running a full FITS-style scenario through the whole engine. A red test here is expected and fine - this phase measures coverage, not correctness.
3. If a business rule can't be isolated without disproportionate scaffolding, or is genuinely ambiguous in the spec text, say so in the unit's `notes` rather than forcing a brittle test or silently skipping it.
4. Run the full existing test suite (`cdsi-engine`, then the reactor) once before finishing, to confirm adding a test file changed nothing else - it shouldn't, since no production code was touched.
5. Commit the test class only, in your own isolated worktree/branch. Update `cdsi-reference/step-tests/status.yaml` for this unit: `test_status: tests_written`, `test_class` set, and leave `fix_status` at `not_started` (that's Role B's job, later). Run `logic-spec validate --version <version>` before considering this done.
6. Report back: how many tests, how many currently red, and for each red test a one-line expected-vs-actual note. Stop there - do not fix anything in this pass.
7. Recording the test file and the status update doesn't need the project owner's review gate (it's new coverage, not a behavior change) - but still stop for review before this becomes the basis for a Role B session, the same way any new finding stays `open` until reviewed.

## Role B - fix the step to match its own tests (a later, separate session)

Given one unit whose Role A pass has already been reviewed and merged:

1. Confirm a clean repository and that the unit's test class actually exists and is on `status.yaml` as `test_status: tests_written`.
2. For each red test, classify it before touching any code, using the same taxonomy as `cdsi-fits-tests/AGENTS.md` (`IMPLEMENTATION_MISMATCH`, `SPECIFICATION_AMBIGUITY`, `SPECIFICATION_DEFECT`, `SUPPORTING_DATA_CONFLICT`, `FITS_DIFFERENCE`, `FIXTURE_IMPORT_DEFECT`, `UNDETERMINED`, `NOT_REPRODUCIBLE`) - it's one finding system, not two.
3. **Before concluding a red test is a defect in this step's own class, check whether the actual cause is upstream or downstream instead** - a different step populating or consuming the shared state (a `PatientSeries` field, a loop-control flag, an orchestration handoff) incorrectly. The step's own `index.md` documents its State Changes; `LogicStepFactory`'s dispatch chain and the neighboring steps' `index.md` files show who runs before and after it. This matters here specifically because a step class can look broken in isolation while actually just faithfully reflecting bad input from another step - and "fix the symptom in the step you were assigned" would be the wrong call.
   - **If you find clear evidence the defect belongs to a different step's class: do not fix it.** Record a finding whose `code_locations` names that other class (the finding schema already supports a code location outside the unit you were assigned), with the evidence you traced and your recommendation. Set this unit's `fix_status: blocked`, `blocked_category: upstream_step_defect`, `blocked_reason` explaining what you suspect and why, and leave the affected test(s) red with a comment pointing at the finding id. This is a normal, expected outcome, not a failure to finish the task - it's the whole point of asking you to check.
4. For a genuine `IMPLEMENTATION_MISMATCH` in this step's own class, make the smallest fix that follows the specification - never special-case a test's specific inputs.
5. **Ignore FITS pass/fail counts as a goal for this round.** Do not regenerate `cdsi-fits-tests/src/test/resources/known-passing-cases.txt` and do not chase FITS deltas - the target is only this unit's own test class going green, plus nothing else regressing (next step).
6. Run, in order: the unit's own test class; the full `cdsi-engine` test suite; the full FITS suite (`mvn -pl cdsi-fits-tests test -Dtest=FitsFixtureTest`, which enforces `known-passing-cases.txt`). **Any regression anywhere - a different `cdsi-engine` test that used to pass, or any allowlisted FITS case - is an automatic stop, not a judgment call.** Do not commit a fix that regresses something else, even if it makes this unit's own tests fully green. Instead: set this unit's `fix_status: blocked`, `blocked_category: would_regress_other_tests`, and `blocked_reason` naming exactly what it would break and why, and report back with both results (this unit's tests, and what broke) so the project owner can decide - a real conflict between two steps' correct-looking behavior is exactly the kind of thing that needs a human, not a guess.
7. If a red test can't be resolved and doesn't fit `upstream_step_defect` either (spec is ambiguous, or you've genuinely run out of evidence), record it as such (`SPECIFICATION_AMBIGUITY`/`SPECIFICATION_DEFECT`/`UNDETERMINED`), set `fix_status: blocked`, `blocked_category: undetermined`, and leave the test red with a comment linking the finding.
8. Record or update the finding for whatever you did resolve. Run `logic-spec validate --version <version>` before considering this done.
9. **Stop for the project owner's review before committing any change to a `cdsi-engine`/`cdsi-web` class**, exactly like `cdsi-fits-tests/AGENTS.md` - propose the finding and the fix together, commit only to your own isolated worktree/branch, never merge unilaterally. Setting `fix_status: fixed_pending_review` (not `merged`) is how you signal "ready for review, not yet reviewed" in `status.yaml`.

## The agent must not (either role)

- Fix, or propose fixing, a step class other than the one unit assigned this session - even a one-line change that looks obviously safe. That decision belongs to whoever runs *that* step's own Role B session, with its own before/after regression check. Elevate it as a finding instead (see step 3 above).
- Treat "my unit's tests are green" as sufficient without having run the full `cdsi-engine` suite and the full FITS suite to check for regressions elsewhere.
- Chase FITS pass-rate improvements in a Role B session, or regenerate `known-passing-cases.txt` - that's explicitly out of scope for this phase.
- Commit a `cdsi-engine`/`cdsi-web` change without the project owner's review, no matter how confident the fix is.
- Maintain a second findings/investigations system, or a second per-unit status file - `cdsi-reference/logic-spec/versions/<version>/findings/` and `cdsi-reference/step-tests/status.yaml` are the only places this information lives.
- Guess at a business rule's intent when the specification text is genuinely ambiguous - record `SPECIFICATION_AMBIGUITY` and move on, don't pick an interpretation silently.
