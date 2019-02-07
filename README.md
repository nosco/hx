# hx

[![Clojars Project](https://img.shields.io/clojars/v/lilactown/hx.svg)](https://clojars.org/lilactown/hx)

A simple, easy to use library for React development in ClojureScript.

```clojure
(ns my-app.core
  (:require [hx.react :as hx :refer [defnc <-state]]
            ["react-dom" :as react-dom]))

;; `defnc` creates a function that takes a props object and returns React
;; elements. You may use it just like any normal React component.
(defnc MyComponent [{:keys [default-name]}]
  ;; use React Hooks for state management
  (let [name (<-state default-name)]
    [:<>
     [:div "Hello " 
      [:span {:style {:font-weight "bold"}} @name] "!"]
     [:div [:input {:on-change #(reset! name (-> % .-target .-value))}]]]))

(react-dom/render
  ;; hx/f transforms Hiccup into a React element
  (hx/f [MyComponent {:default-name "React in CLJS"}])
  (. js/document getElementById "app"))
```

## Dependencies

You'll want to make sure you have the latest version of `react`, `react-is`, and
whatever renderer you are targeting (e.g. `react-dom`).

```
npm i react react-is react-dom
```

If you want to use the React Hooks API (`hx.react.hooks`), you'll need to ensure
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
   - [hx.react.hooks API](./docs/state-management.md#hxreacthooks)
 - [API documentation](./docs/api.md)
 - [Why not Reagent?](./docs/why-not-reagent.md)

## License

Copyright © 2018 Will Acton

Distributed under the MIT license.
