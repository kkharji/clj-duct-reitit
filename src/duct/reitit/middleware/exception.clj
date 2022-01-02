(ns duct.reitit.middleware.exception
  (:require [duct.reitit.util :refer [try-resolve-sym spy compact]]
            [reitit.ring.middleware.exception :as exception :refer [default-handlers create-exception-middleware]]
            [duct.logger :as logger]
            [clojure.pprint :as pprint]))

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

(defn- format-exception-log [e r pretty?]
  (let [log {:message (ex-message e)
             :uri (:uri r)
             :method (:request-method r)
             :params (compact {:body (not-empty (:body-params r))
                               :form (not-empty (:form-params r))
                               :query (not-empty (:query-params r))
                               :path (not-empty (:path-params r))})
             :trace (->> (.getStackTrace e)
                         (map #(hash-map :line-number (.getLineNumber %) :file-name (.getFileName %)))
                         (take 5)
                         (vec))}]
    (if pretty? (str "\n" (with-out-str (pprint/pprint log))) log)))

(defn get-exception-middleware
  "Create custom exception middleware."
  [{:keys [coercion exception logger]}]
  (let [coercion-handlers (coercion-handlers coercion)]
    (with-default-exceptions
      (:handlers exception)
      coercion-handlers
      (when (:log? exception)
        {::exception/wrap
         (fn [handler e request]
           (if logger
             (logger/log logger :error (format-exception-log e request (:pretty? exception)))
             (pprint/pprint (format-exception-log e request false)))
           (handler e request))}))))


