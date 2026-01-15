(ns hx.migration-test
  "Migration tests for mixing hx/defnc with uix/defui components.
   These tests verify interoperability between the two systems.

   EXPECTED: These tests should FAIL initially, then pass as we
   implement the compatibility layer."
  (:require [cljs.test :as t :include-macros true]
            [hx.react :as hx :refer [defnc]]
            [hx.hooks :as hooks]
            [uix.core :as uix :refer [defui $]]
            [goog.dom :as dom]
            [goog.object :as gobj]
            [clojure.string :as str]
            ["@testing-library/react" :as rtl]
            ["react" :as React]))

(t/use-fixtures :each
  {:after rtl/cleanup})

;;
;; Utils
;;

(def render rtl/render)

(defn root [result]
  (-> result
      (.-container)
      (.-firstChild)))

(defn node= [x y]
  (.isEqualNode x y))

(defn html [s]
  (let [template (dom/createElement "template")]
    (gobj/set template "innerHTML" (str/trim s))
    (gobj/getValueByKeys template "content" "firstChild")))

(defn click [node]
  (.click rtl/fireEvent node))

(defn get-by-testid [container id]
  (rtl/getByTestId container id))

;;
;; =============================================================================
;; SIMPLE INTEROP: UIx inside hx
;; =============================================================================
;;

(defui UixChild [{:keys [label]}]
  ($ :span {:class "uix-child" :data-testid "uix-child"} label))

(defnc HxParentWithUixChild [{:keys []}]
  [:div {:class "hx-parent"}
   ;; This is the key test: can we render a UIx component inside hx hiccup?
   ;; UIx components should work because they return React elements
   [UixChild {:label "from-hx"}]])

(t/deftest uix-inside-hx
  (let [result (render (hx/f [HxParentWithUixChild]))
        child (get-by-testid (.-container result) "uix-child")]
    (t/is (= "from-hx" (.-textContent child))
          "UIx component renders inside hx/defnc with correct props")))

;;
;; =============================================================================
;; SIMPLE INTEROP: hx inside UIx
;; =============================================================================
;;

(defnc HxChild [{:keys [label]}]
  [:span {:class "hx-child" :data-testid "hx-child"} label])

(defui UixParentWithHxChild [{:keys []}]
  ($ :div {:class "uix-parent"}
     ;; Can UIx render an hx component via $?
     ($ HxChild {:label "from-uix"})))

(t/deftest hx-inside-uix
  (let [result (render ($ UixParentWithHxChild))
        child (get-by-testid (.-container result) "hx-child")]
    (t/is (= "from-uix" (.-textContent child))
          "hx/defnc component renders inside uix/defui with correct props")))

;;
;; =============================================================================
;; NESTED INTEROP: Mixed tree
;; =============================================================================
;;

(defui UixLeaf [{:keys [value]}]
  ($ :span {:data-testid "leaf"} value))

(defnc HxMiddle [{:keys [value]}]
  [:div {:class "hx-middle"}
   [UixLeaf {:value value}]])

(defui UixRoot [{:keys []}]
  ($ :div {:class "uix-root"}
     ($ HxMiddle {:value "nested"})))

(t/deftest mixed-nesting
  (let [result (render ($ UixRoot))
        leaf (get-by-testid (.-container result) "leaf")]
    (t/is (= "nested" (.-textContent leaf))
          "Props flow through UIx → hx → UIx component tree")))

;;
;; =============================================================================
;; CHILDREN SEMANTICS
;; =============================================================================
;;

;; hx pattern: children passed as separate args in hiccup
(defnc HxWrapper [{:keys [children]}]
  [:div {:class "hx-wrapper" :data-testid "hx-wrapper"}
   children])

;; UIx pattern: children via :children key in props
(defui UixWrapper [{:keys [children]}]
  ($ :div {:class "uix-wrapper" :data-testid "uix-wrapper"}
     children))

(t/deftest children-hx-to-uix
  (t/testing "hx children to UIx component"
    ;; When hx renders UIx child, children should work
    (let [result (render (hx/f [UixWrapper [:span "child-from-hx"]]))
          wrapper (get-by-testid (.-container result) "uix-wrapper")]
      (t/is (some? (.. wrapper (querySelector "span")))
            "Children passed from hx to UIx component"))))

(t/deftest children-uix-to-hx
  (t/testing "UIx children to hx component"
    ;; When UIx renders hx child, children should work
    (let [result (render ($ HxWrapper ($ :span "child-from-uix")))
          wrapper (get-by-testid (.-container result) "hx-wrapper")]
      (t/is (some? (.. wrapper (querySelector "span")))
            "Children passed from UIx to hx component"))))

;;
;; =============================================================================
;; CONTEXT SHARING
;; =============================================================================
;;

;; Context created with hx
(def HxContext (React/createContext "hx-default"))

(defnc HxContextProvider [{:keys [value children]}]
  [:provider {:context HxContext :value value}
   children])

