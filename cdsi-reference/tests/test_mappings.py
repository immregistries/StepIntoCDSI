"""Regression tests for Phase 8's spec-to-engine mapping validator. Run
against the real cdsi-engine source tree (sibling module) and the real
mappings/spec-to-code.yaml, so these catch spec-to-code.yaml drifting out
of sync with cdsi-engine (a renamed class, a new LogicStepType with no
mapping entry) as well as a regression in the parsing itself. Skipped if
cdsi-engine's source isn't present (e.g. someone checked out only
cdsi-reference standalone)."""

import pytest

from cdsi_reference_tools import engine_index, validate


def _engine_or_skip():
    path = engine_index.logic_package_dir() / "LogicStepType.java"
    if not path.exists():
        pytest.skip("cdsi-engine source is not present as a sibling module")
    return path


def test_parses_known_step_types():
    _engine_or_skip()
    types = {t.enum_name: t for t in engine_index.parse_logic_step_types()}
    assert types["EVALUATE_AGE"].section == "6.4"
    assert types["EVALUATE_GENDER"].section == "xx"
    assert types["FORECAST_DATES_AND_REASONS"].section == "7"
    assert "END" in types


def test_factory_covers_every_step_but_end():
    _engine_or_skip()
    types = engine_index.parse_logic_step_types()
    factory = engine_index.parse_logic_step_factory()
    missing = [t.enum_name for t in types if t.enum_name != "END" and t.enum_name not in factory]
    assert missing == []


def test_validate_mappings_has_no_unacknowledged_gaps():
    _engine_or_skip()
    assert validate.validate_mappings("4.6") == []


def test_evaluate_gender_and_forecast_dates_are_acknowledged_not_silent():
    _engine_or_skip()
    gaps = "\n".join(validate.acknowledged_gaps("4.6"))
    assert "EVALUATE_GENDER" in gaps
    assert "FORECAST_DATES_AND_REASONS" in gaps
