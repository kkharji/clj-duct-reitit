(ns duct.module.reitit
  (:require [integrant.core :refer [init-key] :as ig]
            [duct.core :as duct]
            [duct.reitit.util :as util]))

(def ^:private default-config
  {:duct.core/handler-ns 'handler-ns
   :duct.core/middleware-ns 'middleware-ns})

(defn- merge-with-defaults [config]
  (merge default-config config))

(defn- resolve-registry
  "Given a registry"
  {:test #(let [namespaces ["foo.handler" "foo.middleware"]
                assert-eql (fn [a b] (assert (= (resolve-registry namespaces a) b)))]
            (assert-eql
             [[:index {:path (ig/ref :index-path)}]
              [:ping {:message "pong"}]
              [:plus/with-body]]
             {:index [:foo.handler/index {:path (ig/ref :index-path)}]
              :plus/with-body [:foo.handler.plus/with-body {}]
              :ping  [:foo.handler/ping {:message "pong"}]}))}
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

(defmethod init-key :duct.module/reitit [_ _]
  (fn [{::keys [registry routes opts cors] :as config}]
    (let [config     (merge-with-defaults config)
          namespaces (get-namespaces config)
          registry   (resolve-registry namespaces registry)
          extras     {:duct.router/reitit {:routes routes
                                           :registry (registry->key registry)
                                           :opts opts
                                           :cors cors}}
                      ;; :duct.handler/root  {:router (ig/ref :duct.router/reitit)}}
          config (->> (registry->config registry)
                      (merge extras)
                      (duct/merge-configs config))]
      (dissoc config ::opts ::registry ::routes ::cors))))

(comment
  (test #'resolve-registry))
