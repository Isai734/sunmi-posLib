package com.pagatodo.sunmi.poslib.harmonizer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.pagatodo.sigmalib.ApiData
import com.pagatodo.sigmalib.transacciones.AbstractTransaccion
import com.pagatodo.sigmalib.transacciones.TransaccionFactory
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.harmonizer.db.Sync
import com.pagatodo.sunmi.poslib.harmonizer.db.SyncDao
import com.pagatodo.sunmi.poslib.harmonizer.db.SyncDatabase
import com.pagatodo.sunmi.poslib.model.SyncData
import com.pagatodo.sunmi.poslib.posInstance
import com.pagatodo.sunmi.poslib.util.MoshiInstance
import com.pagatodo.sunmi.poslib.util.StatusTrx
import com.pagatodo.sunmi.poslib.view.AbstractEmvFragment
import com.pagatodo.sunmi.poslib.view.BigDecimalAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import net.fullcarga.android.api.oper.TipoOperacion
import java.util.*

class SyncService(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val NOTIFICATION_ID = 102
    var syncDao: SyncDao = SyncDatabase.getDatabase(appContext).databaseDao()

    private val TAG = "SyncService.LOG"
    override suspend fun doWork(): Result {
        val sync = inputData.getString(KEY_INPUT_DATA) ?: ""
        Log.d(TAG, "sync $sync")
        val syncObject = MoshiInstance.create().adapter(Sync::class.java).fromJson(sync)
        Log.d(TAG, "syncObject $syncObject")
        var result: Result = Result.success()
        return try {
            setForeground(createForegroundInfo())
            val status = syncObject?.status ?: StatusTrx.PROGRESS.name
            if (status == StatusTrx.PROGRESS.name) {
                doSync(syncObject) { result = it }
                if(result == Result.success()) syncDao.deleteByDate(syncObject?.dateTime)
                result
            } else {
                Result.success(workDataOf(KEY_MESSAGE to "Estado de Transacción $status"))
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message!!)
            Result.failure()
        }
    }

    private fun doSync(sync: Sync?, result: (Result) -> Unit) {

        try {
            sync ?: result(Result.failure(workDataOf(KEY_MESSAGE to "Operación Sincronización no encontrada.")))
            val syncData = MoshiInstance.create().adapter(SyncData::class.java).fromJson(sync?.data!!)
            syncData ?: result(Result.failure(workDataOf(KEY_MESSAGE to "No se puede parsear datos de objeto Sync.")))
            Log.d(TAG, "syncData $syncData")
            TransaccionFactory.crearTransacion<AbstractTransaccion>(
                TipoOperacion.PCI_SINCRONIZACION,
                { response ->
                    if (response.isCorrecta || response.operacionSiguiente.mtiNext != null) {
                        createStaticNotification("Venta Cancelada")
                        Log.d(TAG, "response.isCorrecta ${response.isCorrecta}")
                        result(Result.success(workDataOf(KEY_MESSAGE to "Transacción Cancelada")))
                    } else
                        result(Result.failure(workDataOf(KEY_MESSAGE to response.msjError)))
                },
                { error ->
                    Log.d(TAG, "error ${error.message}")
                    result(Result.failure(workDataOf(KEY_MESSAGE to error.message)))
                }
            ).withProcod(syncData?.product)
                .withFields(syncData?.params)
                .withStan(ApiData.APIDATA.datosSesion.datosTPV.stanProvider.ultimo)
                .withDatosOpTarjeta(AbstractEmvFragment.createDataOpTarjeta(syncData?.dataCard, syncData?.transactionData))
                .withUser(posInstance().user)
                .realizarOperacion()

        } catch (e: Exception) { e.printStackTrace()
            Log.d(TAG, "catch Exception ${e.message}")
            result(Result.failure(workDataOf(KEY_MESSAGE to e.message)))
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID,
            createServiceNotification("Venta", "Se está cancelando la venta.")
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
                NotificationChannel(
                    NOTIFICATION_ID.toString(),
                    "channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_ID.toString())
            .setSmallIcon(R.drawable.ic_icon_alerta_error)
            .setContentTitle("Venta")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(256244, builder.build())
    }

    companion object {
        const val KEY_INPUT_TIME = "KEY_INPUT_TIME"
        const val KEY_MESSAGE = "KEY_MESSAGE"
        const val KEY_INPUT_DATA = "KEY_INPUT_DATA"
    }
}