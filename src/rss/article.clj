(ns rss.article)

;; Definition of the record which represents an RSS article.
;; All articles will have a type (e.g. Atom or RSS), a title, a description,
;; a hyperlink, and a publication date.
(defrecord Article [type title description link date]
  Object
  (toString [_] (format "%s\n%s\n%s\n%s" title description link date)))
