package com.siaa.app

import android.content.Context
import com.siaa.core.data.ContentPackManager
import org.json.JSONObject

data class SpeakingActivity(val id:String,val cefr:String,val promptEs:String,val targetEn:String,val kcIds:List<String>,val minSimilarity:Double)
data class ReadingActivity(val id:String,val cefr:String,val passage:String,val questionEs:String,val optionA:String,val optionB:String,val correctOption:String,val kcIds:List<String>)
data class WritingActivity(val id:String,val cefr:String,val promptEs:String,val referenceEn:String,val keywords:List<String>,val minWords:Int,val kcIds:List<String>)
data class PracticeCatalog(val speaking:List<SpeakingActivity>,val reading:List<ReadingActivity>,val writing:List<WritingActivity>)

class PracticeContentRepository(private val context: Context, private val contentPacks: ContentPackManager) {
    fun catalog(): PracticeCatalog = load()
    fun speaking(level:String)=pick(load().speaking, level) { it.cefr }
    fun reading(level:String)=pick(load().reading, level) { it.cefr }
    fun writing(level:String)=pick(load().writing, level) { it.cefr }
    private fun <T> pick(items:List<T>, level:String, cefr:(T)->String):List<T> {
        val exact=items.filter { cefr(it).equals(level,true) }
        return if(exact.isNotEmpty()) exact else items
    }
    private fun load(): PracticeCatalog {
        val active = contentPacks.activeRoot()?.resolve("practice/practice_catalog.json")
        val text = if (active?.isFile == true) active.readText() else context.assets.open("practice/practice_catalog.json").bufferedReader().use { it.readText() }
        val root=JSONObject(text)
        fun org.json.JSONArray.strings()=(0 until length()).map { getString(it) }
        val sp=root.getJSONArray("speaking"); val rd=root.getJSONArray("reading"); val wr=root.getJSONArray("writing")
        return PracticeCatalog(
            (0 until sp.length()).map { i -> sp.getJSONObject(i).let { o -> SpeakingActivity(o.getString("id"),o.getString("cefr"),o.getString("promptEs"),o.getString("targetEn"),o.getJSONArray("kcIds").strings(),o.optDouble("minSimilarity",0.6)) } },
            (0 until rd.length()).map { i -> rd.getJSONObject(i).let { o -> ReadingActivity(o.getString("id"),o.getString("cefr"),o.getString("passage"),o.getString("questionEs"),o.getString("optionA"),o.getString("optionB"),o.getString("correctOption"),o.getJSONArray("kcIds").strings()) } },
            (0 until wr.length()).map { i -> wr.getJSONObject(i).let { o -> WritingActivity(o.getString("id"),o.getString("cefr"),o.getString("promptEs"),o.getString("referenceEn"),o.getJSONArray("keywords").strings(),o.optInt("minWords",6),o.getJSONArray("kcIds").strings()) } }
        )
    }
}
