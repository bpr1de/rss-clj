(ns rss.feeds.rss
  (:use [rss.article])
  (:import (java.text SimpleDateFormat)))

(defn make-article
  "Returns an Article consisting of selected keys from the RSS XML.
   Dates are formatted according to RFC 822.
   See https://validator.w3.org/feed/docs/rss2.html"
  [xml]
  (let [rss-keys #{:title :description :pubDate :link}
        rss-map (into {}
                      (for [m xml :when (rss-keys (:tag m))]
                        {(:tag m) (first (:content m))})
                      )
        ]
    (->Article :rss
               (:title rss-map)
               (:description rss-map)
               (:link rss-map)
               (.parse
                 (SimpleDateFormat. "EEE, d MMM yyyy HH:mm:ss Z")
                 (:pubDate rss-map)))
    )
  )
