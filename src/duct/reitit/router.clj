(ns duct.reitit.router
  (:require [clojure.walk :refer [postwalk]]
            [duct.logger :as logger]
            [duct.reitit.util :as util :refer [compact member? resolve-key]]
            [integrant.core :as ig :refer [init-key]]
            [muuntaja.core :refer [instance] :rename {instance muuntaja-instance}]
            #_[reitit.coercion :refer [compile-request-coercers]]
            [reitit.coercion.malli :as malli]
            [reitit.coercion.schema :as schema]
            [reitit.coercion.spec :as spec]
            [reitit.ring :as ring]))

(def ^:private coercion-index
  {:malli malli/coercion :spec spec/coercion :schema schema/coercion})

(defn- get-resolver [registry namespaces]
  (let [resolve (partial resolve-key namespaces)
        member? (partial member? (keys registry))
        valid?  (fn [x] (and (keyword? x) (member? x)))]
    (fn [x]
      (cond (symbol? x) (or (resolve x) x)
            (valid? x)  (get registry x)
            :else x))))

; :compile coercion/compile-request-coercers?
(defn process-options [{:keys [muuntaja environment middleware coercion]}]
  {:data (compact
          {:environment environment
           :middleware middleware
           :muuntaja  (cond (boolean? muuntaja) muuntaja-instance (nil? muuntaja) nil :else muuntaja)
           :coercion (some-> coercion :coercer keyword coercion-index)})})

(defmethod init-key :duct.router/reitit [_ {:keys [logger registry routes namespaces options]}]
  (when logger (logger/log logger :report ::init))
  (ring/router
   (postwalk (get-resolver registry namespaces) routes)
   (process-options options)))
