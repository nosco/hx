# hx to UIx Migration Plan

## Quick Reference for AI Agents

### Running Tests

```bash
# Compile and run all tests (requires Node.js 20+)
cd /Users/orestis/dev/nosco/hx
npx shadow-cljs compile ci && npx karma start --single-run

# Watch mode for development
npx shadow-cljs watch ci
npx karma start  # in another terminal

# Run clj-kondo linting
clj-kondo --lint src test examples
```

### Key Files

| File                          | Purpose                                 |
| ----------------------------- | --------------------------------------- |
| `src/hx/react.clj`            | Main macro file (defnc, defnc-)         |
| `src/hx/react.cljs`           | Runtime implementation                  |
| `src/hx/hiccup.cljc`          | Hiccup parsing (parse-body)             |
| `test/hx/react_test.cljs`     | Core hx functionality tests             |
| `test/hx/migration_test.cljs` | UIx interop tests (4 expected failures) |
| `test/hx/realworld_test.cljs` | nosco-gamma patterns                    |
| `test/hx/uix_smoke_test.cljs` | UIx API verification                    |
| `.clj-kondo/hooks/hx.clj`     | Custom defnc linting hook               |

### Expected Test Results

- **55 passing** — existing hx tests + UIx smoke tests + realworld patterns + most migration tests
- **1 failing** (expected) — `ref-forwarding-new-pattern`: requires defnc → defui rewrite

### Dependencies

- **deps.edn**: UIx, shadow-cljs, dev dependencies via `:dev` alias
- **package.json**: React 18.2.0, @testing-library/react 14.x, Karma with Playwright
- **.tool-versions**: Node.js 20.18.0 (for mise/asdf)

### clj-kondo Hooks

Custom `defnc` hook validates:

- First arg must be map destructuring `{:keys [...]}` OR named `props`
- Second arg (if present) must be named `ref`

---

## Goal

Reimplement `hx.react/defnc` to emit UIx components (`defui`) under the hood while maintaining 100% backward compatibility with existing downstream code. The public API of `hx.react` remains unchanged.

## Architecture Decision

**`defnc` emits UIx components with runtime hiccup parsing.**

Key points:

1. **`defnc` → `defui`**: The `defnc` macro emits `uix.core/defui` under the hood
2. **Runtime hiccup preserved**: Body is still parsed at runtime via `parse-body`, but element creation uses UIx's `$` instead of `react/createElement`
3. **UIx props handling**: No separate hx props layer — use UIx's props handling directly
4. **`hx/f` kept**: For runtime/dynamic hiccup, using `$` underneath
5. **Macro-level features**: `:wrap`, `:pre`, `:post` handled at `defnc` macro level (wrapping the emitted `defui`)

This means:

- All `defnc` components ARE UIx components
- Props flow through UIx's system (`:children` in map, `.-argv` storage)
- Interop "just works" because there's only one component type
- Runtime hiccup parsing preserved for backward compatibility with dynamic hiccup

## Key Constraints

1. **No downstream import changes** — `hx.react` namespace exports the same API
2. **Runtime hiccup parsing preserved** — keep `parse-body` but use `$` for element creation
3. **Both ref patterns work** — old `[props ref]` two-argument AND new `:ref` key pattern
4. **Context, defcomponent, hooks unchanged** — these continue working as-is
5. **`hx/f` preserved** — runtime hiccup helper kept working

## Test Strategy

### Phase 1: Baseline (tests should pass)

1. **Existing hx tests** ([test/hx/react_test.cljs](../test/hx/react_test.cljs))

   - `create-element` — hiccup parsing, fragments, providers, functions
   - `style-prop` — style object conversion
   - `class-prop` — class/className handling
   - `for-prop` — for/htmlFor handling
   - `on-click-prop`, `input-on-change-prop` — event handlers

2. **New UIx smoke tests** (new file: `test/hx/uix_smoke_test.cljs`)
   - Verify we understand UIx's API correctly
   - Basic `defui` component rendering
   - Props handling (`:class`, `:style`, `:on-click`)
   - Children via `:children` key
   - Fragment syntax `:<>`
   - Context with `defcontext`
   - `use-state`, `use-effect`, `use-ref` hooks
   - Interop with JS React components

