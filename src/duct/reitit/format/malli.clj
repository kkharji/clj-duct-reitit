(ns duct.reitit.format.malli
  (:require [clojure.string :as str]
            [malli.dev.pretty :as pretty :refer [-block -printer]]
            [malli.dev.virhe :as v]
            [malli.error :as me]))

(def printer
  (-printer {:title "Malli Coercion Error" :width 80}))

(defn pretty-errors [errors printer]
  (->> (for [e errors]
         [:align 2
          (v/-text "Message: " printer) (v/-color :string (str/capitalize (:message e)) printer) :break
          (v/-text "Path: " printer) (v/-visit (:path e) printer) :break
          (v/-text "Should satisfy: " printer)
          (v/-visit (:schema e) printer)])
       (interpose :break)
       (interpose :break)))

(defn pretty [{:keys [value] :as data}
              {:keys [without-trace? request-info]}]
  (let [m-errors  (-> data me/with-error-messages :errors)
        err-info  (ex-info "Malli Error" {:type type :data data}) ;; not sure why this makes sense?
        err-count (count m-errors)
        err-title (str err-count " " (if (= 1 err-count) "Error" "Errors")  " detected:")
        sec-trace (when-not without-trace? (v/-location err-info (:throwing-fn-top-level-ns-names printer)))
        sec-body  [:group
                   (when-let [r request-info] [:group r :break :break])
                   (v/-text "Value: " printer) (v/-visit value printer) :break :break
                   (-block err-title (pretty-errors m-errors printer) printer)]]

    (-> (if without-trace? "Validation Error" (or (:title data) (:title printer)))
        (v/-section sec-trace sec-body printer)
        (v/-print-doc printer)
        (with-out-str))))

(defn compact [data {:keys [request-info]}]
  (let [errors (->> (:errors (me/with-error-messages data)) (mapv #(select-keys % [:path :message])))
        message (format "Malli Coercion Error(%s): %s" (pr-str (:value data)) (pr-str errors))]
    (if request-info
      (str message ", Request: " request-info) message)))
