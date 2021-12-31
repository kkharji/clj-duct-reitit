(ns duct.module.reitit
  (:require [integrant.core :refer [init-key] :as ig]
            [duct.core :as duct]
            [duct.reitit.util :as util]
            [duct.handler.root]))

(defn- resolve-registry
  "Resolve registry keys into a map of {k [resolve config]}"
  [namespaces registry]
  (letfn [(process [f] (reduce f {} registry))
          (resolve [k] (util/resolve-key namespaces k))]
    (process
     (fn [acc [k v]]
       (when-let [res (resolve k)]
         (assoc acc k [res (or v {})]))))))

(defn- registry->config [registry]
  (letfn [(process [f] (reduce-kv f {} registry))]
    (process
     (fn [m _ v]
       (assoc m (first v) (second v))))))

(defn- registry->key [registry]
  (letfn [(process [f] (reduce-kv f {} registry))]
    (process
     (fn [m k v]
       (assoc m k (ig/ref (first v)))))))

(defn- get-namespaces [config]
  (->> [:duct.core/handler-ns :duct.core/middleware-ns]
       (select-keys config)
       (vals)
       (util/get-namespaces (config :duct.core/project-ns))))

(def ^:private default-config
  {:duct.core/handler-ns 'handler-ns
   :duct.core/middleware-ns 'middleware-ns})

(def ^:private default-opts
  {:muuntaja true
   :coercion true
   :coercer nil})

(defmethod init-key :duct.module/reitit [_ _]
  (fn [{::keys [registry routes opts cors] :as config}]
    (let [config     (merge default-config config)
          opts       (merge default-opts opts)
          namespaces (get-namespaces config)
          registry   (resolve-registry namespaces registry)
          config     (-> config
                         (duct/merge-configs (registry->config registry))
                         (dissoc ::opts ::registry ::routes ::cors))
          merge      (partial merge config)]
      (merge
       {:duct.router/middleware opts
        :duct.router/reitit {:routes routes
                             :registry (registry->key registry)
                             :opts (assoc opts :middleware (ig/ref :duct.router/middleware))
                             :cors cors
                             :namespaces namespaces}
        :duct.handler/root (assoc opts :router (ig/ref :duct.router/reitit))}))))

(comment
  (test #'resolve-registry))
