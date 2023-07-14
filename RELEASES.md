# OpenDCS Release Process

This will document the release process. The initial sections will document user expectations
for when and how releases are made. The remaining sections will detail how a release is generated
so that the process is repeatable by multiple developers.


## Releases

OpenDCS will generate release no more than monthly, except in cases of serious security concerns.

The choice to generate a release is generally made by a combination of, number of changes since the last release (bugfixes or features),
a critical need in a component used by any of our users (for example CWMS, HDB, and some NOAA systems are still embedded within
the project), and security concerns that have been correct.

### Supported Versions

OpenDCS will be using sematic versioning (https://semver.org/) to label releases; in short this is "major.minor.patch". We will attempt to support on an N-1 basis
for major and minor version. We (the OpenDCS project contributors) will support previous major versions for a year after release
of the next major version.

Current supported versions are:

|version|level of support|
|-------|----------------|
|6.8.14 |minimal, egregious bugs. Not feature back porting|
|7.0.8  |full, report bugs and request features|


## Creating a release


### Requirements

If you are creating an official release you will need python3 so the documentation can be built and a system 
with docker so the integration tests can be run.

You will need

- An account at Sonatype to push the opendcs jar to maven central. See https://central.sonatype.org/publish/publish-maven/.
This will be limited to project committers and users verified by other project commiters.
- A GPG key in a public keyring (see previous link) and added to the KEYS file in the opendcs github project
- the gpg client software to use that key for signing.
- the ability to create release on the github project.


### Procedure

1. Determine it is time for a release
2. Install the new version somewhere and go through as many non automated test items as practical in your environment and look for really bad regressions.
3. run `ant release`
4. On GitHub create a release and create the version tag from current commit
   a. Label the release on GitHub with the `version-CANDIDATE`
   b. Starting with the automatically generated release notes add additional information as required/appropriate.
   c. Upload the build/lib/opendcs.jar, `stage/opendcs-ot-<version>.jar`, and the source and javadoc jars from the release directory to the release
5. Login to sonatype with your account and upload the `release/bundle.jar` file.


Send an announcement that we have a release candidate and need feedback and votes from the existing committers. If there are 
no user reports of regressions within the two weeks and the vote passes, remove `-CANDIDATE` from the github release page
and formally announce that this is the new latest release.
