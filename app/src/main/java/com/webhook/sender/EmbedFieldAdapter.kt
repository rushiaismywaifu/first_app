package com.webhook.sender

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.CheckBox
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView

class EmbedFieldAdapter(
    private val fields: MutableList<FieldEntry>
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
        holder.etFieldName.setText(field.name)
        holder.etFieldValue.setText(field.value)
        holder.cbInline.isChecked = field.inline

        holder.etFieldName.setOnFocusChangeListener { _, _ ->
            fields[holder.adapterPosition].name = holder.etFieldName.text.toString()
        }
        holder.etFieldValue.setOnFocusChangeListener { _, _ ->
            fields[holder.adapterPosition].value = holder.etFieldValue.text.toString()
        }
        holder.cbInline.setOnCheckedChangeListener { _, isChecked ->
            if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                fields[holder.adapterPosition].inline = isChecked
            }
        }
        holder.btnRemove.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                fields.removeAt(pos)
                notifyItemRemoved(pos)
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
        for (i in 0 until fields.size) {
            val holder = recyclerView.findViewHolderForAdapterPosition(i) as? FieldViewHolder
            holder?.let {
                fields[i].name = it.etFieldName.text.toString()
                fields[i].value = it.etFieldValue.text.toString()
                fields[i].inline = it.cbInline.isChecked
            }
        }
    }
}
