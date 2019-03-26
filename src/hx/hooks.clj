(ns hx.hooks
  (:require [cljs.analyzer.api]))

(defn- resolve-vars [env body]
  (let [sym-list (atom #{})]
    (clojure.walk/postwalk
     (fn w [x]
       (if (symbol? x)
         (do (swap! sym-list conj x)
             x)
         x))
     body)
    (->> @sym-list
         (map (partial cljs.analyzer.api/resolve env))
         (filter (comp not nil?))
         (map :name)
         vec)))

(defmacro <-smart-effect [& body]
  (let [deps (resolve-vars &env body)]
    `(<-effect (fn []
                 ~@body)
               ~deps)))

(defmacro <-smart-layout-effect [& body]
  (let [deps (resolve-vars &env body)]
    `(<-layout-effect (fn []
                 ~@body)
               ~deps)))

(defmacro <-smart-memo [& body]
  (let [deps (resolve-vars &env body)]
    `(<-memo (fn []
               ~@body)
             ~deps)))
