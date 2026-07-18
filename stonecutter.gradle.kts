plugins {
    id("dev.kikugie.stonecutter")
}

// The version used by default for IDE runs and plain `./gradlew build`/`runClient` invocations.
// 26.1.2 is the version most other mods currently target; 26.2 is ~2 weeks old at time of writing.
// See docs/multi-version.md for how to build/switch versions.
stonecutter active "26.1.2"

// Cross-version string replacements go here (see https://stonecutter.kikugie.dev/wiki/config/params) -
// use this for simple one-token renames. For anything more structural (added/removed code), use
// `//? if <version> { ... //?}` comments directly in the source file instead.
// stonecutter parameters {
//     replacements {
//         string(current.parsed < "26.2") {
//             replace("NewApiName", "OldApiName")
//         }
//     }
// }
