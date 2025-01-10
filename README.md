<p align="center">
  <a href="https://docs.openrewrite.org">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss-dark.svg">
      <source media="(prefers-color-scheme: light)" srcset="https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss-light.svg">
      <img alt="OpenRewrite Logo" src="https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss-light.svg" width='600px'>
    </picture>
  </a>
</p>

<div align="center">
  <h1>rewrite-gradle-tooling-model</h1>
</div>

<div align="center">

<!-- Keep the gap above this line, otherwise they won't render correctly! -->
[![ci](https://github.com/openrewrite/rewrite-gradle-tooling-model/actions/workflows/ci.yml/badge.svg)](https://github.com/openrewrite/rewrite-gradle-tooling-model/actions/workflows/ci.yml)
[![Apache 2.0](https://img.shields.io/github/license/openrewrite/rewrite-gradle-tooling-model.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.openrewrite.gradle.tooling/plugin.svg)](https://mvnrepository.com/artifact/org.openrewrite.gradle.tooling/plugin)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.openrewrite.org/scans)
[![Contributing Guide](https://img.shields.io/badge/Contributing-Guide-informational)](https://github.com/openrewrite/.github/blob/main/CONTRIBUTING.md)
</div>

### What is this?

This project implements a tooling model for Gradle for OpenRewrite to be able to derive information about build files that help recipes make informed decisions.
