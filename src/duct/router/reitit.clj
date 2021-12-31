(ns duct.router.reitit
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
  {:malli  malli/coercion
   :spec   spec/coercion
   :schema schema/coercion})

(defn- get-resolver [registry namespaces]
  (let [resolve (partial resolve-key namespaces)
        member? (partial member? (keys registry))
        valid?  (fn [x] (and (keyword? x) (member? x)))]
    (fn [x]
      (cond (symbol? x) (or (resolve x) x)
            (valid? x)  (get registry x)
            :else x))))

;; TODO: introduce coercion/compile-request-coercers?
(defn ^:private get-muuntaja [muuntaja]
  (cond (boolean? muuntaja) muuntaja-instance
        (nil? muuntaja) nil
        :else muuntaja))

; :compile (when coercer compile-request-coercers) ;; (and compile-coercers?)
(defn get-options [{:keys [muuntaja environment middleware coercer]}]
  {:data
   (compact
    {:environment environment
     :muuntaja (get-muuntaja muuntaja)
     :middleware middleware
     :coercion (some-> coercer keyword coercion-index)})})

(defmethod init-key :duct.router/reitit [_ {:keys [logger registry routes namespaces opts]}]
  (when logger (logger/log logger :report ::init))
  (ring/router
   (postwalk (get-resolver registry namespaces) routes)
   (get-options opts)))
