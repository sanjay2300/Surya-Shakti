package com.example.myapplication

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ReportFragment : Fragment() {

    private lateinit var vm: EnergyViewModel
    private lateinit var adapter: EnergyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(requireActivity())[EnergyViewModel::class.java]

        val totalSavingsText = view.findViewById<TextView>(R.id.totalSavings)
        val totalGenText = view.findViewById<TextView>(R.id.totalGen)
        val independenceScoreText = view.findViewById<TextView>(R.id.independenceScore)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        adapter = EnergyAdapter(
            records = emptyList(),
            onEdit = { record -> showEditDialog(record) },
            onDelete = { record -> showDeleteConfirmation(record) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        vm.allRecords.observe(viewLifecycleOwner) { records ->
            adapter.updateData(records)
            
            val totalGen = records.sumOf { it.generation }
            val totalCon = records.sumOf { it.consumption }
            val totalSavings = records.sumOf { it.savings }
            
            totalSavingsText.text = "Total Saved: ₹${"%.2f".format(totalSavings)}"
            totalGenText.text = "Total Generation: ${"%.1f".format(totalGen)} kWh"
            
            val score = if (totalCon > 0) (minOf(totalGen, totalCon) / totalCon * 100).toInt() else 0
            independenceScoreText.text = "Green Independence: $score%"
        }
    }

    private fun showEditDialog(record: EnergyRecord) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Edit Energy Log")
        
        val view = layoutInflater.inflate(R.layout.dialog_edit_energy, null)
        val editGen = view.findViewById<EditText>(R.id.editGen)
        val editCon = view.findViewById<EditText>(R.id.editCon)
        
        // Note: record.generation is simulated. For editing, we might want to show original or just let user adjust simulated.
        // Let's assume user adjusts simulated value for simplicity, or we store original. 
        // Based on current schema, we only have 'generation' which is simulated.
        editGen.setText(record.generation.toString())
        editCon.setText(record.consumption.toString())
        
        builder.setView(view)
        builder.setPositiveButton("Update") { _, _ ->
            val newGen = editGen.text.toString().toDoubleOrNull() ?: record.generation
            val newCon = editCon.text.toString().toDoubleOrNull() ?: record.consumption
            
            val updatedRecord = record.copy(
                generation = newGen,
                consumption = newCon,
                savings = newGen * 5.0, // Assuming unit rate 5.0
                exported = if (newGen > newCon) newGen - newCon else 0.0
            )
            vm.updateRecord(updatedRecord)
            Toast.makeText(context, "Log updated!", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showDeleteConfirmation(record: EnergyRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Log")
            .setMessage("Are you sure you want to delete this energy log?")
            .setPositiveButton("Delete") { _, _ ->
                vm.deleteRecord(record)
                Toast.makeText(context, "Log deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}