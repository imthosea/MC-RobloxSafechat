plugins {
	java
	id("fabric-loom") version "1.11-SNAPSHOT"
}

fun prop(key: String) = property(key)!!.toString()

version = prop("mod_version")
group = "me.thosea"

repositories {
	maven(url = "https://maven.parchmentmc.org")
}

dependencies {
	minecraft("com.mojang", "minecraft", prop("minecraft_version"))
	modImplementation("net.fabricmc", "fabric-loader", prop("loader_version"))
	mappings(loom.layered {
		officialMojangMappings()
		parchment("org.parchmentmc.data:parchment-${prop("parchment_version")}@zip")
	})
}

java {
	val jvmVersion = prop("java_version").toInt()
	toolchain.languageVersion.set(JavaLanguageVersion.of(jvmVersion))
}

tasks.processResources {
	val properties = mapOf(
		"version" to project.version,
		"minecraft_version" to prop("minecraft_version"),
		"loader_version" to prop("loader_version")
	)
	inputs.properties(properties)
	filesMatching("fabric.mod.json") {
		expand(properties)
	}
}

tasks.jar {
	from("LICENSE")
}