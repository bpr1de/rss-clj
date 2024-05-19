;;
;; Support for notifications using the Oracle Notification Service (ONS).
;; This requires an OCI tenancy that has been configured with a topic and
;; subscribers to deliver published messages to.
;; See https://docs.oracle.com/en-us/iaas/pl-sql-sdk/doc/ons-package.html
;;
(ns rss.ons
  (:require [rss.constants])
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
  "Creates an ONS client using resource principal, instance principal, or
  file-based authentication details providers."
  []
  (try
    (cond
      (System/getenv rss.constants/resource-principal-key)
      (.build (NotificationDataPlaneClient/builder)
              (.build (ResourcePrincipalAuthenticationDetailsProvider/builder)))

      (System/getenv rss.constants/instance-principal-key)
      (.build (NotificationDataPlaneClient/builder)
              (.build (InstancePrincipalsAuthenticationDetailsProvider/builder)))

      true
      (.build (NotificationDataPlaneClient/builder)
              (ConfigFileAuthenticationDetailsProvider.
                (ConfigFileReader/parseDefault)))
      )
    (catch IOException e
      (println (str "Unable to set up notification method: "
                    (.getMessage e) "; will print only."))
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
