# SPEC-4.6-0004: EvaluatePreferableInterval's outcome 0 uses the wrong EvaluationReason

**Status:** open
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`EvaluatePreferableInterval`'s outcome 0 sets `EvaluationReason.GRACE_PERIOD`. Direct comparison with `EvaluateAllowableInterval`'s equivalent, correctly-implemented outcome, and the class's own log message, both indicate the reason for this outcome should be "Too Soon," not "Grace Period."

## Interpretation

This could produce a misleading evaluation reason wherever a dose fails the preferable-interval check - exactly the kind of transparency defect this reference module exists to surface. Tracked, not fixed, per standing project direction.

Separately unresolved in this pass (not part of this finding, recorded for completeness): whether business rules CALCDTINT-1/2/8/9 (which reference date an interval measures from) are implemented anywhere was not verified either way during this documentation pass.

## Affected

- Spec sections: 6.5 (pages 54-57)
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.EvaluatePreferableInterval`
- FITS cases: none identified in this pass
