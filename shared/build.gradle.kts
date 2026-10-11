plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvm()


    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kizitonwose.calendar)
            implementation(libs.kotlinx.datetime)
            implementation(libs.material.icons.extended)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            // AI assistant: Koog agent framework + OpenRouter LLM client. The umbrella
            // artifact also brings the HTTP stack (ktor + Apache5 engine) it needs.
            implementation(libs.koog.agents)
            implementation(libs.koog.openrouter.client)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.jvm.driver)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("data.db")
            srcDirs.setFrom("src/commonMain/sqldelight")
        }
    }
}