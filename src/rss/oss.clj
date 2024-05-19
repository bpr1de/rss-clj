;;
;; Support configuration retrieval from Oracle Object Storage Service (OSS).
;; This requires an OCI tenancy that has been configured with a bucket,
;; object, and policy to support configuration pulls from it.
;;
(ns rss.oss
  (:import (java.io IOException)
           (com.oracle.bmc ConfigFileReader)
           (com.oracle.bmc.auth ConfigFileAuthenticationDetailsProvider
                                InstancePrincipalsAuthenticationDetailsProvider
                                ResourcePrincipalAuthenticationDetailsProvider
                                )
           (com.oracle.bmc.objectstorage ObjectStorageClient)
           (com.oracle.bmc.objectstorage.requests GetObjectRequest)
           )
 )

(defn make-client
  "Creates an OSS client using resource principal, instance principal, or
  file-based authentication details providers."
  []
  (try
    (cond
      (System/getenv rss.constants/resource-principal-key)
      (.build (ObjectStorageClient/builder)
              (.build (ResourcePrincipalAuthenticationDetailsProvider/builder)))

      (System/getenv rss.constants/instance-principal-key)
      (.build (ObjectStorageClient/builder)
              (.build (InstancePrincipalsAuthenticationDetailsProvider/builder)))

      true
      (.build (ObjectStorageClient/builder)
              (ConfigFileAuthenticationDetailsProvider.
                (ConfigFileReader/parseDefault)))
      )
    (catch IOException e
      (println (str "Unable to create client for Object Storage Service: "
                    (.getMessage e)))
      )
    )
  )

(defn get-handle-for
  "Creates an OSS client using resource principal, instance principal, or
  file-based authentication details providers."
  [oss-location]
  (let [client (make-client)
        request (.. GetObjectRequest (builder)
                    (namespaceName (nth oss-location 0)) ; make record
                    (bucketName (nth oss-location 1))
                    (objectName (nth oss-location 2))
                    (build))
        response (.getObject client request)
        stream (.getInputStream response)]
    stream
    )
  )