### Phase 2: Migration tests (start failing, then fix)

3. **Migration compatibility tests** (new file: `test/hx/migration_test.cljs`)

   **Simple interop:**

   - UIx component rendered inside hx/defnc component
   - hx/defnc component rendered inside UIx component
   - Mixed tree: UIx → hx → UIx nesting

   **Props passing:**

   - hx component passing props to UIx child
   - UIx component passing props to hx child
   - Children semantics (hx args vs UIx `:children` key)

   **Context sharing:**

   - Context created with `hx/create-context`, consumed in UIx component
   - Context created with `uix/defcontext`, consumed in hx component

   **Ref forwarding:**

   - Old pattern: `(defnc Comp [{:keys [x]} ref] ...)`
   - New pattern: `(defnc Comp [{:keys [x ref]}] ...)`
   - Both patterns working with UIx components as parents/children

4. **Feature parity tests** (new file: `test/hx/feature_parity_test.cljs`)
   - `:wrap [memo]` option
   - `:pre` / `:post` conditions
   - `extend-tag` custom tags
   - JS interop (native DOM, third-party React components)
   - Error boundaries with `defcomponent`

### Phase 3: Real-world patterns (from nosco-gamma)

5. **Real-world usage patterns** (new file: `test/hx/realworld_test.cljs`)

   Based on nosco-gamma codebase analysis:

   ```clojure
   ;; Pattern: Simple component with children
   (defnc ActionListSection [{:keys [children]}]
     [:div {:class ["nosco-action-list-section"]}
      children])

   ;; Pattern: Component with hooks
   (defnc EvaluatorComments [{:keys [value row]}]
     (let [[showing set-showing] (hooks/useState false)]
       [:<>
        (when showing [Popup ...])
        [Button {:on-click #(set-showing true)}]]))

   ;; Pattern: Context usage
   (def CloseContext (React/createContext (fn [e] ...)))

   ;; Pattern: hx/f for dynamic hiccup
   [React/Suspense {:fallback (hx/f [:div "Loading..."])}
    [Component]]

   ;; Pattern: Props merging with nosco.react/merge-props
   [:div (nosco.react/merge-props
          {:class ["base-class"]}
          (dissoc props :some-key))]

   ;; Pattern: Forward ref (implicit via second arg)
   ;; Need to verify this still works
   ```

### Test Infrastructure

**Karma + Playwright** (following nosco-gamma pattern):

```javascript
// karma.conf.js additions
process.env.CHROMIUM_BIN = require("playwright").chromium.executablePath();
process.env.FIREFOX_BIN = require("playwright").firefox.executablePath();
process.env.WEBKIT_HEADLESS_BIN = require("playwright").webkit.executablePath();

module.exports = function (config) {
  config.set({
    browsers: ["ChromiumHeadless", "FirefoxHeadless", "WebkitHeadless"],
    // ...
  });
};
```

## Implementation Steps

### Step 1: Add UIx dependency ✅

```clojure
;; deps.edn
{:deps {com.pitch/uix.core {:mvn/version "1.4.8"}}
 :aliases
 {:dev {:extra-paths ["test" "examples" "benchmark"]
        :extra-deps {thheller/shadow-cljs {:mvn/version "2.28.20"}
                     binaryage/devtools {:mvn/version "0.9.7"}
                     devcards/devcards {:mvn/version "0.2.5"}
                     reagent/reagent {:mvn/version "0.8.1"}}}}}

;; shadow-cljs.edn uses deps.edn
{:deps {:aliases [:dev]}
 ...}
```

### Step 2: Upgrade to React 18 ✅

Updated package.json:

- `react`: `^18.2.0`
- `react-dom`: `^18.2.0`
- `@testing-library/react`: `^14.0.0` (was react-testing-library)
- Removed deprecated `@material-ui/core`

Key finding: React 18 CSS variable handling changed. Style tests needed adjustment from checking the style object to checking computed CSS values.

