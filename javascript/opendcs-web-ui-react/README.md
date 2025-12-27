# OpenDCS WebUI (React-Bootstrap)

If doing thins in this directory directly you will still need a 21 JDK or higher to
process and generate the typescript api.

To have access to an api instance there are currently three choices

## NPM task

`npm run api:start` will start an API instance locally, in the foreground, either use `&` or run in
another shell

WARNING: with this method data is transient, once the api exists all data is lost.

## docker compose

`docker compose up -d --build`

This will start an instance with "permanent" data. `--build` will be required if any api changes are made.

## manually

the vite.config.js current assumes localhost:7000, setup  accordingly or improve the environment usage of the config.
