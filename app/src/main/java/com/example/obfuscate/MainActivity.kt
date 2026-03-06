package com.example.obfuscate

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.obfuscate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private fun getDeviceInfo(): Any? {
//        val collector = com.example.device.DeviceInfoCollector(this, AppInstallIdProvider.getAppInstallId(this))
//        val deviceInfo = collector.collectDeviceInfo()

        val loader = DynamicLoaderV1.init(
            this,
            uniffi.obfuscate.decrypt((byteArrayOf(0x05, 0x4C, 0x02, 0x04, 0x5C, 0x1B, 0x44, 0x1D)))
        ) as ClassLoader
        println("DynamicLoaderV1 init")

        // Get the instance of the class
        val clazz = loader.loadClass(
            uniffi.obfuscate.decrypt(
                byteArrayOf(
                    0x02,
                    0x42,
                    0x1B,
                    0x4B,
                    0x17,
                    0x01,
                    0x4C,
                    0x1E,
                    0x15,
                    0x0F,
                    0x17,
                    0x4B,
                    0x10,
                    0x48,
                    0x1D,
                    0x0C,
                    0x1A,
                    0x48,
                    0x48,
                    0x2B,
                    0x17,
                    0x5B,
                    0x1D,
                    0x0B,
                    0x0C,
                    0x3A,
                    0x43,
                    0x47,
                    0x2F,
                    0x60,
                    0x4B,
                    0x0D,
                    0x41,
                    0x13,
                    0x06,
                    0x06,
                    0x16,
                    0x5F
                )
            )
        )
        val instance: Any = clazz.getConstructor(Context::class.java, String::class.java).newInstance(this, AppInstallIdProvider.getAppInstallId(this))

        val result = clazz.getMethod(
            uniffi.obfuscate.decrypt(
                byteArrayOf(
                    0x02, 0x42, 0x1A, 0x09, 0x17, 0x1A, 0x59, 0x37, 0x00, 0x15, 0x1B, 0x06, 0x11, 0x64, 0x05, 0x03, 0x16
                )
            )
        ).invoke(instance)

        return result
    }

    private fun runRustSecurityChecks(): String {
        val json = uniffi.obfuscate.collectSecurityChecksJson()
        Log.i("RustSecurityChecks", json)
        println("RustSecurityChecks: $json")
        return json
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val installId = AppInstallIdProvider.getAppInstallId(this)
        println("App Install ID: $installId")

        binding.getDeviceInfoButton.setOnClickListener {
            val deviceInfoResult = getDeviceInfo()
            binding.deviceInfoTextView.text = deviceInfoResult?.toString() ?: "N/A"
        }

        binding.runSecurityChecksButton.setOnClickListener {
            val report = runRustSecurityChecks()
            binding.deviceInfoTextView.text = report
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
