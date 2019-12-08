.PHONY: test deploy-core cli deploy-cli

test:
	clojure "-A:test"

deploy-core: test
	clojure "-Spom"
	mvn deploy

cli:
	clojure "-A:shadow-cljs" release cli

deploy-cli: test cli
	npm publish
