# CeKit Image Validator

This app's purpose is provide schema validation for any file descriptors used by CeKit.

It validate the following file descriptors:

- image
- module
- container
- content_sets
- overrides files, based on image or module.


## Usage

The application requires at least one parameter, which can be a single yaml file or a directory.

Example of usage:

- One single file
    ```bash
    $ ./cekit-image-validator-1.0-SNAPSHOT-runner /data/dev/sources/rhpam-7-openshift-image/kieserver/content_sets.yml 
      __  ____  __  _____   ___  __ ____  ______ 
       --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
       -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
      --\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
      13:25:17 INFO Provided param is a regular file, analyzing it...
      13:25:17 INFO Trying to validate file content_sets.yml
      13:25:17 INFO Content sets file [/data/dev/sources/rhpam-7-openshift-image/kieserver/content_sets.yml] loaded and validated
    ```

- Directory:
    ```bash
    $ ./cekit-image-validator-1.0-SNAPSHOT-runner /data/dev/sources/rhpam-7-openshift-image/kieserver/
    __  ____  __  _____   ___  __ ____  ______ 
     --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
     -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
    --\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
    13:12:48 INFO Provided path is a directory, a recursive search of files will be performed..
    13:12:49 INFO Files collected:
    artifact-overrides.yaml
    content_sets.yml
    my-branch-overrides.yaml
    branch-overrides.yaml
    container.yaml
    image.yaml
    tag-overrides.yaml
    13:12:49 INFO Processing files...
    13:12:49 INFO Trying to validate file artifact-overrides.yaml
    13:12:49 INFO Artifact-overrides file [/data/dev/sources/rhpam-7-openshift-image/kieserver/artifact-overrides.yaml] loaded and validated
    13:12:49 INFO Trying to validate file content_sets.yml
    13:12:49 INFO Content sets file [/data/dev/sources/rhpam-7-openshift-image/kieserver/content_sets.yml] loaded and validated
    13:12:49 INFO Trying to validate file my-branch-overrides.yaml
    13:12:49 INFO Image file [/data/dev/sources/rhpam-7-openshift-image/kieserver/my-branch-overrides.yaml] loaded and validated
    13:12:49 INFO Trying to validate file branch-overrides.yaml
    13:12:49 INFO Image file [/data/dev/sources/rhpam-7-openshift-image/kieserver/branch-overrides.yaml] loaded and validated
    13:12:49 INFO Trying to validate file container.yaml
    13:12:49 INFO Container file [/data/dev/sources/rhpam-7-openshift-image/kieserver/container.yaml] loaded and validated
    13:12:49 INFO Trying to validate file image.yaml
    13:12:49 INFO Image file [/data/dev/sources/rhpam-7-openshift-image/kieserver/image.yaml] loaded and validated: rhpam-7/rhpam-kieserver-rhel8
    13:12:49 INFO Trying to validate file tag-overrides.yaml
    13:12:49 INFO Image file [/data/dev/sources/rhpam-7-openshift-image/kieserver/tag-overrides.yaml] loaded and validated
    ```


In case of any validation fails, the execution will stop right away and a little report printed in the execution's log:

```bash
13:14 $ target/cekit-image-validator-1.0-SNAPSHOT-runner /data/dev/sources/rhpam-7-openshift-image/kieserver/image.yaml 
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
13:14:41 INFO Provided param is a regular file, analyzing it...
13:14:41 INFO Trying to validate file image.yaml
13:14:41 SEVERE Unrecognized field "froms" (class org.kie.cekit.image.descriptors.image.Image), not marked as ignorable (12 known properties: "ports", "osbs", "envs", "modules", "version", "schema_version", "name", "description", "packages", "labels", "from", "run"])
 at [Source: (sun.nio.ch.ChannelInputStream); line: 6, column: 25] (through reference chain: org.kie.cekit.image.descriptors.image.Image["froms"])
```


## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```
$ mvn quarkus:dev
```

## Packaging and running the application

The application can be packaged using `mvn package`.
It produces the `cekit-image-validator-1.0-SNAPSHOT-runner.jar` file in the `/target` directory.
Be aware that it’s not an _uber-jar_ as the dependencies are copied into the `target/lib` directory.

The application is now runnable using `java -jar target/cekit-image-validator-1.0-SNAPSHOT-runner.jar`.

## Creating a native executable

You can create a native executable using: `mvn package -Pnative`.

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: `mvn package -Pnative -Dquarkus.native.container-build=true`.

You can then execute your native executable with: `./target/cekit-image-validator-1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/building-native-image.