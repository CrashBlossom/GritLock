package com.example.fitlock

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * GritLockApp is the base Application class.
 * The @HiltAndroidApp annotation triggers Hilt's code generation.
 * This is the entry point for Dependency Injection in our app.
 */
@HiltAndroidApp
class GritLockApp : Application()
