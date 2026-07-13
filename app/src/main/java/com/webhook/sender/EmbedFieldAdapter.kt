package com.webhook.sender

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView

class EmbedFieldAdapter(
    val fields: MutableList<FieldEntry> = mutableListOf()
) : RecyclerView.Adapter<EmbedFieldAdapter.FieldViewHolder>() {

    data class FieldEntry(
        var name: String = "",
        var value: String = "",
        var inline: Boolean = false
    )

    inner class FieldViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val etFieldName: EditText = itemView.findViewById(R.id.etFieldName)
        val etFieldValue: EditText = itemView.findViewById(R.id.etFieldValue)
        val cbInline: CheckBox = itemView.findViewById(R.id.cbInline)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemoveField)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_embed_field, parent, false)
        return FieldViewHolder(view)
    }

    override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
        val field = fields[position]

        // Remove old watchers during recycling to avoid unwanted callbacks
        (holder.etFieldName.tag as? TextWatcher)?.let { holder.etFieldName.removeTextChangedListener(it) }
        (holder.etFieldValue.tag as? TextWatcher)?.let { holder.etFieldValue.removeTextChangedListener(it) }
        holder.cbInline.setOnCheckedChangeListener(null)

        holder.etFieldName.setText(field.name)
        holder.etFieldValue.setText(field.value)
        holder.cbInline.isChecked = field.inline

        // Create new real-time watchers that update fields instantly as the user types
        val nameWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    fields[pos].name = s?.toString() ?: ""
                }
            }
        }
        val valueWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    fields[pos].value = s?.toString() ?: ""
                }
            }
        }

        holder.etFieldName.tag = nameWatcher
        holder.etFieldValue.tag = valueWatcher
        holder.etFieldName.addTextChangedListener(nameWatcher)
        holder.etFieldValue.addTextChangedListener(valueWatcher)

        holder.cbInline.setOnCheckedChangeListener { _, isChecked ->
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                fields[pos].inline = isChecked
            }
        }

        holder.btnRemove.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < fields.size) {
                fields.removeAt(pos)
                notifyItemRemoved(pos)
                notifyItemRangeChanged(pos, fields.size - pos)
            }
        }
    }

    override fun getItemCount() = fields.size

    fun addField() {
        fields.add(FieldEntry())
        notifyItemInserted(fields.size - 1)
    }

    fun getFields(): List<FieldEntry> = fields.toList()

    fun updateFromViews(recyclerView: RecyclerView) {
        // With TextWatcher attached, `fields` is always synced in real-time even when scrolled off-screen.
        // We perform a supplementary check on currently attached views just in case.
        for (i in 0 until fields.size) {
            val holder = recyclerView.findViewHolderForAdapterPosition(i) as? FieldViewHolder
            holder?.let {
                val nameText = it.etFieldName.text?.toString() ?: ""
                val valueText = it.etFieldValue.text?.toString() ?: ""
                fields[i].name = nameText
                fields[i].value = valueText
                fields[i].inline = it.cbInline.isChecked
            }
        }
    }
}
