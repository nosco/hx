(ns hx.react
  (:require [goog.object :as gobj]
            ["react" :as react]
            [hx.hiccup :as hiccup]
            [hx.utils :as utils :include-macros true])
  (:require-macros [hx.react]))

(defn- props [el first-arg props?]
  (cond
    (and (string? el) props?) (utils/clj->props first-arg)

    props? (utils/clj->props first-arg false)

    true nil))

(defn- fn-as-child [config first-child args]
  (fn [& args]
    (let [ret (apply first-child args)]
      (if (vector? ret)
        (hiccup/-as-element ^non-native ret config)
        ret))))

(defn create-element [config el args]
  (utils/measure-perf
   "create_element"
   (let [first-arg (utils/measure-perf
                    "first-arg"
                    (nth args 0 nil))
         props? (map? first-arg)
         props (utils/measure-perf
                "props"
                (props el first-arg props?))
         children (utils/measure-perf
                   "children"
                   (if props? (-rest args) args))
         first-child (utils/measure-perf
                      "first-child"
                      (nth children 0 nil))]
     (case (count children)
       0 (utils/measure-perf
          "no_children"
          (react/createElement el props))
       1 (if (utils/measure-perf
              "fn?"
              ^boolean (goog/isFunction first-child))
           (react/createElement el
                                nil
                                ;; fn-as-child
                                ;; wrap in a function to parse hiccup from render-fn
                                (fn-as-child config first-child args))
           (utils/measure-perf
            "one_child"
            (react/createElement el props (utils/measure-perf
                                           "one_child_parse"
                                           (hiccup/-as-element
                                            first-child config)))))
       ;; use .apply here for performance
       (utils/measure-perf
        "has_children"
        (.apply
         react/createElement nil
         (utils/measure-perf
          "children_parse_loop"
          (loop [a #js [el props]
                 c children]
            (if-not (nil? c)
              (do
                (.push a (hiccup/-as-element
                          (-first c) config))
                (recur
                 a
                 (-next c)))
              a)))))))))

(def react-hiccup-config
  {:create-element create-element
   :is-element? react/isValidElement
   :fragment react/Fragment})

(defn f [form]
  (hiccup/parse react-hiccup-config form))

(defn parse-body [body]
  (if (vector? body)
    (f body)
    body))

(declare props->clj)

(def fragment react/Fragment)

(hiccup/extend-tag :<> hx.react/fragment)

(hx.react/defnc Provider [{:keys [context value children]}]
  [(.-Provider ^js context)
   {:value value}
   children])

(hiccup/extend-tag :provider Provider)


(comment
  (also-as {:a 1} :a :b)

  (also-as {:a 1} :b :c)

  )

(defn props->clj [props]
  (let [props (utils/shallow-js->clj props :keywordize-keys true)]
    ;; provide `:class-name` property also as `:class` for backwards compat
    (-> props
        (utils/also-as :className :class)
        (utils/also-as :class :class-name)
        (utils/also-as :htmlFor :for)
        ;; (also-as :for :htmlFor)
        )))

(comment
  (props->clj #js {"x0" 1})

  (props->clj #js {"test0" 1})

  (props->clj #js {"testAsdf?" 1})

  (props->clj #js {"test_asdf?" 1})

  (props->clj #js {"testTest.asdf?" 1})

  (props->clj #js {"class" ["asdf"]})
  )

(defn $ [el & args] (hiccup/make-element react-hiccup-config el args))

(defn assign-methods [class method-map]
  (doseq [[method-name method-fn] method-map]
    (gobj/set (.-prototype class)
              (munge (name method-name))
              method-fn))
  class)

(defn create-class [super-class init-fn static-properties method-names]
  (let [ctor (fn [props]
               (this-as this
                 ;; auto-bind methods
                 (doseq [method method-names]
                   (gobj/set this (munge method)
                             (.bind (gobj/get this (munge method)) this)))

                 (init-fn this props)))]
    ;; set static properties on prototype
    (goog/inherits ctor super-class)
    (doseq [[k v] static-properties]
      (gobj/set ctor k v))
    ctor))

(defn create-component [init-fn static-properties method-names]
  (create-class react/Component init-fn static-properties method-names))

(defn create-pure-component [init-fn static-properties method-names]
  (create-class react/PureComponent init-fn static-properties method-names))

(def create-context
  "Just react/createContext"
  react/createContext)

(defn factory
  "Takes a React component, and creates a function that returns
  a new React element"
  [component]
  (partial $ component))

