# SPEC-4.6-0010: Table 6-14's independent assumed-value defaults can produce an unresolvable Table 6-15 state

**Status:** confirmed (a pragmatic fix reviewed and merged by the project owner - see "Fix merged" - but the underlying specification gap itself is **open for CDC/CDSi review**, not resolved by this project's own authority)
**Category:** SPECIFICATION_DEFECT

## Evidence

Table 6-14's "Assumed Value if Empty" column gives Minimum Age Date (CALCDTAGE-4) and Absolute Minimum Age Date (CALCDTAGE-5) the *same* flat default, 01/01/1900 - assigned to each attribute independently, with no stated relationship between them.

Table 6-15's four conditions require exactly one of them to answer YES for each of its four rules:
1. date administered < absolute minimum age date
2. absolute minimum age date <= date administered < minimum age date
3. minimum age date <= date administered < maximum age date
4. date administered >= maximum age date

This implicitly assumes the three boundary dates stay ordered: absolute minimum <= minimum <= maximum. Nothing in Table 6-14 guarantees that ordering when some dates are defined and others fall back to their independent defaults.

**The real, occurring case:** a target dose defines an absolute minimum age (a real, later date, e.g. date of birth + 19 years) but no minimum age at all. Minimum age date falls back to the flat 1900 default - a date *earlier* than the real absolute minimum. For any dose administered before that absolute minimum:
- Condition 1 (before absolute minimum) → YES
- Condition 3 (between the assumed-1900 "minimum" and the assumed-future maximum) → also YES

No rule column has that Yes/No pattern. `LogicTable.evaluate()` records no outcome, and `EvaluateAge.process()`'s own guard throws `NullPointerException("Evaluation should not be null at this point")`.

This is not a constructed corner case. The Supporting Data release bundled with `cdsi-engine` has exactly 5 series doses shaped this way - all Pneumococcal: Dose 3 of the two "Pneumococcal risk 19+ years CSF Leaks or Cochlear Implants" series (PCV-PPSV, PPSV-PCV), and Dose 4 of the three "Pneumococcal risk 19+ years immunocompromised" series (PCV-PPSV-PPSV, PPSV-PCV-PPSV, PPSV-PPSV-PCV). Any dose administered before the patient's 19th birthday against one of those target doses reached this crash.

## Interpretation

Both tables are individually clear. Table 6-14 says what each attribute defaults to, on its own; Table 6-15 says which rule applies, given four Yes/No answers. Neither table's own text is ambiguous. The defect only appears once their independently-correct defaults are combined for this specific input shape - which is exactly why this is filed as **`SPECIFICATION_DEFECT`** rather than `SPECIFICATION_AMBIGUITY`: an implementer reading either table alone would not hesitate, but implementing both faithfully, together, leaves a real input with no valid outcome at all.

## For CDC/CDSi review

This project made a pragmatic, documented engineering decision to keep the reference engine from crashing on real Supporting Data (see "Fix merged" below), but that decision doesn't carry CDC/CDSi's own authority over what Table 6-14's defaults *should* say when Minimum Age Date and Absolute Minimum Age Date are defined independently of each other. Worth raising with CDC/CDSi directly:

- Should Table 6-14 state that Minimum Age Date's assumed value, when empty, is the *greater of* 01/01/1900 and the Absolute Minimum Age Date (rather than a flat, independent 01/01/1900) - i.e., should the specification itself guarantee the ordering Table 6-15 depends on?
- Alternatively, should Table 6-15 gain an explicit rule (or a documented evaluation order / first-match-wins semantics) for the case where more than one condition answers YES, rather than leaving it as an implicit, undocumented invariant?
- Is the resolution this project chose - a dose becomes fully valid, with no grace period, the instant it clears the absolute minimum age when no distinct minimum age was ever defined - the clinically intended behavior, or was some other resolution intended?

## Fix merged

Reviewed and approved by the project owner on 2026-09-08 (explicit sign-off on this specific resolution, given the specification gap above), merged to `develop`.

In `EvaluateAge`'s constructor, when minimum age is unvalued but absolute minimum age is valued, the minimum age date's fallback is now the absolute minimum age date itself, instead of the independent flat 1900 default:

```diff
-      caAbsoluteMinimumAgeDate
-          .setInitialValue(age.getAbsoluteMinimumAge().getDateFrom(dateOfBirth));
-      caMinimumAgeDate.setInitialValue(age.getMinimumAge().getDateFrom(dateOfBirth));
-      if (age.getMaximumAge().isValued()) {
-        caMaximumAgeDate.setInitialValue(age.getMaximumAge().getDateFrom(dateOfBirth));
-      }
+      Date absoluteMinimumAgeDate = age.getAbsoluteMinimumAge().getDateFrom(dateOfBirth);
+      caAbsoluteMinimumAgeDate.setInitialValue(absoluteMinimumAgeDate);
+      if (age.getMinimumAge().isValued()) {
+        caMinimumAgeDate.setInitialValue(age.getMinimumAge().getDateFrom(dateOfBirth));
+      } else if (absoluteMinimumAgeDate != null) {
+        caMinimumAgeDate.setInitialValue(absoluteMinimumAgeDate);
+      }
+      if (age.getMaximumAge().isValued()) {
+        caMaximumAgeDate.setInitialValue(age.getMaximumAge().getDateFrom(dateOfBirth));
+      }
```

This only changes behavior in the exact narrow intersection (minimum age unvalued, absolute minimum age valued). Every other combination is untouched, including the case where both are unvalued (both still fall to the same flat 1900, so no conflict arises there either - the ordering invariant Table 6-15 depends on is restored, not sidestepped).

### Test verification

`EvaluateAgeTest` (20 tests): before, 19 green / 1 red; after, **20 green / 0 red** - unit 6.4 is now fully closed, and the previously-crashing test (`aDoseBeforeTheAbsoluteMinimumAgeIsTooYoungEvenWithNoMinimumAgeDefined`) passes without the `NullPointerException` it used to catch and fail on. Full `cdsi-engine` suite: 767 tests, 187 failures, 1 error - down from 188 failures before this round, a reduction of exactly 1. No other test in the suite changed status in either direction.

### FITS verification

Both runs used reference set `acip-4.6-sd-4.65-fits-222cc1c7` (4896 fixtures), verified by `ReferenceSetVerifier`. `mvn -pl cdsi-engine install -DskipTests` was run before the post-fix FITS run so `cdsi-fits-tests` resolved the freshly-built jar.

| | Before | After |
| --- | --- | --- |
| Executed | 4896 | 4896 |
| Passed | 3364 | 3364 |
| Failed assertions | 1531 | 1531 |
| Execution errors | 1 | 1 |

`changed-cases.json` for the after-run reports `added: []`, `removed: []`, `statusChanged: []` against the immediately preceding run. Strict no-op on this fixture set - none of the 4896 FITS fixtures happens to administer a dose against one of the 5 affected Pneumococcal target doses before its absolute minimum age, so this crash was never actually reachable through the current fixture set, only through the unit test built directly against the bundled Supporting Data's own markup.

## Affected

- Spec sections: 6.4 (page 65)
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.EvaluateAge`
- FITS cases: none - the crash is real against the bundled Supporting Data but not reachable by any of the 4896 bundled FITS fixtures (see FITS verification above)
