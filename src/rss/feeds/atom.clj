(ns rss.feeds.atom
  (:use [rss.article])
  (:require [clojure.instant :as instant])
  (:import (java.time ZoneId)))

(defn make-article
  [xml]
  "Returns an Article consisting of selected keys from the Atom XML.
   Dates are formatted according to RFC 3339.
   See https://validator.w3.org/feed/docs/atom.html"
  (let [atom-keys #{:title :summary :updated :link}
        atom-map (into {}
                       (for [m xml :when (atom-keys (:tag m))]
                         (if (= (:tag m) :link)
                           {(:tag m) (:href (:attrs m))}   ;; link is under attrs
                           {(:tag m) (first (:content m))} ;; everything else is under content
                           )
                         )
                       )
        ]
    (->Article :atom
               (:title atom-map)
               (:summary atom-map)
               (:link atom-map)
               (-> (instant/read-instant-date (:updated atom-map))
                   (.toInstant) (.atZone (ZoneId/of "UTC"))
                   (.toLocalDateTime)))
    )
  )
