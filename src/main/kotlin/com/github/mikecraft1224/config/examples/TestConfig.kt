package com.github.mikecraft1224.config.examples

import com.github.mikecraft1224.BuildConfig
import com.github.mikecraft1224.config.api.*
import org.lwjgl.glfw.GLFW

@Config(title = "SimpleCore Test", subtitle = "Developer testing config")
class TestConfig {

    @Entry("Enabled", "Toggle the feature on/off")
    @EditorBoolean
    var enabled = true

    @Entry("Speed", "Movement speed multiplier")
    @EditorSlider(min = 0.5, max = 5.0, step = 0.5)
    var speed = 1.0

    @Entry("Mode", "Operation mode")
    @EditorDropdown
    var mode = TestMode.NORMAL

    @Separator
    @Entry("Quality", "Render quality preset")
    @EditorDropdown(values = ["Low", "Medium", "High"])
    var quality = 1

    @Entry("Label", "Custom display label")
    @EditorText
    var label = "Player"

    @Entry("Accent color")
    @EditorColor
    var accentColor = java.awt.Color(0x5865F2)

    @Entry("Config key", "Key to open the config screen")
    @EditorKeybind(defaultKey = GLFW.GLFW_KEY_INSERT)
    var configKey = GLFW.GLFW_KEY_INSERT

    @Entry("Build info")
    @EditorInfo
    var buildInfo = "SimpleCore ${BuildConfig.MOD_VERSION}"

    @Separator
    @Entry("Blocked players", "Players to exclude from processing")
    @EditorMutable(defaultString = "")
    var blockedPlayers: MutableList<String> = mutableListOf()

    @Entry("Allowed flags", "Active flags")
    @EditorMutable(defaultBoolean = false)
    var flags: MutableList<Boolean> = mutableListOf()

    @Entry("Reset defaults", "Restore all fields to their default values")
    @EditorButton("Reset")
    var resetAction = Runnable {
        enabled = true
        speed = 1.0
        mode = TestMode.NORMAL
        quality = 1
        label = "Player"
        accentColor = java.awt.Color(0x5865F2)
        configKey = GLFW.GLFW_KEY_INSERT
    }

    @Excluded(config = true)
    var sessionData = "not persisted to file"

    @Category("Advanced", "Fine-grained controls")
    var advanced = AdvancedSettings()
}

enum class TestMode { NORMAL, FAST, STEALTH }

class AdvancedSettings {

    @Entry("Multiplier", "Fine-grained multiplier applied on top of speed")
    @EditorSlider(min = 0.1, max = 2.0, step = 0.1)
    var multiplier = 1.0

    @Entry("Tags", "Optional string tags")
    @EditorMutable(defaultString = "tag")
    var tags: MutableList<String> = mutableListOf()

    @Collapsible
    @DefaultCollapsed
    @Entry("Debug")
    var debug = DebugSettings()

    // Sub-category: appears as an indented item below "Advanced" in the sidebar
    @Category("Network", "Network-related advanced settings")
    var network = NetworkSettings()
}

class NetworkSettings {

    @Entry("Timeout ms", "Connection timeout in milliseconds")
    @EditorSlider(min = 100.0, max = 10000.0, step = 100.0)
    var timeoutMs = 3000

    @Entry("Retry on fail")
    @EditorBoolean
    var retryOnFail = true
}

class DebugSettings {

    @Entry("Debug mode", "Enable verbose logging")
    @EditorBoolean
    var debugMode = false

    @Entry("Max retries")
    @EditorSlider(min = 1.0, max = 10.0, step = 1.0)
    var maxRetries = 3
}
