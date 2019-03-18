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
     [(:some-val props)])
    [:div (.-current counter)]))

(t/deftest <-effect-deps
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

(t/deftest <-effect-deps-native-val
  (let [val-test (-> (hx/f [ValTest {:some-val 1}])
                     (u/render))
        re-render (.-rerender val-test)]
    (-> (hx/f [ValTest {:some-val 1}])
        (re-render))
    (-> (hx/f [ValTest {:some-val 1}])
        (re-render))
    (t/is (u/node= (u/html "<div>1</div>")
                   (u/root val-test)) "number")))

(t/deftest <-effects-deps-map
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

(t/deftest <-effects-deps-vec
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

(t/deftest <-effects-deps-set
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
  (let [count (hooks/<-state 0)]
    [:div {:on-click #(swap! count inc)}
     @count]))

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
  (let [count (hooks/<-state 0)]
    (hooks/<-effect
     (fn []
       (js/setTimeout
        (fn []
          (receive @count))
        100)
       js/undefined))
    [:div {:on-click #(swap! count inc)}
     @count]))

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
  (let [[receive received] (receiver 104)
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
