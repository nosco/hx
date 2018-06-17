(ns hx.state
  (:require [hx.react]
            [clojure.walk :as walk]
            #?(:clj [hx.compiler.core :as hx])))



(def ^:dynamic *reactions*)
(def ^:dynamic *render*)
(def ^:dynamic *reactive-id*)

(defn track! [a]
  (when (not ((deref *reactions*) a))
    (do
      (println "tracking" a "on" *reactive-id*)
      (add-watch a *reactive-id*
                 *render*)
      (vswap! *reactions* conj a))))

(defn deref! [a]
  (track! a)
  (deref a))

#?(:cljs (def reactive
           (let [class (hx.react/create-pure-component
                        (fn [this]
                          (set! (.-reactions this) (volatile! #{}))
                          (set! (.-reactiveId this) (clojure.core/random-uuid))
                          this)
                        {"displayName" "ReactiveContainer"}
                        ['render 'componentWillUnmount])]
              (specify! (.-prototype class)
                Object
                (componentWillUnmount [this]
                  (doseq [a @(.-reactions this)]
                    (println "removing track" a "on" (.-reactiveId this))
                    (remove-watch a (.-reactiveId this))))
                (render [this]
                  (binding [*reactions* (.-reactions this)
                            *render* #(.forceUpdate this)
                            *reactive-id* (.-reactiveId this)]
                    (let [props (hx.react/props->clj (.-props this))]
                      ((:children props))))))
              class)))

(defn transform-sym [sym transform leaf]
  (if (and (seq? leaf) (= (first leaf) sym))
    (transform leaf)
    leaf))

(defn tx-deref [leaf]
  (let [atom-sym (second leaf)]
    `(hx.state/deref!
      ~@(rest leaf))))

(defmacro defrc
  [component-name args & body]
  (let [compiled-body (walk/prewalk (partial transform-sym
                                             'clojure.core/deref
                                             tx-deref) body)]
    `(hx.react/defnc ~component-name
       [props#]
       ~'$[hx.state/reactive
         (fn []
           (let [~@args props#]
             ~@compiled-body))])))
