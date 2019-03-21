(ns hx.hooks-test
  (:require [cljs.test :as t :include-macros true]
            [hx.react :as hx]
            [hx.hooks :as hooks]
            ["react" :as r]
            [react-utils :as u]
            [react-testing-library :as rtl]))

(t/use-fixtures :each
  {:after rtl/cleanup})

(t/deftest <-effect
  (t/testing "no deps, fires on every render"
    (let [counter (atom 0)
          FxTest (fn [props]
                   (hooks/<-effect
                    (fn []
                      (swap! counter inc)
                      js/undefined))
                   (hx/f [:div @counter]))
          test (-> (hx/f [FxTest])
                   (u/render))
          re-render (.-rerender test)]
      ;; re-render twice
      (-> (hx/f [FxTest])
          (re-render))
      (-> (hx/f [FxTest])
          (re-render))
      (t/is (= @counter 3)))))

(t/deftest <-effect-empty-deps
  (t/testing "empty deps, fires only on first render"
    (let [counter (atom 0)
          EmptyTest (fn [props]
                      (hooks/<-effect
                       (fn [] (swap! counter inc)
                         js/undefined)
                       [])
                      (hx/f [:div @counter]))
          empty-test (-> (hx/f [EmptyTest])
                         (u/render))
          re-render (.-rerender empty-test)]
      ;; re-render twice
      (-> (hx/f [EmptyTest])
          (re-render))
      (-> (hx/f [EmptyTest])
          (re-render))
      (t/is (= @counter 1)))))

(hx/defnc ValTest [props]
  (let [counter (r/useRef 0)]
    (hooks/<-effect
     (fn [] (set! (.-current counter) (inc (.-current counter)))
       js/undefined)
     [(hooks/<-value (:some-val props))])
    [:div (.-current counter)]))

(t/deftest <-value-new-val
  (let [val-test (-> (hx/f [ValTest {:some-val 1}])
                     (u/render))
        re-render (.-rerender val-test)]
    (-> (hx/f [ValTest {:some-val 1}])
        (re-render))
    (-> (hx/f [ValTest {:some-val 2}])
        (re-render))
    (-> (hx/f [ValTest {:some-val 2}])
        (re-render))
    (-> (hx/f [ValTest {:some-val 2}])
        (re-render))
    (t/is (u/node= (u/html "<div>2</div>")
                   (u/root val-test)) "number")))

(t/deftest <-value-native-val
  (let [val-test (-> (hx/f [ValTest {:some-val 1}])
                     (u/render))
        re-render (.-rerender val-test)]
    (-> (hx/f [ValTest {:some-val 1}])
        (re-render))
    (-> (hx/f [ValTest {:some-val 1}])
        (re-render))
    (t/is (u/node= (u/html "<div>1</div>")
                   (u/root val-test)) "number")))

(t/deftest <-value-map
  (let [val-test (-> (hx/f [ValTest {:some-val {:asdf "jkl"}}])
                     (u/render))
        re-render (.-rerender val-test)]
    (-> (hx/f [ValTest {:some-val {:asdf "jkl"}}])
        (re-render))
    (-> (hx/f [ValTest {:some-val {:jkl "asdf"}}])
        (re-render))
    (-> (hx/f [ValTest {:some-val {:asdf "jkl"}}])
        (re-render))
    (t/is (u/node= (u/html "<div>2</div>")
                   (u/root val-test)) "change map"))

  (let [val-test (-> (hx/f [ValTest {:some-val {:asdf "jkl"}}])
                     (u/render))
        re-render (.-rerender val-test)]
    (-> (hx/f [ValTest {:some-val {:asdf "jkl"}}])
        (re-render))
    (-> (hx/f [ValTest {:some-val {:asdf "jkl"}}])
        (re-render))
    (-> (hx/f [ValTest {:some-val {:asdf "jkl"}}])
        (re-render))
    (t/is (u/node= (u/html "<div>1</div>")
                   (u/root val-test)) "map")))