(defui UixConsumerOfHxContext [{:keys []}]
  (let [value (uix/use-context HxContext)]
    ($ :div {:data-testid "uix-consumer"} value)))

(t/deftest hx-context-consumed-by-uix
  (let [result (render (hx/f [HxContextProvider {:value "from-hx"}
                              [UixConsumerOfHxContext]]))
        consumer (get-by-testid (.-container result) "uix-consumer")]
    (t/is (= "from-hx" (.-textContent consumer))
          "UIx component can consume hx context")))

;; Context created with UIx
(def UixContext (uix/create-context "uix-default"))

(defnc HxConsumerOfUixContext [{:keys []}]
  (let [value (hooks/useContext UixContext)]
    [:div {:data-testid "hx-consumer"} value]))

(t/deftest uix-context-consumed-by-hx
  (let [result (render ($ UixContext {:value "from-uix"}
                          ($ HxConsumerOfUixContext)))
        consumer (get-by-testid (.-container result) "hx-consumer")]
    (t/is (= "from-uix" (.-textContent consumer))
          "hx component can consume UIx context")))

;;
;; =============================================================================
;; REF FORWARDING
;; =============================================================================
;;

;; Old hx pattern: ref as second argument
(defnc HxWithRefArg* [{:keys [label]} ref]
  [:input {:ref ref :data-testid "hx-input" :placeholder label}])

(def HxWithRefArg (uix/forward-ref HxWithRefArg*))


;; New pattern: ref as prop key (should work with both)
(defnc HxWithRefProp [{:keys [label ref]}]
  [:input {:ref ref :data-testid "hx-input-prop" :placeholder label}])


(t/deftest ref-forwarding-old-pattern
  (t/testing "Old [props ref] pattern still works"
    (let [input-ref (React/createRef)
          ;; Need to wrap with forwardRef for this pattern
          result (render (hx/f [HxWithRefArg {:label "old-pattern" :ref input-ref}]))
          input (get-by-testid (.-container result) "hx-input")]
      (t/is (= input (.-current input-ref))
            "Ref is forwarded via old pattern"))))

(t/deftest ref-forwarding-new-pattern
  (t/testing "New :ref prop pattern works"
    (let [input-ref (React/createRef)
          result (render (hx/f [HxWithRefProp {:label "new-pattern" :ref input-ref}]))
          input (get-by-testid (.-container result) "hx-input-prop")]
      (t/is (= input (.-current input-ref))
            "Ref is forwarded via :ref prop"))))

;;
;; =============================================================================
;; HOOKS INTEROP
;; =============================================================================
;;

