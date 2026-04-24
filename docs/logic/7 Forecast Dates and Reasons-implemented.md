# Chapter 7: Forecast Dates and Reasons

## Summary
Entry point for forecasting phase after all target doses evaluated. Manages transition from evaluation to forecasting with cycle count monitoring to prevent excessive handoffs between phases.

## Methods
- `process(): LogicStep` — Checks forecast handoff cycle count (MAX_FORECAST_HANDOFF_CYCLES=1100); if exceeded forces SELECT_BEST_PATIENT_SERIES; otherwise transitions to EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST
- `printPre(PrintWriter): void` — Pre-processing output
- `printPost(PrintWriter): void` — Post-processing output
- `printTableAndFigure(PrintWriter): void` — Displays forecast process steps

## Cycle Guard
- MAX_FORECAST_HANDOFF_CYCLES: 1100
- Alerts on ALERT.LOOP_DETECTED if exceeded
- Forces safe exit to SELECT_BEST_PATIENT_SERIES

## Forecast Process Steps (from printTableAndFigure)
1. Section 7.1 → Evaluate Conditional Skip
2. Section 7.2 → Determine Evidence of Immunity
3. Section 7.3 → Determine Contraindications
4. Section 7.4 → Determine Forecast Need
5. Section 7.5 → Generate Forecast Dates
6. Section 7.6 → Validate Recommendation

## Key Data Structures
- `TargetDose` — Current target dose being forecast
- `Forecast` — Forecast being generated
- Cycle count tracking

## Next Step
→ 7.1 Evaluate Conditional Skip For Forecast

## Key Dependencies
- DataModel cycle count tracking
- Evaluation/forecast handoff management
