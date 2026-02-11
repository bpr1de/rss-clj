;;
;; Support configuration retrieval from Oracle Object Storage Service (OSS).
;; This requires an OCI tenancy that has been configured with a bucket,
;; object, and policy to support configuration pulls from it.
;;
(ns rss.oss
  (:import (com.oracle.bmc.auth SessionTokenAuthenticationDetailsProvider
                                InstancePrincipalsAuthenticationDetailsProvider
                                ResourcePrincipalAuthenticationDetailsProvider)
           (com.oracle.bmc.objectstorage ObjectStorageClient)
           (com.oracle.bmc.objectstorage.requests GetObjectRequest)
           (java.io ByteArrayInputStream)))

(defn make-client
  "Creates an OSS client using resource principal, instance principal, or
  file-based authentication details providers."
  []
  (try
    (cond
      ;; Resource Principals
      (System/getenv rss.constants/resource-principal-key)
      (.build (ObjectStorageClient/builder)
              (.build (ResourcePrincipalAuthenticationDetailsProvider/builder)))
      ;; Instance Principals
      (System/getenv rss.constants/instance-principal-key)
      (.build (ObjectStorageClient/builder)
              (.build (InstancePrincipalsAuthenticationDetailsProvider/builder)))
      ;; Local config file
      true
      (.build (ObjectStorageClient/builder)
              (SessionTokenAuthenticationDetailsProvider.)))
    (catch Exception e
      (println (str "Unable to create client for Object Storage Service: "
                    (.getMessage e)))
      (System/exit 1))))

(defn get-stream-for
  "Returns an InputStream for a String read from an object in Object Storage
  Service located in the given namespace, bucket, and object tuple from the
  supplied collection."
  [oss-location]
  (let [client (make-client)
        request (-> (GetObjectRequest/builder)
                    (.namespaceName (nth oss-location 0))
                    (.bucketName (nth oss-location 1))
                    (.objectName (nth oss-location 2))
                    (.build))
        response (.getObject client request)
        stream (.getInputStream response)
        config (slurp stream :encoding "UTF-8")]
    (.close client)
    (ByteArrayInputStream. (.getBytes config "UTF-8"))))
