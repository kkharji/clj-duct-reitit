(ns duct.test-helpers
  (:require [duct.core :as core]
            [integrant.core :as ig]
            [reitit.core :as r]))

(defn test-options [base & [profiles]]
  (let [config (core/build-config base profiles)
        in-config (partial get-in config)]
    #(in-config (cons :duct.reitit/options (if (vector? %) % [%])))))

(defn request [method uri req]
  (merge req {:request-method method :uri uri}))

(defn routes [router]
  (reduce (fn [acc [k v]] (assoc acc k v)) {} (r/routes router)))

(def base-config
  {:duct.module/reitit {}
   :duct.module/logging {}
   :duct.profile/base {:duct.core/project-ns 'foo}})

(derive :foo/database :duct/const)
(derive :foo/index-path :duct/const)

(def test-config
  {:duct.core/project-ns    'foo
   :duct.core/handler-ns    'handler ;; Where should handlers keys be localated
   :duct.core/middleware-ns 'middleware  ;; Where should middleware keys be located
   :duct.reitit/routes     [["/" :index] ;; Routes Configuration
                            ["/author" :get-author]
                            ["/ping" {:get {:handler :ping}}]
                            ["/plus" {:post :plus/with-body
                                      :get 'plus/with-query}]
                            ["/divide" {:get :divide}]]
   :duct.reitit/registry  {:index {:path  (ig/ref :foo/index-path)} ;; init foo.handler/index with {:path}
                           :ping  {:message "pong"} ;; init foo.handler/ping with {:message}
                           :plus/with-body {} ;; init foo.handler.plus/with-body
                           :get-author {} ;; init foo.handler/get-author
                           :divide {}}    ;; init foo.handler/divide
   :duct.reitit/exception   (ig/ref :foo.handler/exceptions)
   :duct.reitit/environment {:db (ig/ref :foo/database)}
   :foo/database            [{:author "tami5"}]
   :foo/index-path          "resources/index.html"
   :foo.handler/exceptions  {}})

(defn with-base-config [config]
  (->> config
       (merge test-config)
       (assoc base-config :duct.profile/base)))

(defn init-system
  "Takes reitit options and merge it to base-config for testing"
  [config]
  (-> config core/prep-config ig/init))

