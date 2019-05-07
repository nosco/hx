# hx

[![Clojars Project](https://img.shields.io/clojars/v/lilactown/hx.svg)](https://clojars.org/lilactown/hx)

A simple, easy to use library for React development in ClojureScript.

```clojure
(ns my-app.core
  (:require [hx.react :as hx :refer [defnc]]
            [hx.hooks :as hooks]
            ["react-dom" :as react-dom]))

;; `defnc` creates a function that takes a props object and returns React
;; elements. You may use it just like any normal React component.
(defnc MyComponent [{:keys [initial-name]}]
  ;; use React Hooks for state management
  (let [[name update-name] (hooks/useState initial-name)]
    [:<>
     [:div "Hello " 
      [:span {:style {:font-weight "bold"}} name] "!"]
     [:div [:input {:on-change #(update-name (-> % .-target .-value))}]]]))

(react-dom/render
  ;; hx/f transforms Hiccup into a React element.
  ;; We only have to use it when we want to use hiccup outside of `defnc` / `defcomponent`
  (hx/f [MyComponent {:initial-name "React in CLJS"}])
  (. js/document getElementById "app"))
```

## Dependencies

You'll want to make sure you have the latest version of `react`, and whatever
renderer you are targeting (e.g. `react-dom`).

```
npm i react react-dom
```

If you want to use the React Hooks API (`hx.hooks`), you'll need to ensure
you are using React 16.8 or later.

## What problems does `hx` solve?

`hx` is meant to make it simple, easy and fun to use [React.js](https://reactjs.org/)
within ClojureScript. It is your bridge to the wide world of React.js in 
idiomatic CLJS.

The library is split into (currently) three sections, which you can feel free to 
mix as your project sees fit:

1. A hiccup interpreter. Takes in `[:div {:style {:color "red"}} [:span "foo"]]` and
spits out `React.createElement` calls.

2. Helpers for creating components. `defnc` and `defcomponent` help us write
plain React.js components in idiomatic ClojureScript.

3. Helpers for using React Hooks.

## What problems does `hx` _not_ solve?

No opinionated state management, no custom rendering queue. Use it to build
your awesome opinionated async reactive immutable app framework. `hx` is just
a Clojure-y interface to creating plain, unadulterated React components.

## Documentation

 - [Hiccup](./docs/hiccup.md)
   - [What's hiccup?](./docs/hiccup.md#whats-hiccup)
   - [Hiccup forms & interpreter behavior](./docs/hiccup.md#hiccup-forms--interpreter-behavior)
 - [Authoring Components](./docs/authoring-components.md)
 - [State management](./docs/state-management.md)
   - [hx.hooks API](./docs/state-management.md#hxhooks)
 - [How-To](./docs/how-to.md)
   - [Use higher-order components](./docs/how-to.md#Use-higher-order-components)
   - [Use children-as-function / render-props](./docs/how-to.md#Use-children-as-function--render-props)
 - [API documentation](./docs/api.md)
 - [Why not Reagent?](./docs/why-not-reagent.md)
 
## Examples

Interop:

 - [Material UI](./examples/workshop/material.cljs)
 - [react-dnd](./examples/workshop/react_dnd.cljs)
 - [react-sortable-hoc](./examples/workshop/sortable.cljs)
 
## Projects that use it

 - [punk](https://github.com/Lokeh/punk): A data REBL built for the web
 - [hx-frisk](https://github.com/Lokeh/hx-frisk/): A fork of [data-frisk-reagent](https://github.com/Odinodin/data-frisk-reagent)

## License

Copyright © 2018 Will Acton

Distributed under the MIT license.
