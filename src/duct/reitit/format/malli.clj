(ns duct.reitit.format.malli
  (:require [clojure.string :as str]
            [malli.dev.pretty :as pretty]
            [malli.dev.virhe :as v]
            [malli.error :as me]))

(def ^:private reporter
  (pretty/reporter (pretty/-printer {:title "Malli Coercion Error" :width 80})))

(defn format-errors [data printer]
  (->> (for [e (-> data me/with-error-messages :errors)]
         [:align 2
          (v/-text "Message: " printer) (v/-color :string (str/capitalize (:message e)) printer) :break
          (v/-text "Path: " printer) (v/-visit (:path e) printer) :break
          (v/-text "Should satisfy: " printer)
          (v/-visit (:schema e) printer)])
       (interpose :break)
       (interpose :break)))

(defmethod v/-format ::default [_ _ {:keys [errors value extra] :as data} printer]
  {:body [:group
          (when extra [:group extra :break :break])
          (v/-text "Value: " printer) (v/-visit value printer) :break :break
          (pretty/-block (str (count errors) " Errors detected:") (format-errors data printer) printer)]})

(defn pretty [data request-info]
  (with-out-str (reporter ::default (assoc data :extra request-info))))

(defn compact [data request-info]
  (let [errors (->> (:errors (me/with-error-messages data)) (mapv #(select-keys % [:path :message])))
        message (format "Malli Coercion Error(%s): %s" (pr-str (:value data)) (pr-str errors))]
    (if request-info
      (str message ", Request: " request-info) message)))
