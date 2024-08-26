# Getting Started

### Local development

Integration tests use Testcontainers to start a PostgreSQL container.
To run the tests, you need to have Docker installed on your machine.
Recommended docker application: https://rancherdesktop.io/

### Installing Rancher desktop

Download and install Rancher Desktop from https://rancherdesktop.io/

- For Windows users: You may have to manually add the rancher-desktop kubernetes context to the kubectl configuration.
  You can find the configuration file at `C:\Users\<username>\.kube\config`.
- For Mac users: You may have to manually add the binaries to PATH. To do this, run the following command:
  ```shell
  export PATH=$PATH:/Applications/Rancher\ Desktop.app/Contents/Resources/kubectl
  ```
- For Mac users: If you can't run tests due to "Could not find a valid docker environment" error, you may need to run
  the following command (
  ref https://stackoverflow.com/questions/61108655/test-container-test-cases-are-failing-due-to-could-not-find-a-valid-docker-envi):
  ```shell
  sudo ln -s $HOME/.rd/docker.sock /var/run/docker.sock
  ```

### Reference Documentation

For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.1.5/gradle-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.1.5/gradle-plugin/reference/html/#build-image)
* [Flyway Migration](https://docs.spring.io/spring-boot/docs/3.1.5/reference/htmlsingle/index.html#howto.data-initialization.migration-tool.flyway)
* [Spring for Apache Kafka](https://docs.spring.io/spring-boot/docs/3.1.5/reference/htmlsingle/index.html#messaging.kafka)

### Additional Links

These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

