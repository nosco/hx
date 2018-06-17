(ns hx.state
  (:require [hx.react]
            [clojure.walk :as walk]
            #?(:clj [hx.compiler.core :as hx])))



(def ^:dynamic *reactions*)
(def ^:dynamic *this*)
(def ^:dynamic *reactive-id*)

(defn track! [atom-name a]
  (when (not ((deref *reactions*) atom-name))
    (let [this *this*]
      (println "adding watch" *reactive-id* "on"  a)
      (add-watch a *reactive-id*
                 #(. this forceUpdate))
      (vswap! *reactions* assoc atom-name a))))

(defn deref! [atom-name a]
  (track! atom-name a)
  (deref a))

(defn transform-sym [sym transform leaf]
  (if (and (seq? leaf) (= (first leaf) sym))
    (transform leaf)
    leaf))

(defn tx-deref [leaf]
  (let [atom-sym (second leaf)]
    `(hx.state/deref!
      '~atom-sym
      ~@(rest leaf)))
  ;; (cons 'hx.state/deref!
  ;;       (cons component-name
  ;;             (cons list
  ;;                   'quote
  ;;              (cons (second leaf)
  ;;                    (rest leaf)))))
  )

(defmacro defrc
  [component-name args & body]
  (let [compiled-body (walk/prewalk (partial transform-sym
                                             'clojure.core/deref
                                             tx-deref) body)]
    ;; need to remove-watch when reactions are no longer bound
    `(hx.react/defcomponent ~component-name
     (~'constructor [this#]
      (set! (.-reactions this#) (volatile! {}))
      (set! (.-reactiveId this#) (clojure.core/random-uuid))
      this#)
     (~'componentWillUnmount [this#]
      (println "unmounting")
      (doseq [[k# a#] @(.-reactions this#)]
        (println "removing watch" (.-reactiveId this#) "on" a#)
        (remove-watch a# (.-reactiveId this#))))
     (~'render [this#]
      (binding [*reactions* (.-reactions this#)
                *this* this#
                *reactive-id* (.-reactiveId this#)]
        (let [~@args (hx.react/props->clj (.-props this#))]
        ~@compiled-body))))))
