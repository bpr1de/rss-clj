(ns rss.article)

(defrecord Article [type title description link date]
  Object
  (toString [_] (format "%s\n%s\n%s\n%s" title description link date))
  )
