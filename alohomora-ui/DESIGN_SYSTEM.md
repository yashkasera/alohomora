# Alohomora Design System

The reference for building UI in the Alohomora console. `alohomora-ui` is the single source of
truth for Compose across the mobile console (`:alohomora`) and the desktop companion
(`:desktopApp`). Reach for a wrapper and a token before anything else — raw Material 3 and hardcoded
values are the exception, not the default.

- **Tokens:** `alohomora-ui/src/commonMain/kotlin/io/github/yashkasera/alohomora/ui/theme/`
- **Components:** `alohomora-ui/src/commonMain/kotlin/io/github/yashkasera/alohomora/ui/components/`
- **Icons:** `alohomora-ui/src/commonMain/kotlin/io/github/yashkasera/alohomora/ui/icons/`

---

## Principles

1. **Wrap, don't re-skin.** Use `Alohomora*` wrappers, not raw `androidx.compose.material3.*`. The
   wrappers carry the console's defaults (emphasis is `onBackground`/`background`, not `primary`);
   raw M3 renders in `primary` and drifts.
2. **Hierarchy by size, colour and tracking — never weight.** Only one `*-Regular` face ships per
   family. Asking for `FontWeight.Medium`/`Bold` gets a synthetic stroke-widen that differs per
   platform. Lean on the type scale, `onSurface` vs `onSurfaceVariant`, and the serif display face.
3. **Tokens everywhere.** Spacing, radius, stroke and colour come from `MaterialTheme.dimens`,
   `MaterialTheme.shapes`, `MaterialTheme.colorScheme` and `MaterialTheme.alohomoraColors`. A
   literal
   `16.dp` or `Color(0xFF…)` in a screen is a refactoring smell.
4. **Every component wraps in a theme.** Tokens resolve through composition locals;
   `alohomoraColors`
   throws if `AppTheme { }` is not an ancestor.

---

## Tokens

### Spacing — `MaterialTheme.dimens.margin`

| Token  | Value | Typical use                                             |
|--------|-------|---------------------------------------------------------|
| `xs`   | 4.dp  | chip/badge internal gap, label bottom gap               |
| `sm`   | 8.dp  | tight row gap, icon–label spacing                       |
| `md`   | 12.dp | compact section gap                                     |
| `lg`   | 16.dp | standard card / list-item padding                       |
| `xl`   | 20.dp | screen horizontal edge padding                          |
| `xxl`  | 24.dp | section container padding                               |
| `xxxl` | 32.dp | section separator, empty-state outer padding            |
| `huge` | 48.dp | tall section spacers                                    |
| `fab`  | 88.dp | trailing scroll space clearing a floating action button |

### Icon sizes — `MaterialTheme.dimens.icon`

| Token          | Value | Typical use                           |
|----------------|-------|---------------------------------------|
| `xs`           | 12.dp | tiny dot / indicator                  |
| `sm`           | 14.dp | metadata row icons (clock, drive)     |
| `md`           | 16.dp | small action icons, search-field icon |
| `lg`           | 20.dp | standard trailing / leading icons     |
| `standard`     | 24.dp | primary nav / toolbar icons           |
| `xl`           | 36.dp | empty-state icon glyph                |
| `illustration` | 80.dp | empty-state icon container            |

### Stroke — `MaterialTheme.dimens.stroke`

| Token    | Value  | Typical use                                  |
|----------|--------|----------------------------------------------|
| `thin`   | 0.5.dp | subtle dividers, list-item separators        |
| `small`  | 1.dp   | standard borders, field outlines             |
| `medium` | 2.dp   | emphasis borders (e.g. EmptyState icon ring) |

### Shapes / corners — `MaterialTheme.shapes`

Corner radii are `Shape`s, not `Dp`s, and live in `MaterialTheme.shapes`. **There is no separate
`corner` token** — a second scale under different names for the same radii was deleted. Use
`MaterialTheme.shapes.*` in any `shape =` / `.clip()` / `.border()`.

