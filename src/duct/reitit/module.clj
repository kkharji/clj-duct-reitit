(ns duct.reitit.module
  (:require [integrant.core :refer [init-key] :as ig]
            [duct.core :as duct]
            [duct.reitit.util :as util :refer [get-namespaces]]
            [duct.reitit.handler]))

(defn- resolve-registry
  "Resolve registry keys into a map of {k [resolve config]}"
  [namespaces registry]
  (letfn [(process [f] (reduce f {} registry))
          (resolve [k] (util/resolve-key namespaces k))]
    (process
     (fn [acc [k v]]
       (when-let [res (resolve k)]
         (assoc acc k [res (or v {})]))))))

(defn- with-registry [config registry & configs]
  (let [registry (reduce-kv (fn [m _ v] (assoc m (first v) (second v))) {} registry)]
    (apply duct/merge-configs
           (->> configs
                (cons config)
                (cons registry)))))

(def ^:private default-config
  {:duct.core/handler-ns 'handler-ns
   :duct.core/middleware-ns 'middleware-ns})

(def ^:private default-options
  {:muuntaja true
   :coercion nil})

(defmethod init-key :duct.module/reitit [_ _]
  (fn [{:duct.reitit/keys [registry routes options] :as config}]
    (let [config     (merge default-config config)
          options    (assoc (merge default-options options) :logger (ig/ref :duct/logger))
          namespaces (get-namespaces config)
          registry   (resolve-registry namespaces registry)
          config     (dissoc (with-registry config registry) :duct.reitit/options :duct.reitit/routes)
          merge      (partial with-registry config registry)]
      (merge
       {:duct.reitit/middleware  options
        :duct.reitit/registry   (reduce-kv (fn [m k v] (assoc m k (ig/ref (first v)))) {} registry)
        :duct.handler/root      (assoc options :router (ig/ref :duct.router/reitit))
        :duct.router/reitit     {:routes routes
                                 :registry (ig/ref :duct.reitit/registry)
                                 :options (assoc options :middleware (ig/ref :duct.reitit/middleware))
                                 :namespaces namespaces}}))))

