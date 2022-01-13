(ns duct.reitit.middleware.coercion
  (:require   [reitit.ring.coercion :as rcc]
              [reitit.ring.middleware.exception :as exception]
              [reitit.coercion :as coercion]
              [duct.reitit.format :as format]
              [clojure.string :as str]))

(defn get-coercion-exception-handler [status]
  (let [handler (exception/create-coercion-handler status)]
    (fn [ex req]
      (let [msg (-> (ex-data ex)
                    (format/coercion {:print-spec? true :pretty? true :without-trace? true} nil)
                    (str/replace #"\u001b\[[0-9;]*[mK]" ""))]
        (assoc-in (handler ex req) [:body :message] msg)))))

(defn get-exception-handler
  [{:keys [with-formatted-message?] :as _coercion}]
  (when with-formatted-message?
    {::coercion/request-coercion (get-coercion-exception-handler 400)
     ::coercion/response-coercion (get-coercion-exception-handler 500)}))

(defn get-middleware
  [{{:keys [with-formatted-message? coercer]} :coercion
    {:keys [coercions? exceptions?]} :logging}]
  (when coercer
    [(when-not (or coercions? exceptions? with-formatted-message?)
       rcc/coerce-exceptions-middleware)
     rcc/coerce-request-middleware
     rcc/coerce-response-middleware]))
