# CEKit Cache Server

A (native compatible) artifact cacher for CEKit

### Configuring CEKit Cache Server

To configure the cacher the *cekit-cacher.properties* needs to be updated, valid configurations

TODO - add the environment variables name and update the properties.
```properties
org.kie.cekit.cacher.base.dir (required)- location for the base cacher directory.
org.kie.cekit.cacher.preload.file - configure it with a txt file containing the files that you wants the cacher to preload.

# Product Nightly Builds properties
org.kie.cekit.cacher.enable.nightly.watcher - enables the nightly builds watcher
org.kie.cekit.cacher.product.version (requiresd if watcher is enabled) - rhpam/rhdm product version
org.kie.cekit.cacher.rhdm.url (requiresd if watcher is enabled) - RHDM nightly build properties url
org.kie.cekit.cacher.rhpam.url (requiresd if watcher is enabled) - RHPAM nightly build properties url

## Github integration info
org.kie.cekit.cacher.enable.github.bot - enables github integration
org.kie.cekit.cacher.github.username (required if github bot is enabled) - github account username
org.kie.cekit.cacher.github.password (required if github bot is enabled) - github user password
org.kie.cekit.cacher.github.email (required if github bot is enabled) - github user email
org.kie.cekit.cacher.github.oauth-token (required if github bot is enabled) - github personal user token
org.kie.cekit.cacher.github.reviewers (required if github bot is enabled) - The Google Chats user id, just use the ID in this field, ignore the "user/" prefix.

# Forked repository will be based on the provided username.
org.kie.cekit.cacher.github.rhdm.upstream.project - rhdm upstreagm
org.kie.cekit.cacher.github.rhpam.upstream.project - rhpam upstream
org.kie.cekit.cacher.github.default.branch - rhpam and rhdm upstream default branch

# google chat room webhook conf
# be sure to scape special characters
org.kie.cekit.cacher.gchat.webhook - The Google Chat webhook for a target room or chat, can be obtained on the room.
that you want to send notification.

# Product CR builds
org.kie.cekit.cacher.rhpam.cr.url -  RHPAM CR build properties url
org.kie.cekit.cacher.rhdm.cr.url -  RHDM CR build properties url
```

Note that, the cekit-cacher.properties also supports configuration using environment variables with the `${ENV_NAME}` pattern, example:

```bash
org.kie.cekit.cacher.base.dir=${CACHER_BASE_DIR}
```

This functionality allow us to hide information from the properties file and also allows a easy configuration when running the cacher
on OpenShift.


### Build && starting CeKit cacher

Note that, to run the tests, a few parameters will be needed, the tests will run fine with the following configuration:

```bash
export CACHER_BASE_DIR=/tmp/cacher/data; \
export CACHER_PRODUCT_VERSION=7.10.0; \
export CACHER_ENABLE_GITHUB_BOT=true; \
export CACHER_GITHUB_USERNAME=bsig-gh-bot; \
export CACHER_GITHUB_PASSWORD=password; \
export CACHER_GITHUB_OAUTH_TOKEN=token; \
export CACHER_GITHUB_EMAIL=emailg@emailh.com; \
export CACHER_RHDM_URL=https://url; \
export CACHER_RHPAM_URL=https://url; \
export CACHER_RHDM_UPSTREAM=https://github.com/jboss-container-images/rhdm-7-image.git; \
export CACHER_RHPAM_UPSTREAM=https://github.com/jboss-container-images/rhpam-7-image.git; \
export CACHER_DEFAULT_BRANCH=master; \
export CACHER_GITHUB_REVIEWERS="user/ignore"; \
export CACHER_PRELOAD_FILE=/opt/cacher/load-from-file.txt
```

### Configuring the CeKit to fetch files from Cacher

The endpoint to fetch artifacts are available under the `/resource/{checksum}` path.
In order to configure the CeKit to pull artifacts from cacher, add the following entry under the `~/.cekit/config` 
configuration file:


