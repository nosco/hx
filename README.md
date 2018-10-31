# hx

[![Clojars Project](https://img.shields.io/clojars/v/lilactown/hx.svg)](https://clojars.org/lilactown/hx)

A simple, easy to use library for React development in ClojureScript.

```clojure
(ns my-app.core
  (:require [hx.react :as hx :include-macros true]
            ["react-dom" :as react-dom]))

(hx/defnc MyComponent [{:keys [name]}]
  [:div "Hello " 
   [:span {:style {:font-weight "bold"}} name] "!"])

(react-dom/render
  (hx/$
   MyComponent {:name "React in CLJS"} nil)
  (. js/document getElementById "app"))
```

## What problems does `hx` solve?

`hx` is meant to make it simple, easy and fun to use [React.js](https://reactjs.org/)
within ClojureScript. It is your bridge to the wide world of React.js in 
idiomatic CLJS.

The library is split into (currently) two sections, which you can feel free to 
mix as your project sees fit:

1. A hiccup interpreter. Takes in `[:div {:style {:color "red"}} [:span "foo"]]` and
spits out `React.createElement` calls.

2. Helpers for creating components. `defnc` and `defcomponent` help us write
plain React.js components in idiomatic ClojureScript.

### What's hiccup?

*TL;DR: hiccup is the [JSX](https://reactjs.org/docs/introducing-jsx.html)
of the Clojure ecosystem*

`hx.hiccup` is an implementation of a "hiccup" syntax interpreter.
Hiccup is a way of representing HTML using clojure data structures. 
It uses vectors to represent elements, and maps to represent an elements 
attributes.

The [original hiccup library](https://github.com/weavejester/hiccup) was written for
Clojure and outputs HTML strings. This library is written for use in CLJS and
outputs React data structures. It extends the syntax slightly to accomodate using
any arbitrary React component in place of HTML tags.

The basis of the library is the `parse` function that takes in a
hiccup form and transforms it into calls to React's `createElement` function:

```clojure
(require '[hx.hiccup :as hiccup])

(hiccup/parse
  [ReactComponent {:some-prop #js {:foo "bar"}}
   [:div {:class "greeting"} "Hello, ReactJS!"]])
;; executes:
;;    (react/createElement ReactComponent #js {:some-prop #js {:foo "bar"}}
;;      (react/createElement #js {:className "greeting"}
;;        "Hello, ReactJS!"))
```

The hiccup parser can be extended with custom tags by defining a new 
`hx.hiccup/parse-element` method. For example, here's how we handle `:<>` for
fragments:

```clojure
(defmethod hiccup/parse-element :<>
  [el & args]
  (hiccup/-parse-element
   hx.react/fragment
   args))
```

## What problems does `hx` _not_ solve?

No opinionated state management, no custom rendering queue. Use it to build
your awesome opinionated async reactive immutable app framework. `hx` is just
a Clojure-y interface to creating plain, unadulterated React components.

## Authoring components

`hx` doesn't do anything special in regards to how it calls or creates React 
components. They are assumed to act like native, vanilla React components that 
could be used in any codebase.

In practice, this is fairly easy to handle in ClojureScript. A basic functional
component can be written as just a normal function that returns a React element:

```clojure
(defn my-component [props]
  (hiccup/parse [:div "Hello"]))
```

`props` will always be a *JS object*, so if we want to pull something out of it, we'll
need to use JS interop:

```clojure
(defn my-component [props]
  (let [name (goog.object/get props "name")]
    (hiccup/parse [:div "Hello, " name "!"]))
```

`hx.react/defnc` is a macro that shallowly converts the props object for us and
wraps our function body in `hiccup/parse`, so we can get rid of some of the
boilerplate:

```clojure
(hx/defnc my-component [props]
  (let [name (:name props)]
    [:div "Hello, " name "!"]))
```

Children are also passed in just like any other prop, so if we want to obtain children we
simply peel it off of the props object:

```clojure
(defn has-children [props]
  (let [children (goog.object/get props "children")]
    (hiccup/parse
      [:div 
       {:style {:border "1px solid #000"}}
       children]))

;; or
(hx/defnc has-children [{:keys [children]}]
  [:div
   {:style {:border "1px solid #000"}}
   children])
```

Sometimes we also need access to React's various lifecycle methods like
`componentDidMount`, `componentDidUpdate`, etc. In that case, we should create a
React component class. `hx` exposes a very barebones `hx/defcomponent` macro that
binds closely to the OOP, class-based API React has for maximum flexibility. You 
can also leverage libraries like Om.Next, Reagent, Rum, or other frameworks that
have state management built in.

## Hiccup forms & interpreter behavior

`hx.hiccup` makes several default decisions about how hiccup and components should be
written.

### Writing hiccup

First, all hiccup forms are assumed to follow the same pattern:

`[ <(1)elementType> <(2)props-map?> <(3)child> ... <(n)child> ]`.

The element in the *(1)* first position are assumed to be valid React element types.
They can be:

1. A keyword that maps to a native element - such as `:div`, `:span`, `:nav`.
   These are mapped to a string and passed into `createElement`.

2. A function that returns React elements, aka a [functional component](https://reactjs.org/docs/components-and-props.html#functional-and-class-components).

3. An object that extends the [React.Component](https://reactjs.org/docs/react-component.html) class.

4. One of the built-in React symbols, such as `React.Fragment`, a context provider,
context consumer, etc.

Regardless of the type of the element in position *(1)*, if you want to pass in
props to a component, they are **always** passed in via a map in the *(2)* second
position. Props can be written as a map literally or a symbol bound to a map.
**If the element in position *(2)* is not a map, it is assumed to be a child and
passed into the *children* field of createElement**.

Finally, anything passed into position *(3)* or on is considered a child element.

Here are a few examples. `hx.react/defnc` is used for concision, the only thing
it does is coerce props to a map and wrap the body in a call to `hiccup/parse`:

```clojure
;; (1)
(hx/defnc greet [_]
  [:div "Hello"])

;; (2)
(hx/defnc medium-greet [_]
  [:div {:style {:font-size "28px"}} "Medium hello"])

;; (3)
(hx/defnc big-greet [_]
    (let [props {:style {:font-size "56px"}}
          children "Big hello"]
      [:div props children]))

;; (4)
(hx/defnc all-greets []
  [:div
   [greet]
   [medium-greet]
   [big-greet]])
      
;; using children as a function
(hx/defnc fn-as-child [{:keys [children]}]
  [:div (children "foo")])

;; passing in children as a function
(hx/defnc use-fn-as-child [_]
  [fn-as-child (fn [value]
                [:h1 value])])
```

*(1)* is an example of writing a component that has children, but no props. Strings
are considered valid children.

*(2)* is an example of writing a component that is passed in a map of props.

*(3)* is an example of binding props and children to symbols and passing it into the
element.

*(4)* is an example of passing in multiple children, and calling components that we
defined ourselves (instead of native elements like `:div`).

`hx.hiccup` shallowly converts props it to a JS object at runtime. Your kebab-case
props will be converted to camelCase before passed into a native element. 
`:style` is special-cased to recursively convert to a JS obj to help with using
native elements as well.

## Top-level API

If all you want is the hiccup interpreter, you may simple import `hx.hiccup`:

```clojure
(require '[hx.hiccup :as hiccup])

(hiccup/parse [:div "foo"]) ;; => {$$typeof: Symbol(react.element), type: "div", key: null, ref: null, props: {…}, …}
```

### hx.react

`hx.react` contains a number of helpful functions and macros for doing React
development in ClojureScript.

#### hx.react/defnc: ([name props-bindings & body])

This macro is just like `defn`, but shallowly converts the props object passed
in to the component to a Clojure map. Takes a name, props bindings and a 
function body.

Example usage:
```clojure
(require '[hx.react :as hx])
(require '[hx.hiccup])

(hx/defnc greeting [{:keys [name] :as props}]
  [:span {:style {:font-size "24px"}}
   "Hello, " name "!"])

(react/render
  (hx.hiccup/parse [greeting {:name "Tara"}])
    
  (. js/document getElementById "app"))
```

#### hx.react/defcomponent: ([name constructor & body])

This macro creates a React component class. Is the CLJS equivalent of 
`class {name} extends React.Component { ... `. `constructor` is passed in `this`
and must _return it._ Additional methods and static properties can be passed in,
similar to `defrecord` / `deftype`. Methods are automatically bound to `this`.

Example usage:

```clojure
(hx/defcomponent my-component
  (constructor [this]
    (set! (. this -state) #js {:name "Maria"})
    this)

  ^:static
  (greeting "Hello")
  
  (update-name! [this e]
    (. this setState #js {:name (.. e -target -value)}))

  (render [this]
    (let [state (. this -state)]
      [:div
       [:div (. my-component -greeting) ", " (. state -name)]
       [:input {:value (. state -name)
                :on-change (. this -update-name!)}]])))
```

#### hx.react/$: ([el p & c])

An alias for `react/createElement`.

#### hx.react/factory: ([component])

Creates a factory function from a component (e.g. a function, class, or string)
that, when called, returns a React element.

#### hx.react/shallow-render: ([& body])

Short-circuits the hiccup interpreter to return just the hiccup form returned by
`body`. Very useful for testing React components created using `hx.hiccup`.

Example:

```clojure
(hx/defnc Welcome [{:keys [age]}]
  (if (> age 17))
    [:div "You're allowed!"]
    [:div [:a {:href "http://disney.com"}] "Please go elsewhere"])
    
;; in test
(deftest welcome-allowed
  (is (= (hx/shallow-render (hiccup/parse [Welcome {:age 18}]))
         [:div "You're allowed!"]))
  (is (= (hx/shallow-render (hiccup/parse [Welcome {:age 17}]))
         [:div [:a {:href "http://disney.com"}] "Please go elsewhere"])))
```

## License

Copyright © 2018 Will Acton

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
