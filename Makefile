.PHONY: test deploy

test:
	clj -A:test

deploy: test
	clj -Spom
	mvn deploy
