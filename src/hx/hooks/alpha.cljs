(ns hx.hooks.alpha
  (:require [hx.hooks :refer [useEffect useState useReducer useIRef]])
  (:require-macros [hx.hooks.alpha]))

(defonce states (atom {}))

(defn usePersistentHook
  [use-hook k & {:keys [initial derive]
                 :or {initial nil
                      derive identity}}]
  ;; if a state already exists with name `k`, then pass that value in
  ;; otherwise, pass in the initial value.
  ;; capture the hook returned by the higher-order hook passed in
  (let [hook (use-hook (if (contains? @states k)
                         (@states k)
                         initial))]
    (let [has-mounted? (useIRef false)]
      (useEffect (fn []
                   (if @has-mounted?
                     (swap! states assoc k (derive hook))
                     (reset! has-mounted? true)))
                 [(derive hook)]))
    hook)
  ;; in release mode, just return <-state
  ;; (use-hook initial)
  )

(defn useStateOnce
  "Like useState, but maintains your state across hot-reloads. `k` is a globally
  unique key to ensure you always get the same state back.

  Example: `(useStateOnce 0 ::counter)`"
  [initial k]
  (usePersistentHook
   (fn useStateOnce* [state] (useState state))
   k
   :initial initial
   :derive first))

(defn useReducerOnce
  "Like useReducer, but maintains your state across hot-reloads. `k` is a globally
  unique key to ensure you always get the same state back.

  Example: `(useReducerOnce reducer initial ::counter)`"
  [reducer initial k]
  ;; if a state already exists with name `k`, then pass that value in
  ;; otherwise, pass in the initial value.
  ;; capture the hook returned by the higher-order hook passed in
  (let [hook (useReducer reducer
                         (if (contains? @states k)
                           (@states k)
                           initial))]
    (let [has-mounted? (useIRef false)]
      (useEffect (fn []
                   (if @has-mounted?
                     (swap! states assoc k (first hook))
                     (reset! has-mounted? true)))
                 [(first hook)]))
    hook))
