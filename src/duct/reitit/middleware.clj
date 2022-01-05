(ns duct.reitit.middleware
  "Construct Ring-Reitit Global Middleware"
  (:require [integrant.core :refer [init-key]]
            [duct.reitit.util :refer [compact defm spy]]
            [reitit.ring.middleware.muuntaja :refer [format-middleware]]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]
            [duct.reitit.middleware.exception :as exception]
            [duct.reitit.middleware.coercion :as coercion]))

;; TODO: inject environment keys instead
(defm environment-middleware [opts _ handler request]
  (let [inject #(handler (into request %))]
    (inject (assoc (opts :environment)
                   :id  (java.util.UUID/randomUUID)
                   :start-date (java.util.Date.)))))

(defn- get-format-middleware [muuntaja]
  (when muuntaja format-middleware))

(defn- create-middleware [extras]
  (fn [& defaults]
    (->> (conj extras defaults)
         (apply concat)
         (vec) (compact))))

(defmethod init-key :duct.reitit/middleware
  [_ {:keys [logging] {:keys [muuntaja middleware coercion exception]} :options}]
  (let [{:keys [coerce-response coerce-request coerce-exceptions]} (coercion/get-middleware coercion logging)
        format-middleware    (get-format-middleware muuntaja)
        exception-middleware (exception/get-middleware logging coercion exception)
        create-middleware    (create-middleware middleware)]
    (create-middleware parameters-middleware
                       environment-middleware
                       format-middleware
                       exception-middleware
                       coerce-exceptions
                       coerce-request
                       coerce-response)))

