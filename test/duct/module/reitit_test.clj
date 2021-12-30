(ns duct.module.reitit-test
  (:require [clojure.test :refer [deftest testing is are]]
            [duct.module.reitit]
            [foo.handler]
            [foo.handler.plus]
            [duct.core :as core]
            [integrant.core :as ig]
            [taoensso.timbre :refer [spy]]
            [reitit.ring :as ring]
            [reitit.core :as r]
            [fipp.clojure :refer [pprint]]))

(core/load-hierarchy)

(derive :foo/database :duct/const)
(derive :foo/index-path :duct/const)

(def basic-config
  {:duct.module/reitit {}
   :duct.profile/base
   {:duct.core/project-ns 'foo
    :duct.core/handler-ns 'handler ; default value
    :duct.core/middleware-ns 'middleware ; default value

    :foo/database {}
    :foo/index-path "resources/index.html"

    :duct.module.reitit/routes
    [["/" :index]
     ["/ping" {:get {:handler :ping}}]
     ["/plus" {:post :plus/with-body
               :get 'plus/with-query}]]

    :duct.module.reitit/registry
    {:index {:path (ig/ref :foo/index-path)}
     :ping  {:message "pong"}
     :plus/with-body {}}

    :duct.module.reitit/opts
    {:coercion nil ; default nil
     :environment {:db (ig/ref :foo/database)} ; default nil
     :middleware []}

    :duct.module.reitit/cors
    {:origin [#".*"] ;; defaults in for dev and local environment
     :methods [:get :post :delete :options]}}})

(defn- routes [router]
  (reduce (fn [acc [k v]] (assoc acc k v)) {} (r/routes router)))

(deftest module-test
  (let [config (ig/init (core/prep-config  basic-config))]
    (testing "should read without errors"
      (is (map? config)))
    (testing "should merge registry's integrant keys"
      (are [x] (not= nil (x config))
        :foo.handler/ping
        :foo.handler/index
        :foo.handler.plus/with-body))
    (testing "should initialized duct.router/reitit"
      (let [router (config :duct.router/reitit)
            routes (routes router)]
        (is (= :reitit.core/router (type router)))
        (is (= 3 (count routes)))
        (is (map? (-> (get routes "/") :environment :db)))
        (are [route path] (fn? (get-in (r/match-by-path router route) path))
          "/"     [:data :handler]
          "/ping" [:data :get :handler]
          "/plus" [:data :get :handler]
          "/plus" [:data :post :handler])))))
