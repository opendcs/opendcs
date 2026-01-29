# OpenDCS WebUI (React-Bootstrap)

If doing things in this directory you will still need a 21 JDK or higher to
process and generate the typescript api.

## To have access to an api instance there are currently three choices:

1. NPM task

`npm run api:start` will start an API instance locally, in the foreground, either use `&` or run in
another shell

WARNING: with this method data is transient, once the api exists all data is lost.

2. docker compose

`docker compose up -d --build`

This will start an instance with "permanent" data. `--build` will be required if any api changes are made.

3. manually

the vite.config.js current assumes localhost:7000, setup accordingly or improve the environment usage of the config.

## Formatting

`npm install` will setup a huksy pre-commit hook to run `lint-staged` on commit to ensure formatting remains consistent.
Since that can be disabled by you, a github action step will run the `npm run format:check` command to prevent merges
of incorrectly formatted code.

If you disable the huksy pre-comment hook please run

`npm run precommit` manually to adjust the formatting.
