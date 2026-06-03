package com.github.mikecraft1224.simplecore.examples.config

import com.github.mikecraft1224.simplecore.BuildConfig
import com.github.mikecraft1224.simplecore.config.api.annotations.*
import com.github.mikecraft1224.simplecore.config.api.values.*
import org.lwjgl.glfw.GLFW

@Suppress("Unused")
@Config(title = "SimpleCore Test", subtitle = "Developer testing config")
class TestConfig {

    @Entry(description = "Toggle the feature on/off")
    var enabled = true

    @Entry(description = "Movement speed multiplier")
    @Slider(0.5, 5.0, 0.5)
    var speed = 1.0

    @Entry(description = "Operation mode")
    var mode = TestMode.NORMAL

    @Separator
    @Entry(description = "Render quality preset")
    var quality = Dropdown("Low", "Medium", "High")

    @Entry(description = "Custom display label")
    var label = "Player"

    @Entry("Accent color")
    var accentColor = java.awt.Color(0x5865F2)

    @Entry(description = "Key to open the config screen")
    var configKey = Keybind(GLFW.GLFW_KEY_INSERT)

    @Entry("Build info")
    @Info
    var buildInfo = "SimpleCore ${BuildConfig.MOD_VERSION}"

    @Separator
    @Entry(description = "Players to exclude from processing")
    var blockedPlayers: MutableList<String> = mutableListOf()

    @Entry(description = "Active flags")
    var flags: MutableList<Boolean> = mutableListOf()

    @Entry("Active modes", "Select all modes that should be active simultaneously")
    var activeModes = MultiSelect("Normal", "Fast", "Stealth")

    // --- Dynamic dropdown-list example ---
    // priorityOptions is the editable source of options (saved/loaded via Reference).
    // priorityList lets each slot pick from that same live list via DropdownList.

    @Separator
    @Entry("Priority options", "Add or remove available priority levels")
    var priorityOptions = Reference(PriorityData::options)

    @Entry("Priority list", "Each slot picks a priority from the list above")
    var priorityList = DropdownList(PriorityData::options)

    @Excluded(config = true)
    var priorityCounter = 0

    @Entry("Add custom priority", "Adds a new option to the priority list above")
    var addCustomPriority = Button("Add") {
        PriorityData.options.add("Custom ${++priorityCounter}")
    }

    // --- Object list example - auto-inferred from MutableList<Server> ---
    @Entry(description = "List of server connection profiles")
    var servers: MutableList<Server> = mutableListOf(Server())

    // --- Cross-class visibility via Reference ---
    // experimentalEnabled mirrors FeatureFlags.experimentalEnabled transparently.
    @Separator
    @Entry("Toggle experimental", "Toggles FeatureFlags.experimentalEnabled live")
    var toggleExperimental = Button("Toggle") {
        FeatureFlags.experimentalEnabled = !FeatureFlags.experimentalEnabled
    }

    var experimentalEnabled = Reference(FeatureFlags::experimentalEnabled)

    @Entry("Experimental threshold", description = "Only visible when FeatureFlags.experimentalEnabled is true")
    @Slider(0.0, 1.0, 0.05)
    var experimentalThreshold = Visible(FeatureFlags::experimentalEnabled, 0.5)

    // --- Cross-class dropdown via Reference ---
    var connectionModes = Reference(ServerProfileOptions::connectionModes)

    @Entry("Connection mode", description = "Options loaded live from ServerProfileOptions via Reference")
    var connectionMode = Dropdown(ServerProfileOptions::connectionModes)

    // --- Conditional category ---
    @Entry("Show advanced category")
    var advancedEnabled = true

    @Entry(description = "Restore all fields to their default values")
    var resetAction = Button("Reset") {
        enabled = true
        speed = 1.0
        mode = TestMode.NORMAL
        quality.index = 1
        label = "Player"
        accentColor = java.awt.Color(0x5865F2)
        configKey.packed = GLFW.GLFW_KEY_INSERT
        advancedEnabled = true
    }

    @Excluded(config = true)
    var sessionData = "not persisted to file"

    @Category("Advanced", "Fine-grained controls")
    var advanced = Visible(this::advancedEnabled, AdvancedSettings())
}

@Suppress("Unused")
enum class TestMode { NORMAL, FAST, STEALTH }

@Suppress("Unused")
class AdvancedSettings {

    @Entry(description = "Fine-grained multiplier applied on top of speed")
    @Slider(0.1, 2.0, 0.1)
    var multiplier = 1.0

    @Entry(description = "Optional string tags")
    var tags: MutableList<String> = mutableListOf()

    @Collapsible
    @DefaultCollapsed
    @Entry("Debug")
    var debug = DebugSettings()

    @Category("Network", "Network-related advanced settings")
    var network = NetworkSettings()
}

@Suppress("Unused")
class NetworkSettings {

    @Entry(description = "Connection timeout in milliseconds")
    @Slider(100.0, 10000.0, 100.0)
    var timeoutMs = 3000

    @Entry("Retry on fail")
    var retryOnFail = true
}

@Suppress("Unused")
class DebugSettings {

    @Entry(description = "Enable verbose logging")
    var debugMode = false

    @Entry("Max retries")
    @Slider(1.0, 10.0, 1.0)
    var maxRetries = 3
}

object PriorityData {
    var options: MutableList<String> = mutableListOf("Low", "Normal", "High", "Critical")
}

object FeatureFlags {
    var experimentalEnabled = false
}

object ServerProfileOptions {
    val connectionModes: MutableList<String> = mutableListOf("Direct", "Proxied", "Auto")
}

@Suppress("Unused")
class Server {
    @Entry(description = "Server hostname or IP address")
    var host: String = "localhost"

    @Entry(description = "Connection port")
    @Slider(1.0, 65535.0, 1.0)
    var port: Int = 25565

    @Entry(description = "Include this server in rotation")
    var enabled: Boolean = true

    @Entry(description = "Connection priority level")
    var priority = Dropdown("Low", "Normal", "High")

    override fun toString() = host
}
