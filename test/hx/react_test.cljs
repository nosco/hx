(ns hx.react-test
  (:require [cljs.test :as t :include-macros true]
            [hx.react :as hx]
            [goog.dom :as dom]
            [goog.object :as gobj]
            [clojure.string :as str]
            ["react-testing-library" :as rtl]))

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

(defn func []
  (let [call-count (atom 0)
        f (fn spy-func [& x]
            (swap! call-count inc))]
    (set! (.-callCount f) call-count)
    f))

(defn call-count [f]
  @(.-callCount f))

(defn click [node]
  (.click rtl/fireEvent node))

(defn change [node data]
  (.change rtl/fireEvent node data))


(defn pret [x]
  (js/console.log x)
  x)

;;
;; Tests
;;

(t/deftest create-element
  (t/is (node= (html "<div>hi</div>")
               (root (render (hx/f [:div "hi"])))))
  (t/is (node= (html "<div><span>hi</span><span>bye</span></div>")
               (root (render (hx/f [:div
                                           [:span "hi"]
                                    [:span "bye"]])))))
  (t/testing "LazySeq"
    (t/is (node= (html "<div><span>hi</span><span>bye</span></div>")
                 (-> [:div (map identity [[:span "hi"] [:span "bye"]])]
                     (hx/f)
                     (render)
                     (root)))))

  (t/testing "fragment"
    ;; for fragments, the firstChild of the container is the first element
    (t/is (node= (html "<div>hi</div>")
                 (root (render (hx/f [:<> [:div "hi"]])))))

    ;; so here we test to see if the container matches
    (t/is (node= (html "<div><span>hi</span><span>bye</span></div>")
                 (.-container (render (hx/f [:<>
                                             [:span "hi"]
                                             [:span "bye"]]))))))

  (t/testing "provider"
    (let [c (hx/create-context)]
      (t/is (node= (html "<div>hi</div>")
                   (-> (hx/f [:provider {:context c}
                              [:div "hi"]])
                       (render)
                       (root))))))

  (t/testing "function"
    (let [f (fn test-fn [_] (hx/f [:div "hi"]))]
      (t/is (node= (html "<div>hi</div>")
                   (-> (hx/f [f])
                       (render)
                       (root)))))

    (let [f (fn f-fn [_] (hx/f [:span "hi"]))
          g (fn g-fn [_] (hx/f [:h1 [f]]))
          h (fn h-fn [_] (hx/f [:div [g]]))]
      (t/is (node= (html "<div><h1><span>hi</span></h1></div>")
                   (-> (hx/f [h])
                       (render)
                       (root)))))))

(t/deftest style-prop
  (t/is (node= (html "<div style=\"color: red;\">hi</div>")
               (root (render (hx/f [:div {:style {:color "red"}} "hi"])))))

  (t/is (node= (html "<div style=\"--some-var:foo;\">hi</div>")
               (root (render (hx/f [:div {:style {:--some-var "foo"}} "hi"])))))

  (t/is (node= (html "<div style=\"color: red; background: green;\">hi</div>")
               (root (render (hx/f [:div {:style {:color "red"
                                                  :background "green"}} "hi"])))))

  (let [c (fn [props] (hx/f [:div {:style (.-style props)} "hi"]))]
    (t/is (node= (html "<div style=\"color: red; background: green;\">hi</div>")
                 (root (render (hx/f [c {:style {:color "red"
                                                 :background "green"}} "hi"]))))
          "style pass-through")))

(t/deftest class-prop
  (t/is (node= (html "<div class=\"foo\">hi</div>")
               (root (render (hx/f [:div {:class "foo"} "hi"]))))
        "bare string")

  (t/is (node= (html "<div class=\"foo\">hi</div>")
               (root (render (hx/f [:div {:class ["foo"]} "hi"]))))
        "vec")

  (t/is (node= (html "<div class=\"foo bar\">hi</div>")
               (root (render (hx/f [:div {:class ["foo" "bar"]} "hi"]))))
        "vec with multi")

  (t/is (node= (html "<div class=\"foo bar\">hi</div>")
               (root (render (hx/f [:div {:class ["foo" nil "bar"]} "hi"]))))
        "vec with nils")

  (let [c (fn [props]
            (hx/f [:div (str (= (.-className props) "foo"))]))]
    (t/is (node= (html "<div>true</div>")
                 (-> [c {:class "foo"}] (hx/f) (render) (root))))))

(t/deftest for-prop
  (let [c (fn [props]
            (hx/f [:div (str (= (.-htmlFor props) "foo"))]))]
    (t/is (node= (html "<div>true</div>")
                 (-> [c {:for "foo"}] (hx/f) (render) (root))))))

(t/deftest on-click-prop
  (let [on-click (func)
        node (-> [:div {:on-click on-click}]
                 (hx/f)
                 (render)
                 (root))]
    (click node)
    (t/is (= 1 (call-count on-click)))))

(t/deftest input-on-change-prop
  (let [on-change (func)
        node (-> [:input {:on-change on-change}]
                 (hx/f)
                 (render)
                 (root))]
    (change node #js {:target #js {:value "a"}})
    (t/is (= 1 (call-count on-change)))))
