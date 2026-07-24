---
name: Android Compose review workflow
description: Source-only review conventions for the FairScan Android Compose app.
---

For UI-only Android changes, a source review can validate preserved callbacks, navigation, imports, and diff cleanliness without running Gradle.

**Why:** The workspace may not expose Java through `JAVA_HOME`, and the Gradle wrapper may need to download its distribution, so local builds can be slow or unavailable.

**How to apply:** When the user requests review-only, do not run Gradle; inspect the changed Compose sources and use `git diff --check` instead.