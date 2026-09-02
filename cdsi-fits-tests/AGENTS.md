# cdsi-fits-tests - agent repair workflow

This is the runbook for closing a gap between what CDSi publishes and what `cdsi-engine` does: pick one failing FITS case, find out why, and either fix it (with the project owner's review) or record why it isn't a code defect. This project is **agent-assisted, not automated CI** - the project owner stays in the loop on every clinical-logic change. Read this whole file before touching a failing case; it assumes `cdsi-reference`'s own `AGENTS.md` and `README.md` too.

## What exists today vs. what's still coming

Built and real:

- **Reference sets** (`cdsi-reference`, Phase 16): `reference-set.json` pins the exact Logic Specification version, Supporting Data release, and FITS fixture set this suite runs against. `ReferenceSetVerifier` fails the whole suite clearly if any of those drift.
- **Diagnostic bundles** (Phase 17): every `mvn -pl cdsi-fits-tests test -Dtest=FitsFixtureTest` run writes `target/fits-runs/<run-id>/` - `run.json`, `summary.json`, `results.jsonl` (one entry per case: status, expected/actual values, field-level differences, duration), and `failures/<case-id>/{input,expected,actual,difference}.json` for every non-passing case. This is disposable and gitignored - it's your primary evidence for one investigation session, not a historical record.
- **Findings** (`cdsi-reference`, Phase 9, as amended): the one place a confirmed root cause gets recorded, under `logic-spec/versions/<v>/findings/<id>/`. See its taxonomy below.

Not built yet - work around their absence as noted, don't block on them:

- **Structured per-decision engine trace** (Phase 18): `trace.jsonl` is not written today. Use `results.jsonl`'s `fieldDifferences` plus the step package for the mapped section, and re-run the single case by hand with a debugger or added logging if you need finer-grained evidence.
- **Case-level regression baseline** (Phase 19): there is no `cdsi-fits-tests/history/baseline.yaml` yet distinguishing "known, already-investigated failure" from "new regression." Until it exists, treat every failing case as needing investigation, and rely on git history plus existing findings' `fits_cases` lists to tell whether a case has already been looked at.

## The workflow

1. **Confirm a clean repository and a valid reference set.** `git status` should be clean (or you should know exactly what's uncommitted and why). Run `mvn -pl cdsi-fits-tests test -Dtest=ReferenceSetVerifierTest` - if it fails, stop and fix the reference set binding first (see `cdsi-reference/reference-sets/README.md`), don't investigate FITS cases against a state nobody can reproduce.
2. **Run the suite** (`mvn -pl cdsi-fits-tests test -Dtest=FitsFixtureTest`, or the whole reactor's tests) and find the diagnostic bundle it just wrote under `target/fits-runs/`.
3. **Select one failing case.** Prefer one that looks like it belongs to a cluster you haven't investigated yet (same vaccine group, same kind of field difference in `results.jsonl`) over a random pick - but investigate that one case on its own merits; don't assume the cluster shares a root cause until you've confirmed it.
4. **Reproduce it without changing any code.** Read its `failures/<case-id>/input.json`, `expected.json`, `actual.json`, and `difference.json`. Confirm you understand exactly which field(s) diverged and by how much.
5. **Read the mapped specification step and code.** Use `cdsi-reference/mappings/spec-to-code.yaml` to find the section and engine class(es) the failing vaccine group's forecast goes through; read that step's `index.md` (Purpose, Business Rules, Decision Tables, State Changes) and the actual `cdsi-engine` source, not a paraphrase of either.
6. **Check whether this is already a known finding.** Search `logic-spec/versions/<v>/findings/*/finding.yaml` for this case's `cvx`/group/section already appearing in a finding's evidence, or check if the case id is already in some finding's `fits_cases`. If so, this case is already explained - add its id to that finding's `fits_cases` if it isn't there, and skip to step 12 unless you have new evidence that changes the finding.
7. **Classify before editing**, using the one finding taxonomy (`cdsi-reference/templates/finding.md`):
   - `IMPLEMENTATION_MISMATCH` - the engine doesn't do what the spec says.
   - `SPECIFICATION_AMBIGUITY` - the spec doesn't clearly say what should happen here.
   - `SPECIFICATION_DEFECT` - the spec is clear, but appears to be a genuine CDSi error - report it back to CDC/CDSi, don't just work around it.
   - `SUPPORTING_DATA_CONFLICT` - the Logic Specification and the Supporting Data disagree.
   - `FITS_DIFFERENCE` - the FITS fixture's expected result appears inconsistent with the specification, the Supporting Data, or a correct implementation of both.
   - `FIXTURE_IMPORT_DEFECT` - our own `FitsDownloader` captured or converted this fixture wrong, not FITS itself.
   - `NOT_REPRODUCIBLE` - it doesn't reliably fail (check for date-dependence first: `evalDate` relative to today).
   - `UNDETERMINED` - you've genuinely run out of evidence; record what you tried.
8. **If it's `IMPLEMENTATION_MISMATCH`**, add a focused unit test that isolates the defect below the FITS level when you can. Make the smallest general correction that follows the specification - never special-case a FITS uid or hardcode this case's expected values.
9. **Run the focused test, then this one FITS case, then the related FITS group, then the full suite.** Compare the new `results.jsonl` against the run before your change: confirm the cases you meant to fix now pass, and account for every other case whose status changed (a real fix can legitimately change other cases in the same cluster - that's expected, not a red flag, but check it deliberately rather than assuming).
10. **Record the finding**: create a new `finding.yaml`/`finding.md` pair (`cdsi-reference/tools/cdsi_reference_tools/findings.py`'s `next_finding_id()` allocates the id) or add this case to an existing finding's `fits_cases`. Run `logic-spec validate` before considering this done.
11. **Stop for the project owner's review before committing a clinical-logic change.** Propose the finding and the fix together; do not commit or merge a `cdsi-engine`/`cdsi-web` change unilaterally, no matter how many tests pass locally. Recording a finding, or adding a case id to an existing one, doesn't need that same review gate - it's documentation, not a behavior change.
12. If you're not making a code change this round (the case is `SPECIFICATION_DEFECT`, `FITS_DIFFERENCE`, `UNDETERMINED`, or already explained by an existing finding), your finding record *is* the deliverable for this case - there's nothing further to commit.

## Stop conditions

Stop and ask the project owner rather than guessing when:

- The specification, Supporting Data, and FITS fixture genuinely conflict and you can't tell which one is authoritative.
- Required source data (a Supporting Data field, a spec table, a mapped class) appears to be missing entirely, not just ambiguous.
- The failure isn't reproducible and you can't determine why.
- Resolving this would require clinical judgment beyond what the specification text settles.
- Your fix changes the result of FITS cases well outside the cluster you were investigating, in ways you can't explain from the specification.

## The agent must not

- Change a FITS fixture's expected result merely to make a case pass.
- Hand-edit anything generated (`cdsi-reference/logic-spec/versions/<v>/extracted/`, `normalized/`, `documentation/`) instead of fixing the tool that generates it.
- Assume FITS is authoritative when it conflicts with specification evidence - or assume the specification is authoritative when FITS and Supporting Data agree with each other against it. Investigate; don't default.
- Treat a fix as done because one case now passes, without running the related group and the full suite.
- Commit a clinical-logic fix without the project owner's review, even when every test the agent can run passes.
- Special-case a FITS test-plan id, uid, or group name anywhere in `cdsi-engine`/`cdsi-web` source. A correction belongs at the level of the rule it's fixing, not the case that happened to reveal it.
- Maintain a second findings/investigations system. One finding, one taxonomy, one place they live.
