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

- **52 passing** — existing hx tests + UIx smoke tests + realworld patterns
- **4 failing** (expected) — migration tests for UIx interop not yet implemented:
  - `uix-inside-hx` — UIx components inside hx not wired up
  - `mixed-nesting` — Props flow through UIx→hx→UIx
  - `ref-forwarding-new-pattern` — `:ref` prop pattern
  - `hooks-work-side-by-side` — UIx hook state rendering

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

Reimplement `hx.react/defnc` to use UIx under the hood while maintaining 100% backward compatibility with existing downstream code. The public API of `hx.react` remains unchanged.

## Key Constraints

1. **No downstream import changes** — `hx.react` namespace exports the same API
2. **Runtime hiccup parsing preserved** — use UIx's `from-hiccup` (dev tool) or keep hx's `parse-body` for compatibility
3. **Both ref patterns work** — old `[props ref]` two-argument AND new `:ref` key pattern
4. **Context, defcomponent, hooks unchanged** — these continue working as-is
5. **Mix-and-match with native UIx** — components written with hx/defnc and uix/defui should interoperate seamlessly

## Architecture Decision

Since UIx's `from-hiccup` is **compile-time only** (not runtime), we cannot use it for full backward compatibility. UIx explicitly throws errors for hiccup vectors at runtime.

**Decision**: Keep hx's runtime hiccup parsing (`parse-body`) as the default. Add an optional `defnc-static` macro for opt-in compile-time transformation using UIx's approach.

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

### Step 5: Implement compatibility layer (TODO)

Create `src/hx/react/impl.cljs` (internal, not public):

```clojure
(ns hx.react.impl
  "Internal implementation bridging hx and UIx.
   NOT part of public API."
  (:require [uix.core :as uix]))

;; Bridge functions for the new defnc macro
```

### Step 6: Rewrite defnc macro (TODO)

Modify `src/hx/react.clj`:

```clojure
;; Option A: Wrap UIx's defui
;; - Use UIx's optimizations where possible
;; - Fall back to runtime hiccup parsing for body

;; Option B: Keep current implementation
;; - Just ensure interop works via shared React primitives
;; - Add UIx as peer dependency for mixing components
```

### Step 7: Add defnc-static (opt-in performance) (TODO)

```clojure
(defmacro defnc-static
  "Like defnc but transforms hiccup to $ calls at compile time.
   Faster but doesn't support extend-tag or dynamic hiccup."
  [name & body]
  ;; Use uix.dev/from-hiccup to transform body
  )
```

## Success Criteria

### Iteration 1 ✅ COMPLETE

- [x] Existing hx tests pass (all 23 tests)
- [x] UIx smoke tests pass (17 tests)
- [x] Real-world pattern tests pass (12 tests)

### Iteration 2 (IN PROGRESS)

- [ ] UIx component inside hx component renders correctly
- [ ] Mixed nesting works (UIx → hx → UIx)
- [ ] Both ref patterns work (old `[props ref]` and new `:ref` key)
- [ ] Hooks work in sibling UIx/hx components

### Iteration 3

- [ ] Feature parity tests pass
- [ ] Real-world pattern tests pass

### Iteration 4

- [ ] nosco-gamma compiles with new hx
- [ ] nosco-gamma tests pass
- [ ] Manual QA of key UI flows

## Findings & Notes

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

1. **Children semantics** — hx passes children as args, UIx uses `:children` key. The `defnc` wrapper must handle both.

2. **Props conversion timing** — hx converts props at component boundary, UIx does it at compile time. May cause subtle differences.

3. **Context API differences** — hx uses `[:provider {:context ctx :value v}]`, UIx uses `($ ctx {:value v})`. Need adapter.

4. **extend-tag registry** — This is hx-specific runtime dispatch. Won't work with `defnc-static`. Document limitation.

5. **Hook naming** — hx uses `useState` (camelCase), UIx uses `use-state` (kebab). Both should work since they wrap same React hooks.

## Open Questions

1. Should we try to make `defnc` emit UIx's `defui` under the hood, or keep the implementations separate and just ensure interop?

2. For the children semantic difference, should we:

   - Transform at macro level (extract children from UIx's `:children` into args)?
   - Document the difference and require explicit handling?
   - Support both patterns in the new defnc?

3. How do we handle `hx/f` (runtime hiccup parser) when UIx is the underlying renderer? Keep it as-is since it returns React elements?
