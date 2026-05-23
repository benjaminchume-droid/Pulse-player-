package com.pulseplayer.music.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class PerformanceMode {
    LITE, BALANCED, CINEMATIC
}

data class RealmTheme(
    val id: String,
    val name: String,
    val description: String,
    val gradientColors: List<Color>,
    val accentColor: Color,
    val glowColor: Color,
    val ambientColor: Color,
    val particleColor: Color,
    val particleCount: Int,
    val blurIntensity: Dp,
    val visualizerStyle: String, // "galaxy", "grid", "minimal", "skyline", "ribbon", "molten", "frozen", "sonar", "chrome", "velvet", "clean", "industrial", "retro"
    val soundstageName: String,
    val glyphEmblems: String,
    val atmosphericDepth: Boolean = true,
    val motionStyle: String = "fluid", // "fluid", "aggressive", "calm", "snappy"
    val isLight: Boolean = false
)

object RealmManager {
    // 15 Curated Realms definition array
    val realms = listOf(
        RealmTheme(
            id = "cosmic_atmos",
            name = "Cosmic Atmos",
            description = "Deep interstellar universe floating inside orbital space nebulas.",
            gradientColors = listOf(Color(0xFF070214), Color(0xFF0F052B), Color(0xFF020108)),
            accentColor = Color(0xFF00F0FF),
            glowColor = Color(0xFF9D00FF),
            ambientColor = Color(0x3300F0FF),
            particleColor = Color(0x7D00F0FF),
            particleCount = 50,
            blurIntensity = 24.dp,
            visualizerStyle = "galaxy",
            soundstageName = "Interstellar Chamber",
            glyphEmblems = "🌌"
        ),
        RealmTheme(
            id = "overclocked_cyber",
            name = "Overclocked Cyber",
            description = "High-speed neon neural network loaded with hologram subgrids.",
            gradientColors = listOf(Color(0xFF030A12), Color(0xFF0A1C2A), Color(0xFF010204)),
            accentColor = Color(0xFF00FFCC),
            glowColor = Color(0xFFFF007F),
            ambientColor = Color(0x33FF007F),
            particleColor = Color(0x7D00FFCC),
            particleCount = 40,
            blurIntensity = 12.dp,
            visualizerStyle = "grid",
            soundstageName = "Neural Network Lab",
            glyphEmblems = "💾",
            motionStyle = "aggressive"
        ),
        RealmTheme(
            id = "lunar_spheres",
            name = "Lunar Spheres",
            description = "Silver moonlit futuristic sanctuary with frosted minimal glass.",
            gradientColors = listOf(Color(0xFF141619), Color(0xFF1E2229), Color(0xFF0A0C0E)),
            accentColor = Color(0xFFE2E8F0),
            glowColor = Color(0x33FFFFFF),
            ambientColor = Color(0x1AFFFFFF),
            particleColor = Color(0x62E2E8F0),
            particleCount = 20,
            blurIntensity = 32.dp,
            visualizerStyle = "minimal",
            soundstageName = "Lunar Sanctuary",
            glyphEmblems = "🌙",
            motionStyle = "calm"
        ),
        RealmTheme(
            id = "emerald_neon",
            name = "Emerald Neon",
            description = "Cyber metropolis reflections shimmering through virtual streets.",
            gradientColors = listOf(Color(0xFF02120B), Color(0xFF052B18), Color(0xFF010503)),
            accentColor = Color(0xFF00FF66),
            glowColor = Color(0xFF00E5FF),
            ambientColor = Color(0x3300FF66),
            particleColor = Color(0x7D00FF66),
            particleCount = 35,
            blurIntensity = 16.dp,
            visualizerStyle = "skyline",
            soundstageName = "Emerald City Sidelink",
            glyphEmblems = "🏙️"
        ),
        RealmTheme(
            id = "dark_void",
            name = "Dark Void",
            description = "Pure infinite blackness with isolated deep space edge glows.",
            gradientColors = listOf(Color(0xFF000000), Color(0xFF030303), Color(0xFF000000)),
            accentColor = Color(0xFF7F00FF),
            glowColor = Color(0x1DFFFFFF),
            ambientColor = Color(0x197F00FF),
            particleColor = Color(0x337F00FF),
            particleCount = 10,
            blurIntensity = 8.dp,
            visualizerStyle = "minimal",
            soundstageName = "Abyssal Crypt",
            glyphEmblems = "🕳️",
            motionStyle = "calm"
        ),
        RealmTheme(
            id = "aurora_emerald",
            name = "Aurora Emerald",
            description = "Dynamic northern lights ribbons dancing in fluid atmosphere.",
            gradientColors = listOf(Color(0xFF011414), Color(0xFF052F2F), Color(0xFF010708)),
            accentColor = Color(0xFF00FFA6),
            glowColor = Color(0xFF00E5FF),
            ambientColor = Color(0x3300FFA6),
            particleColor = Color(0x6200FFA6),
            particleCount = 30,
            blurIntensity = 28.dp,
            visualizerStyle = "ribbon",
            soundstageName = "Aurora Canopy",
            glyphEmblems = "✨",
            motionStyle = "fluid"
        ),
        RealmTheme(
            id = "crimson_eclipse",
            name = "Crimson Eclipse",
            description = "Corona of burning obsidian starbursts casting heavy shadows.",
            gradientColors = listOf(Color(0xFF140205), Color(0xFF2C0A0E), Color(0xFF050102)),
            accentColor = Color(0xFFFF0D30),
            glowColor = Color(0xFFFFB700),
            ambientColor = Color(0x40FF0D30),
            particleColor = Color(0x7DFF0D30),
            particleCount = 45,
            blurIntensity = 20.dp,
            visualizerStyle = "molten",
            soundstageName = "Eclipse Corona Core",
            glyphEmblems = "🌑",
            motionStyle = "aggressive"
        ),
        RealmTheme(
            id = "solar_flare",
            name = "Solar Flare",
            description = "Blazing streams of volcanic heat and high-energy gold solar arches.",
            gradientColors = listOf(Color(0xFF1C0900), Color(0xFF381400), Color(0xFF080200)),
            accentColor = Color(0xFFFFAC1C),
            glowColor = Color(0xFFFF3700),
            ambientColor = Color(0x4DFFAC1C),
            particleColor = Color(0x7DFFAC1C),
            particleCount = 60,
            blurIntensity = 18.dp,
            visualizerStyle = "molten",
            soundstageName = "Solar Reactor Room",
            glyphEmblems = "☀️",
            motionStyle = "fluid"
        ),
        RealmTheme(
            id = "frozen_pulse",
            name = "Frozen Pulse",
            description = "Digital cryo crystals fractured in frosted structural glass fields.",
            gradientColors = listOf(Color(0xFF0B141C), Color(0xFF162534), Color(0xFF04080D)),
            accentColor = Color(0xFF76E1FF),
            glowColor = Color(0xFF0091FF),
            ambientColor = Color(0x2B76E1FF),
            particleColor = Color(0x6276E1FF),
            particleCount = 25,
            blurIntensity = 30.dp,
            visualizerStyle = "frozen",
            soundstageName = "Sub-Zero Crypt",
            glyphEmblems = "❄️",
            motionStyle = "calm"
        ),
        RealmTheme(
            id = "ocean_drift",
            name = "Ocean Drift",
            description = "Abyssal underwater sanctuary with floating bubbles and deep sonar.",
            gradientColors = listOf(Color(0xFF020E1A), Color(0xFF041F3A), Color(0xFF01060D)),
            accentColor = Color(0xFF00AAFF),
            glowColor = Color(0xFF4EE2FF),
            ambientColor = Color(0x3300AAFF),
            particleColor = Color(0x7000AAFF),
            particleCount = 35,
            blurIntensity = 26.dp,
            visualizerStyle = "sonar",
            soundstageName = "Abyssal Oceanic Stage",
            glyphEmblems = "🌊",
            motionStyle = "fluid"
        ),
        RealmTheme(
            id = "phantom_chrome",
            name = "Phantom Chrome",
            description = "Iridescent metallic liquid steel warping to electromagnetic bass.",
            gradientColors = listOf(Color(0xFF1A1A24), Color(0xFF30303E), Color(0xFF0E0E14)),
            accentColor = Color(0xFFE5D5FF),
            glowColor = Color(0xFF7000FF),
            ambientColor = Color(0x26FFFFFF),
            particleColor = Color(0x62E5D5FF),
            particleCount = 15,
            blurIntensity = 15.dp,
            visualizerStyle = "chrome",
            soundstageName = "Liquid Metal Dome",
            glyphEmblems = "💿"
        ),
        RealmTheme(
            id = "velvet_midnight",
            name = "Velvet Midnight",
            description = "Luxurious magenta twilight of dark lounge rooms.",
            gradientColors = listOf(Color(0xFF140212), Color(0xFF2C0428), Color(0xFF050105)),
            accentColor = Color(0xFFFF33D6),
            glowColor = Color(0xFFAC00E6),
            ambientColor = Color(0x33FF33D6),
            particleColor = Color(0x7DFF33D6),
            particleCount = 30,
            blurIntensity = 24.dp,
            visualizerStyle = "velvet",
            soundstageName = "Velvet Lounge Room",
            glyphEmblems = "🍷",
            motionStyle = "calm"
        ),
        RealmTheme(
            id = "zenith_white",
            name = "Zenith White",
            description = "Futuristic AI clinical layout made of pure glowing white plates.",
            gradientColors = listOf(Color(0xFFF0F2F5), Color(0xFFFFFFFF), Color(0xFFE2E8F0)),
            accentColor = Color(0xFF4A5568),
            glowColor = Color(0x194A5568),
            ambientColor = Color(0x064A5568),
            particleColor = Color(0x334A5568),
            particleCount = 15,
            blurIntensity = 20.dp,
            visualizerStyle = "clean",
            soundstageName = "AI Zenith Hub",
            glyphEmblems = "🎛️",
            isLight = true
        ),
        RealmTheme(
            id = "toxic_reactor",
            name = "Toxic Reactor",
            description = "Industrial cyber core venting glowing radioactive core blocks.",
            gradientColors = listOf(Color(0xFF050F02), Color(0xFF0E2705), Color(0xFF020501)),
            accentColor = Color(0xFF7FFF00),
            glowColor = Color(0xFFCCFF00),
            ambientColor = Color(0x407FFF00),
            particleColor = Color(0x627FFF00),
            particleCount = 40,
            blurIntensity = 10.dp,
            visualizerStyle = "industrial",
            soundstageName = "Radioactive Core",
            glyphEmblems = "☣️",
            motionStyle = "aggressive"
        ),
        RealmTheme(
            id = "hyperwave_x",
            name = "Hyperwave X",
            description = "VHS retro synthwave skyline loaded with 80s grid structures.",
            gradientColors = listOf(Color(0xFF14011B), Color(0xFF2E013E), Color(0xFF07000B)),
            accentColor = Color(0xFFFF00D6),
            glowColor = Color(0xFF00F0FF),
            ambientColor = Color(0x3DFF00D6),
            particleColor = Color(0x8D00F0FF),
            particleCount = 45,
            blurIntensity = 14.dp,
            visualizerStyle = "retro",
            soundstageName = "Outrun Grid Tunnels",
            glyphEmblems = "🕹️"
        )
    )