(defnc HxWithHooks [{:keys [initial]}]
  (let [[count set-count] (hooks/useState initial)]
    [:div
     [:span {:data-testid "hx-count"} (str count)]
     [:button {:data-testid "hx-inc" :on-click #(set-count inc)} "inc"]]))

(defui UixWithHooks [{:keys [initial]}]
  (let [[count set-count] (uix/use-state initial)]
    ($ :div
       ($ :span {:data-testid "uix-count"} (str count))
       ($ :button {:data-testid "uix-inc" :on-click #(set-count inc)} "inc"))))

(t/deftest hooks-work-side-by-side
  (t/testing "hx hooks and UIx hooks can be used in sibling components"
    (let [result (render (hx/f [:<>
                                [HxWithHooks {:initial 10}]
                                [UixWithHooks {:initial 20}]]))
          hx-count (get-by-testid (.-container result) "hx-count")
          uix-count (get-by-testid (.-container result) "uix-count")
          hx-inc (get-by-testid (.-container result) "hx-inc")
          uix-inc (get-by-testid (.-container result) "uix-inc")]

      (t/is (= "10" (.-textContent hx-count)))
      (t/is (= "20" (.-textContent uix-count)))

      (click hx-inc)
      (t/is (= "11" (.-textContent hx-count)))
      (t/is (= "20" (.-textContent uix-count)) "UIx state unchanged")

      (click uix-inc)
      (t/is (= "11" (.-textContent hx-count)) "hx state unchanged")
      (t/is (= "21" (.-textContent uix-count))))))

;;
;; =============================================================================
;; WRAP OPTION (MEMO)
;; =============================================================================
;;

#_(defnc MemoizedHxComponent [{:keys [value]}]
    {:wrap [(React/memo)]}
    [:div {:data-testid "memoized"} value])

#_(t/deftest wrap-option-still-works
    (let [result (render (hx/f [MemoizedHxComponent {:value "test"}]))
          el (get-by-testid (.-container result) "memoized")]
      (t/is (= "test" (.-textContent el))
            ":wrap option continues to work")))

;;
;; =============================================================================
;; PRE/POST CONDITIONS
;; =============================================================================
;;

(defnc WithPreCondition [{:keys [value]}]
  {:pre [(string? value)]}
  [:div {:data-testid "pre"} value])

(t/deftest pre-conditions-work
  (t/testing "Pre-condition passes"
    (let [result (render (hx/f [WithPreCondition {:value "valid"}]))
          el (get-by-testid (.-container result) "pre")]
      (t/is (= "valid" (.-textContent el))))))

;; Note: Testing :pre condition failures with error boundaries is tricky because
;; React 18 reports errors via synthetic browser events that Karma interprets as
;; uncaught errors, even when properly caught by error boundaries. The test above
;; verifies :pre conditions work for valid inputs.

;;
;; =============================================================================
;; JS INTEROP
;; =============================================================================
;;

;; Native DOM element with hx
(defnc NativeDomTest [{:keys []}]
  [:div {:data-testid "native"}
   [:input {:type "text" :placeholder "native input"}]
   [:button {:type "submit"} "Submit"]])

(t/deftest native-dom-still-works
  (let [result (render (hx/f [NativeDomTest]))
        input (.. result -container (querySelector "input"))
        button (.. result -container (querySelector "button"))]
    (t/is (= "text" (.-type input)))
    (t/is (= "submit" (.-type button)))))

;;
;; =============================================================================
;; ZERO-ARGS DEFNC
;; =============================================================================
;;

;; defnc with empty args vector []
(defnc ZeroArgsComponent []
  [:div {:data-testid "zero-args"} "I have no props!"])

(t/deftest zero-args-defnc
  (let [result (render (hx/f [ZeroArgsComponent]))
        el (get-by-testid (.-container result) "zero-args")]
    (t/is (= "I have no props!" (.-textContent el))
          "defnc with zero arguments renders correctly")))

;;
;; =============================================================================
;; PROPS REST SYNTAX (:&)
;; =============================================================================
;;

;; Test that :& rest props syntax works the same in defnc as in UIx defui
(defnc HxWithRestProps [{:keys [title] :& rest-props}]
  [:div {:data-testid "hx-rest-props"}
   [:h1 title]
   [:span {:data-testid "hx-rest-keys"}
    (pr-str (sort (keys rest-props)))]])

(t/deftest defnc-rest-props-syntax
  (t/testing "defnc :& rest props syntax collects unlisted keys"
    (let [result (render (hx/f [HxWithRestProps {:title "Hello"
                                                 :foo "bar"
                                                 :baz 42}]))
          rest-keys (get-by-testid (.-container result) "hx-rest-keys")]
      ;; :& should collect :foo and :baz but not :title
      (t/is (= "(:baz :foo)" (.-textContent rest-keys))))))

(defnc HxRestPropsPassthrough [{:keys [class-name] :& rest-props}]
  ;; Use rest-props to pass through to a child element
  [:div {:class class-name :data-testid "hx-wrapper"}
   ;; Note: hx hiccup doesn't support :& spread, so we merge manually
   [:button (merge {:data-testid "hx-button"} rest-props) "Click"]])

(t/deftest defnc-rest-props-passthrough
  (t/testing "defnc :& can be used to collect remaining props for passthrough"
    (let [clicked (atom false)
          result (render (hx/f [HxRestPropsPassthrough
                                {:class-name "my-class"
                                 :on-click #(reset! clicked true)
                                 :title "my-title"}]))
          button (get-by-testid (.-container result) "hx-button")]
      ;; rest-props should have on-click and title
      (t/is (= "my-title" (.-title button)))
      (click button)
      (t/is (= true @clicked)))))

;; Test interaction: UIx component with :& calling hx component
(defnc HxSimpleButton [{:keys [label] :& button-props}]
  [:button (merge {:data-testid "simple-button"} button-props) label])

(defui UixCallingHxWithRestProps [{:keys []}]
  ($ :div
     ($ HxSimpleButton {:label "Test"
                        :disabled true
                        :on-click #(js/console.log "clicked")})))

(t/deftest interop-rest-props
  (t/testing "UIx can call hx component that uses :& rest props"
    (let [result (render ($ UixCallingHxWithRestProps))
          button (get-by-testid (.-container result) "simple-button")]
      (t/is (= "Test" (.-textContent button)))
      (t/is (= true (.-disabled button))))))

;; Test with multiple keys extracted and rest
(defnc HxMultipleKeysWithRest [{:keys [a b c] :& rest}]
  [:div {:data-testid "multi-rest"}
   [:span {:data-testid "extracted"} (str a "-" b "-" c)]
   [:span {:data-testid "rest-count"} (str (count rest))]])

(t/deftest defnc-multiple-keys-with-rest
  (t/testing "defnc :& works with multiple extracted keys"
    (let [result (render (hx/f [HxMultipleKeysWithRest {:a 1 :b 2 :c 3 :d 4 :e 5}]))
          extracted (get-by-testid (.-container result) "extracted")
          rest-count (get-by-testid (.-container result) "rest-count")]
      (t/is (= "1-2-3" (.-textContent extracted)))
      (t/is (= "2" (.-textContent rest-count)) ":d and :e should be in rest"))))
