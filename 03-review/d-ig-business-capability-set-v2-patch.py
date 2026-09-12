#!/usr/bin/env python3
"""Generate the physical D-IG Capability Set v2 from the untouched v1 YAML.

This is intentionally a byte-preserving, assertion-heavy patch operation.
It does not invent capability evidence. It fails closed if the expected v1
structure is not found exactly once.

Usage:
  python d-ig-business-capability-set-v2-patch.py \
    03-review/d-ig-business-capability-set-001.yaml \
    03-review/d-ig-business-capability-set-002.yaml

Requires PyYAML. The script writes the patched YAML only after all assertions
pass. The original v1 file is never modified.
"""

from __future__ import annotations

import copy
import sys
from pathlib import Path

import yaml


EXPECTED_IMPLEMENTED_V1 = 32
EXPECTED_IMPLEMENTED_V2 = 37
EXPECTED_UNKNOWN_BOUNDARIES_V2 = 7


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def main() -> int:
    if len(sys.argv) != 3:
        print(f"usage: {sys.argv[0]} INPUT_V1 OUTPUT_V2", file=sys.stderr)
        return 2

    src = Path(sys.argv[1])
    dst = Path(sys.argv[2])
    original = src.read_text(encoding="utf-8")
    data = yaml.safe_load(original)

    metadata = data["capability_set_metadata"]
    capabilities = data["capabilities"]
    require(len(capabilities) == EXPECTED_IMPLEMENTED_V1,
            f"expected 32 v1 capabilities, found {len(capabilities)}")

    ids = [c["capability_id"] for c in capabilities]
    require(len(ids) == len(set(ids)), "duplicate capability IDs in v1")

    by_id = {c["capability_id"]: c for c in capabilities}
    for required_id in ("CAP-IMPL-026", "CAP-IMPL-030", "CAP-IMPL-031"):
        require(required_id in by_id, f"missing required source capability {required_id}")

    # Preserve all unrelated capability evidence exactly at the YAML-object level.
    untouched = copy.deepcopy(by_id)

    replacements = {
        "CAP-IMPL-026": [
            ("CAP-IMPL-026A", "Record receipts"),
            ("CAP-IMPL-026B", "Record fund flows"),
        ],
        "CAP-IMPL-030": [
            ("CAP-IMPL-030A", "Maintain employee attendance records"),
            ("CAP-IMPL-030B", "Process employee salary records"),
        ],
        "CAP-IMPL-031": [
            ("CAP-IMPL-031A", "Maintain member records"),
            ("CAP-IMPL-031B", "Manage member levels"),
            ("CAP-IMPL-031C", "Manage member coupon operations"),
            ("CAP-IMPL-031D", "Process member recharge operations"),
        ],
    }

    new_caps = []
    for cap in capabilities:
        cid = cap["capability_id"]
        if cid not in replacements:
            new_caps.append(cap)
            continue
        for new_id, new_name in replacements[cid]:
            item = copy.deepcopy(cap)
            item["capability_id"] = new_id
            item["capability_name"] = new_name
            item["source_status"] = "IMPLEMENTED"
            item["notes"] = (
                f"V2 split from {cid} under D-IG Business Capability Set Review-001. "
                "The split is a capability-boundary remediation only and does not imply an Identity conclusion."
            )
            item["lineage"] = {
                "derived_from": cid,
                "review_reference": "D-IG-CAPSET-REVIEW-001 §2",
            }
            new_caps.append(item)

    require(len(new_caps) == EXPECTED_IMPLEMENTED_V2,
            f"expected 37 v2 capabilities, found {len(new_caps)}")

    # Remove UNKNOWN-008 only; keep all other unknown boundaries untouched.
    if "unknown_boundaries" in data:
        before = len(data["unknown_boundaries"])
        data["unknown_boundaries"] = [
            u for u in data["unknown_boundaries"]
            if u.get("unknown_id") != "UNKNOWN-008"
        ]
        require(before == 8, f"expected 8 v1 unknown boundaries, found {before}")
        require(len(data["unknown_boundaries"]) == EXPECTED_UNKNOWN_BOUNDARIES_V2,
                "UNKNOWN-008 was not the only removed boundary")

    metadata["status"] = "OPEN"
    metadata["establishment_status"] = "REVISION_V2_GENERATED_PENDING_ADVERSARIAL_REVIEW"
    metadata["implemented_capability_count"] = EXPECTED_IMPLEMENTED_V2
    metadata["unknown_boundary_count"] = EXPECTED_UNKNOWN_BOUNDARIES_V2
    metadata["completeness_review_status"] = "PENDING"
    metadata["pre_screen_authorization"] = "BLOCKED"
    metadata["owner_review_status"] = "PENDING"
    metadata["identity_decision"] = "NOT_MADE"
    metadata["h1_h2_decision"] = "NOT_MADE"
    metadata["schema_authorization"] = "NO"
    metadata["migration_authorization"] = "NO"

    data["capabilities"] = new_caps
    data["unknown_capability_checks"] = [
        {
            "check_id": "UCC-001",
            "capability_name": "Recipe",
            "status": "UNKNOWN_REQUIRES_BEHAVIOR_PATH",
            "evidence_note": "dish-recipes API is observed, but an active business behavior path is not sufficiently established in the current evidence package.",
        },
        {
            "check_id": "UCC-002",
            "capability_name": "Pricing",
            "status": "UNKNOWN_REQUIRES_OWNER_AND_BEHAVIOR_EVIDENCE",
            "evidence_note": "Owner pricing rules are unavailable and no sufficient active pricing behavior path is established.",
        },
        {
            "check_id": "UCC-003",
            "capability_name": "Inventory Count / Adjustment",
            "status": "UNKNOWN_REQUIRES_BEHAVIOR_PATH",
            "evidence_note": "Inventory-count UI evidence exists, but no sufficiently locatable business behavior path is established.",
        },
        {
            "check_id": "UCC-004",
            "capability_name": "Inventory Transaction Query / Trace",
            "status": "UNKNOWN_REQUIRES_BEHAVIOR_PATH",
            "evidence_note": "Inventory transaction data exists, but an independent query/trace behavior path is not sufficiently evidenced.",
        },
    ]
    data["pending_capability_checks"] = [
        {
            "check_id": "PCC-001",
            "capability_name": "Member points / loyalty points",
            "status": "EVIDENCE_CHECK_REQUIRED",
            "counted_as_implemented": False,
            "rule": "Add as IMPLEMENTED only if an independent business behavior path is evidenced; otherwise do not register it as a capability.",
        }
    ]

    data["logical_v2_reference"] = {
        "status": "ESTABLISHED_FROM_V1_PLUS_REVIEW_001",
        "review_reference": "03-review/d-ig-business-capability-set-review-001.md",
        "logical_record": "03-review/d-ig-business-capability-set-v2-logical-001.md",
        "adversarial_review_required": True,
        "owner_review_blocked_until_adversarial_pass": True,
    }

    # Re-assert that no unrelated source capability disappeared.
    final_ids = {c["capability_id"] for c in new_caps}
    for cid in untouched:
        if cid not in replacements:
            require(cid in final_ids, f"unrelated v1 capability disappeared: {cid}")

    dst.write_text(yaml.safe_dump(data, allow_unicode=True, sort_keys=False), encoding="utf-8")
    print(f"wrote {dst} with {len(new_caps)} IMPLEMENTED capabilities and {len(data.get('unknown_boundaries', []))} UNKNOWN boundaries")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
