(ns duct.router.middleware
  "Construct Ring-Reitit Global Middleware"
  (:require [integrant.core :refer [init-key]]
            [duct.reitit.util :refer [compact]]
            [reitit.ring.coercion :as rcc]
            [reitit.ring.middleware.muuntaja :as m]
            [reitit.ring.middleware.parameters :as param]))

(defn- extend-middleware [middleware defaults]
  (->> (or middleware [])
       (concat defaults)
       (vec)
       (compact)))

;; TODO: pretty coercion errors
(defmethod init-key :duct.router/middleware [_ opts]
  (let [{:keys [muuntaja middleware coercion]} opts]
    (extend-middleware
     middleware
     [param/parameters-middleware
      (when muuntaja m/format-middleware)
      (when coercion rcc/coerce-exceptions-middleware)
      (when coercion rcc/coerce-request-middleware)
      (when coercion rcc/coerce-response-middleware)])))
