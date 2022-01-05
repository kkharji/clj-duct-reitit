(ns duct.reitit.middleware.format
  (:require [clojure.string :as str]
            [expound.alpha :as expound]
            [reitit.coercion :as coercion :refer [-get-name] :rename {-get-name spec-type}]
            [duct.reitit.request :as request]
            [duct.reitit.util :refer [spy]]))

(defn coercion-ex? [type]
  (or (= :reitit.coercion/request-coercion  type)
      (= :reitit.coercion/response-coercion type)))

(defn ex-trace [exception]
  (->> (.getStackTrace exception)
       (map #(str (.getFileName %) ":" (.getLineNumber %)))
       (take 5)
       (str/join " => ")))

(defn spec-print [{:keys [pre problems print-spec?]}]
  (let [cfg {:theme :figwheel-theme :print-specs? print-spec?}
        -print (expound/custom-printer (if pre (assoc cfg :value-str-fn pre) cfg))]
    (-print problems)))

(defmulti ex-format
  (fn [exception _request {:keys [coercions?]}]
    (let [data (ex-data exception)
          kind (if (and coercions? (coercion-ex? (:type data))) :coercion :exception)
          type (when (= :coercion kind) (-> data :coercion spec-type))]
      [kind type])))

(defmethod ex-format [:coercion :spec]
  [exception request {:keys [_pretty? with-req-info? print-spec? coercions?]}]
  (when coercions?
    (let [with-info
          (when with-req-info?
            (fn [_name form path _value]
              (let [message (str (pr-str form) "\n\n" "Path: " (pr-str path))]
                (if with-req-info?
                  (str (request/info request) "\n\n" message)
                  message))))]
      (with-out-str
        (spec-print
         {:pre with-info
          :problems (:problems (ex-data exception))
          :print-spec? print-spec?})))))

(defmethod ex-format [:exception nil]
  [exception request {:keys [pretty? with-req-info? exceptions?]}]
  (when exceptions?
    (let [req-info   (when with-req-info? (request/info request))
          ex-trace   (ex-trace exception)
          ex-cause   (ex-cause exception)
          ex-message (ex-message exception)]
      (if pretty?
        (let [header "-- Exception Thrown ----------------"
              footer (str "\n" (apply str (repeat (count header) "-")) "\n")
              ifline #(when %1 (str "Exception " (str/upper-case %2) ": " (pr-str %1) "\n"))
              header (str "\n" header "\n\n")]
          (str header
               (when req-info (str req-info "\n"))
               (ifline ex-cause "cause")
               (ifline ex-message "message")
               (ifline ex-trace "trace")
               footer))
        (format "Exception: :uri %s, :method %s, :params %s, :message %s, :trace %s"
                (request :uri)
                (request :request-method)
                (request/params request)
                ex-message ex-trace)))))
