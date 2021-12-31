(ns duct.router.middleware-test
  (:require [duct.router.middleware]
            [clojure.test :refer [is are deftest testing]]
            [integrant.core :as ig :refer [init-key]]
            [reitit.ring :as ring]
            [clojure.pprint :refer [pprint]]
            [duct.reitit.util :refer [compact]]
            [taoensso.timbre :refer [spy]]
            [reitit.coercion.spec :as coercion.spec]
            [clojure.string :as str])
  (:import [clojure.lang PersistentArrayMap]
           [java.util UUID]
           [java.util Date]
           [java.lang String]))

(defn- new-router [routes config]
  (ring/router routes {:data config}))

(defn- new-middleware [cfg]
  (init-key :duct.router/middleware cfg))

(defn- count-result [res]
  (-> res :reitit.core/match :result compact count))

(deftest default-middleware-behavior
  (let [middleware (new-middleware {:munntaja false :coercion false})
        routes     [["/identity" identity]
                    ["/identity-get" {:get identity}]
                    ["/user/:id" {:get identity}]
                    ["/err" #(throw (ex-info "error" {}))]]
        router     (new-router routes {:middleware middleware :environment {:name "tami5"}})
        app        (ring/ring-handler router)]

    (testing "Match result"
      (are [m req c] (= c (count-result (app (assoc req :request-method m))))
        :get {:uri "/identity"} 9            ;; Creates the same handler for all http methods.
        :get {:uri "/identity-get"} 2))      ;; Creates get and options handler

    (testing "Request Keys"
      (let [request (app {:request-method :get :uri "/identity"})]
        (is (= [:environment                 ;; environment key [environment-middleware]
                :form-params                 ;; ..
                :id                          ;; Request id [environment-middleware]
                :params                      ;; Merge of all types of params. NOTE: Doesn't seem accurate.
                :path-params                 ;; ..
                :query-params                ;; ..
                :request-method              ;; Request method
                :start-date                  ;; Request Start date [environment-middleware]
                :uri                         ;; ..
                :reitit.core/match           ;; Matches
                :reitit.core/router]         ;; Reitit Router
               (sort (keys request))))
        (are [path expected-type] (= expected-type (type (get-in request path)))
          [:id] UUID                         ;; Random UUID to the request injected by environment-middleware
          [:start-date] Date                 ;; Start date injected by environment-middleware
          [:environment :name] String        ;; Environment map injected by environment-middleware
          [:params] PersistentArrayMap)))))  ;; A marge of all params types injected by parameters-middleware
(defn is-int [str] (try (Integer/parseInt str) (catch Exception _ nil)))

(deftest coercion-middleware-behavior
  (let [middleware (new-middleware {:munntaja false :coercion true})
        base {:get {:coercion coercion.spec/coercion
                    :parameters {:path {:company #(and (string? %)
                                                       (not (is-int %)))
                                        :user-id int?}}
                    :handler identity}}
        routes [["/identity/:company/users/:user-id" base]
                ["/:company/users/:user-id"
                 (-> base
                     (assoc-in [:get :response]
                               {200 {:body #(and (string? %) (str/includes? % "works at"))}})
                     (assoc-in [:get :handler]
                               (fn [{{:keys [db]}  :environment
                                     {{:keys [user-id company]} :path} :parameters}]
                                 {:status 200 :body (format "%s works at %s" (get-in db [:users user-id :user-name]) company)})))]]
        environment {:db {:companies {1 {:company "github"}}
                          :users {1 {:company 1 :user-name "tami5"}}}}
        router (new-router routes {:middleware middleware :environment environment})
        app (ring/ring-handler router)]

    (testing "Request Keys"
      (let [request (app {:request-method :get :uri "/identity/github/users/1"})]
        (is (= {:user-id 1 :company "github"} (:path (:parameters request)))
            "Should place path arguments inside parameters")

        (are [path expected-type] (= expected-type (type (get-in request path)))
          [:id] UUID                                ;; Random UUID to the request injected by environment-middleware
          [:start-date] Date                        ;; Start date injected by environment-middleware
          [:environment :db] PersistentArrayMap     ;; Environment map injected by environment-middleware
          [:params] PersistentArrayMap)))
    (testing "Exception"
      (let [request (app {:request-method :get :uri (str "/identity/" 32 "/users/" 1)})]
        (is (= 400 (:status request))
            "Should result in 400 status")
        (is (= '(:spec :problems :type :coercion :value :in) (keys (:body request)))
            "Should provide the following keys in body")))))



