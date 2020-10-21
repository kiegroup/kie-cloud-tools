IMAGE_VERSION := $(shell cat image.yaml | egrep ^version  | cut -d"\"" -f2)
BUILD_ENGINE := podman
MD5SUM := $(shell ls target/*-runner.jar | xargs md5sum)
.DEFAULT_GOAL := build

.PHONY: build
build: test
	sh ./prepare.sh
	BUILDAH_LAYERS=false cekit -v --target=./target-cekit build ${BUILD_ENGINE}
	cekit -v --target=./target-cekit test behave
	# rm -rfv ./cekit-modules/configure-cekit-cacher/module.yaml

test:
	sudo rm -rf /tmp/cacher
	export CACHER_PRODUCT_VERSION=7.10.0; \
	export CACHER_BASE_DIR=/tmp/cacher/data; \
	export CACHER_ENABLE_GITHUB_BOT=true; \
	export CACHER_GITHUB_USERNAME=bsig-gh-bot; \
	export CACHER_GITHUB_PASSWORD=password; \
	export CACHER_GITHUB_EMAIL=emailg@gmail.com; \
	export CACHER_RHDM_URL=https://url; \
	export CACHER_RHPAM_URL=https://url; \
	export CACHER_RHDM_UPSTREAM=https://github.com/jboss-container-images/rhdm-7-image.git; \
	export CACHER_RHPAM_UPSTREAM=https://github.com/jboss-container-images/rhpam-7-image.git; \
	export CACHER_DEFAULT_BRANCH=master; \
	export CACHER_GITHUB_REVIEWERS="user/ignore"; \
	export CACHER_PRELOAD_FILE=/opt/cacher/load-from-file.txt && mvn clean package

.PHONY: push
push: build _push
_push:
	docker push quay.io/kiegroup/kogito-quarkus-centos:${IMAGE_VERSION}
