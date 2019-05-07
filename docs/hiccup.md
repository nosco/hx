# What's hiccup?

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
  [ReactComponent {:someProp #js {:foo "bar"}}
   [:div {:class "greeting"} "Hello, ReactJS!"]])
;; executes:
;;    (react/createElement ReactComponent #js {:someProp #js {:foo "bar"}}
;;      (react/createElement "div" #js {:className "greeting"}
;;        "Hello, ReactJS!"))
```

The hiccup parser can be extended with custom tags by using the function
`hx.hiccup/extend-tag`. For example, here's how we handle `:<>` for
fragments:

```clojure
(hiccup/extend-tag :<> react/Fragment)
```

## Hiccup forms & interpreter behavior

`hx.hiccup` makes several default decisions about how hiccup and components should be
written.

For a number of examples, check out the [workshop in the examples folder](../examples/workshop/). 

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
;;  has children, but no props. Strings are considered valid children.
(hx/defnc Greet [_]
  [:div "Hello"])

;; component that is passed in a map of props
(hx/defnc MediumGreet [_]
  [:div {:style {:font-size "28px"}} "Medium hello"])

;; binding props and children to symbols and passing it into the element.
(hx/defnc BigGreet [_]
    (let [props {:style {:font-size "56px"}}
          children "Big hello"]
      [:div props children]))

;; passing in multiple children, and calling components that we defined ourselves
;; (instead of native elements like `:div`).
(hx/defnc AllGreets [_]
  [:div
   [Greet]
   [MediumGreet]
   [BigGreet]])

;; sequences are wrapped in react Fragments for ease of use
(hx/defnc Sequence [_]
  [:div
   (for [color ["red" "green" "blue"]]
     ;; add a key prop to help React
     [:strong {:style {:color color} :key color} color])])
      
;; using children as a function
(hx/defnc FnAsChild [{:keys [children]}]
  [:div (children "foo")])

;; passing in children as a function
(hx/defnc UseFnAsChild [_]
  [FnAsChild (fn [value]
                [:h1 value])])
```

`hx.hiccup` shallowly converts props to a JS object at runtime. Your kebab-case
props will be converted to camelCase before passed into a native element. 
`:style` is special-cased to recursively convert to a JS obj to help with using
native elements as well.

