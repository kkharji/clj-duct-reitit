(ns duct.reitit.router
  (:require [clojure.walk :refer [postwalk]]
            [duct.logger :as logger]
            [duct.reitit.util :as util :refer [compact member? resolve-key defm new-date new-uuid]]
            [integrant.core :as ig :refer [init-key]]
            [muuntaja.core :refer [instance] :rename {instance muuntaja-instance}]
            [reitit.coercion.malli :as malli]
            [reitit.coercion.schema :as schema]
            [reitit.coercion.spec :as spec]
            [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :refer [format-middleware]]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]
            [duct.reitit.middleware.exception :as exception]
            [duct.reitit.middleware.coercion :as coercion]
            [ring.middleware.cors :refer [wrap-cors]]
            [duct.reitit.middleware.logging :as logging]))

(def ^:private coercers
  {:malli malli/coercion :spec spec/coercion :schema schema/coercion})

(defn- get-coercion
  "Returns coercion coercion is non-nil and coercer is defiend"
  [coercion]
  (some-> coercion :coercer keyword coercers))

(defn- get-muuntaja
  "Returns muuntaja instance, when boolean use the default one, otherwise x"
  [x]
  (when x (if (boolean? x) muuntaja-instance x)))

(defn- get-cors-middleware
  "Creates reitit cross-origin middleware."
  [arguments]
  (let [to-key  (fn [k] (keyword (str "access-control-allow-" (name k))))
        arguments (apply concat (reduce-kv #(assoc %1 (to-key %2) %3) {} arguments))]
    {:name ::cors
     :wrap (fn [handler]
             (apply wrap-cors handler arguments))}))

(defm environment-middleware [{:keys [environment]} _ handler request]
  (->> environment
       (merge {:start-date (new-date) :id (new-uuid)})
       (into request)
       (handler)))

(defn- get-router-middleware [{:keys [cross-origin muuntaja middleware logging] :as options}]
  (let [format-middleware       (when muuntaja format-middleware)
        exception-middleware    (exception/get-middleware options)
        coercion-middlewares    (coercion/get-middleware options)
        log-request-middleware  (when (:requests? logging) [logging/request-middleware])
        log-response-middleware (when (:requests? logging) [logging/response-middleware])
        cors-middleware         (when cross-origin (get-cors-middleware cross-origin))
        default-middelwares     [parameters-middleware cors-middleware format-middleware exception-middleware]
        conact-middleware       (fn [& handlers] (compact (vec (apply concat handlers))))]
    (conact-middleware [environment-middleware]
                       log-request-middleware
                       default-middelwares
                       coercion-middlewares
                       middleware
                       log-response-middleware)))

(defmethod init-key :duct.reitit/routes [_ {:keys [routes registry namespaces]}]
  (let [resolve-key (partial resolve-key namespaces)
        member?     (partial member? (keys registry))
        valid?  (fn [x] (and (keyword? x) (member? x)))
        resolve (fn [x] (cond (symbol? x) (or (resolve-key x) x)
                              (valid? x)  (get registry x)
                              :else x))]
    (postwalk resolve routes)))

(defmethod init-key :duct.reitit/router
  [_ {:keys [routes log options]}]
  (let [environment {:environment (:environment options)}
        muuntaja    {:muuntaja (get-muuntaja (:muuntaja options))}
        coercion    {:coercion (get-coercion (:coercion options))}
        middleware  {:middleware (get-router-middleware (assoc options :log log))}
        logger      {:logger (merge (:logging options) {:log log})}
        data [environment muuntaja coercion middleware logger]]
    (ring/router routes {:data (compact (apply merge data))})))

