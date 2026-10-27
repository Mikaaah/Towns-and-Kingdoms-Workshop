# Workshop — Development

Requires Java 21.

```sh
./gradlew build
./gradlew runGameTestServer
```

The GameTest suite covers spawner capture, placement and protection checks. Passing automated tests does not complete the full-pack visual and combat checks in [the Goliath checklist](SCULK_GOLIATH_TEST_CHECKLIST.md). Preserve the `tnkcore` registry namespace and packet compatibility when changing features.
