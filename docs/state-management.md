# State management

`hx` fully embraces vanilla React development; it does not provide any 
additional functionality than the core React library.

However, React already provides a rich API for doing state management and other
common effects in a UI application: [React Hooks](https://reactjs.org/docs/hooks-overview.html).

React Hooks are a powerful system for representing side effects we want to occur
while our application is operating such as changing state, firing network
requests, subscribing to external sources, etc.

`hx` provides a small helper library called `hx.hooks` that offers a
Clojure-first API to the React Hooks API.

You may also leverage any Hooks libraries that you may find, since `hx` 
components are pure React components that are compatible with all of the 
external React ecosystem.

## Examples:
 - [workshop.sortable](../examples/workshop/sortable.cljs): An example using the
 "react-sortable-hoc" library and React Hooks.

## hx.hooks

The idiom that this library provides is: any Hook starts with `<-` 
(instead of `use`) to provide at-a-glance recognition of what Hooks a component
uses.

Anything missing from here can also be accessed via the React library, e.g.:
`(react/useMemo)`.

All of the [Rules of Hooks](https://reactjs.org/docs/hooks-overview.html#%EF%B8%8F-rules-of-hooks)
apply.

### <-state: ([initial])

Takes an initial value. Returns an atom that will re-render component on change.

### <-ref: ([initial])

Takes an initial value. Returns an atom that will _NOT_ re-render component on
change.

### <-deref: ([iref])

Takes an atom. Returns the currently derefed value of the atom, and re-renders 
the component on change.

### <-reducer: ([reducer initialArg init])
Just [react/useReducer](https://reactjs.org/docs/hooks-reference.html#usereducer).

### <-effect: ([f deps])
Just [react/useEffect](https://reactjs.org/docs/hooks-reference.html#useeffect).
`deps` can be a CLJS collection.

### <-context: ([context])
Just [react/useContext](https://reactjs.org/docs/hooks-reference.html#usecontext).

### <-memo: ([f deps])
Just [react/useMemo](https://reactjs.org/docs/hooks-reference.html#usememo).
`deps` can be a CLJS collection.

### <-callback: ([f])
Just [react/useCallback](https://reactjs.org/docs/hooks-reference.html#useCallback).

### <-imperative-handle: ([ref createHandle deps])
Just [react/useImperativeHandle](https://reactjs.org/docs/hooks-reference.html#useimperativehandle).
`deps` can be a CLJS collection.

### <-layout-effect: ([f deps])
Just [react/useLayoutEffect](https://reactjs.org/docs/hooks-reference.html#uselayouteffect).
`deps` can be a CLJS collection.

### <-debug-value: ([v formatter])
Just [react/useDebugValue](https://reactjs.org/docs/hooks-reference.html#usedebugvalue).
`deps` can be a CLJS collection.
