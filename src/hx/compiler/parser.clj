(ns hx.compiler.parser)

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

;; handle symbols and other forms inside of child seqs
(defn parse-children [form]
  (if (vector? form)
    (parse-element form)
    form))

(defn make-node [el & args]
  {:hx/parsed true
   :el el
   :args args})

(extend-protocol IElement
  nil
  (-parse-element [_ _]
    nil)
  java.lang.Long
  (-parse-element [n _]
    n)
  java.lang.String
  (-parse-element [s _]
    s)
  clojure.lang.Keyword
  (-parse-element [el args]
    (let [props (first args)
          children (rest args)]
      (apply make-node
             (name el)
             (if (vector? props) (parse-element props) props)
             (into [] (map parse-children children)))))
  clojure.lang.APersistentVector
  (-parse-element [form _]
    (apply parse-element form))
  clojure.lang.PersistentList
  (-parse-element [form _]
    form)
  clojure.lang.LazySeq
  (-parse-element [form _]
    (doall form))
  clojure.lang.Symbol
  (-parse-element [sym args]
    (let [props (first args)
          children (rest args)]
      (apply make-node
             sym
             (if (vector? props) (parse-element props) props)
             (into [] (map parse-children children))))))

(defn parse [hiccup]
  (apply parse-element hiccup))

(def interceptor
  {:name :hx.compiler/parser
   :enter (fn [context]
            (assoc context ::out
                   (parse (::form context))))})

#_(parse [:div {:id "asdf"} "hi"])
#_(parse [:div [:span "wat"]])
#_(parse [:div [:span "wat"]
          [:input {:type "button"} [:i {:id "jkl"} "sup"]]])
#_(parse [:div.thing [:span "wat"]])
#_(parse [:div#thing [:span "wat"]])
#_(parse ['asdf [:span 'jkl "wat"]])
