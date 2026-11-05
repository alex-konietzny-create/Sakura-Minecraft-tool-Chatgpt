# 0.6.3: no fire attacks

All automatic fire-extinguishing attack inputs have been removed from Anchor routines. Anchor placement continues to accept normal and soul fire as replaceable target cells and uses only native Use input. If fire obscures a subsequent charge ray, Safe/Double camera reacquisition must find an actually visible face; it never attacks fire to clear it.

Double's optional native Anchor click series has a maximum of 20 attempts, including the first blast input. It is active only while waiting for the first blast's confirmed removal with Anchor held. Pulses do not overlap. On confirmation, the normal second placement and charging sequence takes over and the final hotbar slot is used. The block interaction guard still suppresses duplicate uses, wrong target cells and unintended adjacent placement; the series is not 20 guaranteed server interactions.

This does not implement unsupported air placement. Native Minecraft clicks cannot place a block in empty air without an eligible clicked/supporting surface. The second target stays fixed, and destroyed or invisible support may still prevent placement. No action packets or fake hit results are constructed.

Validation for this release: build, deterministic burst-budget checks and existing state/input/geometry regressions, plus actual-client UI smoke. The successful 0.6.2 gameplay tests are documented separately and are not claimed as a new gameplay test of 0.6.3.
