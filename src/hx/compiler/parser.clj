(ns hx.compiler.parser)

(defprotocol IElement
  (-parse-element [el props children] "Parses an element"))

;; we use a multimethod to dispatch on identity so that consumers
;; can override this for custom values e.g. :<> for React fragments
(defmulti parse-element
  (fn [el & more]
    (identity el))
  :default ::default)

;; if no multimethod for specific el, then apply general parsing rules
(defmethod parse-element
  ::default
  ([el]
   (parse-element el nil nil))
  ([el props & children]
   (-parse-element el props children)))

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
  (-parse-element [_ _ _]
    nil)
  java.lang.Long
  (-parse-element [n _ _]
    n)
  java.lang.String
  (-parse-element [s _ _]
    s)
  clojure.lang.Keyword
  (-parse-element [el props children]
    (apply make-node
           (name el)
           (if (vector? props) (parse-element props) props)
           (into [] (map parse-children children))))
  clojure.lang.APersistentVector
  (-parse-element [form _ _]
    (apply parse-element form))
  clojure.lang.PersistentList
  (-parse-element [form _ _]
    form)
  clojure.lang.LazySeq
  (-parse-element [form _ _]
    (doall form))
  clojure.lang.Symbol
  (-parse-element [sym props children]
    (apply make-node
           sym
           (if (vector? props) (parse-element props) props)
           (into [] (map parse-children children)))))

(defn parse [hiccup]
  (apply parse-element hiccup))

#_(parse [:div {:id "asdf"} "hi"])
#_(parse [:div [:span "wat"]])
#_(parse [:div [:span "wat"]
          [:input {:type "button"} [:i {:id "jkl"} "sup"]]])
#_(parse [:div.thing [:span "wat"]])
#_(parse [:div#thing [:span "wat"]])
#_(parse ['asdf [:span 'jkl "wat"]])
