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
            implementation("com.kizitonwose.calendar:compose-multiplatform:2.10.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0-0.6.x-compat")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation(libs.sqldelight.coroutines)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
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