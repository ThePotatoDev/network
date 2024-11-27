package gg.tater.shared.player.chat.color

data class ChatColor(val name: String, val startColor: String, val endColor: String) {

    companion object Registry {
        val CHAT_COLORS: MutableMap<String, ChatColor> = mutableMapOf(
            "Fall" to ChatColor("Fall", "#F3904F", "#A05812")
        )

        fun get(name: String): ChatColor? {
            return CHAT_COLORS[name]
        }
    }
}