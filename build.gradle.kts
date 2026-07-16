plugins {
    id("java-library")
    id("maven-publish")
    jacoco
    checkstyle
    id("net.nemerosa.versioning") version "4.0.1"
    id("org.owasp.dependencycheck") version "12.2.2"
    id("org.cyclonedx.bom") version "3.2.4"
}

versioning {
    releaseMode = "snapshot"
    displayMode = "snapshot"
    releaseBuild = false
}

group = "pe.edu.nova.java.libs"
version = findProperty("version") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

val junitVersion = "6.0.0"
val archunitVersion = "1.4.1"

dependencies {
    api("com.tngtech.archunit:archunit-junit5:$archunitVersion")
    api("org.junit.jupiter:junit-jupiter-api:$junitVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml")
        )
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:all", "-quiet")
        encoding = "UTF-8"
        charSet = "UTF-8"
    }
}

checkstyle {
    sourceSets = listOf(project.sourceSets.main.get())
}

dependencyCheck {
    failBuildOnCVSS = (System.getenv("NOVA_OWASP_FAIL_ON_CVSS") ?: "11").toFloat()
    nvd.apiKey = System.getenv("NVD_API_KEY") ?: ""
    data.directory = System.getenv("NOVA_OWASP_DATA_DIR")
        ?: "${System.getProperty("user.home")}/.dependency-check-data"
    scanConfigurations = listOf("compileClasspath", "runtimeClasspath")
    analyzers {
        retirejs.enabled = false
        assemblyEnabled = false
        nuspecEnabled = false
        nugetconfEnabled = false
        msbuildEnabled = false
        golangDepEnabled = false
        golangModEnabled = false
        swiftEnabled = false
        swiftPackageResolvedEnabled = false
        cocoapodsEnabled = false
        composerEnabled = false
        cpanEnabled = false
        cmakeEnabled = false
        autoconfEnabled = false
        bundleAuditEnabled = false
        pyDistributionEnabled = false
        pyPackageEnabled = false
        rubygemsEnabled = false
        dartEnabled = false
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ahincho/nova-java-architecture-rules")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
