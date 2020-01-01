.PHONY: clean test

# Cleans all of the output directories
clean:
	rm -rf .cpcache .css-gardener .shadow-cljs target

# Runs all of the tests for the project
test:
	clojure -A:test -m cognitect.test-runner

# Copies all of the java dependencies into target/java-deps
# This directory is included in the npm package, and is on
# the classpath when the npm binary invokes the Clojure
# code with the java executable.
java-deps:
	clojure -A:pack -m mach.pack.alpha.skinny \
		--lib-dir target/java-deps \
		--lib-type keep \
		--no-project

# Builds the node js binary for npm distribution
npm-binary:
	clojure -A:shadow-cljs -m shadow.cljs.devtools.cli release cli

# Deploys the Clojure code to Clojars
deploy-clojars: test
	clojure "-Spom"
	mvn deploy

# Deploys the npm package
deploy-npm: test java-deps npm-binary
	npm publish
