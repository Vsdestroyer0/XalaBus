package com.example.xalabus

import androidx.compose.ui.window.ComposeUIViewController
import com.example.xalabus.db.DriverFactory
import com.example.xalabus.ui.App

fun MainViewController() = ComposeUIViewController { 
    App(DriverFactory()) 
}
