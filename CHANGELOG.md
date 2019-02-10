# Changelog

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
