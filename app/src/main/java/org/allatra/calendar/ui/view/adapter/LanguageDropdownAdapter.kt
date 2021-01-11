package org.allatra.calendar.ui.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.allatra.calendar.R


class LanguageDropdownAdapter(
    context: Context, textViewResourceId: Int,
    private val objects: Array<String>
): ArrayAdapter<String>(context, textViewResourceId, objects) {

    private fun getCustomLanguagesView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val layout: View = inflater.inflate(R.layout.spinner_dialog_item, parent, false)
        val tvLanguage = layout.findViewById<TextView>(R.id.tv_language)
        val tvLangCode = layout.findViewById<TextView>(R.id.tv_lang_code)

        tvLangCode.text = objects[position]
        tvLanguage.text = getLanguage(objects[position])

        return layout
    }

    private fun getSpinnerView(position: Int, convertView: View?, parent: ViewGroup): View  {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val layout: View = inflater.inflate(R.layout.spinner_dropdown_item, parent, false)
        val tvLangCode = layout.findViewById<TextView>(R.id.tv_lang_code_drop)
        tvLangCode.text = objects[position]

        return layout
    }

    private fun getLanguage(langCode: String): String {
        return when (langCode) {
            "cs" -> "Česky"
            "ru" -> "Русский"
            "en" -> "English"
            "de" -> "Deutsch"

            else -> "Unkown"
        }
    }

    // It gets a View that displays in the drop down popup the data at the specified position
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomLanguagesView(position, convertView, parent)
    }

    // It gets a View that displays the data at the specified position
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getSpinnerView(position, convertView, parent)
    }
}