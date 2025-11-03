plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("org.jetbrains.kotlin.plugin.compose")
	id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
	namespace = "com.curatrack.app"
	compileSdk = 34

	defaultConfig {
		applicationId = "com.curatrack.app"
		minSdk = 24
		targetSdk = 34
		versionCode = 1
		versionName = "0.1.0"
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}

	buildFeatures { compose = true }
	kotlinOptions { jvmTarget = "17" }
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
	val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
	implementation(composeBom)
	androidTestImplementation(composeBom)

	implementation("androidx.core:core-ktx:1.13.1")
	implementation("androidx.activity:activity-compose:1.9.2")
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("androidx.compose.material3:material3:1.3.0")
	debugImplementation("androidx.compose.ui:ui-tooling")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

	// Navigation Compose
	implementation("androidx.navigation:navigation-compose:2.8.1")

	// DataStore
	implementation("androidx.datastore:datastore-preferences:1.0.0")

	// WorkManager
	implementation("androidx.work:work-runtime-ktx:2.9.1")

	// ML Kit Text Recognition (on-device)
	implementation("com.google.mlkit:text-recognition:16.0.1")

	// Photo Picker (no runtime permission for gallery on modern devices)
	implementation("androidx.activity:activity-ktx:1.9.2")

	// CameraX (we'll wire camera later; picker first keeps it simple)
	implementation("androidx.camera:camera-core:1.3.4")
	implementation("androidx.camera:camera-camera2:1.3.4")
	implementation("androidx.camera:camera-lifecycle:1.3.4")

	// Coil for image loading
	implementation("io.coil-kt:coil-compose:2.7.0")
}

