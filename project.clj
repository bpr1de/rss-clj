(defproject rss "0.1.0-SNAPSHOT"
  :description "RSS article reader with Oracle Notification Service support."
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.slf4j/slf4j-nop "2.0.5"]
                 [com.oracle.oci.sdk/oci-java-sdk-common-httpclient-jersey3 "3.2.0"]
                 [com.oracle.oci.sdk/oci-java-sdk-ons "3.2.0"]
                 [com.oracle.oci.sdk/oci-java-sdk-core "3.2.0"]
  ]
  :main rss.core
  :aot rss.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
