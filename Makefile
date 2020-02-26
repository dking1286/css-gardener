.PHONY: clean test

# Builds the docker image for running the CI pipeline
ci-runner-docker-image:
	docker build -t dking1286/css-gardener-ci-runner .

# Deploys the docker image for running the CI pipeline
deploy-ci-runner-docker-image:
	docker push dking1286/css-gardener-ci-runner

# Downloads and installs all project dependencies
deps:
	clojure -A:dev:test:shadow-cljs:pack -e 'nil'
	npm install

# Cleans all of the output directories
clean:
	rm -rf .cpcache .css-gardener .shadow-cljs target

# Builds and watches the main cljs bundle for development
cljs-main-watch:
	clojure -A:dev:shadow-cljs -m shadow.cljs.devtools.cli watch main

# Runs the cljs repl for development
# Note: For this to work, you must run `make cljs-main-watch`
# in another terminal
cljs-main-repl:
	node target/main-dev/main.js

# Watches and reruns ClojureScript tests on change
cljs-tests-watch:
	clojure -A:dev:shadow-cljs -m shadow.cljs.devtools.cli watch main-test

# Runs all of the tests for the project
test:
	clojure -A:test -m cognitect.test-runner
	clojure -A:test:shadow-cljs -m shadow.cljs.devtools.cli release main-test
	node target/main-test/main.js

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
	chmod +x target/cli/main.js

# Deploys the Clojure code to Clojars
deploy-clojars: test
	clojure "-Spom"
	mvn deploy

# Deploys the npm package
deploy-npm: test java-deps npm-binary
	npm publish
