# Sakura 0.3.3 interface update

Uses static Sakura Grid font derivatives from OFL Figtree: 600 body, 750 headings; 12px glyph source with 4x oversampling. The existing font switch remains available.

Visible, frame-time based transitions: 320ms eased window entry; 300ms staggered card entry on page changes; sliding sidebar selection; interpolated button/card hover; 14/s switch track and thumb tween. Reduced motion snaps all transitions to final state.

Original Sakura branding and existing module behavior retained. No system-input prototype, license bypass or copied program assets.

Validation: Gradle build and existing state regressions; actual Minecraft UI smoke tests for key/mouse binding, numeric fields, saved settings, theme and detail navigation at three sizes. Live frame captures demonstrate menu entry and switching. These checks do not test gameplay.
