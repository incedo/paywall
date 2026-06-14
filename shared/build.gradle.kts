import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    jacoco
}

kotlin {
    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.register<JacocoReport>("jacocoJvmReport") {
    dependsOn("jvmTest")
    classDirectories.setFrom(files(layout.buildDirectory.dir("classes/kotlin/jvm/main")))
    sourceDirectories.setFrom(files("src/commonMain/kotlin", "src/jvmMain/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory) { include("jacoco/jvmTest.exec") })
    reports {
        xml.required = true
        csv.required = true
        html.required = true
    }
}
