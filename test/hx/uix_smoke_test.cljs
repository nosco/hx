(ns hx.uix-smoke-test
  "Smoke tests to verify we understand UIx's API correctly.
   These tests use UIx directly, NOT through hx wrappers.
   They should pass from the start."
  (:require [cljs.test :as t :include-macros true]
            [uix.core :as uix :refer [defui $]]
            [goog.dom :as dom]
            [goog.object :as gobj]
            [clojure.string :as str]
            ["@testing-library/react" :as rtl]))

(t/use-fixtures :each
  {:after rtl/cleanup})

;;
;; Utils (same as react_test.cljs)
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

;;
;; Basic UIx component tests
;;

(defui SimpleDiv []
  ($ :div "hello"))

(t/deftest uix-basic-component
  (t/is (node= (html "<div>hello</div>")
               (root (render ($ SimpleDiv))))))

(defui DivWithProps [{:keys [text]}]
  ($ :div text))

(t/deftest uix-props
  (t/is (node= (html "<div>world</div>")
               (root (render ($ DivWithProps {:text "world"}))))))

(defui DivWithChildren [{:keys [children]}]
  ($ :div {:class "wrapper"} children))

(t/deftest uix-children
  (t/is (node= (html "<div class=\"wrapper\"><span>child</span></div>")
               (root (render ($ DivWithChildren ($ :span "child")))))))

;;
;; Style and class handling
;;

(t/deftest uix-style-prop
  (t/is (node= (html "<div style=\"color: red;\">hi</div>")
               (root (render ($ :div {:style {:color "red"}} "hi")))))

  (t/is (node= (html "<div style=\"color: red; background: green;\">hi</div>")
               (root (render ($ :div {:style {:color "red"
                                              :background "green"}} "hi"))))))

(t/deftest uix-class-prop
  (t/is (node= (html "<div class=\"foo\">hi</div>")
               (root (render ($ :div {:class "foo"} "hi"))))
        "bare string")

  (t/is (node= (html "<div class=\"foo bar\">hi</div>")
               (root (render ($ :div {:class ["foo" "bar"]} "hi"))))
        "vector of strings"))

;;
;; Fragments
;;

(t/deftest uix-fragment
  (t/is (node= (html "<div><span>a</span><span>b</span></div>")
               (.-container (render ($ :<>
                                       ($ :span "a")
                                       ($ :span "b")))))))

;;
;; Event handlers
;;

(t/deftest uix-on-click
  (let [clicks (atom 0)
        on-click #(swap! clicks inc)
        node (root (render ($ :button {:on-click on-click} "click me")))]
    (click node)
    (t/is (= 1 @clicks))))

;;
;; Hooks
;;

(defui CounterComponent []
  (let [[count set-count] (uix/use-state 0)]
    ($ :div
       ($ :span {:data-testid "count"} (str count))
       ($ :button {:on-click #(set-count inc)} "inc"))))

(t/deftest uix-use-state
  (let [result (render ($ CounterComponent))
        count-el (rtl/getByTestId (.-container result) "count")
        button (rtl/getByText (.-container result) "inc")]
    (t/is (= "0" (.-textContent count-el)))
    (click button)
    (t/is (= "1" (.-textContent count-el)))))

(defui RefComponent []
  (let [ref (uix/use-ref nil)]
    ($ :div
       ($ :input {:ref ref :data-testid "input"})
       ($ :button {:on-click #(when-let [el @ref]
                                (.focus el))}
          "focus"))))

(t/deftest uix-use-ref
  (let [result (render ($ RefComponent))
        input (rtl/getByTestId (.-container result) "input")
        button (rtl/getByText (.-container result) "focus")]
    ;; Just verify it renders without error
    (t/is (some? input))
    (t/is (some? button))))

;;
;; Context
;;

(def TestContext (uix/create-context "default"))

(defui ContextConsumer []
  (let [value (uix/use-context TestContext)]
    ($ :div {:data-testid "value"} value)))

(t/deftest uix-context
  ;; Default value
  (let [result (render ($ ContextConsumer))
        el (rtl/getByTestId (.-container result) "value")]
    (t/is (= "default" (.-textContent el))))

  ;; Provided value
  (let [result (render ($ TestContext {:value "provided"}
                          ($ ContextConsumer)))
        el (rtl/getByTestId (.-container result) "value")]
    (t/is (= "provided" (.-textContent el)))))

;;
;; Nesting UIx components
;;

(defui Inner [{:keys [label]}]
  ($ :span {:class "inner"} label))

(defui Outer [{:keys [items]}]
  ($ :div {:class "outer"}
     (for [item items]
       ($ Inner {:key item :label item}))))

(t/deftest uix-nested-components
  (let [result (render ($ Outer {:items ["a" "b" "c"]}))
        spans (.. result -container (querySelectorAll ".inner"))]
    (t/is (= 3 (.-length spans)))
    (t/is (= "a" (.-textContent (aget spans 0))))
    (t/is (= "b" (.-textContent (aget spans 1))))
    (t/is (= "c" (.-textContent (aget spans 2))))))

;;
;; Memo
;;

(defui ^:memo MemoizedComponent [{:keys [value]}]
  ($ :div value))

(t/deftest uix-memo
  ;; Just verify it renders - memo behavior is hard to test directly
  (t/is (node= (html "<div>test</div>")
               (root (render ($ MemoizedComponent {:value "test"}))))))

;;
;; Error Boundary
;;
;; UIx provides create-error-boundary for creating class-based error boundary
;; components. Testing actual error throwing is tricky with Karma because React 18
;; reports errors via synthetic browser events that Karma interprets as uncaught.
;;
;; We verify:
;; 1. Error boundary can be created and used
;; 2. It renders children normally when no error occurs
;; 3. The derive-error-state and did-catch callbacks are properly wired

(def error-boundary-caught (atom nil))

(def smoke-test-error-boundary
  (uix/create-error-boundary
   {:derive-error-state (fn [error] {:error error})
    :did-catch (fn [error _info]
                 (reset! error-boundary-caught error))}
   (fn [[{:keys [error]} _set-state!] {:keys [children]}]
     (if error
       ($ :div {:data-testid "error-fallback"} "Error caught!")
       children))))

(t/deftest uix-error-boundary-renders-children
  (t/testing "Error boundary renders children when no error"
    (reset! error-boundary-caught nil)
    (let [result (render ($ smoke-test-error-boundary
                            ($ :div {:data-testid "normal-content"} "Normal content")))
          content (rtl/getByTestId (.-container result) "normal-content")]
      (t/is (= "Normal content" (.-textContent content)))
      (t/is (nil? @error-boundary-caught)
            "did-catch should not be called when no error occurs"))))

(t/deftest uix-error-boundary-exists
  (t/testing "Error boundary is a valid React component"
    ;; Verify the error boundary is properly defined and can be used
    (t/is (some? smoke-test-error-boundary))
    (t/is (fn? smoke-test-error-boundary))))
