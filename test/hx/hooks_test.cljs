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
  (t/testing "dependencies compared as value"
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
                     (u/root val-test)) "number"))))

(t/deftest <-effect-deps-native-val
  (t/testing "dependencies compared as value"
    (let [val-test (-> (hx/f [ValTest {:some-val 1}])
                       (u/render))
          re-render (.-rerender val-test)]
      (-> (hx/f [ValTest {:some-val 1}])
          (re-render))
      (-> (hx/f [ValTest {:some-val 1}])
          (re-render))
      (t/is (u/node= (u/html "<div>1</div>")
                     (u/root val-test)) "number"))))

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
