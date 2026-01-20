# Meroppfolgin Backend"

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles til team-esyfo

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #esyfo.

## Development

### 🧹 Code style and formatting

We use **Ktlint** (`intellij_idea` style) to ensure consistent Kotlin formatting.

👉 Please install the **Ktlint** plugin in IntelliJ:
- Go to *Preferences → Plugins → Marketplace → search “Ktlint” → Install*
- Then enable **“Format on Save”**

Alternatively, you can always run:
```bash
./gradlew ktlintFormat
```### Installing Rancher desktop

### Run tests with Rancher Desktop

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