| Role         | Radius |
|--------------|--------|
| `extraSmall` | 4.dp   |
| `small`      | 8.dp   |
| `medium`     | 12.dp  |
| `large`      | 16.dp  |
| `extraLarge` | 28.dp  |

`AlohomoraBottomSheetShape` (top corners only, 4.dp) covers the one asymmetric case the symmetric
scale can't express.

### Typography — `MaterialTheme.typography`

Three bundled families, one `Regular` face each. Emphasis roles render identically to their base
(the console has one weight — reach for a larger size or a stronger colour role instead).

| M3 roles                | Family             |
|-------------------------|--------------------|
| `display*`, `headline*` | Instrument Serif   |
| `title*`                | Newsreader (serif) |
| `body*`, `label*`       | JetBrains Mono     |

`labelLarge` is tracked to `0.5.sp` to match its label siblings once its faux-Medium weight is gone.

### Colours

Two layers. Standard Material roles come from `MaterialTheme.colorScheme` (`primary`, `surface`,
`onSurface`, `onSurfaceVariant`, `outline`, `outlineVariant`, `scrim`, `surfaceContainer*`, …).

Semantic status colours come from the extension `MaterialTheme.alohomoraColors`:

| Token                          | Meaning                              |
|--------------------------------|--------------------------------------|
| `accent`                       | brand/interactive accent             |
| `success` / `successContainer` | success state + its low-opacity fill |
| `warning` / `warningContainer` | warning state + its low-opacity fill |
| `info`                         | informational state                  |
| `fatal`                        | fatal error / crash accent           |

`warningContainer` defaults to `warning` at 12% alpha (the `successContainer` recipe), so content
on it keeps the `onSurface`/`onSurfaceVariant` contract — never pair a solid `warning` fill with
`inverseOnSurface` text.

Also on the theme object: `id`, `displayName`, `isDark`, `materialColorScheme`.

### Themes

Five themes, each with a light and dark variant, in `AlohomoraThemes`:
`material`, `monochrome`, `dracula`, `nord`, `solarized`. Wrap UI in `AppTheme`:

```kotlin
AppTheme(themeId = "dracula", initialIsDark = true) {
    // content — colorScheme, typography, shapes, dimens and alohomoraColors are all in scope
}
```

`themeId` defaults to `"default"`, which is not a registered key and falls back to `material`.
`AlohomoraThemes.ids` lists the real keys; `forId(id, isDark)` resolves one.

---

## Component catalog

Every component lives in `…/ui/components/`. Signatures below show the primary overload; open the
file for the full parameter list and any content-slot overloads.

### Buttons — `AlohomoraButtons.kt`

`AlohomoraButtonDefaults` supplies `shape` (`shapes.small`), `uppercase` (true), `iconSpacing`,
`contentPadding(size)`, `minHeight(size): Dp`, `textStyle(size)`. Sizes: `AlohomoraButtonSize.{SMALL,
MEDIUM, LARGE}`.

```kotlin
AlohomoraFilledButton(text = "Save", onClick = ::save)
AlohomoraOutlinedButton(text = "Cancel", onClick = ::dismiss)
AlohomoraTextButton(text = "Learn more", onClick = ::open)

// Content-slot overload when `text` would be dead (custom row content):
AlohomoraFilledButton(onClick = ::send) {
    AlohomoraCircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
}
```

- `AlohomoraFilledButton` — primary action. Emphasis fill (`onBackground`/`background`).
- `AlohomoraOutlinedButton` — secondary action. Border gates on `enabled`.
- `AlohomoraTextButton` — low-emphasis action.
- `AlohomoraIconButton(onClick, …, style, shape, content)` — icon-only action. Styles:
  `AlohomoraIconButtonStyle.{DEFAULT, FILLED, OUTLINED, TONAL}`. The most-used wrapper (60+ sites);
  M3 handles its minimum touch target.

### Chips — `AlohomoraChips.kt`

`AlohomoraChipDefaults`: `shape` (`shapes.extraSmall`), `uppercase`, `contentPadding` (6×2),
`getContentColorFor(container)`.

