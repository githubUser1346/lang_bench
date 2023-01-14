import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    application
    kotlin("plugin.serialization") version "1.6.21"
}

repositories {
    mavenCentral()
}

tasks.withType(KotlinCompile::class.java).all {
    kotlinOptions.freeCompilerArgs =
        listOf(*kotlinOptions.freeCompilerArgs.toTypedArray(), "-Xadd-modules=jdk.incubator.vector")
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.1")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.21")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.21")
    implementation("org.eclipse.collections:eclipse-collections:11.1.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.jsoniter:jsoniter:0.9.23")
    implementation("com.google.guava:guava:11.0.2")
    implementation("commons-codec:commons-codec:1.15")


}

application {
    mainClass.set("slacroix.AppKt")
    applicationDefaultJvmArgs = listOf("--add-modules", "jdk.incubator.vector")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    jvmArgs("--add-modules", "jdk.incubator.vector")
}