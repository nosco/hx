(ns hx.defnc-lint-test
  "Test file to verify clj-kondo linting of defnc macro.

   This file intentionally contains both valid and invalid defnc usages
   to test the linting rules."
  (:require [hx.react :refer [defnc]]))

;; VALID: First arg is map destructuring
(defnc ValidComponent1 [{:keys [name age]}]
  [:div "Hello " name])

;; VALID: First arg is map destructuring with :as
(defnc ValidComponent2 [{:keys [name] :as props}]
  [:div "Hello " name])

;; VALID: Empty map destructuring
(defnc ValidComponent3 [{}]
  [:div "No props"])

;; VALID: With ref
(defnc ValidComponent4 [{:keys [name]} ref]
  [:div {:ref ref} name])

;; VALID: With docstring
(defnc ValidComponent5
  "A documented component"
  [{:keys [title]}]
  [:h1 title])

;; VALID: First arg named `props` (allowed exception)
(defnc ValidComponent6 [props]
  [:div (:name props)])

;; INVALID: First arg is a symbol but NOT named `props`
;; Should warn: "defnc first argument should be a map destructuring or named `props`"
(defnc InvalidComponent1 [p]
  [:div (:name p)])

;; INVALID: Second arg is not named `ref`
;; Should warn: "defnc second argument should be named `ref`, got `r`"
(defnc InvalidComponent2 [{:keys [name]} r]
  [:div {:ref r} name])

;; INVALID: Both violations - first arg not map/props, second arg not ref
;; Should warn about first arg AND second arg
(defnc InvalidComponent3 [p the-ref]
  [:div {:ref the-ref} (:name p)])

;; VALID: :wrap with a vector
(defnc ValidWrapComponent [{:keys [x]}]
  {:wrap [(js/Function.prototype)]}
  [:div x])

;; INVALID: :wrap without vector (common mistake)
;; Should error: "defnc :wrap option must be a vector, e.g. {:wrap [(react/memo)]}"
;; Note: This is commented out because it would cause a compile-time error
;; Uncomment to verify linting works:
;; (defnc InvalidWrapComponent [{:keys [x]}]
;;   {:wrap (identity)}
;;   [:div x])
