#!/usr/bin/env bash
set -euo pipefail

repo_root="$(pwd -P)"
web_ui_dir="${repo_root}/javascript/opendcs-web-ui-react"

git config --global --add safe.directory "${repo_root}"

npm --prefix "${web_ui_dir}" ci --ignore-scripts
(
	cd "${web_ui_dir}"
	npx --no-install playwright install --with-deps chromium firefox
)

"${repo_root}/gradlew" --version
