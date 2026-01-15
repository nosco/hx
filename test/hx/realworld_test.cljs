(ns hx.realworld-test
  "Tests based on real-world usage patterns from nosco-gamma.
   These represent actual component patterns that must continue working."
  (:require [cljs.test :as t :include-macros true]
            [hx.react :as hx :refer [defnc]]
            [hx.hooks :as hooks]
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

(defn get-by-text [container text]
  (rtl/getByText container text))

;;
;; =============================================================================
;; PATTERN: Simple component with children
;; From: nosco/ui/dropdowns.cljs
;; =============================================================================
;;

(defnc ActionListSection [{:keys [children]}]
  [:div {:class ["nosco-action-list-section"] :data-testid "section"}
   children])

(t/deftest pattern-simple-children
  (let [result (render (hx/f [ActionListSection
                              [:span "Item 1"]
                              [:span "Item 2"]]))
        section (get-by-testid (.-container result) "section")
        spans (.. section (querySelectorAll "span"))]
    (t/is (= 2 (.-length spans)))))

;;
;; =============================================================================
;; PATTERN: Component with hooks (useState)
;; From: nosco/views/userlevel/evaluation_center_single_team.cljs
;; =============================================================================
;;

(defnc EvaluatorComments [{:keys [value comments]}]
  (let [[showing set-showing] (hooks/useState false)]
    [:<>
     (when showing
       [:div {:class "popup" :data-testid "popup"}
        (for [c comments]
          [:div {:key (:id c)} (:text c)])])
     [:button {:data-testid "toggle"
               :on-click #(set-showing not)}
      value]]))

(t/deftest pattern-hooks-toggle
  (let [comments [{:id "1" :text "Comment 1"} {:id "2" :text "Comment 2"}]
        result (render (hx/f [EvaluatorComments {:value "Show" :comments comments}]))
        container (.-container result)
        toggle (get-by-testid container "toggle")]

    ;; Initially popup not showing
    (t/is (nil? (rtl/queryByTestId container "popup")))

    ;; Click to show
    (click toggle)
    (t/is (some? (get-by-testid container "popup")))

    ;; Click to hide
    (click toggle)
    (t/is (nil? (rtl/queryByTestId container "popup")))))

;;
;; =============================================================================
;; PATTERN: Context creation and usage
;; From: nosco/ui/dropdowns.cljs
;; =============================================================================
;;

(def CloseContext (React/createContext (fn [] (js/console.log "default close"))))

(defnc MenuItem [{:keys [label]}]
  (let [close-fn (hooks/useContext CloseContext)]
    [:button {:data-testid "menu-item"
              :on-click #(do
                           (js/console.log "clicked" label)
                           (close-fn))}
     label]))

(defnc Menu [{:keys [children on-close]}]
  [:provider {:context CloseContext :value on-close}
   [:div {:class "menu" :data-testid "menu"}
    children]])

(t/deftest pattern-context-provider
  (let [close-called (atom false)
        on-close #(reset! close-called true)
        result (render (hx/f [Menu {:on-close on-close}
                              [MenuItem {:label "Action"}]]))
        item (get-by-testid (.-container result) "menu-item")]

    (t/is (= false @close-called))
    (click item)
    (t/is (= true @close-called)
          "Context value (close fn) was called")))

;;
;; =============================================================================
;; PATTERN: hx/f for dynamic hiccup in React Suspense fallback
;; From: nosco/views/integration.cljs
;; =============================================================================
;;

(defnc LoadingSpinner [{:keys []}]
  [:div {:data-testid "spinner"} "Loading..."])

(defnc SuspenseWrapper [{:keys [children]}]
  [React/Suspense {:fallback (hx/f [LoadingSpinner])}
   children])

(t/deftest pattern-hx-f-in-suspense
  ;; The key pattern: hx/f to convert hiccup for use in JS props
  (let [result (render (hx/f [SuspenseWrapper
                              [:div {:data-testid "content"} "Loaded!"]]))
        content (get-by-testid (.-container result) "content")]
    (t/is (= "Loaded!" (.-textContent content)))))

;;
;; =============================================================================
;; PATTERN: Breadcrumbs with conditional styling
;; From: nosco/views/create_users_2.cljs
;; =============================================================================
;;

(defnc Breadcrumb [{:keys [text active on-click]}]
  [:div {:class [(when active "active")]
         :data-testid "breadcrumb"
         :on-click on-click}
   [:span {:class (if active "bold" "regular")}
    text]])

(defnc Breadcrumbs [{:keys [current-state]}]
  [:div {:class "breadcrumbs" :data-testid "breadcrumbs"}
   [Breadcrumb {:text "Step 1" :active (= :step1 current-state)}]
   [:span {:class "chevron"} ">"]
   [Breadcrumb {:text "Step 2" :active (= :step2 current-state)}]
   [:span {:class "chevron"} ">"]
   [Breadcrumb {:text "Step 3" :active (= :step3 current-state)}]])

(t/deftest pattern-conditional-classes
  (let [result (render (hx/f [Breadcrumbs {:current-state :step2}]))
        crumbs (.. result -container (querySelectorAll "[data-testid='breadcrumb']"))
        step2 (aget crumbs 1)]
    (t/is (= 3 (.-length crumbs)))
    (t/is (.. step2 -classList (contains "active")))))

;;
;; =============================================================================
;; PATTERN: Form with controlled inputs
;; From: nosco/ui/inputs_cards.cljs
;; =============================================================================
;;

(defnc NumberInput [{:keys [value on-change]}]
  [:input {:type "number"
           :data-testid "number-input"
           :value (or value "")
           :on-change #(on-change (.. % -target -value))}])

(defnc FormWithNumberInput [{:keys []}]
  (let [[value set-value] (hooks/useState nil)]
    [:div {:data-testid "form"}
     [:pre {:data-testid "value"} (str value)]
     [NumberInput {:value value :on-change set-value}]]))

(t/deftest pattern-controlled-input
  (let [result (render (hx/f [FormWithNumberInput]))
        input (get-by-testid (.-container result) "number-input")
        value-el (get-by-testid (.-container result) "value")]

    (t/is (= "" (.-textContent value-el)))

    (rtl/fireEvent.change input #js {:target #js {:value "42"}})
    (t/is (= "42" (.-textContent value-el)))))

;;
;; =============================================================================
;; PATTERN: Component with ^:deprecated metadata
;; From: nosco/ui/chips.cljs
;; =============================================================================
;;

(defnc ^:deprecated CondensedTag [{:keys [label selected] :as props}]
  [:span {:title label
          :class ["nosco-tag" (when selected "selected")]
          :data-testid "tag"}
   label])

(t/deftest pattern-deprecated-metadata
  ;; Should still work, just marked as deprecated
  (let [result (render (hx/f [CondensedTag {:label "Test" :selected true}]))
        tag (get-by-testid (.-container result) "tag")]
    (t/is (= "Test" (.-textContent tag)))
    (t/is (.. tag -classList (contains "selected")))))

;;
;; =============================================================================
;; PATTERN: useEffect for side effects on mount
;; From: nosco/views/main.cljs
;; =============================================================================
;;

(defnc EffectOnMount [{:keys [on-mount]}]
  (hooks/useEffect
   (fn []
     (on-mount)
     js/undefined)  ; Return undefined, not nil
   [])
  [:div {:data-testid "effect-component"} "Mounted"])

(t/deftest pattern-use-effect-mount
  (let [mounted (atom false)
        result (render (hx/f [EffectOnMount {:on-mount #(reset! mounted true)}]))]
    (t/is (= true @mounted)
          "useEffect callback ran on mount")))

;;
;; =============================================================================
;; PATTERN: Fragment with multiple children
;; From: various components
;; =============================================================================
;;

(defnc FragmentExample [{:keys [items]}]
  [:<>
   (for [item items]
     [:div {:key (:id item) :data-testid "item"} (:name item)])])

(t/deftest pattern-fragment-with-keys
  (let [items [{:id "1" :name "A"} {:id "2" :name "B"} {:id "3" :name "C"}]
        result (render (hx/f [FragmentExample {:items items}]))
        els (.. result -container (querySelectorAll "[data-testid='item']"))]
    (t/is (= 3 (.-length els)))
    (t/is (= "A" (.-textContent (aget els 0))))
    (t/is (= "B" (.-textContent (aget els 1))))
    (t/is (= "C" (.-textContent (aget els 2))))))

;;
;; =============================================================================
;; PATTERN: useReducer for complex state
;; From: nosco/ui/comments.cljs
;; =============================================================================
;;

(defn comments-reducer [state action]
  (case (:type action)
    :add (update state :comments conj (:comment action))
    :remove (update state :comments #(remove (fn [c] (= (:id c) (:id action))) %))
    state))

(defnc CommentsWithReducer [{:keys [initial-comments]}]
  (let [[state dispatch] (hooks/useReducer comments-reducer {:comments initial-comments})]
    [:div {:data-testid "comments"}
     [:div {:data-testid "count"} (str (count (:comments state)))]
     [:button {:data-testid "add"
               :on-click #(dispatch {:type :add :comment {:id (random-uuid) :text "New"}})}
      "Add"]]))

(t/deftest pattern-use-reducer
  (let [result (render (hx/f [CommentsWithReducer {:initial-comments [{:id 1 :text "First"}]}]))
        count-el (get-by-testid (.-container result) "count")
        add-btn (get-by-testid (.-container result) "add")]

    (t/is (= "1" (.-textContent count-el)))
    (click add-btn)
    (t/is (= "2" (.-textContent count-el)))))

;;
;; =============================================================================
;; PATTERN: Render function as child (function-as-child)
;; From: nosco/ui/buttons2_cards.cljs (SplitDropdownButton)
;; =============================================================================
;;

(defnc Dropdown [{:keys [button children]}]
  (let [[open set-open] (hooks/useState false)]
    [:div {:data-testid "dropdown"}
     [:div {:on-click #(set-open not)} button]
     (when open
       [:div {:class "dropdown-content" :data-testid "content"}
        ;; children is a render function
        (children {:close #(set-open false)})])]))

(t/deftest pattern-render-function-child
  (let [result (render (hx/f [Dropdown
                              {:button [:span "Open"]}
                              (fn [{:keys [close]}]
                                [:button {:data-testid "close-btn" :on-click close}
                                 "Close"])]))
        container (.-container result)
        trigger (get-by-text container "Open")]

    ;; Initially closed
    (t/is (nil? (rtl/queryByTestId container "content")))

    ;; Open
    (click trigger)
    (t/is (some? (get-by-testid container "content")))

    ;; Close via render function
    (click (get-by-testid container "close-btn"))
    (t/is (nil? (rtl/queryByTestId container "content")))))
