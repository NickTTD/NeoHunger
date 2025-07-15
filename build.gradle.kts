plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.named("runClient") {
    // Removed dependsOn("copyCoremodJar") as the task no longer exists
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "FMLCorePluginContainsFMLMod" to true
        )
    }
}
