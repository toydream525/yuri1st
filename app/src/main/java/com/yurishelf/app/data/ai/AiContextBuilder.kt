package com.yurishelf.app.data.ai

import com.yurishelf.app.data.remote.SubjectDto
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

fun SubjectDto.toAiContext(): AiSubjectContext = AiSubjectContext(
    title = name,
    titleCn = nameCn.orEmpty(),
    typeLabel = when (type) {
        1 -> "书籍（漫画/轻小说）"
        2 -> "动画"
        3 -> "音乐"
        4 -> "游戏"
        6 -> "三次元"
        else -> "条目类型 $type"
    },
    date = date.orEmpty(),
    platform = platform.orEmpty(),
    score = rating?.takeIf { it.total > 0 }?.let {
        String.format(Locale.US, "%.1f（%d 人评分）", it.score, it.total)
    }.orEmpty(),
    summary = summary.orEmpty(),
    tags = tags.map { it.name },
    metaTags = metaTags,
    infobox = infobox.joinToString("；") { item ->
        val valueText = runCatching {
            item.value.jsonPrimitive.contentOrNull
        }.getOrNull() ?: item.value.toString()
        "${item.key}：$valueText"
    },
)
