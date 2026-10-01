package com.opsec4ever.questledtest

import android.app.Activity
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private lateinit var status: TextView
    private var service: ILedService? = null

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        if (result == android.content.pm.PackageManager.PERMISSION_GRANTED) bind()
        else status.text = "Shizuku permission denied"
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = ILedService.Stub.asInterface(binder)
            status.text = "connected uid=${service?.getUid()}"
        }
        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            status.text = "service disconnected"
        }
    }

    private val args = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, LedUserService::class.java.name)
    ).daemon(false).processNameSuffix("led").debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(40, 40, 40, 40)
        status = TextView(this)
        root.addView(status)
        fun button(label: String, action: () -> Unit) {
            Button(this).apply {
                text = label
                setOnClickListener { action() }
                root.addView(this)
            }
        }
        button("CONNECT") { connect() }
        button("RED") { setColor(0xFFFF0000.toInt()) }
        button("GREEN") { setColor(0xFF00FF00.toInt()) }
        button("BLUE") { setColor(0xFF0000FF.toInt()) }
        button("WHITE") { setColor(0xFFFFFFFF.toInt()) }
        button("OFF") { clear() }
        setContentView(root)
        status.text = if (Shizuku.pingBinder()) "Shizuku connected" else "Shizuku not connected"
    }

    private fun connect() {
        if (!Shizuku.pingBinder()) { status.text = "Shizuku not connected"; return }
        try {
            if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED)
                Shizuku.requestPermission(100)
            else bind()
        } catch (t: Throwable) { status.text = "${t.javaClass.name}: ${t.message}" }
    }

    private fun bind() {
        try {
            Shizuku.bindUserService(args, connection)
            status.text = "binding..."
        } catch (t: Throwable) { status.text = "${t.javaClass.name}: ${t.message}" }
    }

    private fun setColor(color: Int) {
        val s = service ?: run { status.text = "not connected"; return }
        Thread {
            val result = try { s.setColor(color) } catch (t: Throwable) { "${t.javaClass.name}: ${t.message}" }
            runOnUiThread { status.text = result }
        }.start()
    }

    private fun clear() {
        val s = service ?: run { status.text = "not connected"; return }
        Thread {
            val result = try { s.clear() } catch (t: Throwable) { "${t.javaClass.name}: ${t.message}" }
            runOnUiThread { status.text = result }
        }.start()
    }

    override fun onDestroy() {
        try { Shizuku.unbindUserService(args, connection, true) } catch (_: Throwable) {}
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        super.onDestroy()
    }
}
