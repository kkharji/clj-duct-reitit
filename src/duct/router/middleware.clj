(ns duct.router.middleware
  "Construct Ring-Reitit Global Middleware"
  (:require [integrant.core :refer [init-key]]
            [duct.reitit.util :refer [compact]]
            [reitit.ring.coercion :as rcc]
            [reitit.ring.middleware.muuntaja :refer [format-middleware]]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]))

(defn- wrap [name f]
  {:name name
   :compile (fn [opts _] (fn [handler] (fn [request] (f opts _ handler request))))})

(defn- extend-middleware [middleware defaults]
  (->> (or middleware [])
       (concat defaults)
       (vec)
       (compact)))

(def environment-middleware
  (wrap ::environment
        (fn [{:keys [environment]} _ handler request]
          (->> {:environment environment
                :id  (java.util.UUID/randomUUID)
                :start-date (java.util.Date.)}
               (into request)
               (handler)))))

;; TODO: pretty coercion errors
(defmethod init-key :duct.router/middleware [_ opts]
  (let [{:keys [muuntaja middleware coercion]} opts]
    (->> [parameters-middleware
          environment-middleware
          (when muuntaja format-middleware)
          (when coercion rcc/coerce-exceptions-middleware)
          (when coercion rcc/coerce-request-middleware)
          (when coercion rcc/coerce-response-middleware)]
         (extend-middleware middleware))))
