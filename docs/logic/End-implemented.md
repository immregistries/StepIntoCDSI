# Chapter End: End

## Summary
Terminal logic step that concludes the evaluation and forecasting process. Generates final forecast output summary and prepares data for presentation to user.

## Methods
- `process(): LogicStep` — Returns null (terminal step)
- `printPre(PrintWriter): void` — Displays forecast results table with vaccine group details
- `printPost(PrintWriter): void` — [Post-processing output]

## Output Display
Displays table with columns:
- Antigen
- Target Dose
- VGF Status (Vaccine Group Forecast Status)
- Earliest Date
- Adjusted Recommended Date
- Adjusted Past Due Date
- Latest Date
- Unadjusted Recommended Date
- Unadjusted Past Due Date
- Forecast Reason

## Results Summary
- Vaccine group name (header)
- Best patient series list size
- Forecast list size (count of all forecasts generated)
- Detailed forecast table

## Key Data Structures
- `VaccineGroupForecast` — Final vaccine group forecasts
- `Forecast` — Individual antigen forecasts
- `PatientSeries` — Associated series for each forecast

## Error Checking
- Alerts if best patient series list is null
- Validates forecast list is populated

## Next Step
→ None (Process complete)

## Key Dependencies
- VaccineGroup
- VaccineGroupForecast list
- Forecast list
- PatientSeries results
