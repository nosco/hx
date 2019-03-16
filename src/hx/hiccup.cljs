(ns hx.hiccup
  (:require [clojure.walk :as walk]
            [hx.utils :as util]
            ["react" :as react]
            ["react-is" :as react-is]))

(defprotocol IElement
  (-parse-element [el config args] "Parses an element"))

;; we use a multimethod to dispatch on identity so that consumers
;; can override this for custom values e.g. :<> for React fragments
(defmulti parse-element
  (fn [config el & more]
    (identity el))
  :default ::default)

;; if no multimethod for specific el, then apply general parsing rules
(defmethod parse-element
  ::default
  ([config el & args]
   (-parse-element el config args)))

(defn make-node [config el props children]
  (if (seq? children)
    (apply react/createElement el props children)
    (react/createElement el props children)))

(defn parse [config hiccup]
  (apply parse-element config hiccup))

(defn make-element [config el args]
  (let [props? (map? (first args))
        props (if props? (first args) nil)
        children (if props? (rest args) args)]
    (make-node
     config
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
             (apply parse-element config ret)
             ret)))
       (map (partial parse-element config) children)))))

(extend-protocol IElement
  nil
  (-parse-element [_ _ _]
    nil)
  number
  (-parse-element [n _ _]
    n)
  string
  (-parse-element [s _ _]
    s)
  PersistentVector
  (-parse-element [form config _]
    (apply parse-element config form))

  LazySeq
  (-parse-element [a config b]
    (make-node
     config
     react/Fragment
     nil
     (map (partial parse-element config) a)))

  Keyword
  (-parse-element [el config args]
    (make-element config (name el) args))

  function
  (-parse-element [el config args]
    (make-element config el args))

  react/Component
  (-parse-element [el config args]
    (make-element config el args))

  default
  (-parse-element [el config args]
    (cond
      (react/isValidElement el) el

      (react-is/isValidElementType el)
      (make-element config el args)

      ;; handle array of children already parsed
      (and (array? el) (every? react/isValidElement el))
      el

      (var? el)
      (make-element config
                    (fn VarEl [& args] (apply el args))
                    args)

      :default
      (do
        (throw
         (js/Error. (str "Unknown element type found while parsing hiccup form: "
                         (.toString el))))))))
