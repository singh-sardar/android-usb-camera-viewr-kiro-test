pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        // Ensure JitPack is included to allow fetching the UVC camera library.
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://verve.jfrog.io/artifactory/verve-gradle-dev") }  //.gradle 
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://raw.githubusercontent.com/saki4510t/libcommon/master/repository/") }
    }
}

rootProject.name = "WebViewCamberViewr"
include(":app")
