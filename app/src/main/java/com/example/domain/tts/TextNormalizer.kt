package com.example.domain.tts

import java.util.Locale

object TextNormalizer {

    /**
     * Normalizes raw LLM / Agent markdown response into clean, natural speech-friendly text.
     */
    fun normalize(
        rawText: String,
        skipCodeBlocks: Boolean = true,
        skipUrls: Boolean = true,
        skipJsonMetadata: Boolean = true
    ): String {
        var text = rawText

        // 1. Remove or skip markdown code blocks ```lang ... ```
        if (skipCodeBlocks) {
            text = text.replace(Regex("```[\\s\\S]*?```"), " ")
            text = text.replace(Regex("`[^`\n]+`"), " ")
        } else {
            text = text.replace("```", " ")
            text = text.replace("`", " ")
        }

        // 2. Remove JSON metadata / tool internal objects if any
        if (skipJsonMetadata) {
            text = text.replace(Regex("\\{[\\s\\S]*?\\}"), " ")
        }

        // 3. Remove URLs
        if (skipUrls) {
            text = text.replace(Regex("https?://\\S+"), " ")
        } else {
            text = text.replace(Regex("https?://(\\S+)")) { match ->
                val host = match.groupValues.getOrNull(1)?.substringBefore('/') ?: "web link"
                "link to $host"
            }
        }

        // 4. Strip markdown syntax: headings, bold, italic, strikethrough, blockquotes, bullets
        text = text.replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        text = text.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        text = text.replace(Regex("\\*(.*?)\\*"), "$1")
        text = text.replace(Regex("__(.*?)__"), "$1")
        text = text.replace(Regex("_(.*?)_"), "$1")
        text = text.replace(Regex("~~(.*?)~~"), "$1")
        text = text.replace(Regex("^>\\s+", RegexOption.MULTILINE), "")
        text = text.replace(Regex("^[\\*\\-\\+]\\s+", RegexOption.MULTILINE), "")
        text = text.replace(Regex("^\\d+\\.\\s+", RegexOption.MULTILINE), "")

        // 5. Clean extra whitespace
        text = text.replace(Regex("[ \\t]+"), " ")
        text = text.replace(Regex("\\n{2,}"), "\n")

        return text.trim()
    }

    /**
     * Splits normalized text into natural sentence chunks for smooth progressive synthesis.
     */
    fun segmentIntoSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        // Splits on standard western punctuation and newlines
        val rawSegments = text.split(Regex("(?<=[.!?\\n])\\s+"))
        val sentences = mutableListOf<String>()

        for (seg in rawSegments) {
            val trimmed = seg.trim()
            if (trimmed.isNotBlank()) {
                // If segment is very long (over 180 chars, common in Thai where spaces separate clauses), split on commas or spaces
                if (trimmed.length > 200 && containsThai(trimmed)) {
                    val thaiClauses = trimmed.split(Regex("(?<=[,;])\\s+|\\s{2,}"))
                    for (c in thaiClauses) {
                        if (c.isNotBlank()) sentences.add(c.trim())
                    }
                } else {
                    sentences.add(trimmed)
                }
            }
        }

        return if (sentences.isEmpty()) listOf(text.trim()) else sentences
    }

    /**
     * Detects dominant language of the text.
     */
    fun detectLocale(text: String): Locale {
        var thaiChars = 0
        var cjkChars = 0
        var japaneseKanaChars = 0
        var totalLetters = 0

        for (char in text) {
            val code = char.code
            when {
                // Thai: \u0E00 - \u0E7F
                code in 0x0E00..0x0E7F -> {
                    thaiChars++
                    totalLetters++
                }
                // Japanese Hiragana & Katakana: \u3040 - \u30FF
                code in 0x3040..0x30FF -> {
                    japaneseKanaChars++
                    totalLetters++
                }
                // CJK Ideographs: \u4E00 - \u9FFF
                code in 0x4E00..0x9FFF -> {
                    cjkChars++
                    totalLetters++
                }
                char.isLetter() -> {
                    totalLetters++
                }
            }
        }

        return when {
            thaiChars > 0 && thaiChars * 2 >= totalLetters / 3 -> Locale("th", "TH")
            japaneseKanaChars > 0 -> Locale.JAPANESE
            cjkChars > 0 && cjkChars * 2 >= totalLetters / 3 -> Locale.CHINESE
            else -> Locale.ENGLISH
        }
    }

    fun containsThai(text: String): Boolean {
        return text.any { it.code in 0x0E00..0x0E7F }
    }
}
