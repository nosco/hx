(ns hx.hiccup
  (:require [clojure.walk :as walk]
            [hx.utils :as util]
            ["react" :as react]
            ["react-is" :as react-is]))

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

(defn make-node [el props children]
  (if (seq? children)
    (apply react/createElement el props children)
    (react/createElement el props children)))

(defn parse [hiccup]
  (apply parse-element hiccup))

(defn make-element [el args]
  (let [props? (map? (first args))
        props (if props? (first args) nil)
        children (if props? (rest args) args)]
    (make-node
     el
     (if props?
       (-> props
           (util/clj->props))
       nil)
     (if (and (= (count children) 1) (fn? (first children)))
       ;; fn-as-child
       ;; wrap in a function to parse hiccup from render-fn
       (fn [& args]
         (let [ret (apply (first children) args)]
           (if (vector? ret)
             (apply parse-element ret)
             ret)))
       (map parse-element children)))))

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
    (make-node
     react/Fragment
     nil
     (map parse-element a)))

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

      (react-is/isValidElementType el)
      (make-element el args)

      ;; handle array of children already parsed
      (and (array? el) (every? react/isValidElement el))
      el

      (var? el)
      (make-element (fn VarEl [& args] (apply el args))
                    args)

      :default
      (do
        (js/console.log el)
        (throw
         (js/Error. (str "Unknown element type found while parsing hiccup form: "
                         (.toString el))))))))
