(ns hx.hiccup
  (:require [clojure.walk :as walk]
            [hx.utils :as util]
            ["react" :as react]))

(defprotocol IElement
  (-parse-element [el args] "Parses an element"))

;; we use a multimethod to dispatch on identity so that consumers
;; can override this for custom values e.g. :<> for React fragments
(defmulti parse-element
  (fn [el & more]
    (identity el))
  :default ::default)

;; if no multimethod for specific el, then apply general parsing rules
(defmethod parse-element
  ::default
  ([el & args]
   (-parse-element el args)))

(defn make-node [el props & children]
  (apply react/createElement el props children))

(defn parse [hiccup]
  (apply parse-element hiccup))

(defn make-element [el args]
  (let [props (first args)
        children (rest args)
        props? (map? props)]
    (apply make-node
           el
           (if props?
             (-> props
                 (util/clj->props))
             nil)
           (into (if props? [] [(parse-element props)]) (map parse-element children)))))

(extend-protocol IElement
  nil
  (-parse-element [_ _]
    nil)
  number
  (-parse-element [n _]
    n)
  string
  (-parse-element [s _]
    s)
  PersistentVector
  (-parse-element [form _]
    (apply parse-element form))

  LazySeq
  (-parse-element [a b]
    (apply make-node
           react/Fragment
           nil
           (into [] (map parse-element a))))

  Keyword
  (-parse-element [el args]
    (make-element (name el) args))

  function
  (-parse-element [el args]
    (make-element el args))

  react/Component
  (-parse-element [el args]
    (make-element el args))

  default
  (-parse-element [el args]
    (cond
      (react/isValidElement el) el
      (= (goog/typeOf el) "symbol") (make-element el args)
      :default
      (throw
       (js/Error. "Unknown element type found while parsing hiccup form")))))
