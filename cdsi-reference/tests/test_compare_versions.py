"""Phase 10 isn't implemented (no second Logic Specification version
exists yet to test comparison logic against - see compare_versions.py's
module docstring for why that isn't done speculatively). These tests lock
in the one thing that *is* built: that the stub fails clearly, with the
full required-steps checklist embedded, rather than silently returning a
wrong or empty comparison."""

import pytest

from cdsi_reference_tools import compare_versions


def test_ten_steps_embedded_verbatim_from_the_plan():
    assert len(compare_versions.PHASE_10_STEPS) == 10


def test_compare_rejects_an_unregistered_version():
    with pytest.raises(compare_versions.NoSuchVersion):
        compare_versions.compare("4.6", "9.9-does-not-exist")


def test_compare_of_two_registered_versions_fails_clearly_not_silently():
    pytest.importorskip("pymupdf")
    from cdsi_reference_tools import paths

    if not paths.manifest_path("4.6").exists():
        pytest.skip("4.6 is not registered under logic-spec/versions/4.6/")
    with pytest.raises(NotImplementedError) as exc_info:
        compare_versions.compare("4.6", "4.6")
    assert "Phase 10" in str(exc_info.value)
    assert compare_versions.PHASE_10_STEPS[0] in str(exc_info.value)