```kotlin
AlohomoraChip(label = "GET")                                     // static label
AlohomoraChip(label = "12", containerColor = alohomoraColors.success)
AlohomoraFilterChip(label = "Errors", selected = on, onClick = ::toggle)
AlohomoraAssistChip(label = "Copy", onClick = ::copy)
```

- `AlohomoraChip` — non-interactive status/count label. `contentColor` resolves against non-M3
  container colours (so `alohomoraColors.*` work).
- `AlohomoraFilterChip` — selectable filter.
- `AlohomoraAssistChip` — optional inline action.

### Cards — `AlohomoraCards.kt`

`AlohomoraCardDefaults`: `shape` (`shapes.small`), `colors()` (container `surfaceContainerLow`),
`border`.

```kotlin
AlohomoraCard { /* content */ }                       // static
AlohomoraCard(onClick = ::open) { /* content */ }     // clickable list row
AlohomoraOutlinedCard { /* content */ }               // outlined
```

Default container is `surfaceContainerLow` (visible on a `surface` background — do not force
`surface`). No `contentPadding` param; pad inside.

### Text fields — `AlohomoraTextFields.kt`

```kotlin
AlohomoraTextField(
    value = url,
    onValueChange = { url = it },
    label = "URL",
    supportingText = error,
    isError = error != null,
)

AlohomoraSearchTextField(query = q, onQueryChange = { q = it })
```

- `AlohomoraTextField` — labelled field with `isError`/`supportingText`, focus-aware border, caret
  preserved across `StateFlow`-driven updates.
- `AlohomoraSearchTextField` — search field with leading search icon and a clearable trailing
  button.

### Toggles & tabs — `AlohomoraToggleGroups.kt`, `AlohomoraTabs.kt`

```kotlin
AlohomoraSingleChoiceToggleGroup(
    items = windows.map { AlohomoraToggleItem(it.name, it.label) },
    selectedId = selected.name,
    onSelectedIdChange = { onSelect(valueOf(it)) },
)

AlohomoraPrimaryTabRow(selectedTabIndex = index) {
    tabs.forEachIndexed { i, t -> AlohomoraTab(selected = i == index, onClick = { select(i) }, text = t.label) }
}
```

- `AlohomoraSingleChoiceToggleGroup` — spaced single-select toggle group (`AlohomoraToggleItem`).
  Note: this renders *spaced* items, not a connected segmented bar. For a connected bar with
  per-item corner shaping, raw M3 `SingleChoiceSegmentedButtonRow` is still the tool.
- `AlohomoraPrimaryTabRow` / `AlohomoraScrollableTabRow` + `AlohomoraTab` — tab bars.

### Dividers — `AlohomoraDividers.kt`

```kotlin
AlohomoraHorizontalDivider()   // defaults to outlineVariant, 1.dp — matches M3 DividerDefaults
```

### Progress — `AlohomoraProgressIndicators.kt`

```kotlin
AlohomoraCircularProgressIndicator()   // defaults color = onBackground (NOT M3's primary)
```

### Selection controls — `AlohomoraSelectionControls.kt`

`AlohomoraCheckbox`, `AlohomoraTriStateCheckbox`, `AlohomoraSwitch`, `AlohomoraRadioButton` —
standard
selection controls tuned to the console palette.

### Dialogs & sheets — `AlohomoraAlertDialog.kt`, `AlohomoraBottomSheetModal.kt`,

`AlohomoraConfirmationBottomSheet.kt`

```kotlin
AlohomoraAlertDialog(
    onDismissRequest = ::dismiss,
    title = "Delete rule?",
    confirmButton = { AlohomoraFilledButton(text = "Delete", onClick = ::confirm) },
    dismissButton = { AlohomoraTextButton(text = "Cancel", onClick = ::dismiss) },
) { Text("This cannot be undone.") }

AlohomoraBottomSheetModal(onDismissRequest = ::dismiss) { /* ColumnScope content */ }

ConfirmationBottomSheet(title = "…", message = "…", onConfirm = ::go, onDismiss = ::dismiss)
```

