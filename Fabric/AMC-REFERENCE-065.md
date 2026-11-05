# User-provided Double Anchor reference

The supplied `double anchor.amc` describes two cycles of Anchor-slot key, Use click, Glowstone-slot key, Use click, final-item slot key, Use click. Its description assigns Anchor to slot 4, Glowstone to slot 5 and a sword/other final item to slot 3. Every native down/up transition has a 24 ms delay in the reference. No executable file or external program is imported.

Double now defaults to the reference activation order: both blasts use the selected final slot. The previous first-blast-with-Anchor behavior remains available by turning off `AMC-Reihenfolge` in Behavior. Extra click bursts are not active in the AMC sequence.

The Timing tab's AMC profile sets per-Double pauses, slot preparation and pulse length to 24 ms without altering any of the user's Anchor/Glowstone/final slot numbers or the normal Anchor input pulse. Its pulse can be edited independently in Double's Input tab. Other profiles remain available, including zero-added-wait Turbo.

The implementation still observes actual slots, target validity and server confirmations. Therefore timings are not promised to be identical to a fixed external script, and the reference does not provide unsupported air placement.
