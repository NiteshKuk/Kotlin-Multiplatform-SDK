import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.plugins.signing.SigningExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.maven.publish)
}

group = findProperty("sdkGroup")?.toString() ?: "in.co.niteshkukreja"
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
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            api(libs.koin.core)
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
            // SDK-owned DB; schema evolves with library releases (draft_store, etc.).
            verifyMigrations.set(false)
        }
    }
}

configure<MavenPublishBaseExtension> {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    coordinates(
        groupId = group.toString(),
        artifactId = "kmp-sdk",
        version = version.toString(),
    )

    pom {
        name.set("KmpSDK")
        description.set("Headless Kotlin Multiplatform SDK for Android and iOS.")
        inceptionYear.set("2025")
        url.set("https://github.com/NiteshKuk/Kotlin-Multiplatform-SDK")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("NiteshKuk")
                name.set("NiteshKuk")
                url.set("https://github.com/NiteshKuk")
            }
        }
        scm {
            url.set("https://github.com/NiteshKuk/Kotlin-Multiplatform-SDK")
            connection.set("scm:git:git://github.com/NiteshKuk/Kotlin-Multiplatform-SDK.git")
            developerConnection.set("scm:git:ssh://git@github.com/NiteshKuk/Kotlin-Multiplatform-SDK.git")
        }
    }
}

extensions.configure<SigningExtension> {
    useGpgCmd()
}