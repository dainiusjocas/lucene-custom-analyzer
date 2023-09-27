(ns lucene.custom.analyzer-wrappers-test
  (:require [clojure.test :refer [deftest is testing]]
            [lucene.custom.analyzer-wrappers :as wrappers]
            [lucene.custom.text-analysis :as analysis])
  (:import (java.util HashMap)))

(deftest per-field-analyzer-wrappers
  (testing "per field wrapped analyzer"
    (let [text "foo bar baz"
          field (name :foo-field)
          analyzer (wrappers/->per-field-analyzer-wrapper
                     {:token-filters [:uppercase]}
                     {field {:token-filters [:reverseString]}})]
      (is (= ["FOO" "BAR" "BAZ"] (analysis/text->token-strings text analyzer)))
      (is (= ["oof" "rab" "zab"] (analysis/text->token-strings text analyzer field)))))

  (testing "mutable hashmap"
    (let [text "foo bar baz"
          field (name :foo-field)
          analyzer (wrappers/->per-field-analyzer-wrapper
                     {:token-filters [:uppercase]}
                     (doto (HashMap.)
                       (.put field {:token-filters [:reverseString]})))]
      (is (= ["FOO" "BAR" "BAZ"] (analysis/text->token-strings text analyzer)))
      (is (= ["oof" "rab" "zab"] (analysis/text->token-strings text analyzer field))))))
