package com.hllous.plantravel.data.destination

import java.text.Normalizer

object DestinationTextNormalizer {
    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase()
        .trim()
}
