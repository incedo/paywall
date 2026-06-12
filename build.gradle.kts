plugins {
    base
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

// Version policy: latest stable only — no prereleases (see CLAUDE.md).
val verifyStableVersions by tasks.registering {
    group = "verification"
    description = "Fails when the version catalog or Gradle wrapper pins a non-stable version"

    val catalog = layout.projectDirectory.file("gradle/libs.versions.toml")
    val wrapper = layout.projectDirectory.file("gradle/wrapper/gradle-wrapper.properties")
    inputs.files(catalog, wrapper)

    doLast {
        val unstable = Regex("(?i)(alpha|beta|rc|dev|snapshot|eap|milestone|-M\\d)")
        val offenders = mutableListOf<String>()

        var inVersions = false
        catalog.asFile.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("[")) inVersions = trimmed == "[versions]"
            if (inVersions && "=" in trimmed && !trimmed.startsWith("#")) {
                val (key, value) = trimmed.split("=", limit = 2)
                if (unstable.containsMatchIn(value)) offenders.add("libs.versions.toml: ${key.trim()} = ${value.trim()}")
            }
        }
        wrapper.asFile.readLines()
            .firstOrNull { it.startsWith("distributionUrl") }
            ?.let { if (unstable.containsMatchIn(it)) offenders.add("gradle-wrapper.properties: $it") }

        check(offenders.isEmpty()) {
            "Non-stable versions found (policy: stable only, see CLAUDE.md):\n" +
                offenders.joinToString("\n")
        }
    }
}

tasks.named("check") {
    dependsOn(verifyStableVersions)
}
