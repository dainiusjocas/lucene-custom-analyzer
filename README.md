[![Clojars Project](https://img.shields.io/clojars/v/lt.jocas/lucene-custom-analyzer.svg)](https://clojars.org/lt.jocas/lucene-custom-analyzer)
[![cljdoc badge](https://cljdoc.org/badge/lt.jocas/lucene-custom-analyzer)](https://cljdoc.org/d/lt.jocas/lucene-custom-analyzer/CURRENT)
[![Tests](https://github.com/dainiusjocas/lucene-custom-analyzer/actions/workflows/test.yml/badge.svg)](https://github.com/dainiusjocas/lucene-custom-analyzer/actions/workflows/test.yml)

# lucene-custom-analyzer

(Micro)Library to build [Lucene](https://lucene.apache.org) analyzers in a data-driven fashion.

## Why Would You Want to Use `lucene-custom-analyzer`?

- Current Clojure Lucene libraries (e.g. [jaju/lucene-clj](https://github.com/jaju/lucene-clj), [federkasten/clucie](https://github.com/federkasten/clucie)) doesn't provide a mechanism to build your custom Lucene Analyzers.
- Data-driven.
- Allows for extensibility using standard [Lucene SPI](https://lucene.apache.org/core/9_1_0/core/org/apache/lucene/analysis/AnalysisSPILoader.html), i.e. just put a JAR in the CLASSPATH.
- Allows to specify a directory from which resources will be loaded, e.g. synonyms dictionaries.
- Lucene 9+ supported.
- Already includes the most commonly used Lucene analysis components.

## Quickstart

Dependencies:

```clojure
lt.jocas/lucene-custom-analyzer {:mvn/version "1.0.28"}
```

Code:

```clojure
(require '[lucene.custom.analyzer :as custom-analyzer])

(custom-analyzer/create
  {:tokenizer              {:standard {:maxTokenLength 4}}
   :char-filters           [{:patternReplace {:pattern     "foo"
                                              :replacement "foo"}}]
   :token-filters          [{:uppercase nil}
                            {:reverseString nil}]
   :offset-gap             2
   :position-increment-gap 3
   :config-dir             "."})
;; =>
;; #object[org.apache.lucene.analysis.custom.CustomAnalyzer
;;         0x4686f87d
;;         "CustomAnalyzer(org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory@2f1300,org.apache.lucene.analysis.standard.StandardTokenizerFactory@7e71a244,org.apache.lucene.analysis.core.UpperCaseFilterFactory@54e9f0d6,org.apache.lucene.analysis.reverse.ReverseStringFilterFactory@3e494ba7)"]
```

Short notation for analysis components:

```clojure
(custom-analyzer/create
  {:tokenizer :standard
   :char-filters [:htmlStrip]
   :token-filters [:uppercase]})
;; =>
;; #object[org.apache.lucene.analysis.custom.CustomAnalyzer
;;        0x16716eb1
;;        "CustomAnalyzer(org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory@4c7f61fa,org.apache.lucene.analysis.standard.StandardTokenizerFactory@6fc69052,org.apache.lucene.analysis.core.UpperCaseFilterFactory@3944ccba)"]
```

If no options are provided then an Analyzer with just the standard tokenizer is created:

```clojure
(custom-analyzer/create {})
;; =>
;; #object[org.apache.lucene.analysis.custom.CustomAnalyzer
;;         0x456fe86
;;         "CustomAnalyzer(org.apache.lucene.analysis.standard.StandardTokenizerFactory@5703f5b3)"]
```

If you want to check which analysis components are available run:

```clojure
(lucene.custom.analyzer/char-filter-factories)
(lucene.custom.analyzer/tokenizer-factories)
(lucene.custom.analyzer/token-filter-factories)
```

## Design

Under the hood this library uses the factory classes `TokenizerFactory`, `TokenFilterFactory`, and `CharFilterFactory`.
The actual factories are loaded with `java.util.ServiceLoader`.
All the available classes are automatically discovered.

If you want to include additional factory classes, e.g. your implementation of the `TokenFilterFactory,` you need to add it to the classpath 2 things:
 1. The implementation class of one of the Factory classes
 2. Under the `META-INF/services` add/change a file named `org.apache.lucene.analysis.TokenFilterFactory` that lists the classes from the step 1.

An example can be found [here](https://github.com/dainiusjocas/lucene-grep/tree/main/modules/raudikko).

## Future work

- [ ] Conditional token filters

## License

Copyright &copy; 2022 [Dainius Jocas](https://www.jocas.lt).

Distributed under The Apache License, Version 2.0.
