package com.votar.list

object BengaliCleaner {
    fun clean(rawText: String?): String {
        if (rawText.isNullOrEmpty()) return ""
        var text = rawText

        // ১. আপনার নির্দিষ্ট ভুল শব্দগুলোর ডিকশনারি ( "ভুল বানান" to "সঠিক বানান" )
        val wordFixes = mapOf(
            "মেোছাঃ" to "মোছাঃ",
            "শিঞ্জী" to "শিল্পী",
            "স্বীৃতি" to "স্মৃতি",
            "খঁান" to "খান",
            "আব্দুন্নাহ" to "আব্দুল্লাহ",
            "নীলাম্বরী" to "নীলাম্বর", // (যদি এটি নীলাম্বর হয়, আপনার সঠিক বানানটি এখানে বসাবেন)

            // কমন কিছু বাংলা ওসির ভুল
            "মাছাঃ" to "মোছাঃ",
            "মোছাঃ" to "মোছাঃ",
            "মােছাঃ" to "মোছাঃ",
            "মােঃ" to "মোঃ",
            "জাস্তাতুল" to "জান্নাতুল",
            "জাস্তাতুন" to "জান্নাতুন",
            "ফেরদৌসী" to "ফেরদৌসী"
        )

        // ২. ফন্টের ভাঙা ম্যাজিক ক্যারেক্টার ফিক্স
        val magicMap = mapOf(
            "Ï" to "ে", "Ĕ" to "ত্র", "ĥ" to "ন্ম", "Ŕ" to "শ্চ", "ƀ" to "সু",
            "ĺ" to "ব্দ", "×" to "ক্ত", "õ" to "জ্জ", "Ë" to "্য", "ধÎ" to "ঞ",
            "Î" to "্য", "Ó" to "ও", "Ô" to "ও", "Ř" to "শ্র", "ă" to "ড্র",
            "ƃ" to "দু", "ſ" to "নু", "Ō" to "ন্ন", "Ƅ" to "শু", "ƣ" to "কু"
        )

        // ডিকশনারি অনুযায়ী ভুল শব্দগুলো সঠিক শব্দ দিয়ে পরিবর্তন করা হচ্ছে
        for ((bad, good) in wordFixes) {
            text = text!!.replace(bad, good)
        }

        for ((bad, good) in magicMap) {
            text = text!!.replace(bad, good)
        }

        // ৩. বাংলা 'কার' চিহ্নের পজিশন ঠিক করা
        text = text!!.replace(Regex("ি((?:[ক-য়ড়ঢ়ৎংঃঁ]্)*[ক-য়ড়ঢ়ৎংঃঁ])"), "$1ি")
        text = text!!.replace(Regex("ে((?:[ক-য়ড়ঢ়ৎংঃঁ]্)*[ক-য়ড়ঢ়ৎংঃঁ])"), "$1ে")
        text = text!!.replace(Regex("([ক-য়ড়ঢ়ৎংঃঁ])ে\\s*া"), "$1ো")

        return text!!.trim()
    }
}