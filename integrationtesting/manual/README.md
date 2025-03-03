# Manual Testing

The files in this directory are for defining manual tests that may be required to verify OpenDCS operations.

The target environment for these tests are the KiwiTCMS (https://kiwitcms.org) and are organized by Kiwi "Component"

## The soft bits

Test should be written such that given a source database (to be stored "TDB") anyone can run them.

CAVEAT: Given that the implementation under test supports those operations.


## The hard bits


If anyone wants to write in the formal Gherkin Syntax (https://cucumber.io/docs/gherkin/reference/) we can
convert on loading into a test system so that's fine, just use the proper extension.
