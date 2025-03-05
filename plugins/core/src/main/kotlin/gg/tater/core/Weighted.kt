package gg.tater.core

import io.netty.util.internal.ThreadLocalRandom

fun <T> getRandomWeightedItem(items: List<Pair<T, Int>>): T {
    // Step 1: Calculate the total weight (should sum to 100 for percentages)
    val totalWeight = items.sumOf { it.second }

    // Step 2: Generate a random number between 0 and total weight
    val randomValue = ThreadLocalRandom.current().nextInt(0, totalWeight)

    // Step 3: Determine the item based on the random value
    var runningWeight = 0
    for (item in items) {
        runningWeight += item.second
        if (randomValue < runningWeight) {
            return item.first
        }
    }

    // Fallback, although it shouldn't happen
    throw IllegalArgumentException("No item selected")
}