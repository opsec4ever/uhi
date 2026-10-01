package com.opsec4ever.questledtest

import android.content.Context
import android.hardware.lights.LightState
import android.hardware.lights.LightsManager
import android.hardware.lights.LightsRequest
import android.os.Process

class LedUserService : ILedService.Stub() {
    private var context: Context? = null
    private var session: LightsManager.LightsSession? = null
    constructor()
    constructor(context: Context) { this.context = context }

    override fun getUid() = Process.myUid()

    override fun setColor(color: Int): String = try {
        val ctx = context ?: return "no context"
        val manager = ctx.getSystemService(LightsManager::class.java) ?: return "LightsManager unavailable"
        val light = manager.lights.firstOrNull { it.id == 1 } ?: return "light 1 not found"
        if (session == null) session = manager.openSession()
        session!!.requestLights(LightsRequest.Builder().addLight(light, LightState.Builder().setColor(color).build()).build())
        "ok uid=${Process.myUid()} light=${light.id} color=${String.format("%08X", color)}"
    } catch (t: Throwable) {
        "${t.javaClass.name}: ${t.message}"
    }

    override fun clear(): String = try {
        val ctx = context ?: return "no context"
        val manager = ctx.getSystemService(LightsManager::class.java) ?: return "LightsManager unavailable"
        val light = manager.lights.firstOrNull { it.id == 1 } ?: return "light 1 not found"
        val current = session ?: return "no active session"
        current.requestLights(LightsRequest.Builder().clearLight(light).build())
        current.close()
        session = null
        "ok"
    } catch (t: Throwable) {
        "${t.javaClass.name}: ${t.message}"
    }

    override fun destroy() {
        try { session?.close() } catch (_: Throwable) {}
        session = null
        System.exit(0)
    }
}
