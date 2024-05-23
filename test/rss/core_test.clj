(ns rss.core-test
  (:require [clojure.test :refer :all]
            [rss.core :refer :all])
  )

(deftest parse-rss-date
  (testing "Parsing RSS dates"
    (is (= (str (rss.feeds.rss/parse-date
           "Wed, 22 May 2024 19:52:30 -0800"))
           "2024-05-23T03:52:30Z"))
    (is (= (str (rss.feeds.rss/parse-date
           "Wed, 22 May 2024 06:13:59 EST"))
           "2024-05-22T10:13:59Z"))
    )
  )

(deftest parse-atom-date
  (testing "Parsing ATOM dates"
    (is (= (str (rss.feeds.atom/parse-date
           "2024-05-23T00:53:14+00:00"))
           "2024-05-23T00:53:14"))
    (is (= (str (rss.feeds.atom/parse-date
           "2024-05-22T20:52:01-04:00"))
           "2024-05-23T00:52:01"))
    (is (= (str (rss.feeds.atom/parse-date
           "2024-05-22T20:52:01+04:30"))
           "2024-05-22T16:22:01"))
    )
  )