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
(defnc HxWithRefArg [{:keys [label]} ref]
  [:input {:ref ref :data-testid "hx-input" :placeholder label}])

;; New pattern: ref as prop key (should work with both)
(defnc HxWithRefProp [{:keys [label ref]}]
  [:input {:ref ref :data-testid "hx-input-prop" :placeholder label}])

(t/deftest ref-forwarding-old-pattern
  (t/testing "Old [props ref] pattern still works"
    (let [input-ref (React/createRef)
          ;; Need to wrap with forwardRef for this pattern
          Wrapped (React/forwardRef HxWithRefArg)
          result (render (hx/f [Wrapped {:label "old-pattern" :ref input-ref}]))
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

(defnc MemoizedHxComponent [{:keys [value]}]
  {:wrap [(React/memo)]}
  [:div {:data-testid "memoized"} value])

(t/deftest wrap-option-still-works
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
      (t/is (= "valid" (.-textContent el)))))

  (t/testing "Pre-condition fails"
    ;; This should throw an assertion error
    (t/is (thrown? js/Error
                   (render (hx/f [WithPreCondition {:value 123}]))))))

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
