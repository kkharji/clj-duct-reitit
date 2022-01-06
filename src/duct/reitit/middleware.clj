(ns duct.reitit.middleware
  "Construct Ring-Reitit Global Middleware"
  (:require [duct.reitit.util :refer [compact defm spy]]
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

(defn- merge-middlewares [{:keys [middleware] :as options}]
  (let [coercion-middlewares  (coercion/get-middleware options)]
    (fn [& defaults]
      (-> (concat defaults coercion-middlewares middleware)
          (vec)
          (compact)))))

(defn create-router-middleware [{:keys [muuntaja] :as options}]
  (let [format-middleware    (get-format-middleware muuntaja)
        exception-middleware (exception/get-middleware options)
        merge-middlewares    (merge-middlewares options)]
    (merge-middlewares parameters-middleware
                       environment-middleware
                       format-middleware
                       exception-middleware)))

