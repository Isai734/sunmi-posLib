package com.pagatodo.sunmi.poslib.harmonizer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.pagatodo.sigmalib.transacciones.AbstractTransaccion
import com.pagatodo.sigmalib.transacciones.TransaccionFactory
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.harmonizer.db.Sync
import com.pagatodo.sunmi.poslib.harmonizer.db.SyncDao
import com.pagatodo.sunmi.poslib.harmonizer.db.SyncDatabase
import com.pagatodo.sunmi.poslib.model.SyncData
import com.pagatodo.sunmi.poslib.posInstance
import com.pagatodo.sunmi.poslib.util.StatusTrx
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import net.fullcarga.android.api.oper.TipoOperacion
import java.util.*

class HarmonizerService(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val NOTIFICATION_ID = 102
    private val waitTime = 2 * 60 * 1000L
    var syncDao: SyncDao = SyncDatabase.getDatabase(appContext).databaseDao()

    private val TAG = "SaleWmanager.LOG"
    override suspend fun doWork(): Result {
        val datetime = inputData.getLong(KEY_INPUT_TIME, 0)
        val dataSync = inputData.getString(KEY_INPUT_TIME)
        var result: Result = Result.success()
        return try {
            setForeground(createForegroundInfo())
            val sync = syncDao.getByDate(Date(datetime))
            val status = sync.status ?: StatusTrx.PROGRESS.name
            while (status == StatusTrx.PROGRESS.name && Date().time.minus(datetime) < waitTime)
                delay(2000L)// 1 Segundo
            if (status == StatusTrx.PROGRESS.name) {
                doSync(sync) { result = Result.success() }
                result
            } else {
                createStaticNotification("Venta Realizada con Exito.")
                Result.success()
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message!!)
            Result.failure()
        }
    }

    private fun doSync(sync: Sync?, result: (Result) -> Unit) {

        try {
            sync ?: result(Result.failure())
            val syncData =
                Moshi.Builder().build().adapter(SyncData::class.java).fromJson(sync?.data!!)
            syncData ?: result(Result.failure())

            TransaccionFactory.crearTransacion<AbstractTransaccion>(
                TipoOperacion.PCI_SINCRONIZACION,
                { response ->
                    if (response.isCorrecta || response.operacionSiguiente.mtiNext != null) {
                        createStaticNotification("Venta Cancelada")
                        result(Result.success())
                    } else
                        result(Result.failure())
                },
                { error ->
                    result(Result.failure())
                }
            ).withProcod(syncData?.product)
                .withFields(syncData?.params)
                .withStan(syncData?.stan)
                .withDatosOpTarjeta(syncData?.dataCard)
                .withUser(posInstance().user)
                .realizarOperacion()

        } catch (e: Exception) {
            result(Result.failure())
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID,
            createServiceNotification("Venta", "Venta en proceso.")
        )
    }

    private fun createServiceNotification(title: String, description: String): Notification {

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel("101", "channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        return NotificationCompat.Builder(applicationContext, "101")
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(R.drawable.icono_exitoso)
            .setProgress(0, 0, true)
            .build()
    }

    private fun createStaticNotification(message: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(NOTIFICATION_ID.toString(), "channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_ID.toString())
            .setSmallIcon(R.drawable.ic_icon_alerta_error)
            .setContentTitle("Información de Venta")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(256244, builder.build())
    }

    companion object {
        const val KEY_INPUT_TIME = "KEY_INPUT_TIME"
        const val KEY_INPUT_DATA = "KEY_INPUT_DATA"
    }
}