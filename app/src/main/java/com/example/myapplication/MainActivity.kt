package com.example.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private var permisosOk = false

    private val permisos = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    private val lanzador = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {resultados ->
        permisosOk = resultados.values.all { it }
        if (permisosOk){
            Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Sin Permisos no hay conexion", Toast.LENGTH_SHORT).show()
        }
    }

    private val adapter: BluetoothAdapter by lazy {
        (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"
    )
    private lateinit var textViewMensaje: TextView

    @SuppressLint("MissingPermission")
    private fun iniciarComoServidor() {
        Toast.makeText(this, "Esperando conexión...", Toast.LENGTH_SHORT).show()
        val serverSocket = adapter.listenUsingRfcommWithServiceRecord(
            "MiAppIoT", uuid
        )
        Thread {
            val socket = serverSocket.accept()
            runOnUiThread {
                Toast.makeText(this, "¡Cliente conectado!", Toast.LENGTH_SHORT).show()
            }
            manejarConexion(socket)
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun iniciarComoCliente() {
        Toast.makeText(this, "Buscando servidor...", Toast.LENGTH_SHORT).show()
        val dispositivo = adapter.bondedDevices
            .firstOrNull { it.name == "motorola_moto_gO4s" }
        if (dispositivo == null) {
            Toast.makeText(this, "No encontré ese dispositivo emparejado", Toast.LENGTH_SHORT).show()
            return
        }
        val socket = dispositivo.createRfcommSocketToServiceRecord(uuid)
        Thread {
            adapter.cancelDiscovery()
            socket.connect()
            runOnUiThread {
                Toast.makeText(this, "¡Conectado al servidor!", Toast.LENGTH_SHORT).show()
            }
            manejarConexion(socket)
        }.start()
    }


    @SuppressLint("MissingPermission")
    private fun manejarConexion(socket: BluetoothSocket) {
        val salida = socket.outputStream
        val entrada = socket.inputStream

        salida.write("ENCENDER_LED".toByteArray())

        val buffer = ByteArray(1024)
        while (true) {
            val bytes = entrada.read(buffer)
            val mensaje = String(buffer, 0, bytes)
            runOnUiThread { textViewMensaje.text = mensaje }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        textViewMensaje = findViewById(R.id.textViewMensaje)
        val btnServidor = findViewById<Button>(R.id.btnServidor)
        val btnCliente = findViewById<Button>(R.id.btnCliente)
        val btnContador = findViewById<Button>(R.id.btnContador)


        lanzador.launch(permisos)

        lanzador.launch(permisos)

        btnServidor.setOnClickListener {
            if (permisosOk) iniciarComoServidor()
        }
        btnCliente.setOnClickListener {
            if (permisosOk) iniciarComoCliente()
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

}