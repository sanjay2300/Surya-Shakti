package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController

class DashboardFragment : Fragment() {

    private lateinit var vm: EnergyViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(requireActivity())[EnergyViewModel::class.java]

        val weatherSpinner = view.findViewById<Spinner>(R.id.weather)
        val genInput = view.findViewById<EditText>(R.id.gen)
        val conInput = view.findViewById<EditText>(R.id.con)
        val btn = view.findViewById<Button>(R.id.btn)
        val resultText = view.findViewById<TextView>(R.id.result)
        val percentText = view.findViewById<TextView>(R.id.percent)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress)
        val btnHistory = view.findViewById<Button>(R.id.btn_history)

        val weatherOptions = listOf("Sunny", "Cloudy", "Rainy")
        weatherSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, weatherOptions)

        // Observe Today's Record to show progress bar correctly
        vm.todayRecord.observe(viewLifecycleOwner) { record ->
            if (record != null) {
                val (percent, msg) = vm.getAnalysis(record.generation, record.consumption)
                resultText.text = "Today: $msg"
                percentText.text = "$percent%"
                progressBar.progress = percent
                
                // Optional: Disable input or show it's already logged
                btn.text = "Update Today's Log" 
            } else {
                resultText.text = "No logs for today yet."
                percentText.text = "0%"
                progressBar.progress = 0
                btn.text = "Log Today's Energy"
            }
        }

        btn.setOnClickListener {
            val g = genInput.text.toString().toDoubleOrNull() ?: 0.0
            val c = conInput.text.toString().toDoubleOrNull() ?: 0.0
            val weather = weatherSpinner.selectedItem.toString()

            if (g < 0 || c < 0) {
                Toast.makeText(requireContext(), "Please enter valid energy values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentRecord = vm.todayRecord.value
            if (currentRecord != null) {
                // Update existing record
                val simulatedGen = vm.simulate(g, weather)
                val updated = currentRecord.copy(
                    generation = simulatedGen,
                    consumption = c,
                    weather = weather,
                    savings = simulatedGen * 5.0,
                    exported = if (simulatedGen > c) simulatedGen - c else 0.0
                )
                vm.updateRecord(updated)
                Toast.makeText(requireContext(), "Today's log updated!", Toast.LENGTH_SHORT).show()
            } else {
                // Save new record
                vm.calculateAndSave(g, c, weather) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Log saved!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            // Notification logic for both new and update
            val simGen = vm.simulate(g, weather)
            if (weather == "Sunny") showNotification("Peak Solar Sun! ☀️ Perfect time for heavy appliances.")
        }

        btnHistory.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_reportFragment)
        }
    }

    private fun showNotification(message: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val builder = NotificationCompat.Builder(requireContext(), "energy_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Surya-Shakti Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(requireContext()).notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {}
    }
}