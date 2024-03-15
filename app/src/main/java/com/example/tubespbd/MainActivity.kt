package com.example.tubespbd

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.tubespbd.databinding.ActivityMainBinding
import android.location.LocationManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.tubespbd.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationManager: LocationManager
    private lateinit var transactionManager: TransactionManager
    private lateinit var appDatabase: AppDatabase
    private val transactionRepository by lazy { TransactionRepository(appDatabase.transactionDao()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Room Database
        appDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "transaction.db"
        )
            .fallbackToDestructiveMigration()
            .build()

        // Routing
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_scan, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        checkAndRequestLocationPermissions()
    }

    private fun initializeAfterPermissionsGranted() {
        // Initialize TransactionManager after permissions are granted
        transactionManager = TransactionManager(this, locationManager)

        performDatabaseOperations()
    }

    private fun getLocationString(): String {
        return transactionManager.getLocationString()
    }

    private fun performDatabaseOperations() = CoroutineScope(Dispatchers.IO).launch {
        val locationString = if (hasLocationPermissions() && isLocationEnabled()) {
            getLocationString()
        } else {
            "none"
        }

        // Insert a transaction
        val transaction = Transaction(title = "Mi Ayam", category = "Pembelian", amount = 15000f, location = locationString, tanggal = "2023-02-01 12:00:00")
        val transactionId = transactionRepository.insertTransaction(transaction)
        println("Inserted transaction with ID: $transactionId")

        // Retrieve all transactions
        val transactions = transactionRepository.getAllTransactions()
        transactions.forEach { transaction ->
            println("Transaction ID: ${transaction.id}, Title: ${transaction.title}, Category: ${transaction.category}, Amount: ${transaction.amount}, Location: ${transaction.location}, Date: ${transaction.tanggal}")
        }

        // Update last transaction
        val lastTransaction = transactions.last()
        val updatedTransaction = lastTransaction.copy(title = "Nasi Goreng")
        transactionRepository.updateTransaction(updatedTransaction)
        println("Updated transaction with ID: ${updatedTransaction.id}")

        // Retrieve all transactions after update
        val afterUpdateTransactions = transactionRepository.getAllTransactions()
        afterUpdateTransactions.forEach { transaction ->
            println("Transaction ID: ${transaction.id}, Title: ${transaction.title}, Category: ${transaction.category}, Amount: ${transaction.amount}, Location: ${transaction.location}, Date: ${transaction.tanggal}")
        }

        // Delete the last transaction
        transactionRepository.deleteTransaction(lastTransaction)
        println("Deleted transaction with ID: ${lastTransaction.id}")
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun hasLocationPermissions() = ActivityCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
    }

    private fun checkAndRequestLocationPermissions() {
        if (!hasLocationPermissions()) {
            requestLocationPermissions()
        } else {
            initializeAfterPermissionsGranted()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            initializeAfterPermissionsGranted()
        } else {
            println("Location permission was denied by the user.")
        }
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 100
    }
}