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
  (t/testing "dependencies compared as value"
    (let [ValTest (fn [props]
                    (let [[counter set-counter] (r/useState 0)]
                      (hooks/<-effect
                       (fn [] (set-counter inc))
                       [(.-someVal props)])
                      (hx/f [:div counter])))]

      (let [val-test (-> (hx/f [ValTest {:someVal 1}])
                         (u/render))
            re-render (.-rerender val-test)]
        (-> (hx/f [ValTest {:someVal 1}])
            (re-render))
        (t/is (u/node= (u/html "<div>1</div>")
                       (u/root val-test)) "number"))

      (let [val-test (-> (hx/f [ValTest {:someVal {:asdf "jkl"}}])
                         (u/render))
            re-render (.-rerender val-test)]
        (-> (hx/f [ValTest {:someVal {:asdf "jkl"}}])
            (re-render))
        (t/is (u/node= (u/html "<div>1</div>")
                       (u/root val-test)) "map"))

      (let [val-test (-> (hx/f [ValTest {:someVal [:asdf :jkl]}])
                         (u/render))
            re-render (.-rerender val-test)]
        (-> (hx/f [ValTest {:someVal [:asdf :jkl]}])
            (re-render))
        (t/is (u/node= (u/html "<div>1</div>")
                       (u/root val-test)) "vector"))

      (let [val-test (-> (hx/f [ValTest {:someVal #{:asdf :jkl}}])
                         (u/render))
            re-render (.-rerender val-test)]
        (-> (hx/f [ValTest {:someVal #{:asdf :jkl}}])
            (re-render))
        (t/is (u/node= (u/html "<div>1</div>")
                       (u/root val-test)) "set")))))
