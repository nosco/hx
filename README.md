# hx

[![Clojars Project](https://img.shields.io/clojars/v/lilactown/hx.svg)](https://clojars.org/lilactown/hx)

A simple, easy to use library for React development in ClojureScript.

## Usage

```clojure
(require '[hx.react :as hx :include-macros true])
(require '["react-dom" :as react-dom])

(hx/defnc MyComponent [{:keys [name]}]
  (hx/c [:div "Hello," 
         [:span {:style {:font-weight "bold"}} name] "!"]))

(react-dom/render
  (hx/c
   [MyComponent {:name "React in CLJS"}])
             
  (. js/document getElementById "app"))
```

## What problems does `hx` solve?

`hx` is meant to make it simple, easy and fun to use [React.js](https://reactjs.org/)
within ClojureScript.

The library is split into (currently) two sections, which you can feel free to 
mix as your project sees fit:

1. A hiccup compiler. Takes in `[:div {:style {:color "red"}} [:span "foo"]]` and
spits out `React.createElement` calls.

2. Helpers for creating components. `defnc` and `defcomponent` help us write
plain React.js components in idiomatic ClojureScript.

### What's hiccup?

*TL;DR: hiccup is the [JSX](https://reactjs.org/docs/introducing-jsx.html)
of the Clojure ecosystem, and `hx` aims to solve that problem just as well.*

`hx.hiccup` is an implementation of a "hiccup" syntax compiler. Hiccup is a way 
of representing HTML using clojure data structures. 
It uses vectors to represent elements, and maps to represent an elements 
attributes.

The [original hiccup library](https://github.com/weavejester/hiccup) was written for
Clojure and outputs HTML strings. This library is written for use in CLJS and
outputs React data structures. It extends the syntax slightly to accomodate using
any arbitrary React component in place of HTML tags.

The basis of the library is the `compile-hiccup` function that takes in a
hiccup form and transforms it into calls to React's `createElement` function:

```clojure
(require '[hx.compiler.core :refer [compile-hiccup]])

(compile-hiccup
  [ReactComponent {:some-prop #js {:foo "bar"}}
   [:div {:class "greeting"} "Hello, ReactJS!"]]

 'react/createElement)
;; => (react/createElement ReactComponent #js {:some-prop #js {:foo "bar"}}
;;      (react/createElement #js {:className "greeting"}
;;        "Hello, ReactJS!"))
```

This is then used to build a macro so that it can be used on our CLJS code
at compile time. `hx` comes with it's own macro out of the box with sane
defaults, but the core compiler is also available should you have different
needs.

## What problems does `hx` _not_ solve?

No state management, no custom rendering queue, no opinions. Use it to build
your awesome opinionated async reactive immutable app framework. `hx` is just
a Clojure-y interface to creating plain, unadulterated React components.


## Goals

1. **Simple** interop between vanilla React features and CLJS code, to ease
   adoption of new features & technologies in the JS world.
   
2. **Performant** parsing of hiccup syntax; impact is minimized by using macros,
   to remove the need for runtime parsing of hiccup and minimize marshalling of
   CLJS data.
   
3. **Extensible** API so that parsing, analysis & code generation of the hiccup
   compiler can evolve to meet the needs of different ecosystems.


## Motivation
   
There are a lot of cool things coming out of React 16 that are contesting some
initial design decisions of other React wrappers in the CLJS ecosystem:

#### 1. Maximal interop

Up until now, CLJS wrappers have been implementing async rendering in user-land.
Firstly, async rendering is [generally](https://reagent-project.github.io/news/reagent-is-async.html)
[a good](http://swannodette.github.io/2013/12/17/the-future-of-javascript-mvcs) [thing](https://www.youtube.com/watch?v=nLF0n9SACd4).

Now that React is implementing async rendering in the framework itself, we
should endeavor to leverage the framework rather than our various user-land
implementations.

Secondly, the second-half of the Dan Abramov video above is quite impressive,
and I am very excited about React "suspense." Again, this is something that we 
have solved many times in CLJS-land, but once this lands there will be an 
explosion of features & functionality that we will not be able to access easily 
unless we can bind closely to this new API.

Thirdly, React's new context API greatly simplifies passing state around an app
in an async-safe way. This is especially important when considering server-side
rendering, a use-case that many CLJS libraries still do not weigh very heavily.
Removing the layers of abstraction between CLJS & vanilla React is important for
using React context and (more generally) render-props/function-as-children.

#### 2. Building blocks

Some frameworks such as Reagent, Rum, etc. define their own way of parsing
hiccup and creating components. While this allows them to build tight integrations,
it also means that our code must subscribe to many ways in which these frameworks
control our application code. We can combine them at the seams, but doing a
full-on replacement is often difficult.

`hx` aims to not control state management, rendering, or anything else about
your application. It should only give you a way of creating and using React
components in your ClojureScript applications.

#### 3. Uniform & easy to use

[Sablono](https://github.com/r0man/sablono/) and [Hicada](https://github.com/rauhs/hicada)
are two other great libraries for parsing & compiling hiccup syntax into React
components. `hx.hiccup` is different in two significant ways:

1. A uniform syntax for calling React components (as in, functions and React obj).
   No need to constantly mix `[:div ..]` with `(my-component ...)`, creating
   factories, etc.

2. No runtime interpretation of hiccup syntax; always assumes that things are
   tags or React elements.
   
3. Out-of-the-box defaults allow the library to be easily used right away, while
   providing APIs to extend and change the parsing, analysis and generation of
   hiccup → React elements as your needs evolve.
   
### Authoring components

`hx` doesn't do anything special in regards to how it calls or creates React 
components. They are assumed to act like native, vanilla React components that 
could be used in any codebase.

In practice, this is fairly easy to handle in ClojureScript. A basic functional component
can be written as just a normal function that returns a React element:

```clojure
(defn my-component [props]
  (hx/c [:div "Hello"]))
```

`props` will always be a *JS object*, so if we want to pull something out of it, we'll
need to use JS interop:

```clojure
(defn my-component [props]
  (let [name (goog.object/get props "name")]
    (hx/c [:div "Hello, " name "!"]))
```

`hx.react/defnc` is a macro that shallowly converts the props object for us, so
we can get rid of some of the boilerplate:

```clojure
(hx/defnc my-component [props]
  (let [name (:name props)]
    (hx/c [:div "Hello, " name "!"])))
```

Children are also passed in just like any other prop, so if we want to obtain children we
simply peel it off of the props object:

```clojure
(defn has-children [props]
  (let [children (goog.object/get props "children")]
    (hx/c [:div 
           {:style {:border "1px solid #000"}}
           children]))

;; or
(hx/defnc has-children [props]
  (let [children (:children props)]
    (hx/c [:div
           {:style {:border "1px solid #000"}}
           children])))
```

Sometimes we also need access to React's various lifecycle methods like
`componentDidMount`, `componentDidUpdate`, etc. In that case, we should create a
React component class. `hx` exposes a very barebones `hx/defcomponent` macro that
binds closely to the OOP, class-based API React has for maximum flexibility. You 
can also leverage libraries like Om.Next, Reagent, Rum, or other frameworks that
have state management built in.

## Hiccup forms & compiler behavior

`hx.hiccup` makes several default decisions about how hiccup and components should be
written.


### Writing hiccup

First, all hiccup forms are assumed to follow the same pattern:

`[ <(1)component> <(2)props-map?> <(3)child> ... <(n)child> ]`.

The element in the *(1)* first position are assumed to be valid React elements.
They can be:

1. A keyword that maps to a native element - such as `:div`, `:span`, `:nav`.
   These are mapped to a string and passed into `createElement`.

2. A function that returns React elements, aka a [functional component](https://reactjs.org/docs/components-and-props.html#functional-and-class-components).

3. An object that extends the [React.Component](https://reactjs.org/docs/react-component.html) class.

Regardless of the type of the element in position *(1)*, if you want to pass in
props to a component, they are **always** passed in via a map in the *(2)* second
position. Props can be written as a map literally or a symbol bound to a map.
**If the element in position *(2)* is not a map, it is assumed to be a child and
passed into the *children* field of createElement**.

Finally, anything passed into position *(3)* or on is considered a child element.

Here are a few examples:

```clojure
;; (1)
(defn greet []
  (hx/c [:div "Hello"]))

;; (2)
(defn medium-greet []
  (hx/c [:div {:style {:font-size "28px"}} "Medium hello"]))

;; (3)
(defn big-greet []
    (let [props {:style {:font-size "56px"}}
          children "Big hello"]
      (hx/c [:div props children])))

;; (4)
(defn all-greets []
  (hx/c
    [:div
      [greet]
      [medium-greet]
      [big-greet]]))
```

*(1)* is an example of writing a component that has children, but no props. Strings
are considered valid children.

*(2)* is an example of writing a component that is passed in a map of props.

*(3)* is an example of binding props and children to symbols and passing it into the
element.

*(4)* is an example of passing in multiple children, and calling components that we
defined ourselves (instead of native elements like `:div`).

### Mixing hiccup and clojure forms

`hx.hiccup` doesn't do any special runtime evaluation or macro magic of your 
clojure forms, so when you use `for`, `let`, `map` etc. inside of a hiccup form,
you'll need to wrap the return value in `hx.react/c` as well.

Examples:

```clojure
(hx/c [:div (map #(hx/c [:div "Hello, " %]) ["Mary" "Uma" "Stu"])])
;; => (react/createElement "div" nil
;;      (map #(react/createElement "div" nil "Hello, " %)) ["Mary" "Uma" "Stu"])

(hx/c [:ul {:style {:border "1px solid #eee"}}
       (for [n [1 2 3 4 5]]
         (hx/c [:li n]))])
;; => (react/createElement #js {:style #js {:border "1px solid #eee"}}
;;      (for [n [1 2 3 4 5]]
;;        (react/createElement "li" nil n)))
```

### Optimizations

Let's pause here and talk about the difference between *(2)* and *(3)* in the example
above. Functionally, they are equivalent, but there are some things that the compiler
will do differently depending on whether a symbol or map literal is passed in as the
second element.

React's `createElement` function expects props to be passed in as a JS object.
It will attempt to introspect this object. So we need to marshall the props map into
a JS object before we can pass it off to React.

The `hx` compiler attempts to be clever: when it detects that the second argument is
a map literal, it will shallowly rewrite it into a native JS object:

```clojure
(hx/c [:div {:foo "bar" 
             :baz {:asdf ["jkl" 1234]}}])
;; =>
(React/createElement
 "div"
 (js-obj "foo" "bar"
         "baz" {:asdf ["jkl" 1234]}
 nil)
```

It only rewrites the first level; any nested structures are left untouched.

--

Sidenote: If you're working with a vanilla React component (implemented in JS), you
may have to write something like this to convert the nested structures into native
JS types:

```clojure
(hx/c [SomeWidget {:config #js {:foo "bar" :baz #js ["jkl" 1234]}}])
```

Currently, `:style` is special cased where it will recursively marshall it so that it's
easy to work with native elements. Any other props will need this manual conversion.

--

If the compiler doesn't see a map literal in the second position, it effectively
treats it as a child element and simply passes it through unchanged.

As a convenience, `hx.react` will check if the first child
is a map, and if so, shallowly convert it to a JS object at runtime. There should be no
functional difference between doing this at runtime vs. compile-time, but there may be
a slight performance hit. In most cases, this will be unnoticeable; however if you have
a component that is on the hot path and the marshalling does become a performance
bottleneck, writing out props as a map literal will improve it.

## Top-level API

This top-level macro is meant to serve as sane defaults for users (app developers,
library developers) to compile their hiccup out-of-the-box. It provides a good
mix of performance, ease of use and interoperability.

### hx.react/c: ([form])

This macro takes in form as hiccup and transforms it into React.createElement
calls.

Example usage:

```clojure
(require '[hx.react :as hx])

(let [numbers [1 2 3 4 5]]
  (hx/c [:ul {:style {:list-style-type "square"}}
         (map #(hx/c [:li {:key %} %])
              numbers)]))
```

Will become the equivalent:

```clojure
(let [numbers [1 2 3 4 5]]
  (react/createElement "ul" #js {:style #js {:listStyleType "square"}}
    (map #(react/createElement "li" #js {:key %} %)
         numbers)]))
```

### hx.react/defnc: ([name props-bindings & body])

This macro is just like `defn`, but shallowly converts the props object passed
in to the component to a Clojure map. Takes a name, props bindings and a 
function body.

Example usage:
```clojure
(require '[hx.react :as hx])

(hx/defnc greeting [{:keys [name] :as props}]
  (hx/c [:span {:style {:font-size "24px"}}
         "Hello, " name "!"]))

(react/render
  (hx/c [greeting {:name "Tara"}])
    
  (. js/document getElementById "app"))
```

### hx.react/defcomponent: ([name constructor & body])

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
      (hx/c [:div
             [:div (. my-component -greeting) ", " (. state -name)]
              [:input {:value (. state -name)
                       :on-change (. this -update-name!)}]]))))
```

## License

Copyright © 2018 Will Acton

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
