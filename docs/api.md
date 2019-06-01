## Top-level API

### hx.hiccup

If all you want is the hiccup interpreter, you may simple import `hx.hiccup`:

```clojure
(require '[hx.hiccup :as hiccup])

(hiccup/parse [:div "foo"]) ;; => {$$typeof: Symbol(react.element), type: "div", key: null, ref: null, props: {…}, …}
```

### hx.react

`hx.react` contains a number of helpful functions and macros for doing React
development in ClojureScript.

#### hx.react/f ([form])

An alias of `hx.hiccup/parse`, so that if you need to do some ad-hoc 
transformation of hiccup one doesn't have to import `hx.hiccup` as well as
`hx.react`.

#### hx.react/props=: ([& ks])

Takes a variable number of keywords, and returns a function that will return
true or false based on whether two JS objects have the same value at those keys.

Useful when used with `react/memo`.

#### hx.react/defnc: ([name props-bindings & body])

This macro is just like `defn`, but shallowly converts the props object passed
in to the component to a Clojure map and intelligently interprets the return 
value of the body as hiccup. If the body doesn't return a vector, it simply
returns that value.

Takes a name, props bindings and a function body.

Example usage:
```clojure
(require '[hx.react :as hx])
(require '[hx.hiccup])

(hx/defnc Greeting [{:keys [name] :as props}]
  [:span {:style {:font-size "24px"}}
   "Hello, " name "!"])

(react/render
  (hx.hiccup/parse [Greeting {:name "Tara"}])
    
  (. js/document getElementById "app"))
```

#### hx.react/defcomponent: ([name constructor & body])

This macro creates a React component class. Is the CLJS equivalent of 
`class {name} extends React.Component { ... `. `constructor` is passed in `this`
and must _return it._ Additional methods and static properties can be passed in,
similar to `defrecord` / `deftype`. Methods are automatically bound to `this`.

Example usage:

```clojure
(hx/defcomponent MyComponent
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

An alias for `react/createElement`. It will marshall props from a map to JS and
interpret any hiccup children.

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
  (is (= (hx/shallow-render (Welcome {:keys age}))
         [:div "You're allowed!"]))
  (is (= (hx/shallow-render (Welcome {:age 17}))
         [:div [:a {:href "http://disney.com"}] "Please go elsewhere"])))
```

