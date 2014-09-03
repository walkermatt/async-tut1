(ns async-tut1.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.string :as string]
            [goog.dom :as dom]
            [goog.events :as events]
            [cljs.core.async :refer [put! chan <!]])
  (:import [goog.net Jsonp]
           [goog Uri]))

(def wiki-search-url
  "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=")

(enable-console-print!)

(defn listen [ch el type]
  (events/listen
    el type (fn [e] (put! ch e)))
  ch)

(defn listen-enter [ch el]
  (events/listen
    el "keypress" (fn [e] (if (= (.-keyCode e) 13) (put! ch e))))
  ch)

(defn jsonp [uri]
  (let [out (chan)
        req (Jsonp. (Uri. uri))]
    (.send req nil (fn [res] (put! out res)))
    out))

(defn query-url [q]
  (str wiki-search-url q))

(defn user-query []
  (.-value (dom/getElement "query")))

(defn render-query [results]
  (str
    "<ul>"
    (apply str
           (for [result results]
             (str "<li><a href='http://en.wikipedia.org/wiki/" (string/replace result " " "_") "'>" result "</a></li>")))
    "</ul>"))

(defn init []
  (let [ch (listen-enter (listen (chan) (dom/getElement "search") "click") (dom/getElement "query"))
        results-view (dom/getElement "results")]
    (go (while true
          (<! ch)
          (let [[_ results] (<! (jsonp (query-url (user-query))))]
            (do
              ; (.log js/console results)
              (set! (.-innerHTML results-view) (render-query results))))))))

(init)
