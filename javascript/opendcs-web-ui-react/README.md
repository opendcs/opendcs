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

## UI Tests and Development

### Development

We use https://storybook.js to handle UI testing and development.
If you are doing UI work run `npm run storybook`

Within the environment there will be, or you will need to create, Mocks of the API. In general UI tests should not assume the presence of an active API.
Follow the Storybook practice of component isolation. For example, look at the current sites test. The SiteTable component is what will be rendered and would be
provided with data from the API; however, we "mimic" that API with local source of data and provide appropriate callback functions to either retrieve or save data.

To confirm the behavior of a component rendering from an API we will use appropriate Mocks. As of this writing the exact method has not been determined.

When creating a component please create a Story that includes a play method that performs no actions. This is beneficial for exploratory use as Storybook will
automatically run the play functions, and depending on what the developer does, remove the user actions.

### Tests

We have found some quirks in how tests run. While we're sure some of this is how we are using Storybook we have yet to "fix" the situtation. That said, the quirks
are managable.

1. The internationalization (i18n) Decorator doesn't seem to finish before the Story is fully rendered and the play method starts running when storybook first loads.
   Usually clicking the reload test fixes this and tests run fine. You'll see errors with i18n keys instead of expanded strings when this is happening.
2. For some reason we have to keep call `userEvent.<anything>` within an `await act(async () => userEvent.<anything>)`. Otherwise tests fail when run in vitest, but will pass in the storybook UI.
   The Storybook UI will log a bunch of "act not supported" or "you've called act within act" but so far they run.
3. Especially for anything using DataTables, make sure to `const canvas = await mount()` or just `await mount()` at once at the top of the play method.

### Basic standards

While more rigorous standards are currently being developed the following are some basic guidelines to get everyone started:

1. Components should handle their local state internally. Look at PropertiesTable, don't do what it's doing as-of 2026-02-04.
2. Use Bootstraps (and React Bootstrap) components and guidelines, deviate if it required, but don't "work around it"
3. Avoid modals unless it's something where keeping the user from moving on is absolutely required.
4. Use Bootstrap Icons for iconography.
5. While ordering action buttons go "least destructive to most destructive". Examples (Cancel, Save), (Edit, Delete), (Save, Delete) and so on.
6. All UI text should _MUST_ be in i18n translation strings. We will not require translations to be provided, though they are welcome, but the en-US keys _MUST_ exist and be used.
7. Apply appropriate `aria-labels` to action components -- `aria-labels` _MUST_ use the i18n translation. Fortunately you will find not doing this to make testing rather difficult as your
   tests won't be able to find the control.
8. Use the i18n translation interface in the tests.
