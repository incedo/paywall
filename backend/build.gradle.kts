plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
    jacoco
}

application {
    mainClass.set("nl.incedo.paywall.backend.ApplicationKt")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    // Include shared jvmTest execution so shared-only tests (e.g. serialization
    // roundtrip, decision model edge cases) also contribute to BC coverage.
    dependsOn(tasks.test, ":shared:jvmTest")
    executionData.setFrom(
        tasks.test.get().extensions.getByType<JacocoTaskExtension>().destinationFile,
        project(":shared").layout.buildDirectory.file("jacoco/jvmTest.exec"),
    )

    // Include both backend and shared JVM classes in the combined report so all
    // 16 BCs are visible. Exclude classes that cannot be covered in unit/integration
    // tests without external infrastructure (Postgres, Ory Kratos, real JWT keys)
    // and Kotlin compiler artefacts that JaCoCo can never reach.
    classDirectories.setFrom(
        (sourceSets.main.get().output.classesDirs +
         files(project(":shared").layout.buildDirectory.dir("classes/kotlin/jvm/main")))
        .asFileTree.matching {
            // Q-3: PostgresEventStore (persistence/**) is EXCLUDED from this report.
            // It is covered by the Postgres integration tests gated on PAYWALL_TEST_PG_URL
            // (see README / CLAUDE.md), but those tests don't feed this exec file.
            // The headline coverage % intentionally excludes the hardest infra boundary.
            // To include it: run `PAYWALL_TEST_PG_URL=… ./gradlew :backend:test` in CI
            // with a PG 16 service container and merge the resulting exec into executionData.
            exclude("nl/incedo/paywall/backend/persistence/**")
            // Real CIAM HTTP client — requires Ory Kratos running
            exclude("nl/incedo/paywall/backend/KratosCiamSessionClient**")
            // JWT JWKS companion — fetches keys from a live CIAM endpoint
            exclude("nl/incedo/paywall/backend/auth/CiamJwtValidator\$Companion**")
            // Serialization module for Postgres JSONB — only used by PostgresEventStore
            exclude("nl/incedo/paywall/core/EventSerializationKt**")
            // @JvmInline value class box wrappers — compiler erases them at call sites
            // so JaCoCo can never see them executed (known Kotlin/JaCoCo limitation).
            exclude("nl/incedo/paywall/core/ArticleId**")
            exclude("nl/incedo/paywall/core/BrandId**")
            exclude("nl/incedo/paywall/core/ExperimentId**")
            exclude("nl/incedo/paywall/core/GrantId**")
            exclude("nl/incedo/paywall/core/PartnerId**")
            exclude("nl/incedo/paywall/core/PlanId**")
            exclude("nl/incedo/paywall/core/SubjectId**")
            exclude("nl/incedo/paywall/core/SubscriptionId**")
            exclude("nl/incedo/paywall/core/UserId**")
            exclude("nl/incedo/paywall/core/VisitorId**")
            exclude("nl/incedo/paywall/core/WallId**")
            // Server startup code — main() wires production infra not present in tests
            exclude("nl/incedo/paywall/backend/ApplicationKt\$main**")
            // Kotlin interface DefaultImpls — never directly instantiated (JaCoCo limitation)
            exclude("nl/incedo/paywall/cep/CepClient**")
            // PaymentProvider interface + DefaultImpls — external boundary, only MockPaymentProvider used in tests
            exclude("nl/incedo/paywall/backend/PaymentProvider**")
            // Dead code: ShareTokenService.IssueResult.NotEntitled is defined but never returned from issue()
            exclude("nl/incedo/paywall/backend/ShareTokenService\$IssueResult\$NotEntitled**")
            // ShareTokenResponse: only reachable when jwtValidator is present (live CIAM infra, not in tests)
            exclude("nl/incedo/paywall/backend/ShareTokenResponse**")
        }
    )
    sourceDirectories.setFrom(
        sourceSets.main.get().allSource.srcDirs +
        files(
            "../shared/src/commonMain/kotlin",
            "../shared/src/jvmMain/kotlin",
        )
    )
    reports {
        xml.required = true
        csv.required = true
        html.required = true
    }
}
