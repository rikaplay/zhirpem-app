pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        // Репозиторий для Node.js (нужен для JS/Wasm таргетов)
        ivy {
            url = uri("https://nodejs.org/dist")
            patternLayout {
                artifact("v[revision]/[artifact]-[revision]-[classifier].[ext]")
            }
            metadataSources { artifact() }
            content { includeGroup("org.nodejs") }
        }
    }
}

rootProject.name = "zhirpem_app"
include(":app")
