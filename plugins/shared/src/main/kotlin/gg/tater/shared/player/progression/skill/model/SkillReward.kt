package gg.tater.shared.player.progression.skill.model

data class SkillReward(val level: Int, val type: SkillType, val xp: Double, val rewards: List<String>)