(ns duct.reitit.middleware
  "Construct Ring-Reitit Global Middleware"
  (:require [integrant.core :refer [init-key]]
            [duct.reitit.util :refer [compact try-resolve-sym]]
            [reitit.ring.coercion :as rcc]
            [reitit.ring.middleware.muuntaja :refer [format-middleware]]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]
            [reitit.ring.middleware.exception :as exception :refer [create-exception-middleware]]))

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

(defn coercion-error-handler [expound-printer status]
  (let [printer (expound-printer {:theme :figwheel-theme, :print-specs? false})
        handler (exception/create-coercion-handler status)]
    (fn [exception request]
      (printer (-> exception ex-data :problems))
      (handler (-> exception) request))))

;; TODO: use custom coercion error formater for response
(def pretty-coercion-errors
  (if-let [expound-printer (try-resolve-sym 'expound.alpha/custom-printer)]
    [(create-exception-middleware
      (merge exception/default-handlers
             {:reitit.coercion/request-coercion (coercion-error-handler expound-printer 400)
              :reitit.coercion/response-coercion (coercion-error-handler expound-printer 500)}))]))

(defmethod init-key :duct.reitit/middleware [_ options]
  (let [{:keys [muuntaja middleware coercion]} options]
    (->> [parameters-middleware
          environment-middleware
          (when muuntaja format-middleware)
          (when coercion
            (if (coercion :pretty-coercion?)
              pretty-coercion-errors
              rcc/coerce-exceptions-middleware))
          (when coercion rcc/coerce-request-middleware)
          (when coercion rcc/coerce-response-middleware)]
         (extend-middleware middleware))))
