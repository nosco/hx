# Changelog

## [0.3.3] - Mar 08, 2019

### Added

- `:provider` component and `hx.react/create-context` function

### Fixed

- Race condition in `<-deref` hook
- Props with numbers, two-letter camel-case were dropped or truncated


## [0.3.2] - Feb 25, 2019

### Fixed

- `<-state`: `swap!` works on a `<-state` atom inside of `<-effect`
- `className` prop is available as either `:class` or `:class-name` when 
parsing props in `defnc` to provide parity with hiccup parser's conversion of
`:class` prop to `className`
- namespaced keywords are now preserved in props maps

## [0.3.1] - Feb 20, 2019

### Fixed

- `<-ref`: Pass initial value to `react/useRef`

## [0.3.0] - Feb 10, 2019

### Changed

- Moved `hx.react.hooks` to `hx.hooks` (breaking)
- `<-deref`: no longer takes a `deps` argument, but will automatically re-watch
if atom argument changes (breaking)
- `<-deref`: Fix bug where two calls on the same atom didn't work as expected
- `<-effect`: Convert second argument `deps` from CLJS collection to array
- `<-ref`: Have deref return the `current` value in React ref (breaking)

### Added

- Additional core hooks:
  - `<-memo`
  - `<-callback`
  - `<-imperative-handle`
  - `<-layout-effect`
  - `<-debug-value`