- `AlohomoraBottomSheetModal` — modal sheet with configurable `DragHandleConfig` and default padding
  (`AlohomoraBottomSheetDefaults`).
- `ConfirmationBottomSheet` — pre-built confirm/cancel sheet (`ConfirmationConfig`,
  `isDestructive`).

### Dropdowns — `AlohomoraDropdownMenu.kt`

```kotlin
AlohomoraDropdownMenu(expanded = open, onDismissRequest = ::close) {
    AlohomoraDropdownMenuItem(text = { Text("Export") }, onClick = ::export)
}
```

### Top bars — `AlohomoraTopBar.kt`, `AlohomoraAppBars.kt`

```kotlin
AlohomoraTopBar(title = "Traffic", subtitle = "42 requests", layout = TopBarLayout.START_ALIGNED)
```

- `AlohomoraTopBar` — the shared bar. `TopBarLayout.{CENTER_ALIGNED, START_ALIGNED}`, optional
  `subtitle`, `showDivider`, `navigationIcon`, `actions`.
- `AlohomoraLargeTopAppBar` — large collapsible bar with `scrollBehavior`
  (`AlohomoraTopAppBarDefaults`).

### Floating action buttons — `AlohomoraFloatingActionButton.kt`

`AlohomoraFloatingActionButton` and `AlohomoraExtendedFloatingActionButton` — emphasis-coloured
FABs.
Pair with `Modifier`-side `fabClearanceItem()` (below) so the last list row isn't covered.

### Feedback & display

- `EmptyState(icon, title, subtitle?, action?)` — `EmptyState.kt`. Icon ring + title + optional
  subtitle and action.
- `AlohomoraOverlay(scrimColor, content)` — `AlohomoraOverlays.kt`. Full-screen scrim
  (`colorScheme.scrim`).
- `AlohomoraTable(columns, rows, onCellEdit?)` — `AlohomoraTable.kt`. Scrollable data table with
  double-tap cell editing.
- `AlohomoraCodeBlock(content, accentBorder?, jsonPrettify?)` — `AlohomoraCodeBlock.kt`. Mono
  code/JSON block with optional error accent.
- `MethodBadge(method)` — `AlohomoraMethodBadge.kt`. HTTP-method badge.
- `ConnectionStatusDot(state)` — `ConnectionStatusDot.kt`. Animated status dot
  (`ConnectionDotState.{Connected, Disconnected, Reconnecting}`).
- `NeedsAttentionPager(items, …)` — `NeedsAttentionPager.kt`. Error/failed-traffic peek-scroll
  pager: solid `errorContainer`/`warningContainer` cards, `MaterialShapes` icon badges, page
  indicator below.
- `AlohomoraPageIndicator(pageCount, currentPage)` — `AlohomoraPageIndicator.kt`. Width-morphing
  page dots (active page stretches into a pill). Renders nothing for a single page.

### List helpers — `ScrollToTop.kt`, `FabClearance.kt`

- `FollowNewest(listState, itemCount)` — auto-scrolls a newest-first list to the top on new items.
- `BoxScope.ScrollToTopButton(...)` / `BoxScope.ScrollToBottomButton(...)` — floating scroll
  buttons.
- `LazyListScope.fabClearanceItem()` / `LazyGridScope.fabClearanceItem()` — trailing FAB clearance.

---

## Do / Don't

Reach for the wrapper. Raw M3 renders in the wrong emphasis colour and skips the console defaults.

