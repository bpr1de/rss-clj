;;
;; Support for notifications using the Oracle Notification Service (ONS).
;; This requires an OCI tenancy that has been configured with a topic and
;; subscribers to deliver published messages to.
;; See https://docs.oracle.com/en-us/iaas/pl-sql-sdk/doc/ons-package.html
;;
(ns rss.ons
  (:import (java.io IOException)
           (com.oracle.bmc ConfigFileReader)
           (com.oracle.bmc.auth ConfigFileAuthenticationDetailsProvider
                                InstancePrincipalsAuthenticationDetailsProvider
                                ResourcePrincipalAuthenticationDetailsProvider
                                )
           (com.oracle.bmc.ons NotificationDataPlaneClient)
           (com.oracle.bmc.ons.model MessageDetails)
           (com.oracle.bmc.ons.requests PublishMessageRequest)
    )
 )

(defn make-client
  "Creates an ONS client first trying local configuration, and then falling
   back to using an instance principal."
  [type]
  (try
    (cond
      (= type "file")
      (.build (NotificationDataPlaneClient/builder)
              (ConfigFileAuthenticationDetailsProvider.
                (ConfigFileReader/parseDefault)))

      (= type "instance")
      (.build (NotificationDataPlaneClient/builder)
              (.build (InstancePrincipalsAuthenticationDetailsProvider/builder)))

      (or (= type "resource") (System/getenv "OCI_RESOURCE_PRINCIPAL_RPST"))
      (.build (NotificationDataPlaneClient/builder)
              (.build (ResourcePrincipalAuthenticationDetailsProvider/builder)))

      true
      (println "Unrecognized notification method; will print only")
      )
    (catch IOException e
      (println (str "Unable to set up notification method: " (.getMessage e)))
      (System/exit -1)
      )
    )
  )

(defn notify
  "Send out notifications for the article, using ONS if a topic is defined."
  [client topic article]
  (println (str article))
  (when (and client topic)
    (let [message (.. MessageDetails (builder)
                      (title (format "RSS: %s" (:title article)))
                      (body (str article))
                      (build))
          request (.. PublishMessageRequest (builder)
                      (topicId topic)
                      (messageDetails message)
                      (build))]
      (.publishMessage client request)
      )
    )
  )
