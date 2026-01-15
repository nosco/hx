(ns workshop.core
  "Interactive workshop demonstrating hx + UIx integration.

   This workshop shows:
   1. Plain hx components (defnc)
   2. Plain UIx components (defui)
   3. Interop: hx inside UIx, UIx inside hx
   4. Children passing both directions
   5. Context sharing
   6. State management (useState, useReducer)
   7. Ref forwarding"
  (:require ["react" :as react]
            [hx.react :as hx]
            [hx.hooks :as hooks]
            [uix.core :as uix :refer [defui $]]))

;; =============================================================================
;; STYLING HELPERS
;; =============================================================================

(def styles
  {:section {:border "1px solid #ddd"
             :border-radius "8px"
             :padding "16px"
             :margin-bottom "16px"
             :background-color "#fafafa"}
   :section-title {:margin-top 0
                   :margin-bottom "12px"
                   :color "#333"
                   :border-bottom "2px solid #4CAF50"
                   :padding-bottom "8px"}
   :card {:background "white"
          :border "1px solid #e0e0e0"
          :border-radius "4px"
          :padding "12px"
          :margin-bottom "8px"}
   :button {:background-color "#4CAF50"
            :color "white"
            :border "none"
            :padding "8px 16px"
            :border-radius "4px"
            :cursor "pointer"
            :margin-right "8px"}
   :button-secondary {:background-color "#2196F3"
                      :color "white"
                      :border "none"
                      :padding "8px 16px"
                      :border-radius "4px"
                      :cursor "pointer"
                      :margin-right "8px"}
   :input {:padding "8px"
           :border "1px solid #ddd"
           :border-radius "4px"
           :margin-right "8px"}
   :badge {:display "inline-block"
           :padding "2px 8px"
           :border-radius "12px"
           :font-size "12px"
           :margin-right "4px"}
   :hx-badge {:background-color "#e3f2fd" :color "#1565c0"}
   :uix-badge {:background-color "#fff3e0" :color "#e65100"}})

(hx/defnc Badge [{:keys [type label]}]
  [:span {:style (merge (:badge styles)
                        (if (= type :hx) (:hx-badge styles) (:uix-badge styles)))}
   label])

;; =============================================================================
;; SECTION 1: PLAIN HX COMPONENTS
;; =============================================================================

