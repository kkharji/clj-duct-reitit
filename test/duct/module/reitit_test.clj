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
            [fipp.clojure :refer [pprint]]
            [duct.reitit.util :refer [to-edn]]))

(core/load-hierarchy)

(derive :foo/database :duct/const)
(derive :foo/index-path :duct/const)

(def basic-config
  {:duct.module/reitit {}
   :duct.profile/base
   {:duct.core/project-ns 'foo
    :duct.core/handler-ns 'handler ; default value
    :duct.core/middleware-ns 'middleware ; default value

    :foo/database [{:author "tami5"}]
    :foo/index-path "resources/index.html"

    :duct.module.reitit/routes
    [["/" :index]
     ["/author" :get-author]
     ["/ping" {:get {:handler :ping}}]
     ["/plus" {:post :plus/with-body
               :get 'plus/with-query}]]

    :duct.module.reitit/registry
    {:index {:path (ig/ref :foo/index-path)}
     :ping  {:message "pong"}
     :plus/with-body {}
     :get-author {}}

    :duct.module.reitit/opts
    {:coercion true ; default true
     :munntaja true ; default true
     :coercer 'spec ; default nil
     :environment {:db (ig/ref :foo/database)} ; default nil
     :middleware []}

    :duct.module.reitit/cors
    {:origin [#".*"] ;; defaults in for dev and local environment
     :methods [:get :post :delete :options]}}})

(defmethod ig/init-key :foo.handler/get-author [_ _]
  (fn [{{:keys [db]} :environment}]
    {:status 200 :body  db}))

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
        (is (= 4 (count routes)))
        (is (vector? (-> (get routes "/") :environment :db)))
        (are [route path] (fn? (get-in (r/match-by-path router route) path))
          "/"     [:data :handler]
          "/ping" [:data :get :handler]
          "/plus" [:data :get :handler]
          "/plus" [:data :post :handler]
          "/author" [:data :handler])))

    (testing "reitit ring handler"
      (let [handler (:duct.handler/root config)]
        (is (fn? handler))
        (is (nil? (handler {:request-method :get :uri "/not-a-route"})))
        (is (string? (:body (handler {:request-method :get :uri "/"}))))
        (is (= "pong" (-> {:request-method :get :uri "/ping"} handler to-edn :message)))
        (is (= 9 (-> {:request-method :post :uri "/plus" :body-params {:y 3 :x 6}} handler to-edn :total)))
        (is (= 9 (-> {:request-method :get :uri "/plus" :query-params {:y 3 :x 6}} handler to-edn :total)))))))
        ; (is (= "tami5" (-> {:request-method :get :uri "/author"} handler)))))))
