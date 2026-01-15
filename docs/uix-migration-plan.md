# hx to UIx Migration Plan

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
;; shadow-cljs.edn - added to :dependencies
[io.pitch/uix.core "1.2.0"]
```

### Step 2: Create test files ✅

Created:

1. [test/hx/uix_smoke_test.cljs](../test/hx/uix_smoke_test.cljs) — UIx API verification
2. [test/hx/migration_test.cljs](../test/hx/migration_test.cljs) — interop tests (initially failing)
3. [test/hx/realworld_test.cljs](../test/hx/realworld_test.cljs) — real patterns from nosco-gamma

Updated:

- [package.json](../package.json) — added `@testing-library/react`, playwright, karma launchers
- [karma.conf.js](../karma.conf.js) — added playwright browser support
- [test/hx/react_test.cljs](../test/hx/react_test.cljs) — updated to use `@testing-library/react`

### Step 3: Implement compatibility layer

Create `src/hx/react/impl.cljs` (internal, not public):

```clojure
(ns hx.react.impl
  "Internal implementation bridging hx and UIx.
   NOT part of public API."
  (:require [uix.core :as uix]))

;; Bridge functions for the new defnc macro
```

### Step 4: Rewrite defnc macro

Modify `src/hx/react.clj`:

```clojure
;; Option A: Wrap UIx's defui
;; - Use UIx's optimizations where possible
;; - Fall back to runtime hiccup parsing for body

;; Option B: Keep current implementation
;; - Just ensure interop works via shared React primitives
;; - Add UIx as peer dependency for mixing components
```

### Step 5: Add defnc-static (opt-in performance)

```clojure
(defmacro defnc-static
  "Like defnc but transforms hiccup to $ calls at compile time.
   Faster but doesn't support extend-tag or dynamic hiccup."
  [name & body]
  ;; Use uix.dev/from-hiccup to transform body
  )
```

## Success Criteria

### Iteration 1

- [ ] Existing hx tests pass
- [ ] UIx smoke tests pass
- [ ] Simplest migration test passes: UIx component inside hx component

### Iteration 2

- [ ] All migration tests pass
- [ ] Context sharing works both directions
- [ ] Both ref patterns work

### Iteration 3

- [ ] Feature parity tests pass
- [ ] Real-world pattern tests pass

### Iteration 4

- [ ] nosco-gamma compiles with new hx
- [ ] nosco-gamma tests pass
- [ ] Manual QA of key UI flows

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
