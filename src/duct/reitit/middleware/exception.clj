(ns duct.reitit.middleware.exception
  (:require [duct.reitit.util :refer [try-resolve-sym]]
            [reitit.ring.middleware.exception :as exception :refer [default-handlers create-exception-middleware]]))

(derive ::error ::exception)
(derive ::failure ::exception)
(derive ::horror ::exception)

(defn handler [message exception request]
  {:status 500
   :body {:message message
          :exception (str exception)
          :uri (:uri request)}})

(defn coercion-error-handler [status expound-printer _formatter]
  (let [printer (expound-printer {:theme :figwheel-theme, :print-specs? false})
        handler (exception/create-coercion-handler status)]
    (if printer
      (fn [exception request]
        (printer (-> exception ex-data :problems))
        (handler exception request))
      (fn [exception request] ;; TODO: format
        (handler exception request)))))

(defn coercion-handlers [{:keys [pretty-print? formatter]}]
  (let [printer (when pretty-print? (try-resolve-sym 'expound.alpha/custom-printer))]
    (when (or printer formatter)
      #:reitit.coercion
       {:request-coercion (coercion-error-handler 400 printer formatter)
        :response-coercion (coercion-error-handler 500 printer formatter)})))

(defn- with-default-exceptions [& handlers]
  (->> (cons default-handlers handlers)
       (apply merge)
       (create-exception-middleware)))

(defn get-exception-middleware
  "Create custom exception middleware."
  [{:keys [coercion _exception]}]
  (with-default-exceptions
    (coercion-handlers coercion)))