(hx/defnc HxCounter [{:keys [initial-count]}]
  (let [[count set-count] (hooks/useState (or initial-count 0))]
    [:div {:style (:card styles)}
     [Badge {:type :hx :label "hx"}]
     [:span {:style {:margin-left "8px"}} "Counter: " [:strong count]]
     [:div {:style {:margin-top "8px"}}
      [:button {:style (:button styles)
                :on-click #(set-count inc)}
       "+"]
      [:button {:style (:button-secondary styles)
                :on-click #(set-count dec)}
       "-"]
      [:button {:style {:background-color "#ff5722"
                        :color "white"
                        :border "none"
                        :padding "8px 16px"
                        :border-radius "4px"
                        :cursor "pointer"}
                :on-click #(set-count 0)}
       "Reset"]]]))

(hx/defnc HxGreeting [{:keys [name]}]
  [:div {:style (:card styles)}
   [Badge {:type :hx :label "hx"}]
   [:span {:style {:margin-left "8px"}} "Hello, " [:strong name] "!"]])

(hx/defnc HxWithChildren [{:keys [title children]}]
  [:div {:style (merge (:card styles) {:border-left "4px solid #4CAF50"})}
   [Badge {:type :hx :label "hx"}]
   [:strong {:style {:margin-left "8px"}} title]
   [:div {:style {:margin-top "8px" :padding-left "16px"}}
    children]])

(hx/defnc PlainHxSection [_]
  [:div {:style (:section styles)}
   [:h3 {:style (:section-title styles)} "1. Plain hx Components"]
   [HxGreeting {:name "World"}]
   [HxCounter {:initial-count 5}]
   [HxWithChildren {:title "Children Example"}
    [:div "Child 1: I'm inside an hx component"]
    [:div "Child 2: So am I!"]]])

;; =============================================================================
;; SECTION 2: PLAIN UIX COMPONENTS
;; =============================================================================

(defui UixCounter [{:keys [initial-count]}]
  (let [[count set-count] (uix/use-state (or initial-count 0))]
    ($ :div {:style (:card styles)}
       ($ Badge {:type :uix :label "UIx"})
       ($ :span {:style {:margin-left "8px"}} "Counter: " ($ :strong count))
       ($ :div {:style {:margin-top "8px"}}
          ($ :button {:style (:button styles)
                      :on-click #(set-count inc)}
             "+")
          ($ :button {:style (:button-secondary styles)
                      :on-click #(set-count dec)}
             "-")
          ($ :button {:style {:background-color "#ff5722"
                              :color "white"
                              :border "none"
                              :padding "8px 16px"
                              :border-radius "4px"
                              :cursor "pointer"}
                      :on-click #(set-count 0)}
             "Reset")))))

(defui UixGreeting [{:keys [name]}]
  ($ :div {:style (:card styles)}
     ($ Badge {:type :uix :label "UIx"})
     ($ :span {:style {:margin-left "8px"}} "Hello, " ($ :strong name) "!")))

(defui UixWithChildren [{:keys [title children]}]
  ($ :div {:style (merge (:card styles) {:border-left "4px solid #ff9800"})}
     ($ Badge {:type :uix :label "UIx"})
     ($ :strong {:style {:margin-left "8px"}} title)
     ($ :div {:style {:margin-top "8px" :padding-left "16px"}}
        children)))

(defui PlainUixSection [_]
  ($ :div {:style (:section styles)}
     ($ :h3 {:style (:section-title styles)} "2. Plain UIx Components")
     ($ UixGreeting {:name "Universe"})
     ($ UixCounter {:initial-count 10})
     ($ UixWithChildren {:title "Children Example"}
        ($ :div "Child 1: I'm inside a UIx component")
        ($ :div "Child 2: Me too!"))))

;; =============================================================================
;; SECTION 3: INTEROP - HIX INSIDE UIX
;; =============================================================================

(defui UixWrapperForHx [{:keys []}]
  ($ :div {:style (:section styles)}
     ($ :h3 {:style (:section-title styles)} "3. hx Components Inside UIx")
     ($ :p "These hx components are rendered inside a UIx parent:")
     ;; hx components used inside UIx $
     ($ HxGreeting {:name "from UIx"})
     ($ HxCounter {:initial-count 3})
     ($ HxWithChildren {:title "hx inside UIx"}
        ($ :div "UIx child inside hx"))))

;; =============================================================================
;; SECTION 4: INTEROP - UIX INSIDE HX
;; =============================================================================

(hx/defnc HxWrapperForUix [_]
  [:div {:style (:section styles)}
   [:h3 {:style (:section-title styles)} "4. UIx Components Inside hx"]
   [:p "These UIx components are rendered inside an hx parent:"]
   ;; UIx components used inside hx hiccup
   [UixGreeting {:name "from hx"}]
   [UixCounter {:initial-count 7}]
   [UixWithChildren {:title "UIx inside hx"}
    [:div "hx child inside UIx"]]])

;; =============================================================================
;; SECTION 5: DEEP NESTING
;; =============================================================================

(defui UixOuter [{:keys [children]}]
  ($ :div {:style {:border "2px solid #ff9800" :padding "8px" :margin "4px"}}
     ($ :span {:style {:color "#ff9800"}} "UIx Outer")
     children))

(hx/defnc HxMiddle [{:keys [children]}]
  [:div {:style {:border "2px solid #4CAF50" :padding "8px" :margin "4px"}}
   [:span {:style {:color "#4CAF50"}} "hx Middle"]
   children])

(defui UixInner [{:keys []}]
  ($ :div {:style {:border "2px solid #ff9800" :padding "8px" :margin "4px"}}
     ($ :span {:style {:color "#ff9800"}} "UIx Inner")
     ($ :strong " - The deepest level!")))

(hx/defnc DeepNestingSection [_]
  [:div {:style (:section styles)}
   [:h3 {:style (:section-title styles)} "5. Deep Nesting (UIx → hx → UIx)"]
   [:p "Components nested: UIx > hx > UIx"]
   [UixOuter
    [HxMiddle
     [UixInner]]]])

;; =============================================================================
;; SECTION 6: SHARED CONTEXT
;; =============================================================================

(def theme-context (react/createContext "light"))

(defui ThemeToggle [{:keys []}]
  (let [theme (uix/use-context theme-context)]
    ($ :div {:style {:padding "8px"
                     :background (if (= theme "dark") "#333" "#fff")
                     :color (if (= theme "dark") "#fff" "#333")
                     :border-radius "4px"}}
       ($ Badge {:type :uix :label "UIx"})
       " Current theme: " ($ :strong theme))))

(hx/defnc HxThemeDisplay [_]
  (let [theme (hooks/useContext theme-context)]
    [:div {:style {:padding "8px"
                   :background (if (= theme "dark") "#333" "#fff")
                   :color (if (= theme "dark") "#fff" "#333")
                   :border-radius "4px"
                   :margin-top "8px"}}
     [Badge {:type :hx :label "hx"}]
     " Current theme: " [:strong theme]]))

(hx/defnc ContextSection [_]
  (let [[theme set-theme] (hooks/useState "light")]
    [:div {:style (:section styles)}
     [:h3 {:style (:section-title styles)} "6. Shared Context"]
     [:p "Both hx and UIx components read from the same React context:"]
     [:button {:style (:button styles)
               :on-click #(set-theme (if (= theme "light") "dark" "light"))}
      "Toggle Theme"]
     [:provider {:context theme-context :value theme}
      [:div {:style {:margin-top "12px"}}
       [ThemeToggle]
       [HxThemeDisplay]]]]))

;; =============================================================================
;; SECTION 7: STATE MANAGEMENT - useReducer
;; =============================================================================

(defn todo-reducer [state action]
  (case (:type action)
    :add (update state :todos conj {:id (random-uuid)
                                    :text (:text action)
                                    :done false})
    :toggle (update state :todos
                    (fn [todos]
                      (mapv (fn [todo]
                              (if (= (:id todo) (:id action))
                                (update todo :done not)
                                todo))
                            todos)))
    :remove (update state :todos
                    (fn [todos]
                      (filterv #(not= (:id %) (:id action)) todos)))
    state))

(hx/defnc TodoItem [{:keys [todo on-toggle on-remove]}]
  [:div {:style {:display "flex"
                 :align-items "center"
                 :padding "8px"
                 :background (if (:done todo) "#e8f5e9" "white")
                 :border "1px solid #ddd"
                 :border-radius "4px"
                 :margin-bottom "4px"}}
   [:input {:type "checkbox"
            :checked (:done todo)
            :on-change on-toggle
            :style {:margin-right "8px"}}]
   [:span {:style {:flex 1
                   :text-decoration (when (:done todo) "line-through")}}
    (:text todo)]
   [:button {:style {:background "#f44336"
                     :color "white"
                     :border "none"
                     :padding "4px 8px"
                     :border-radius "4px"
                     :cursor "pointer"}
             :on-click on-remove}
    "×"]])

(hx/defnc TodoApp [_]
  (let [[state dispatch] (hooks/useReducer todo-reducer {:todos []})
        [input-text set-input-text] (hooks/useState "")]
    [:div {:style (:card styles)}
     [:h4 {:style {:margin-top 0}} "Todo List " [Badge {:type :hx :label "hx + useReducer"}]]
     [:div {:style {:display "flex" :margin-bottom "12px"}}
      [:input {:style (merge (:input styles) {:flex 1})
               :placeholder "What needs to be done?"
               :value input-text
               :on-change #(set-input-text (.. % -target -value))
               :on-key-down #(when (= (.-key %) "Enter")
                               (when (seq input-text)
                                 (dispatch {:type :add :text input-text})
                                 (set-input-text "")))}]
      [:button {:style (:button styles)
                :on-click #(when (seq input-text)
                             (dispatch {:type :add :text input-text})
                             (set-input-text ""))}
       "Add"]]
     [:div
      (for [todo (:todos state)]
        ^{:key (:id todo)}
        [TodoItem {:todo todo
                   :on-toggle #(dispatch {:type :toggle :id (:id todo)})
                   :on-remove #(dispatch {:type :remove :id (:id todo)})}])]
     (when (empty? (:todos state))
       [:p {:style {:color "#999" :text-align "center"}} "No todos yet. Add one above!"])]))

(hx/defnc StateSection [_]
  [:div {:style (:section styles)}
   [:h3 {:style (:section-title styles)} "7. State Management"]
   [TodoApp]])

;; =============================================================================
;; SECTION 8: REF FORWARDING
;; =============================================================================

;; Old style: manually wrapping with forwardRef
(hx/defnc FocusableInput* [{:keys [placeholder]} ref]
  [:input {:ref ref
           :style (merge (:input styles) {:width "200px"})
           :placeholder placeholder}])

(def FocusableInput (react/forwardRef FocusableInput*))

;; New style: using :wrap option (preferred)
(hx/defnc FocusableInputWithWrap [{:keys [placeholder]} ref]
  {:wrap [(react/forwardRef)]}
  [:input {:ref ref
           :style (merge (:input styles) {:width "200px" :border-color "#4CAF50"})
           :placeholder placeholder}])

(hx/defnc RefSection [_]
  (let [input-ref (react/useRef nil)
        input-ref-wrap (react/useRef nil)]
    [:div {:style (:section styles)}
     [:h3 {:style (:section-title styles)} "8. Ref Forwarding"]
     [:p "Click the buttons to focus the inputs (ref forwarding with forwardRef):"]

     [:div {:style {:margin-bottom "16px"}}
      [:strong "Manual wrap (old style):"]
      [:div {:style {:display "flex" :align-items "center" :margin-top "8px"}}
       [FocusableInput {:ref input-ref :placeholder "Manual forwardRef"}]
       [:button {:style (:button styles)
                 :on-click #(when-let [el (.-current input-ref)]
                              (.focus el))}
        "Focus"]]]

     [:div
      [:strong "Using :wrap option (new style):"]
      [:div {:style {:display "flex" :align-items "center" :margin-top "8px"}}
       [FocusableInputWithWrap {:ref input-ref-wrap :placeholder ":wrap [(react/forwardRef)]"}]
       [:button {:style (:button styles)
                 :on-click #(when-let [el (.-current input-ref-wrap)]
                              (.focus el))}
        "Focus"]]]]))
;; SECTION 9: FUNCTION AS CHILD
;; =============================================================================

(hx/defnc RenderProp [{:keys [children]}]
  (let [[count set-count] (hooks/useState 0)]
    [:div {:style (:card styles)}
     [:div {:style {:margin-bottom "8px"}}
      [:button {:style (:button styles) :on-click #(set-count inc)} "Increment"]]
     ;; Call children as a function
     (children count)]))

(hx/defnc FunctionAsChildSection [_]
  [:div {:style (:section styles)}
   [:h3 {:style (:section-title styles)} "9. Function as Child (Render Props)"]
   [:p "The child is a function that receives the count:"]
   [RenderProp
    (fn [count]
      [:div {:style {:font-size "24px" :font-weight "bold"}}
       "Count: " count])]])

;; =============================================================================
;; MAIN APP
;; =============================================================================

(hx/defnc App [_]
  [:div {:style {:max-width "800px"
                 :margin "0 auto"
                 :padding "20px"
                 :font-family "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"}}
   [:h1 {:style {:color "#333" :border-bottom "3px solid #4CAF50" :padding-bottom "12px"}}
    "hx + UIx Integration Workshop"]
   [:p {:style {:color "#666"}}
    "This workshop demonstrates hx and UIx components working together. "
    [:span {:style (:hx-badge styles)} "hx"] " components use "
    [:code "defnc"] " and hiccup syntax. "
    [:span {:style (:uix-badge styles)} "UIx"] " components use "
    [:code "defui"] " and " [:code "$"] " syntax."]

   [PlainHxSection]
   [PlainUixSection]
   [UixWrapperForHx]
   [HxWrapperForUix]
   [DeepNestingSection]
   [ContextSection]
   [StateSection]
   [RefSection]
   [FunctionAsChildSection]

   [:footer {:style {:margin-top "32px"
                     :padding-top "16px"
                     :border-top "1px solid #ddd"
                     :color "#999"
                     :text-align "center"}}
    "hx + UIx Integration Demo"]])
