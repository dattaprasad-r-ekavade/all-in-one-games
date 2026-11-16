plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.datathecodie.witgrid.bankgen.MainKt")
}

dependencies {
    implementation(project(":engine"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

val validateBank = tasks.register<JavaExec>("validateBank") {
    group = "verification"
    description = "Validates all committed puzzle bank files."
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    workingDir = rootProject.projectDir
    args("validate", "--all")
}

tasks.named("check") {
    dependsOn(validateBank)
}