### Step 3: Create test files ✅

Created:

1. [test/hx/uix_smoke_test.cljs](../test/hx/uix_smoke_test.cljs) — UIx API verification
2. [test/hx/migration_test.cljs](../test/hx/migration_test.cljs) — interop tests (4 expected failures)
3. [test/hx/realworld_test.cljs](../test/hx/realworld_test.cljs) — real patterns from nosco-gamma
4. [test/hx/defnc_lint_test.cljs](../test/hx/defnc_lint_test.cljs) — clj-kondo hook validation

Updated:

- [package.json](../package.json) — React 18, @testing-library/react, playwright, karma launchers
- [karma.conf.js](../karma.conf.js) — playwright browser support (Chromium, Firefox, WebKit)
- [test/hx/react_test.cljs](../test/hx/react_test.cljs) — updated imports, React 18 style handling
- [deps.edn](../deps.edn) — UIx dependency, shadow-cljs, dev alias
- [shadow-cljs.edn](../shadow-cljs.edn) — use deps.edn via `:deps {:aliases [:dev]}`

### Step 4: Set up clj-kondo ✅

- Imported UIx clj-kondo configs via deps.edn
- Created custom hook for `defnc` validation in `.clj-kondo/hooks/hx.clj`
- Validates first arg is map destructuring or `props`
- Validates second arg (if present) is named `ref`

### Step 5: Make UIx components work in hx hiccup ✅

**Problem:** When hx's `create-element` encounters a UIx component, it passes props as a plain JS object. But UIx expects props under `.-argv`.

**Solution:** Added UIx component detection in `src/hx/react.cljs`:

```clojure
(defn- uix-component? [el]
  (and (fn? el) (true? (.-uix-component? ^js el))))

(defn- create-uix-element [el props-map children config]
  ;; Parse children through hiccup, merge as :children in argv
  (let [parsed-children (mapv #(hiccup/-as-element % config) children)
        argv (if (seq parsed-children)
               (assoc props-map :children ...)
               props-map)]
    (react/createElement el #js {:argv argv})))
```

**Tests now passing:**

- `uix-inside-hx` ✅
- `mixed-nesting` ✅
- `hooks-work-side-by-side` ✅
- `children-hx-to-uix` ✅
- All context sharing tests ✅

### Step 6: Rewrite defnc to emit defui (TODO)

Modify `src/hx/react.clj` to emit `defui`:

