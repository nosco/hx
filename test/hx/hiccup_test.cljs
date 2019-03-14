(ns hx.hiccup-test
  (:require [cljs.test :as t :include-macros true]
            [hx.hiccup :as h]
            [goog.dom :as dom]
            [goog.object :as gobj]
            [clojure.string :as str]
            ["react-testing-library" :as rtl]))

(t/use-fixtures :each
  {:after rtl/cleanup})

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

(defn func []
  (let [call-count (atom 0)
        f (fn spy-func [& x]
            (swap! call-count inc))]
    (set! (.-callCount f) call-count)
    f))

(defn call-count [f]
  @(.-callCount f))

(t/deftest create-element
  (t/is (node= (html "<div>hi</div>")
               (root (rtl/render (h/parse [:div "hi"])))))
  (t/is (node= (html "<div><span>hi</span><span>bye</span></div>")
               (root (rtl/render (h/parse [:div
                                           [:span "hi"]
                                           [:span "bye"]]))))))

(t/deftest style-prop
  (t/is (node= (html "<div style=\"color: red;\">hi</div>")
               (root (rtl/render (h/parse [:div {:style {:color "red"}} "hi"])))))

  (t/is (node= (html "<div style=\"color: red; background: green;\">hi</div>")
               (root (rtl/render (h/parse [:div {:style {:color "red"
                                                         :background "green"}} "hi"]))))))

(t/deftest class-prop
  (t/is (node= (html "<div class=\"foo\">hi</div>")
               (root (rtl/render (h/parse [:div {:class "foo"} "hi"])))))

  (t/is (node= (html "<div class=\"foo\">hi</div>")
               (root (rtl/render (h/parse [:div {:class ["foo"]} "hi"])))))

  (t/is (node= (html "<div class=\"foo bar\">hi</div>")
               (root (rtl/render (h/parse [:div {:class ["foo" "bar"]} "hi"]))))))

(t/deftest on-click-prop
  (let [on-click (func)
        node (-> [:div {:on-click on-click}]
                 (h/parse)
                 (rtl/render)
                 (root))]
    (.click rtl/fireEvent node)
    (t/is (= 1 (call-count on-click)))))
