(ns duct.lib.module
  (:require [duct.core :refer [merge-configs]]
            [integrant.core :as ig]))

(defn- not-member? [coll v]
  (nil? (some (fn [x] (= x v)) coll)))

(defn- unqualify [key]
  (keyword (name key)))

(defn- into-options?
  "Returns true, when the given key's namespace == root
  and key doesn't exist in 'blacklist"
  [root key blacklist]
  (and (= root (namespace key))
       (not-member? blacklist key)))

(defn- into-options
  "Returns a map with keys with root are taken into root/options."
  [root schema-keys config]
  (let [root (if (keyword? root) (name root) root)]
    (reduce-kv
     (fn [m k v]
       (if (into-options? root k schema-keys)
         (assoc-in m [(keyword root "options") (unqualify k)] v)
         (assoc m k v)))
     {} config)))

(defn- vector-map-transformer
  [store]
  (let [ref-or-store #(if (qualified-keyword? %) (ig/ref %) (get store %))
        process-key (fn [k] [(unqualify k) (ref-or-store k)])]
    #(into {} (mapv process-key %))))

(defn- transform-schema
  [schema store]
  (let [vector-transform (vector-map-transformer store)
        vec-of-refs? #(and (vector? %) (every? keyword? %))]
    (reduce-kv
     (fn [m k v]
       (assoc m k (cond
                    (keyword? v) (ig/ref v)
                    (vec-of-refs? v) (vector-transform v)
                    :else v))) {} schema)))

(defn init [{:keys [root config extra store schema]}]
  (let [root-namesspace (if (keyword? root) (name root) root)
        stripped-config (into-options root-namesspace (keys schema) config)
        final-config (apply merge-configs (cons stripped-config extra))]
    (merge final-config (transform-schema schema store))))
