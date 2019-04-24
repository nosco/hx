(ns hx.hiccup
  (:require [clojure.walk :as walk]
            [hx.utils :as util :include-macros true]))

(defprotocol IElement
  (-as-element [el config] "Converts to an element"))

;; (declare -as-element)

(defonce tag-registry (atom {}))

(defn extend-tag [tag impl]
  (swap! tag-registry assoc tag impl))

(defn tag->impl [tag]
  (if-let [t (get @tag-registry tag nil)]
    t
    (name tag)))

;; ;; we use a multimethod to dispatch on identity so that consumers
;; ;; can override this for custom values e.g. :<> for React fragments
;; (defmulti extend-tag
;;   identity
;;   :default ::default)

;; ;; if no multimethod for specific el, then apply general parsing rules
;; (defmethod extend-tag
;;   ::default
;;   ([el]
;;    (name el)))

(defn parse-tag [el]
  (cond
    ^boolean (keyword? el) (tag->impl el)
    ^boolean (var? el) (fn VarEl [& args] (apply el args))
    true el))

(defn make-element [config el args]
  (util/measure-perf
   "make_element"
   ((:create-element config) config el args)))

(defn parse [config hiccup]
  (util/measure-perf
   "parse"
   (make-element config (parse-tag (nth hiccup 0)) (rest hiccup))))

#?(:clj (defn array? [x]
          (coll? x)))

(defn ex [s]
  #?(:clj (Exception. s)
     :cljs (js/Error. s)))

(extend-protocol IElement
  nil
  (-as-element [_ _]
    (util/measure-perf
     "-as_element_nil"
     nil))

  #?(:clj Number
     :cljs number)
  (-as-element [n _]
    (util/measure-perf
     "-as_element_number"
     n))

  #?(:clj String
     :cljs string)
  (-as-element [s _]
    (util/measure-perf
     "-as_element_string"
     s))

  #?(:clj clojure.lang.PersistentVector
     :cljs PersistentVector)
  (-as-element [form config]
    (util/measure-perf
     "-as_element_vector"
     (make-element config (parse-tag (nth form 0)) (rest form))))

  #?(:clj clojure.lang.LazySeq
     :cljs LazySeq)
  (-as-element [a config]
    (util/measure-perf
     "-as_element_lazyseq"
     (make-element
      config
      (:fragment config)
      (cons nil (map #(-as-element % config) a)))))

  #?(:cljs array)
  #?(:cljs (-as-element [a config]
                        (util/measure-perf
                         "-as_element_array"
                         (make-element
                          config
                          (:fragment config)
                          (cons nil (map #(-as-element % config) a))))))

  #?(:clj Object
     :cljs default)
  (-as-element [el config]
    (cond
      ((:is-element? config) el) el

      :default
      (throw
       (ex (str "Unknown element type " (pr-str (type el))
                " found while parsing hiccup form: "
                (.toString el)))))))
