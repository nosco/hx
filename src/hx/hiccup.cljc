(ns hx.hiccup
  (:require [clojure.walk :as walk]
            [hx.utils :as util :include-macros true]))

(defprotocol IElement
  (-parse-element [el config args] "Parses an element"))

;; (declare -parse-element)

;; we use a multimethod to dispatch on identity so that consumers
;; can override this for custom values e.g. :<> for React fragments
(defmulti parse-element
  (fn [config el args]
    el)
  :default ::default)

;; if no multimethod for specific el, then apply general parsing rules
(defmethod parse-element
  ::default
  ([config el args]
   (util/measure-perf
    "parse_element_default"
    (-parse-element el config args))))

(defn parse [config hiccup]
  (util/measure-perf
   "parse"
   (parse-element config (-nth hiccup 0) (rest hiccup))))

(defn make-element [config el args]
  (util/measure-perf
   "make_element"
   ((:create-element config) config el args)))

#?(:clj (defn array? [x]
          (coll? x)))

(defn ex [s]
  #?(:clj (Exception. s)
     :cljs (js/Error. s)))

;; (defn -parse-element [form config args]
;;   (cond
;;     (nil? form) form

;;     (number? form) form

;;     (string? form) form

;;     (vector? form) (parse-element config (-nth form 0) (rest form))

;;     (seq? form) (make-element config (:fragment config)
;;                               (cons nil (map #(parse-element config (first %) (rest %)) form)))

;;     (keyword? form) (make-element config (name form) args)

;;     (ifn? form) (make-element config form args)

;;     ((:is-element? config) form) form

;;     ((:is-element-type? config) form)
;;     (make-element config form args)

;;     ;; handle array of children already parsed
;;     (and (array? form) (every? (:is-element? config) form))
;;     form

;;     (var? form)
;;     (make-element config
;;                   (fn VarEl [& args] (apply form args))
;;                   args)

;;     :default
;;     (throw
;;      (ex (str "Unknown element type " (prn-str (type form))
;;               " found while parsing hiccup form: "
;;                                 (.toString form))))

;;     ))

(extend-protocol IElement
  nil
  (-parse-element [_ _ _]
    (util/measure-perf
     "-parse_element_nil"
     nil))

  #?(:clj Number
     :cljs number)
  (-parse-element [n _ _]
    (util/measure-perf
     "-parse_element_number"
     n))

  #?(:clj String
     :cljs string)
  (-parse-element [s _ _]
    (util/measure-perf
     "-parse_element_string"
     s))

  #?(:clj clojure.lang.PersistentVector
     :cljs PersistentVector)
  (-parse-element [form config _]
    (util/measure-perf
     "-parse_element_vector"
     (parse-element config (-nth form 0) (rest form))))

  #?(:clj clojure.lang.LazySeq
     :cljs LazySeq)
  (-parse-element [a config b]
    (util/measure-perf
     "-parse_element_lazyseq"
    (make-element
     config
     (:fragment config)
     (cons nil (map #(parse-element config (first %) (rest %)) a)))))

  #?(:clj clojure.lang.Keyword
     :cljs Keyword)
  (-parse-element [el config args]
    (util/measure-perf
     "-parse_element_keyword"
     (make-element config (name el) args)))

  #?(:clj clojure.lang.AFn
     :cljs function)
  (-parse-element [el config args]
    (util/measure-perf
     "-parse_element_afn"
     (make-element config el args)))

  #?(:clj Object
     :cljs default)
  (-parse-element [el config args]
    (util/measure-perf
     "-parse_element_object"
     (cond
       ((:is-element? config) el) el

       ((:is-element-type? config) el)
       (make-element config el args)

       ;; handle array of children already parsed
       (and (array? el) (every? (:is-element? config) el))
       el

       (var? el)
       (make-element config
                     (fn VarEl [& args] (apply el args))
                     args)

       :default
       (throw
        (ex (str "Unknown element type " (prn-str (type el))
                 " found while parsing hiccup form: "
                 (.toString el))))))))
