package com.datathecodie.witgrid.data.progress

import com.datathecodie.witgrid.engine.core.Pack
import com.datathecodie.witgrid.games.LevelRef

/** Legacy saves use their bank position; numbered saves retain their position across loops. */
private fun latestAttempt(refs: List<LevelRef>, progress: List<LevelProgress>): LevelProgress? {
    val bankRefs = refs.filterNot { it.daily || it.isIntro }
    if (bankRefs.isEmpty()) return null
    return progress.mapNotNull { saved ->
        val index = bankRefs.indexOfFirst { it.gameId == saved.gameId && it.levelId == saved.levelId }
        if (index < 0) return@mapNotNull null
        val number = saved.attemptNumber.takeIf { it > 0 } ?: (index + 1L)
        if ((number - 1) % bankRefs.size != index.toLong()) return@mapNotNull null
        saved.copy(attemptNumber = number) to (saved.attemptNumber > 0)
    }.maxWithOrNull(compareBy<Pair<LevelProgress, Boolean>> { it.first.attemptNumber }.thenBy { it.second })?.first
}

fun nextPackLevel(refs: List<LevelRef>, progress: List<LevelProgress>): LevelRef? {
    val intro = refs.firstOrNull { it.isIntro }
    val bankRefs = refs.filterNot { it.daily || it.isIntro }
    val regularProgress = progress.any { saved ->
        bankRefs.any { ref -> ref.gameId == saved.gameId && ref.levelId == saved.levelId }
    }
    val introProgress = intro?.let { introRef ->
        progress.firstOrNull { it.gameId == introRef.gameId && it.levelId == introRef.levelId }
    }
    if (intro != null && (introProgress?.completed == false || (introProgress == null && !regularProgress))) {
        return intro
    }
    if (bankRefs.isEmpty()) return null
    val latest = latestAttempt(bankRefs, progress)
    val number = when {
        latest == null -> 1L
        latest.completed -> Math.addExact(latest.attemptNumber, 1L)
        else -> latest.attemptNumber
    }
    return LevelRef.forLevel(number, refs)
}

/** Prefer a saved active attempt, then a pack already played, then the first available pack. */
fun nextGameLevel(refs: List<LevelRef>, progress: List<LevelProgress>): LevelRef? {
    val packs = Pack.entries.map { pack -> refs.filter { !it.daily && it.pack == pack } }
    val activeIntroPack = packs.firstOrNull { packRefs ->
        val intro = packRefs.firstOrNull { it.isIntro }
        intro != null && progress.any { it.gameId == intro.gameId && it.levelId == intro.levelId && !it.completed }
    }
    val selected = activeIntroPack
        ?: packs.firstOrNull { latestAttempt(it, progress)?.completed == false }
        ?: packs.firstOrNull { latestAttempt(it, progress) != null }
        ?: packs.firstOrNull { it.isNotEmpty() }
        ?: return null
    return nextPackLevel(selected, progress)
}
