package com.paketcast.analytics.data.opml

import com.paketcast.analytics.data.model.OpmlSubscription
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

/**
 * Parst den OPML-Export von Pocket Casts (Einstellungen -> Import & Export).
 * Jeder Podcast ist dort ein <outline>-Element mit xmlUrl-Attribut.
 */
object OpmlParser {

    fun parse(input: InputStream): List<OpmlSubscription> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(input, "UTF-8")
        val result = mutableListOf<OpmlSubscription>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.equals("outline", ignoreCase = true)) {
                val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                val text = parser.getAttributeValue(null, "text")
                    ?: parser.getAttributeValue(null, "title")
                if (xmlUrl != null) {
                    result.add(OpmlSubscription(title = text ?: xmlUrl, feedUrl = xmlUrl))
                }
            }
            event = parser.next()
        }
        return result
    }
}
