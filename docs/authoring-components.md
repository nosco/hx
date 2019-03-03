## Authoring components

`hx` doesn't do anything special in regards to how it calls or creates React 
components. They are assumed to act like native, vanilla React components that 
could be used in any codebase.

In practice, this is fairly easy to handle in ClojureScript. A basic functional
component can be written as just a normal function that returns a React element:

```clojure
(defn MyComponent [props]
  (hiccup/parse [:div "Hello"]))
  
(react-dom/render (hiccup/parse [MyComponent]) (. js/document getElementById "app"))
```

`props` will always be a *JS object*, so if we want to pull something out of it, we'll
need to use JS interop:

```clojure
(defn MyComponent [props]
  (let [name (goog.object/get props "name")]
    (hiccup/parse [:div "Hello, " name "!"]))
    
(react-dom/render (hiccup/parse [MyComponent {:name "Uma"}])
                  (. js/document getElementById "app"))

```

`hx.react/defnc` is a macro that shallowly converts the props object for us and
wraps our function body in `hiccup/parse`, so we can get rid of some of the
boilerplate:

```clojure
(hx/defnc MyComponent [{:keys [name]}]
  [:div "Hello, " name "!"])
```

Children are also passed in just like any other prop, so if we want to obtain children we
simply peel it off of the props object:

```clojure
(defn HasChildren [props]
  (let [children (goog.object/get props "children")]
    (hiccup/parse
      [:div 
       {:style {:border "1px solid #000"}}
       children]))

;; or
(hx/defnc HasChildren [{:keys [children]}]
  [:div
   {:style {:border "1px solid #000"}}
   children])
```

Sometimes we also need access to React's various lifecycle methods like
`componentDidMount`, `componentDidUpdate`, etc. In that case, we should create a
React component class. `hx` exposes a very barebones `hx/defcomponent` macro that
binds closely to the OOP, class-based API React has for maximum flexibility. You 
can also leverage libraries like Om.Next, Reagent, Rum, or other frameworks that
have state management built in. Note though that React Hooks takes away most, if 
not all of the need to use the class-based React lifecycle methods, in a very nice
and functional way that meshes very well with ClojureScript. It is advised that
you read more on React Hooks at the [official documentation](https://reactjs.org/docs/hooks-intro.html).
