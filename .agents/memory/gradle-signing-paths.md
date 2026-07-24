---
name: Gradle signing paths
description: Android module signing files are resolved relative to the module directory.
---

For Android module signing, a `storeFile` value passed through Gradle project properties is resolved relative to the module's project directory, not the repository root.

**Why:** Passing a repository-root-style path from a workflow can duplicate the module directory and make `validateSigningRelease` fail with a missing keystore.

**How to apply:** When the workflow creates `app/keystore/<file>`, pass `keystore/<file>` to the app module's `RELEASE_STORE_FILE` property.