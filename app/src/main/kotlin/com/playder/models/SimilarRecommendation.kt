// SimilarRecommendation.kt

package com.playder.models

import com.music.innertube.models.YTItem
import com.playder.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
