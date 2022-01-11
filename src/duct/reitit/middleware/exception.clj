(ns duct.reitit.middleware.exception
  (:require [duct.reitit.format :as format]
            [duct.reitit.middleware.coercion :as coercion]
            [duct.reitit.request :as request]
            [reitit.ring.middleware.exception :as exception :refer [create-exception-middleware default-handlers]]
            [duct.reitit.util :refer [spy]]))

(defn coercion-ex? [type]
  (or (= :reitit.coercion/request-coercion  type)
      (= :reitit.coercion/response-coercion type)))

(defmulti ex-format
  (fn [exception _request {:keys [coercions?]}]
    (let [{:keys [type]} (ex-data exception)]
      (if (and coercions? (coercion-ex? type))
        :coercion :exception))))

(defmethod ex-format :coercion [exception request {:keys [pretty? with-req-info? coercions?] :as opts}]
  (when coercions?
    (let [data (ex-data exception)
          request-info (when with-req-info? (request/info request pretty?))]
      (format/coercion data opts request-info))))

(defmethod ex-format :exception [exception request {:keys [pretty? with-req-info? exceptions?]}]
  (when exceptions?
    (let [req-info   (when with-req-info? (request/info request pretty?))
          ex-trace   (format/trace-compact exception)
          ex-cause   (ex-cause exception)
          ex-message (ex-message exception)]
      (if pretty?
        (format/exception-pretty req-info ex-trace ex-cause ex-message)
        (format/exception-compact request ex-trace ex-message)))))

(defn ^:private get-exception-wrapper [log config]
  (let [config (merge config {:with-req-info? true})]
    (fn [handler exception request]
      (log :error (ex-format exception request config))
      (handler exception request))))

(defn get-middleware
  "Create custom exception middleware."
  [{:keys [coercion exception log logging]}]
  (let [{:keys [enable coercions? exceptions?]} logging
        should-wrap (or (and enable coercions?) (and enable exceptions?))
        create-middleware #(create-exception-middleware (apply merge default-handlers %))]
    (create-middleware
     [(when should-wrap {::exception/wrap (get-exception-wrapper log logging)})
      (when (coercion :with-formatted-message?) (coercion/get-exception-handler coercion))
      exception])))