| Don't (`androidx.compose.material3`)   | Do (`…ui.components`)                                 |
|----------------------------------------|-------------------------------------------------------|
| `IconButton`                           | `AlohomoraIconButton`                                 |
| `Button`                               | `AlohomoraFilledButton`                               |
| `OutlinedButton`                       | `AlohomoraOutlinedButton`                             |
| `TextButton`                           | `AlohomoraTextButton`                                 |
| `Badge`                                | `AlohomoraChip`                                       |
| `FilterChip` / `AssistChip`            | `AlohomoraFilterChip` / `AlohomoraAssistChip`         |
| `Card` / `OutlinedCard`                | `AlohomoraCard` / `AlohomoraOutlinedCard`             |
| `HorizontalDivider`                    | `AlohomoraHorizontalDivider`                          |
| `CircularProgressIndicator`            | `AlohomoraCircularProgressIndicator`                  |
| `Checkbox` / `TriStateCheckbox`        | `AlohomoraCheckbox` / `AlohomoraTriStateCheckbox`     |
| `Switch` / `RadioButton`               | `AlohomoraSwitch` / `AlohomoraRadioButton`            |
| `AlertDialog`                          | `AlohomoraAlertDialog`                                |
| `DropdownMenu` / `DropdownMenuItem`    | `AlohomoraDropdownMenu` / `AlohomoraDropdownMenuItem` |
| `FloatingActionButton`                 | `AlohomoraFloatingActionButton`                       |
| `TopAppBar` / `CenterAlignedTopAppBar` | `AlohomoraTopBar`                                     |

Legitimate raw-M3 exceptions: `Text`, `Icon`, `Scaffold`, `Surface` (primitives the theme already
styles), a modal `Card` that genuinely needs elevation, and `SingleChoiceSegmentedButtonRow` for a
connected segmented bar.

---

## Icons

Icons are hand-translated [Lucide](https://lucide.dev/icons/) paths as cached `ImageVector`s in
`…/ui/icons/` (~57 today). Each is a lazy extension `val` on the `Icons` object:

```kotlin
val Icons.MyIcon: ImageVector
    get() {
        if (_myIcon != null) return _myIcon!!
        _myIcon = ImageVector.Builder(
            name = "MyIcon", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
                moveTo(12f, 2f); lineTo(12f, 22f)
                // translate <path>/<circle> to moveTo/lineTo/arcToRelative…
            }
        }.build()
        return _myIcon!!
    }

private var _myIcon: ImageVector? = null
```

Size icons with `dimens.icon.*`; tint with a `colorScheme`/`alohomoraColors` role.

---

## Extending the design system

Adding a component, token or icon — keep it consistent:

- **Name it `Alohomora*`** and put it in `…/ui/components/`. Model it on the closest existing
  wrapper.
- **Add a `*Defaults` object** for shape, colours and padding rather than inlining literals,
  following
  `AlohomoraButtonDefaults` / `AlohomoraChipDefaults` / `AlohomoraCardDefaults`.
- **`modifier: Modifier = Modifier` is the first optional parameter.** Non-negotiable Compose API
  guideline; callers must be able to pad/size/position without a wrapper `Box`.
- **Add a content-slot overload** wherever a `text: String` would go dead once a caller passes
  custom
  content (see the button overloads).
- **Wrap, don't re-skin.** Default emphasis to `onBackground`/`background`, not `primary`. Default
  progress/indicators to `onBackground`.
- **Reuse tokens.** `dimens`, `shapes`, `colorScheme`, `alohomoraColors`. No literal `dp` or hex.
- **Add a `private @Preview`** at the bottom of the file, wrapped in `AppTheme { }`, so the IDE
  renders it. The function stays private, but its composable lambdas are hoisted by the Compose
  compiler into public `ComposableSingletons$…Kt` accessors that *do* land in the JVM API dump — so
  run `./gradlew :alohomora-ui:apiDump` after adding or removing one.
- **Test tags** for anything a Compose test asserts go through the `…/ui/testing/` module.
- **`alohomora-ui` is API-validated.** After any change to the public surface run:

  ```bash
  ./gradlew :alohomora-ui:apiDump
  ```

  and if the change touches the `Alohomora` object, keep `:alohomora`/`:alohomora-noop` parity
  (`./gradlew consumerParity`).
- **New icon:** translate the Lucide SVG to the `ImageVector.Builder` pattern above.

### Verify a change compiles on every target

```bash
./gradlew :alohomora-ui:compileKotlinJvm \
          :alohomora-ui:compileKotlinIosArm64 \
          :alohomora-ui:compileDebugKotlinAndroid
./gradlew :alohomora-ui:apiCheck
```
