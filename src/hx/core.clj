(ns hx.core
  (:require [clojure.zip :as zip]
            [hx.utils :as utils])
  (:refer-clojure :exclude [compile]))

(defprotocol IElement
  (-create-element [el options props children] "Creates an element"))

;; we use a multimethod to dispatch on identity so that consumers
;; can override this for custom values e.g. :<> for React fragments
(defmulti create-element
  (fn [options el & more]
    (identity el))
  :default ::default)

;; if no multimethod for specific el, then apply general parsing rules
(defmethod create-element
  ::default
  ([options el]
   (create-element options el nil nil))
  ([options el props & children]
   (-create-element el options props children)))

;; handle symbols and other forms inside of child seqs
(defn create-children [options form]
  (if (vector? form)
    (create-element options form)
    form))

;; optimize case when map literal is passed in for props
(defn convert-literal-props [props]
  (if (map? props)
    (utils/shallow-clj->js props)
    props))

(extend-protocol IElement
     nil
     (-create-element [_ _ _ _]
       nil)
     java.lang.Long
     (-create-element [n _ _ _]
       n)
     java.lang.String
     (-create-element [s _ _ _]
       s)
     clojure.lang.Keyword
     (-create-element [el options props children]
       (if (vector? props)
         `(apply ~(:create-element options)
                 ~(name el)
                 nil
                 ~(into [] (map (partial create-children options)
                                (cons props children))))
         `(apply ~(:create-element options)
                 ~(name el)
                 ~(convert-literal-props props)
                 ~(into [] (map (partial create-children options) children)))))
     clojure.lang.APersistentVector
     (-create-element [form options _ _]
       (apply create-element options form))
     clojure.lang.PersistentList
     (-create-element [form _ _ _]
       form)
     clojure.lang.LazySeq
     (-create-element [form _ _ _]
       (doall form))
     clojure.lang.Symbol
     (-create-element [sym options props children]
       (if (vector? props)
         `(apply ~(:create-element options)
                 ~sym
                 nil
                 ~(into [] (map (partial create-children options)
                                (cons props children))))
         `(apply ~(:create-element options)
                 ~sym
                 ~(convert-literal-props props)
                 ~(into [] (map (partial create-children options) children))))))

(defn compile [options hiccup]
  (apply create-element options hiccup))

(defmacro compile* [options hiccup]
  (compile options hiccup))

(defn seq-vec-zip [root]
  (zip/zipper
   ;; branch?
   (fn [v] (or (seq? v) (vector? v)))

   ;; children
   seq

   ;; make-node
   (fn [node children]
     (if (vector? node)
       (with-meta (vec children) (meta node))
       (with-meta children (meta node))))

   root))

(defn convert-compile-sym [sym options body]
  (loop [loc (seq-vec-zip body)]
    (if (not (zip/end? loc))
      (let [node (zip/node loc)]
        (if (= node sym)
          ;; remove the $ symbol
          (let [no$ (zip/remove loc)
                next (zip/next no$)]
            (-> next
                ;; add the hx macro to the next form
                (zip/replace
                 (list 'hx.core/compile* options (zip/node next)))
                (recur)))

          (recur (zip/next loc))))
      (zip/root loc))))
