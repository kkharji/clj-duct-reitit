(ns duct.reitit.middleware
  "Construct Ring-Reitit Global Middleware"
  (:require [integrant.core :refer [init-key]]
            [duct.reitit.util :refer [compact defm]]
            [reitit.ring.coercion :as rcc]
            [reitit.ring.middleware.muuntaja :refer [format-middleware]]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]
            [duct.reitit.middleware.exception :as exception :refer [get-exception-middleware]]))

;; TODO: inject environment keys instead
(defm environment-middleware [{:keys [environment]} _ handler request]
  (let [inject #(handler (into request %))]
    (inject {:environment environment
             :id  (java.util.UUID/randomUUID)
             :start-date (java.util.Date.)})))

(defn- get-coercion-middleware [{:keys [pretty?] :as coercion}]
  (when coercion
    {:coerce-exceptions (when-not pretty? rcc/coerce-exceptions-middleware)
     :coerce-request rcc/coerce-request-middleware
     :coerce-response rcc/coerce-response-middleware}))

(defn- get-format-middleware [muuntaja]
  (when muuntaja format-middleware))

(defn- extend-middleware [middleware & defaults]
  (->>  defaults (conj (or middleware [])) (apply concat) vec compact))

(defmethod init-key :duct.reitit/middleware [_ options]
  (let [{:keys [muuntaja middleware coercion]} options
        {:keys [coerce-response coerce-request coerce-exceptions]} (get-coercion-middleware coercion)
        format-middleware    (get-format-middleware muuntaja)
        exception-middleware (get-exception-middleware options)
        create-middleware (partial extend-middleware middleware)]
    (create-middleware
     parameters-middleware
     environment-middleware
     format-middleware
     exception-middleware
     coerce-exceptions
     coerce-request
     coerce-response)))

