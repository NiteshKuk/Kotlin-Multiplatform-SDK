import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    `maven-publish`
}

group = findProperty("sdkGroup")?.toString() ?: "com.kmpsdk"
version = findProperty("sdkVersion")?.toString() ?: "1.0.0-SNAPSHOT"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        publishLibraryVariants("release")
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "KmpSDK"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.kmpsdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

sqldelight {
    databases {
        create("KmpSdkDatabase") {
            packageName.set("com.kmpsdk.data.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri(
                findProperty("publishRepoUrl") as String?
                    ?: "https://maven.pkg.github.com/NiteshKuk/Kotlin-Multiplatform-SDK",
            )
            credentials {
                username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR") ?: ""
                password = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }

    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("KmpSDK")
            description.set("Headless Kotlin Multiplatform SDK for Android and iOS.")
            url.set("https://github.com/NiteshKuk/Kotlin-Multiplatform-SDK")
            developers {
                developer {
                    id.set("NiteshKuk")
                    name.set("NiteshKuk")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/NiteshKuk/Kotlin-Multiplatform-SDK.git")
                developerConnection.set("scm:git:ssh://github.com/NiteshKuk/Kotlin-Multiplatform-SDK.git")
                url.set("https://github.com/NiteshKuk/Kotlin-Multiplatform-SDK")
            }
        }
    }
}
