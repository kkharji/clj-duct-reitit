(ns duct.router.middleware-test
  (:require [duct.router.middleware]
            [clojure.test :refer [is are deftest testing]]
            [integrant.core :as ig :refer [init-key]]
            [reitit.ring :as ring]
            [clojure.pprint :refer [pprint]]
            [duct.reitit.util :refer [compact]]
            [taoensso.timbre :refer [spy]])
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
                    ["/identity-get" {:get identity}]]
        router     (new-router routes {:middleware middleware :environment {:name "tami5"}})
        app        (ring/ring-handler router)]

    (testing "Match result"
      (are [m req c] (= c (count-result (app (assoc req :request-method m))))
        :get {:uri "/identity"} 9            ;; Creates the same handler for all http methods.
        :get {:uri "/identity-get"} 2))      ;; Creates get and options handler

    (testing "Request Keys"
      (let [request (app {:request-method :get :uri "/identity"})]
        (is (= [:environment                 ;; User defined injected environment keys
                :form-params                 ;; ..
                :id                          ;; Request id injected by environment-middleware
                :params                      ;; Merge of all types of params
                :path-params                 ;; ..
                :query-params                ;; ..
                :request-method              ;; Request method
                :start-date                  ;; Request Start date injected by environment-middleware
                :uri                         ;; ..
                :reitit.core/match           ;; Matches
                :reitit.core/router]         ;; Reitit Router
               (sort (keys request))))
        (are [path expected-type] (= expected-type (type (get-in request path)))
          [:id] UUID                         ;; Random UUID to the request injected by environment-middleware
          [:start-date] Date                 ;; Start date injected by environment-middleware
          [:environment :name] String        ;; Environment map injected by environment-middleware
          [:params] PersistentArrayMap)))))  ;; create params, a marge of all params types injected by parameters-middleware

