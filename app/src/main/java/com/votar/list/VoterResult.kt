package com.votar.list

data class VoterResult(
    val fileName: String,
    val pageNum: Int,
    val data: String,
    var isBookmarked: Boolean = false,
) {
    fun getSerial(): String {
        val regexes = listOf(
            Regex("(?:ক্রমিক|ক্রমিক\\s*নং|ক্রমিক\\s*নম্বর|sl|serial)[:\\s\\-]*([0-9০-৯]+)", RegexOption.IGNORE_CASE),
            Regex("([0-9০-৯]+)[.|।\\-]\\s*(?:নাম|nam)", RegexOption.IGNORE_CASE),
            Regex("^([0-9০-৯]+)[.|।\\-]")
        )
        for (r in regexes) {
            val match = r.find(data) ?: continue
            return match.groupValues[1].trim()
        }
        return ""
    }

    fun getName(): String {
        val match = Regex("(?:নাম|নামঃ|nam)[:\\s\\-]*([^\\n,;]+)", RegexOption.IGNORE_CASE).find(data)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    fun getVoterId(): String {
        val regexes = listOf(
            Regex("(?:ভোটার\\s*নং|ভোটার\\s*নম্বর|ভোটার\\s*আইডি|voter\\s*no|voter\\s*id|ভোটার\\s*নংঃ|ভোটার\\s*নম্বরঃ)[:\\s\\-]*([0-9০-৯a-zA-Z]+)", RegexOption.IGNORE_CASE),
            Regex("([0-9]{10,17}|[০-৯]{10,17})")
        )
        for (r in regexes) {
            val match = r.find(data) ?: continue
            return match.groupValues[1].trim()
        }
        return ""
    }

    fun getFatherName(): String {
        val match = Regex("(?:পিতা|পিতার\\s*নাম|পিতাঃ|father)[:\\s\\-]*([^\\n,;]+)", RegexOption.IGNORE_CASE).find(data)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    fun getMotherName(): String {
        val match = Regex("(?:মাতা|মাতার\\s*নাম|মাতাঃ|mother)[:\\s\\-]*([^\\n,;]+)", RegexOption.IGNORE_CASE).find(data)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    fun getProfessionAndDob(): String {
        val match = Regex("(?:পেশা|জন্ম\\s*তারিখ|পেশা/জন্ম\\s*তারিখ)[:\\s\\-]*([^\\n]+)", RegexOption.IGNORE_CASE).find(data)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    fun getAddress(): String {
        val match = Regex("(?:ঠিকানা|ঠিকানাঃ|address)[:\\s\\-]*([^\\n,;]+)", RegexOption.IGNORE_CASE).find(data)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }
}
