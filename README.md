# hx

A modern hiccup compiler, targeted at maximal React interop.

## Usage

```clojure
(require '[hx.react :as hx])
(require '[react :as react])

(react/render
  (hx/compile
    $[:span {:style {:font-weight "bold"}} "Hello, world!"])
             
  (. js/document getElementById "app"))
```

## Goals

1. **Simple** interop between vanilla React features and CLJS code, to ease
   adoption of new features & technologies in the JS world.
   
2. **Performant** parsing of hiccup syntax; impact is minimized by using macros,
   to remove the need for runtime parsing of hiccup and minimize marshalling of
   CLJS data.
   
3. **Extensible** API so that parsing, analysis & code generation of the hiccup
   compiler can evolve to meet the needs of different ecosystems.
   
   
## Top-level API

### hx.react/compile: ([& form])

This macro takes in an arbitrary clojure form. It parses all `$` to mean "parse
the next form into React `createElement` calls."

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
  (createElement "ul" #js {:style #js {:listStyleType "square"}}
    (map #(do (createElement "li" #js {:key %} %)])
         numbers)]))
```

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

This macro creates a React component class. Is the JS equivalent of 
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

## License

Copyright © 2018 Will Acton

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
