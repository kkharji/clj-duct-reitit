(ns duct.router.reitit
  (:require [duct.logger :as logger]
            [integrant.core :as ig :refer [init-key]]
            [muuntaja.core :refer [instance] :rename {instance m-instance}]
            [reitit.ring.middleware.muuntaja :as m]
            [reitit.ring.middleware.parameters :as param]
            [reitit.ring.coercion :as rcc]
            [reitit.coercion.malli :as malli]
            [reitit.coercion.spec :as spec]
            [reitit.coercion.schema :as schema]
            [duct.reitit.util :as util :refer [member? compact resolve-key]]
            [clojure.walk :refer [postwalk]]
            [reitit.coercion :refer [compile-request-coercers]]
            [reitit.ring :as ring]))

(def ^:private coercion-index
  {:malli  malli/coercion
   :spec   spec/coercion
   :schema schema/coercion})

(defn- get-middleware [munntaja middleware coercion]
  (let [extend #(->> % (concat (or middleware [])) (vec))]
    (-> []
        (conj param/parameters-middleware)
        (conj (when munntaja m/format-middleware))
        (conj (when coercion rcc/coerce-exceptions-middleware))
        (conj (when coercion rcc/coerce-request-middleware))
        (conj (when coercion rcc/coerce-response-middleware))
        (extend)
        (compact))))

(defn- get-coercion [coercer coercion]
  (when coercion
    (some-> coercer keyword coercion-index)))

(defn- get-routes [registry routes namespaces]
  (let [resolve (partial resolve-key namespaces)
        member?  (partial member? (keys registry))
        valid? (fn [x] (and (keyword? x) (member? x)))]
    (postwalk
     (fn [x]
       (cond (symbol? x) (or (resolve x) x)
             (valid? x)  (get registry x) :else x))
     routes)))

;; TODO: introduce coercion/compile-request-coercers?
;; TODO: pretty coercion errors
(defn process-config
  [{{:keys [munntaja environment middleware coercion coercer]} :opts
    :keys [registry routes namespaces]}]
  (let [routes (get-routes registry routes namespaces)
        config (->> {:environment environment
                     :middleware (get-middleware munntaja middleware coercion)
                     :coercion   (get-coercion coercer coercion)
                     :muuntaja   (when munntaja (if (boolean? munntaja) m-instance munntaja))
                     :compile    (when coercer compile-request-coercers)}
                    (compact)
                    (hash-map :data))]
    [routes config]))

(def ^:private default-opts
  {:munntaja true
   :coercion true
   :spec nil})

(defmethod init-key :duct.router/reitit
  [_ {:keys [logger opts] :as config}]
  (when logger (logger/log logger :report ::init))
  (->> (merge default-opts opts)
       (assoc config :opts)
       (process-config)
       (apply ring/router)))
