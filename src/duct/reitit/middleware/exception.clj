(ns duct.reitit.middleware.exception
  (:require [duct.reitit.util :refer [try-resolve-sym spy compact member?]]
            [reitit.ring.middleware.exception :as exception :refer [default-handlers create-exception-middleware]]
            [duct.logger :as logger]
            [clojure.pprint :as pprint]
            [duct.reitit.util :as util]))

(defn get-coercion-error-handler [status expound-printer _formatter]
  (let [printer (expound-printer {:theme :figwheel-theme, :print-specs? false})
        handler (exception/create-coercion-handler status)]
    (if printer
      (fn [exception request]
        (printer (-> exception ex-data :problems))
        (handler exception request))
      (fn [exception request] ;; TODO: format
        (handler exception request)))))

(defn coercion-handlers [{:keys [formatter]} {:keys [pretty? types]}]
  (let [printer (when (and pretty? (member? types :coercion))
                  (try-resolve-sym 'expound.alpha/custom-printer))]
    (when (or printer formatter)
      #:reitit.coercion
       {:request-coercion (get-coercion-error-handler 400 printer formatter)
        :response-coercion (get-coercion-error-handler 500 printer formatter)})))

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

(defn- get-exception-wrapper [{:keys [logger pretty?]}]
  (fn [handler exception request]
    (if logger
      (logger/log logger :error (format-exception-log exception request pretty?))
      (pprint/pprint (format-exception-log exception request false)))
    (handler exception request)))

(defn get-exception-middleware
  "Create custom exception middleware."
  [{:keys [coercion exception logging]}]
  (let [coercion-handlers (coercion-handlers coercion logging)
        exception-wrapper (when (member? (:types logging) :exception)
                            {::exception/wrap (get-exception-wrapper logging)})]
    (with-default-exceptions
      exception
      coercion-handlers
      exception-wrapper)))
