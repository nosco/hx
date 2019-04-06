(ns workshop.core
  (:require [devcards.core :as dc :include-macros true]
            ["react" :as react]
            ["react-dom" :as react-dom]
            [hx.react :as hx]
            [hx.hooks :as hooks]
            ["scheduler" :as scheduler]))

(defn Example [props]
  (react/createElement "div" nil (prn-str props)))

(dc/defcard hiccup
  (hx/f [:div {:style {:background-color "red"}}
         [:h1 "foo"]
         [:button {:on-click #(js/alert "hi")} "click"]]))

(hx/defnc DefncExample [{:keys [foo children]}]
  [:<>
   [:div "mm"]
   [:div foo]
   (let [x 1
         y 2]
     [:div (+ x y)])
   (for [n [1 2 3]]
     [:div {:key n} (+ n 1)])
   children])

(dc/defcard defnc
  (hx/$ DefncExample {:foo "bar"} "child"))

(hx/defnc ChildrenEx [{:keys [children]}]
  [:div {:id "childrenex"} children])

(dc/defcard children
  (hx/f [ChildrenEx
         [:span "hi"]
         [:strong "bye"]]))

(hx/defnc SingleLetters [{:keys [k v]}]
  [:div "Working: " (str (not= k v))])

(dc/defcard single-letters
  (hx/f [SingleLetters {:k 1 :v 2}]))

(hx/defnc VarQuote [_]
  [:div "var qoute"])

(dc/defcard var-quote
  (hx/f [#'VarQuote]))

(hx/defnc Rc [{:keys [children]}]
  [:div
   (children 3)])

(dc/defcard render-fn-child
  (hx/$ Rc
        (fn [n]
          [:<>
           [:div (hx/$ "span" "hi")]
           [:span {:style {:color "red"}} (+ n 1)]])))

(hx/defnc Shallow* [{:keys [name]}]
  [:div "Hello " [:span {:style {:color "blue"}} name] "!"])

(dc/defcard shallow
  (hx/shallow-render (Shallow* {:name "Will"} nil)))

(hx/defcomponent ClassComp
  (constructor [this]
               this)
  (render [this]
          [:h1 "foo"]))

(dc/defcard class-component
  (hx/$ ClassComp))

(def some-context (hx/create-context "default context value"))

(hx/defnc ContextConsumer [_]
  [:div
   [(.-Consumer some-context)
    (fn [v]
      [:div v])]])

(dc/defcard context-default
  (hx/$ ContextConsumer))

(hx/defnc ContextProvider [_]
  [:provider {:context some-context
              :value "provider context value"}
   [:div
    [ContextConsumer]]])

(dc/defcard context
  (hx/$ ContextProvider))

(hx/defnc RefConsumer* [{:keys [on-click] :as props} ref]
  [:button {:ref ref :on-click on-click} "Click me"])

(def RefConsumer (react/forwardRef RefConsumer*))

(hx/defnc RefProvider [_]
  (def ref (react/createRef))
  [RefConsumer {:ref ref :on-click #(println ref)}])

(dc/defcard ref
  (hx/$ RefProvider))

(hx/defnc ComponentOne [_]
  [:<>
   [:div "hi"]
   [:div "bye"]])

(dc/defcard strict-mode
  (hx/f
   [react/StrictMode
    [:div "hello"]
    [ComponentOne]]))

(hx/defnc PrePostPass [_]
  {:pre [(true? true)]
   :post [(not false)]}
  [:div "Pre-post always passes"])

(hx/defnc PreCond [{:keys [name]}]
  {:pre [(= name "Suzy")]}
  [:div "Pre pass: " name])

(hx/defnc PostCond [{:keys [age]}]
  {:post [(= % [:div "Age: " 19])]}
  [:div "Age: " age])

(hx/defcomponent ErrorBoundary
  (constructor
   [this]
   (set! (. this -state)
         #js {:hasError false
              :message ""})
   this)

  ^:static (getDerivedStateFromError
            (fn [error]
              #js {:hasError true
                   :message (.toString error)}))

  (render
   [this]
   (if (.. this -state -hasError)
     [:div "💥 " (.. this -state -message)]
     (.. this -props -children))))

(dc/defcard pre-post
  (hx/f [:<>
         [PrePostPass]
         [ErrorBoundary
          [PreCond {:name "Suzy"}]]
         [ErrorBoundary
          [PreCond {:name "Bill"}]]
         [ErrorBoundary
          [PostCond {:age 19}]]
         [ErrorBoundary
          [PostCond {:age 42}]]]))

(hx/defnc ClassNameProp [{:keys [class class-name]}]
  [:<>
   [:ul
    [:li "class: \"" class "\""]
    [:li "class-name: \"" class-name "\""]]
   (str (= class class-name))])

(dc/defcard class-name-prop
  (hx/f [ClassNameProp {:class "foo"}]))

(hx/defnc NamespaceKeywords [{:keys [namespace/value] :as props}]
  [:<>
   [:div (prn-str props)]
   [:div "namespace/value: " value]])

(dc/defcard namespace-keywords
  (hx/f [NamespaceKeywords {:namespace/value "hhhhh"}]))

(hx/defnc StateWithEffect [{:keys [receive]}]
  (let [[count update-count] (hooks/<-state 0)]
    (hooks/<-effect
     (fn []
       (js/setTimeout
        (fn []
          (prn count))
        3000)))
    [:div {:on-click #(update-count inc)}
     count]))

(dc/defcard state-with-effect
  (hx/f [StateWithEffect]))

(def lifecycle (atom nil))

(add-watch lifecycle :lifecycle (fn [_ _ o n] (prn "watch:" n)))

(hx/defnc WhenApplied [_]
  (reset! lifecycle :rendering)
  (let [[count update-count] (hooks/<-state 0)]
    (reset! lifecycle nil)
    [:div {:on-click #(update-count (fn [n]
                                      (prn "update:" @lifecycle)
                                      (inc n)))}
     count]))

(dc/defcard when-applied
  (hx/f [WhenApplied]))

(def schedule (atom :high))

(hx/defnc Scheduler [_]
  (let [[updates set-updates] (hooks/<-state [])]
    [:div
     [:div "Updates: " (prn-str updates)]
     [:div
      [:input
       {:on-change (fn update []
                     (case @schedule
                       :low (do
                              (reset! schedule :high)
                              (scheduler/unstable_scheduleCallback
                               #(set-updates conj :low)))
                       :high (do
                               (reset! schedule :low)
                               (set-updates conj :high))))}]]
     [:div [:button {:on-click #(set-updates [])} "reset"]]]))

(dc/defcard scheduler
  (hx/f [react/unstable_ConcurrentMode
         [Scheduler]]))

(def Hello (hx/fnc Hello [_] [:div "Fnc"]))

(dc/defcard fnc
  (hx/f [Hello]))

(defn hoc-example [c]
  (let [hoc (hx/fnc Example [_]
                    [:h1 [c]])]
    (set! (.-displayName hoc) (str "Header(" (.-displayName c) ")"))
    hoc))

(defn hoc-example2 [color]
  (fn example2 [c]
    (let [hoc (hx/fnc Example [_]
                      [:div {:style {:color color}} [c]])]
      (set! (.-displayName hoc) (str "Color(" (.-displayName c) ")"))
      hoc)))

(hx/defnc HoCWrapper [_]
  {:wrap [hoc-example
          (react/memo =)
          ((hoc-example2 "green"))
          ]}
  [:span "HoC should be header and green"])

(dc/defcard hoc-wrapper
  (hx/f [HoCWrapper]))


(hx/defnc Clones [{:keys [children]}]
  [:<>
   [:div "Cloned:"
    (react/cloneElement children #js {:className "foo"})]
   ;; [:div "Not cloned:"
   ;;  children]
   ])

(hx/defnc Cloned [{:keys [className class] :as props}]
  (js/console.log props)
  [:<>
   [:div "className: " className]
   [:div "class: " class]])

(dc/defcard clone-element
  (hx/f [Clones [Cloned]]))
