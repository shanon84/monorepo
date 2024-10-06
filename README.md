# Monorepo Readme

## the idea behind

Maven as a build tool is capable of handling bigger setups, but has it limitations.
You can either start using a monorepo or use multiple repositories.
Going the way to introduce multiple repositories
adds many limits in developer experience:

-   no easy overarching refactorings
-   no feature driven development, because to share something, you need to merge to develop

By using a monorepo we circumvent these problems.
With nx we have a well establish monorepo tooling used in javaScript frontends.
Thanks to the jnxplus plugin it is possible to integrate maven/gradle directly now.

Nx has some fascinating features maven is missing:

-   affected builds: only build projects (maven modules) which has code changed
-   caching: a successfully ran target can be cached (if allowed)
-   remote caching: a successfully ran target can be cached remotely (if allowed)

this leads to lesser build/testtime.
Also, it made it more useful to split code into more projects (modules) as before.

## Scripts

in package.json all predefined scripts are listed (except affected,because they do the same as the base target):

### build-BE

compiles all java projects.

### unittest-BE

runs all junit tests.

### integrationtest-BE

runs all integration tests.

### generate-api

generates all openapis.

## How to upgrade

### nx

```
nx migrate latest
```

### other JS dependencies

```
yarn upgrade-interactive
```

### maven

```
.\scripts\update-maven-dependencies.sh
```