```clojure
(defmacro defnc [name & args]
  ;; Parse args to extract: docstring?, props-binding, opts-map?, body
  ;; Handle :wrap, :pre, :post at macro level
  ;; Emit:
  `(uix.core/defui ~name [{:keys [...] :as ~'props}]
     ;; Handle old [props ref] pattern: extract :ref from props
     ;; Wrap body with parse-body for runtime hiccup
     (hx.react/parse-body (do ~@body))))
```

Key transformations:

- Old `[{:keys [x]} ref]` → `[{:keys [x ref]}]` (ref comes from UIx props)
- `:wrap [memo]` → wrap the defui with `(react/memo ...)`
- `:pre/:post` → wrap body with assertions

### Step 7: Update hx/f to use $ (TODO)

Keep `hx/f` working for runtime hiccup:

```clojure
(defn f [hiccup]
  ;; Same runtime parsing, but make-element uses $ underneath
  (parse hiccup))
```

```

## Success Criteria

### Iteration 1 ✅ COMPLETE

- [x] Existing hx tests pass (all 23 tests)
- [x] UIx smoke tests pass (17 tests)
- [x] Real-world pattern tests pass (12 tests)

### Iteration 2 (IN PROGRESS)

**Goal: Simplest defnc working with UIx**

- [x] UIx components work inside hx hiccup (via `create-uix-element`)
- [x] Props passed correctly
- [x] Children work (passed as `:children` in props)
- [ ] Basic `defnc` emits `defui` and renders
- [ ] Simple hiccup body renders via runtime parsing with `$`

### Iteration 3

**Goal: Full defnc feature parity**

- [ ] `:wrap [memo]` option works
- [ ] `:pre` / `:post` conditions work
- [ ] Old `[props ref]` two-arg pattern works (ref extracted from UIx props)
- [ ] `hx/f` works for runtime hiccup

### Iteration 4

**Goal: All tests pass**

- [ ] All migration tests pass (currently 4 expected failures)
- [ ] All existing hx tests still pass
- [ ] `extend-tag` custom tags work (if keeping this feature)

### Iteration 4

- [ ] nosco-gamma compiles with new hx
- [ ] nosco-gamma tests pass
- [ ] Manual QA of key UI flows

## Findings & Notes

### Architecture: How It Works

```

┌─────────────────────────────────────────────────────────┐
│ User code │
│ (defnc MyComp [{:keys [name]}] │
│ [:div {:class "foo"} name]) │
└─────────────────────────────────────────────────────────┘
│
▼ defnc macro
┌─────────────────────────────────────────────────────────┐
│ Emitted code │
│ (uix.core/defui MyComp [{:keys [name]}] │
│ (hx.react/parse-body │
│ [:div {:class "foo"} name])) │
└─────────────────────────────────────────────────────────┘
│
▼ runtime (parse-body)
┌─────────────────────────────────────────────────────────┐
│ Element creation │
│ ($ :div {:class "foo"} name) │
└─────────────────────────────────────────────────────────┘

````

- `defnc` is a macro that emits `defui`
- `parse-body` walks the hiccup at runtime
- Element creation uses UIx's `$` (not `react/createElement`)
- Props are UIx props (`:children` in map, no separate conversion)

### Props Flow

**UIx props structure:**
```clojure
;; React props object for UIx component:
#js {:argv {:prop1 val1 :children [child1 child2]}}

;; What component receives after UIx's glue-args:
{:prop1 val1 :children [child1 child2]}
````

**hx code using props:**

```clojure
(defnc MyComp [{:keys [label children]}]
  [:div label children])  ;; children is now in the map
```

### React 18 Changes

1. **CSS variables in style prop**: React 18 handles CSS custom properties differently. Tests checking `(.-style el)` for CSS variables need to use `getComputedStyle` or check the actual CSS value.

2. **Strict Mode**: React 18's Strict Mode renders components twice in development. May affect tests that count renders.

3. **createRoot API**: React 18 uses `createRoot` instead of `ReactDOM.render`. @testing-library/react 14.x handles this automatically.

### UIx Specifics

1. **UIx version**: Using `com.pitch/uix.core "1.4.8"` (not `io.pitch`)

2. **clj-kondo configs**: UIx provides clj-kondo configs that can be imported. Added to deps.edn and imported via `clj-kondo --copy-configs`.

3. **Compile-time hiccup**: UIx's `$` macro transforms hiccup at compile time. The `from-hiccup` function is for development/tooling only.

### Build Configuration

1. **shadow-cljs with deps.edn**: Use `:deps {:aliases [:dev]}` in shadow-cljs.edn to pull dependencies from deps.edn instead of duplicating them.

2. **Node.js version**: Node.js 20+ required. Pin via `.tool-versions` for mise/asdf.

---

## Risk Areas

1. **Children semantics change** — Old hx passed children as positional args. New approach uses `:children` in props map. Downstream code using `(defnc Comp [{:keys [x]}] ...)` with implicit children may need `{:keys [x children]}`.

2. **extend-tag registry** — This hx-specific runtime dispatch may need rework to call `$` correctly.

3. **Props object differences** — UIx stores props in `.-argv`. Code that directly accesses JS props objects may break.

4. **`hx/f` return type** — Must return React elements compatible with both hx and UIx component trees.

## Open Questions (RESOLVED)

1. ~~Should we try to make `defnc` emit UIx's `defui` under the hood?~~ **YES** — defnc emits defui.

2. ~~For the children semantic difference?~~ **Use UIx's pattern** — children in props map via `:children` key.

3. ~~How do we handle `hx/f`?~~ **Keep it** — runtime hiccup using `$` underneath.
