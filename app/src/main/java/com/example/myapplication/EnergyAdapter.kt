package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class EnergyAdapter(
    private var records: List<EnergyRecord>,
    private val onEdit: (EnergyRecord) -> Unit,
    private val onDelete: (EnergyRecord) -> Unit
) : RecyclerView.Adapter<EnergyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.dateText)
        val weatherText: TextView = view.findViewById(R.id.weatherText)
        val genText: TextView = view.findViewById(R.id.genText)
        val savingsText: TextView = view.findViewById(R.id.savingsText)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_energy_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.dateText.text = sdf.format(Date(record.date))
        holder.weatherText.text = record.weather
        holder.genText.text = "${"%.1f".format(record.generation)} kWh"
        holder.savingsText.text = "₹${"%.1f".format(record.savings)}"
        
        holder.btnEdit.setOnClickListener { onEdit(record) }
        holder.btnDelete.setOnClickListener { onDelete(record) }
    }

    override fun getItemCount() = records.size

    fun updateData(newRecords: List<EnergyRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }
}