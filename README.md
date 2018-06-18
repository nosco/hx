# hx

An easy to use, decomplected hiccup compiler for React.

## Usage

```clojure
(require '[hx.react :as hx])
(require '[react :as react])

(react/render
  (hx/compile
    $[:span {:style {:font-weight "bold"}} "Hello, world!"])
             
  (. js/document getElementById "app"))
```

## What problem does `hx` solve?

*TL;DR: hiccup is the [JSX](https://reactjs.org/docs/introducing-jsx.html)
of the Clojure ecosystem, and `hx` aims to solve that problem just as well.*

`hx` is an implementation of a "hiccup" syntax compiler. Hiccup is a way of
representing HTML using clojure data structures. 
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
your plain, unadulterated hiccup → React library.


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
hiccup and creating React elements. While this allows them to build integrations,
it also means that our code must subscribe to many ways in which these frameworks
control our application code. We can combine them at the seams, but doing a
full-on replacement is often difficult.

`hx` aims to not control state management, rendering, or anything else about
your application. It should only give you a way of describing React data easily
in your CLJS applications.

#### 3. Uniform & easy to use

[Sablono](https://github.com/r0man/sablono/) and [Hicada](https://github.com/rauhs/hicada)
are two other great libraries for parsing & compiling hiccup syntax into React
components. `hx` is different in two significant ways:

1. A uniform syntax for calling React components (as in, functions and React obj).
   No need to constantly mix `[:div ..]` with `(my-component ...)`, creating
   factories, etc.

2. No runtime interpretation of hiccup syntax; always assumes that things are
   tags or React elements.
   
3. Out-of-the-box defaults allow the library to be easily used right away, while
   providing APIs to extend and change the parsing, analysis and generation of
   hiccup → React elements as your needs evolve.
   
## Top-level API

This top-level macro is meant to serve as sane defaults for users (app developers,
library developers) to use out-of-the-box. It provides a good mix of performance,
ease of use and interoperability.

### hx.react/compile: ([& form])

This macro takes in an arbitrary clojure form. It parses all `$` to mean "parse
the next form as hiccup into React.createElement calls."

Example usage:

```clojure
(require '[hx.react :as hx])

(hx/compile
   (let [numbers [1 2 3 4 5]]
     $[:ul {:style {:list-style-type "square"}}
       (map #(do $[:li {:key %} %])
            numbers)]))
```

Will become the equivalent:

```clojure
(let [numbers [1 2 3 4 5]]
  (react/createElement "ul" #js {:style #js {:listStyleType "square"}}
    (map #(do (react/createElement "li" #js {:key %} %))
         numbers)]))
```

## Extra sauce

Along with compilation of hiccup into React API calls, it also comes with a few
other helpful macros & functions for creating React components. It handles shallowly
marshalling props into CLJS data structures and some other quality of life
improvements.

Feel free to ignore them if you want to build something cooler.

### hx.react/defnc: ([name props-bindings & body])

This macro is just like `defn`, but has some helpers for defining functional
React components. Takes a name, props bindings and a body that will be passed to
`hx.react/compile`.

Example usage:
```clojure
(require '[hx.react :as hx])

(hx/defnc greeting [{:keys [name] :as props}]
  $[:span {:style {:font-size "24px"}}
    "Hello, " name "!"])

(react/render
  (hx/compile
    $[greeting {:name "Tara"}])
    
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
      $[:div
        [:div (. my-component -greeting) ", " (. state -name)]
         [:input {:value (. state -name)
                  :on-change (. this -update-name!)}]])))
```


## Compiler API

### hx.compiler.core/compile-hiccup ([hiccup create-element])

Compiles a `hiccup` form into function calls to the passed in `create-element` symbol.

Example usage:

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

STUB

## License

Copyright © 2018 Will Acton

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
