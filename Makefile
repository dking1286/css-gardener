.PHONY: clean test

# Downloads and installs all project dependencies
deps:
	npm install
	# Force shadow-cljs to run so that it downloads mvn dependencies
	npx shadow-cljs clj-eval "nil"

# Cleans all of the output directories
clean:
	rm -rf .cpcache .css-gardener .shadow-cljs target

# Runs all of the tests for the project
test:
	npx shadow-cljs release main-test
	node target/main-test/main.js

# Builds and watches the main cljs bundle for development
cljs-main-watch:
	npx shadow-cljs watch main

# Runs the cljs repl for development
# Note: For this to work, you must run `make cljs-main-watch`
# in another terminal
cljs-main-repl:
	node target/main-dev/main.js

# Watches and reruns ClojureScript tests on change
cljs-tests-watch:
	npx shadow-cljs watch main-test