```bash
[common]
cache_url = http://localhost:8080/resource/#hash#
```
Remember to update the url by changing localhost with the cacher's valid url.


### Make CEKit Cache Server serves only as a artifact cacher

This app also includes a few more features which are:

    - Nighlty builds watcher.
    - Git Hub bot to create new Pull Requests when a nighlty build is found with new artifacts.

With that said, we can disable those functionality telling this app to serve only as a cacher, it can be done by setting the 
following properties:

    - org.kie.cekit.cacher.enable.nightly.watcher=false
    - org.kie.cekit.cacher.enable.github.bot=false

When the cacher is started, a log message will tell you that the github bot is disabled:
```
INFO  [org.kie.cekit.cacher.builds.github.GitRepository] Github integration bot is disabled.
```

The watcher, when requested will only return a text message saying that it is disabled.

Or just ignore all other confs and just set the `org.kie.cekit.cacher.base.dir` property.


#### Building
```bash
mvn clean package
```

#### Build native image
```bash
mvn clean package -Pnative
```

#### Running Locally
```bash
java -jar target/cekit-cacher-1.0-SNAPSHOT-runner.jar
```

#### Running native-image locally
```bash
$ ./target/cekit-cacher-1.0-SNAPSHOT-runner
```

#### Generating container image

A Makefile is provided, to build the image you can just do:

```bash
$ make build
```

It will build the Java application a run the tests as well. The default builder engine used is *podman*, to change it just
update the BUILD_ENGINE parameter in the Makefile.

This build will produce the image ready to use, however the properties needs to be set using environment variables.
Otherwise the bootstrap will fail.

To run the image:

```bash
$ podman run -it -rm -p 8080:8080 -v /path/on/your/machine:/opt/cacher/data --env .... bsig-cekit-cacher:latest
```

Remember to correctly set the envs, otherwise the cache can not work properly and to set the paths and the
*CACHER_BASE_DIR* property accordingly.



##### Running on openShift

After to build the image, tag it properly and push to the OpenShift registry or another registry which is reachable by 
your OCP instance, i.e.:

```bash
$ podman tag localhost/bsig-cekit-cacher:1.0 docker-registry-default.apps.internal.cloud/openshift/bsig-cekit-cacher:1.0
podman login -p $(oc whoami -t)  -u unused docker-registry-default.apps.internal.cloud
podman push docker-registry-default.apps.spolti.cloud/openshift/bsig-cekit-cacher:1.0 Getting image source signatures
Copying blob 73b5b703c4fa done
...
Writing manifest to image destination
Copying config 25710a6fad done
Writing manifest to image destination
Storing signatures
```



### Accessing the CEKit Cache Server

Once started the cacher will be available under the *root* path.
I.e. if running locally will be available at http://localhost:8080

From the web UI you can:
- add new artifacts
- see the list of artifacts persisted or in the *Downloading* status
- Download a single artifact
- Delete single artifact
- trigger new nightly build.


### Interacting with CEKit Cache Server through rest API

The Cacher exposes a few actions through rest API, you see all of them with the swagger ui available under the
**/swagger-ui** path.


### Useful information.

**Temp files** - there is a timer that runs once a day which will remove all files older than 1 day.

**Retry specific build date** - there is a endpoint for this purpose:  `GET /watcher/{buildDate}`

**Retry specific build date with custom branch and version** - `GET /watcher/try/{version}/{branch}/{buildDate}`

**Update CR builds** - `GET /crbuild/use/{version}/{releaseBranch}/{crBuild}`
 - Example: `http://localhost:8080/crbuild/use/7.9.1/7.9.x/2`
 - Requires the target repositories to be already updated for release.

**Upload artifacts** - It can be done through the UI or using command line, e.g.:
 - ```bash
   $ curl -i -X POST -H "Content-Type: multipart/form-data" -F "file=@/tmp/test.log" -F "fileName=test.log" http://localhost:8080/resource/file/upload
   ```
