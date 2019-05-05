(ns hx.hooks.alpha
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

(defmacro useSmartEffect [& body]
  (let [deps (resolve-vars &env body)]
    `(hx.hooks/useEffect (fn []
                  ~@body)
                ~deps)))

(defmacro useSmartLayoutEffect [& body]
  (let [deps (resolve-vars &env body)]
    `(hx.hooks/useLayoutEffect (fn []
                        ~@body)
                      ~deps)))

(defmacro useSmartMemo [& body]
  (let [deps (resolve-vars &env body)]
    `(hx.hooks/useMemo (fn []
                ~@body)
              ~deps)))
