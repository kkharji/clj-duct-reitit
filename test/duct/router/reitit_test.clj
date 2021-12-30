(ns duct.router.reitit-test
  (:require [clojure.test :refer [are deftest is testing]]
            [duct.router.reitit :refer [process-config]]
            [foo.handler]
            [foo.handler.plus]
            [integrant.core :refer [init-key]]
            [reitit.coercion :as reitit.coercion]
            [reitit.core :as r]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as rrc]
            [taoensso.timbre :refer [spy]]
            [reitit.coercion.spec :as coercion.spec]
            [jsonista.core :as jsonista]))

(def routes
  [["/" :index]
   ["/ping" {:get {:handler :ping}}]
   ["/plus" {:get 'plus/with-query
             :post :plus/with-body}]
   ["/fail" {:handler (fn [_] (throw (Exception. "fali")))}]])

(def registry
  {:index (init-key :foo.handler/index {:path "public/index.html"})
   :plus/with-body (init-key :foo.handler.plus/with-body {})
   :ping  (init-key :foo.handler/ping {:message "pong"})})

(def base-config
  {:registry registry
   :routes routes
   :namespaces ['foo.handler 'foo.middleware]})

(def extend-config
  (partial merge base-config))

(deftest config-processing
  (testing "Default-Configuration"
    (let [router-config (->> nil extend-config process-config)
          routes (first router-config)
          data (:data (second router-config))
          middleware (data :middleware)]
      (is (vector? routes))
      (is (map? data))

      (testing ":registry-processing"
        (is (= 4 (count routes)))
        (is (fn?  (-> routes first second)))
        (is (fn?  (-> routes second second :get :handler)))
        (is (map? (-> routes (get 2) second :get)))
        (is (fn?  (-> routes (get 2) second :get :handler)))
        (is (map? (-> routes (get 2) second :post)))
        (is (fn?  (-> routes (get 2) second :post :handler))))

      (testing ":default-middlewares"

        (is (= :reitit.ring.middleware.parameters/parameters
               (:name (first middleware))))

        (is (= 1 (count middleware)))))

    (testing "munntaja"
      (let [addition {:opts {:munntaja true}}
            router-config (-> addition extend-config process-config)
            data (:data (second router-config))
            middleware (:middleware data)]

        (is (not (nil? (:muuntaja data))))

        (is (= :reitit.ring.middleware.muuntaja/format (:name (second middleware))))

        (is (= 2 (count middleware)))))

    (testing "Malli coercion"
      (let [addition {:opts {:coercion true} :coercer 'malli}
            router-config (process-config (extend-config addition))
            data (:data (second router-config))
            middleware (:middleware data)]
        (is (= :malli
               (reitit.coercion/-get-name (data :coercion))))

        (is (= [::rrc/coerce-exceptions ::rrc/coerce-request ::rrc/coerce-response]
               (mapv :name (rest middleware))))

        (is (= 4 (count middleware)))))))

;; FIXME: This should be automatically done ! when munntaja is on!
(defn to-edn [response]
  (-> response
      (:body)
      (slurp)
      (jsonista/read-value jsonista/keyword-keys-object-mapper)))

(deftest reitit-routing
  (let [extra-config  {:coercer 'spec :opts {:coercion true :munntaja true}}
        config (extend-config extra-config)
        router (init-key :duct.router/reitit config)]

    (testing "routes"
      (is (= 4 (count (r/routes router)))
          "Should only have our four routes defined.")
      (is (= :reitit.core/router (type router))
          "Should be of type router")
      (are [path handler-path] (fn? (get-in (r/match-by-path router path) handler-path))
        "/"     [:data :handler]
        "/ping" [:data :get :handler]
        "/plus" [:data :get :handler]
        "/plus" [:data :post :handler]))

    (testing "handler"
      (let [handler (ring/ring-handler router)]
        (is (nil? (handler {:request-method :get :uri "/not-a-route"})))
        (is (string? (:body (handler {:request-method :get :uri "/"}))))
        (is (= "pong" (-> {:request-method :get :uri "/ping"} handler to-edn :message)))
        (is (= 9 (-> {:request-method :post :uri "/plus" :body-params {:y 3 :x 6}} handler to-edn :total)))
        (is (= 9 (-> {:request-method :get :uri "/plus" :query-params {:y 3 :x 6}} handler to-edn :total)))))))

