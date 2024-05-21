(ns rss.feeds.rss
  (:use [rss.article])
  (:import (java.time LocalDateTime)
           (java.time.format DateTimeFormatter)))

;; There are various date forms used with RSS; we need to try them all.
(def date-formats [
    (DateTimeFormatter/ofPattern "EEE, dd MMM yyyy HH:mm:ss z")
    (DateTimeFormatter/ofPattern "EEE, dd MMM yyyy HH:mm:ss Z")
  ]
  )

(defn parse-date
  [s]
  "Attempt to parse the date using each of the supplied formats, stopping at
   the first successful parse. If nothing succeeds, return nil."
  (loop [f date-formats]
    (if (empty? f)
      nil
      (or (try (LocalDateTime/parse s (first f))
               (catch Exception e nil))
          (recur (next f))
          )
      )
    )
  )

(defn make-article
  [xml]
  "Returns an Article consisting of selected keys from the RSS XML.
   Dates are formatted according to RFC 822.
   See https://www.rssboard.org/rss-specification"
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
               (parse-date (:pubDate rss-map)))
    )
  )