    // Observable states leveraging Kotlin flows for high performance updates
    private val _currentTheme = MutableStateFlow(realms[0])
    val currentTheme: StateFlow<RealmTheme> get() = _currentTheme

    private val _performanceMode = MutableStateFlow(PerformanceMode.BALANCED)
    val performanceMode: StateFlow<PerformanceMode> get() = _performanceMode

    private val _amoledMode = MutableStateFlow(false)
    val amoledMode: StateFlow<Boolean> get() = _amoledMode

    // Audio reactive state floats driven dynamically by audio player frequencies
    val bassIntensity = MutableStateFlow(0f)
    val midIntensity = MutableStateFlow(0f)
    val trebleIntensity = MutableStateFlow(0f)

    // Optional audio weather (dynamic daylight adaptives: e.g. dark at night/dawn, glowing in day)
    private val _weatherAtmosphere = MutableStateFlow("Cosmic Dusk")
    val weatherAtmosphere: StateFlow<String> get() = _weatherAtmosphere

    fun selectTheme(themeId: String) {
        realms.find { it.id == themeId }?.let {
            _currentTheme.value = it
        }
    }

    fun setPerformanceMode(mode: PerformanceMode) {
        _performanceMode.value = mode
    }

    fun setAmoledMode(enabled: Boolean) {
        _amoledMode.value = enabled
    }

    fun setWeatherAtmosphere(value: String) {
        _weatherAtmosphere.value = value
    }
}
