# Why not Reagent?

_[Converted from a reddit post]_

I love Reagent, it is an amazing library. I use it at work every day and it has made me insanely productive. I expect I will continue to use and enjoy Reagent in the projects that are written with it for a long while. But the React world has continued to evolve and develop. New features are coming out that subsume and build upon features and ideas that Reagent had 3+ years ago, but with Facebook's resources behind them.

Reagent does a lot; it implements:

- it's own custom async render queue with batching and other performance enhancements
- it's own ergonomic state management solution through RAtoms that integrate with it's async render solution
- it turns your hiccup-returning components into full-fledge React components

These features were a truly amazing value-add when Reagent was released, and has continued to be over the years.

However, in 2019, React is releasing a number of things in the core library:

- Async rendering with batching, improved scheduling, and more
- React Hooks that provide a functional, ergonomic way to do effectful things like state management
- React Suspense, which allows us to control the loading state of our components which rely on e.g. network requests for data in a much better way
- Streaming server-side rendering in conjunction with React Suspense

So far, only React Hooks has fully landed; Suspense is not stable yet except for lazy loading components, and async rendering is currently opt-in and considered unstable.

However, in my experience, most of the value-add of Reagent comes from it's ability to access and update state in a very ergonomic way (atoms), and convert hiccup to React elements. This is why I think that `hx` is a better decision now: you can get the same ergonomics of Reagent with `hx` and React Hooks, today, with less code and better interop.

This is insanely long, but I want to leave you with a couple examples. A short snippet of component-local state:

Reagent code:

```clojure
(defn count-widget [foo bar]
  (let [state (r/atom {:count 0})]
    (fn [foo bar]
      [:div 
       [:div "Foo: " foo] [:div "Bar: " bar]
       [:div "Count: " count]
       [:button {:on-click #(swap! state update :count inc)} "+"]])))
```

hx + Hooks code:

```clojure
(defnc count-widget [{:keys [foo bar]}]
  (let [state (<-state {:count 0})]
    [:div 
     [:div "Foo: " foo] [:div "Bar: " bar]
     [:div "Count: " count]
     [:button {:on-click #(swap! state update :count inc)} "+"]]))
```

Another, more complex example:

- [Using react-sortable-hoc in reagent](https://github.com/reagent-project/reagent/blob/master/examples/react-sortable-hoc/src/example/core.cljs)
- [Using react-sortable-hoc in hx](../examples/workshop/sortable.cljs)