(t/deftest <-value-vec
  (let [val-test (-> (hx/f [ValTest {:some-val [:asdf :jkl]}])
                     (u/render))
        re-render (.-rerender val-test)]
    (-> (hx/f [ValTest {:some-val [:asdf :jkl]}])
        (re-render))
    (-> (hx/f [ValTest {:some-val [:asdf :jkl]}])
        (re-render))
    (-> (hx/f [ValTest {:some-val [:asdf :jkl]}])
        (re-render))
    (t/is (u/node= (u/html "<div>1</div>")
                   (u/root val-test)) "vector")))

(t/deftest <-value-set
  (let [val-test (-> (hx/f [ValTest {:someVal #{:asdf :jkl}}])
                     (u/render))
        re-render (.-rerender val-test)]
    (-> (hx/f [ValTest {:someVal #{:asdf :jkl}}])
        (re-render))
    (-> (hx/f [ValTest {:someVal #{:asdf :jkl}}])
        (re-render))
    (-> (hx/f [ValTest {:someVal #{:asdf :jk}}])
        (re-render))
    (t/is (u/node= (u/html "<div>1</div>")
                   (u/root val-test)) "set")))

(hx/defnc OnClickState [_]
  (let [[count update-count] (hooks/<-state 0)]
    [:div {:on-click #(update-count inc)}
     count]))

(t/deftest <-ref
  (let [ref-test (fn [props]
                   (let [ref (hooks/<-ref 0)]
                     (swap! ref inc)
                     (hx/f [:div @ref])))
        rendering (u/render (hx/f [ref-test]))
        re-render (.-rerender rendering)]
    (t/is (u/node= (u/html "<div>1</div>")
                   (u/pret (u/root rendering))))
    (re-render (hx/f [ref-test]))
    (t/is (u/node= (u/html "<div>2</div>")
                   (u/pret (u/root rendering))))
    (re-render (hx/f [ref-test]))
    (t/is (u/node= (u/html "<div>3</div>")
                   (u/pret (u/root rendering))))   ))

(t/deftest <-state
  (let [state-test (-> (hx/f [OnClickState])
                       (u/render)
                       (u/root)
                       ;; click 3 times
                       (u/click)
                       (u/click)
                       (u/click))]
    (t/is (u/node= (u/html "<div>3</div>")
                   state-test))))

(hx/defnc StateWithEffect [{:keys [receive]}]
  (let [[count update-count] (hooks/<-state 0)]
    (hooks/<-effect
     (fn []
       (js/setTimeout
        (fn []
          (receive count))
        10)
       js/undefined))
    [:div {:on-click #(update-count inc)}
     count]))

(defn receiver
  "Returns a function and a promise. The promise resolves after `timeout` ms
  with a vector of the values that the function has been called with."
  [timeout]
  (let [called (atom [])
        p (js/Promise. (fn [res rej]
                         (js/setTimeout
                          (fn [] (res @called))
                          timeout)))
        f #(swap! called conj %)]
    [f p]))

(t/deftest <-state-with-effect
  (let [[receive received] (receiver 20)
        state-test (-> (hx/f [StateWithEffect {:receive receive}])
                       (u/render)
                       (u/root)
                       ;; click 3 times
                       (u/click)
                       (u/click)
                       (u/click))]
    (t/async done
             (-> received
                 (.then (fn [called-with]
                          (t/is (= [0 1 2 3] called-with))
                          (done)))))))

(def lifecycle (atom nil))

(hx/defnc WhenApplied [_]
  (reset! lifecycle :rendering)
  (let [[count update-count] (hooks/<-state 0)]
    (reset! lifecycle nil)
    [:div {:on-click #(update-count (fn [n]
                                     (prn @lifecycle)
                                     (inc n)))}
     count]))

(t/deftest when-applied
  (let [state-test (-> (hx/f [WhenApplied])
                       (u/render)
                       (u/root)
                       ;; click 3 times
                       (u/click)
                       (u/click)
                       (u/click))]
    (t/is (u/node= (u/html "<div>3</div>")
                   state-test))))
