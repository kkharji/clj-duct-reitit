(ns foo.handler.plus
  (:require [integrant.core :refer [init-key]]))

(def with-query
  {:summary "Plus with Query-Params"
   :parameters {:query {:x int?, :y int?}}
   :handler (fn [{{{:keys [x y]} :query} :parameters}]
              {:status 200
               :body {:total (+ x y)}})})

(defmethod init-key ::with-body [_ _]
  {:summary "Plus with body-params"
   :parameters {:body {:x int?, :y int?}}
   :handler (fn [{{{:keys [x y]} :body} :parameters}]
              {:status 200
               :body {:total (+ x y)}})})
