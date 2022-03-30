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
import androidx.work.workDataOf
import com.pagatodo.sigmalib.transacciones.AbstractTransaccion
import com.pagatodo.sigmalib.transacciones.TransaccionFactory
import com.pagatodo.sunmi.poslib.PosLib
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.harmonizer.db.Sync
import com.pagatodo.sunmi.poslib.harmonizer.db.SyncDao
import com.pagatodo.sunmi.poslib.harmonizer.db.SyncDatabase
import com.pagatodo.sunmi.poslib.model.SyncData
import com.pagatodo.sunmi.poslib.posInstance
import com.pagatodo.sunmi.poslib.util.*
import com.pagatodo.sunmi.poslib.view.AbstractEmvFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import net.fullcarga.android.api.data.respuesta.RespuestaTrxCierreTurno
import net.fullcarga.android.api.oper.TipoOperacion

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
        return try {
            setForeground(createForegroundInfo())
            val status = syncObject?.status ?: StatusTrx.PROGRESS.name
            if (status == StatusTrx.PROGRESS.name) {
                doSyncIO(syncObject)
            } else {
                Result.success(workDataOf(KEY_MESSAGE to "Estado de Transacción $status"))
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message!!)
            Result.failure()
        }
    }

    private suspend fun doSyncIO(sync: Sync?): Result {
        var result = Result.failure()
        return withContext(Dispatchers.IO){
            sync ?: run { result = Result.failure(workDataOf(KEY_MESSAGE to "Operación Sincronización no encontrada.")) }
            val syncData = MoshiInstance.create().adapter(SyncData::class.java).fromJson(sync?.data!!)
            syncData ?: run { result = Result.failure(workDataOf(KEY_MESSAGE to "No se puede parsear datos de objeto Sync.")) }
            Log.d(TAG, "syncData $syncData")
            TransaccionFactory.crearTransacion<AbstractTransaccion>( TipoOperacion.PCI_SINCRONIZACION,
                { response ->
                    result = if (response.isCorrecta) {
                        createStaticNotification("Venta Cancelada")
                        syncDao.deleteByDate(sync.dateTime)
                        if (response is RespuestaTrxCierreTurno) {
                            PosLogger.d(TAG, "response.isCorrecta ${response.isCorrecta}")
                            val resp = MoshiInstance.create().adapter(SyncData::class.java).toJson(syncData)
                            LazyStore.response = response
                            Result.success(workDataOf(KEY_MESSAGE to SyncState.WithTrx.name, KEY_RESPONSE_MSG to resp))
                        } else {
                            Result.success(workDataOf(KEY_MESSAGE to SyncState.SuccessEmpty.name, KEY_RESPONSE_MSG to response.msjError))
                        }
                    } else  if(response.msjError.trim() == "La operacion esta anulada") {
                        syncDao.deleteByDate(sync.dateTime)
                        Result.failure(workDataOf(KEY_MESSAGE to response.msjError))
                    } else
                        Result.failure(workDataOf(KEY_MESSAGE to response.msjError))
                },
                { error ->
                    Log.d(TAG, "error ${error.message}")
                    result = Result.failure(workDataOf(KEY_MESSAGE to error.message))
                }
            ).withProcod(syncData?.product)
                .withFields(syncData?.params)
                .withStan(syncData?.stan)
                .withDatosOpTarjeta(AbstractEmvFragment.createDataOpTarjeta(syncData?.dataCard, syncData?.transactionData))
                .withUser(posInstance().user)
                .realizarOperacion()
            result
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
        const val KEY_RESPONSE_MSG = "KEY_RESPONSE_MSG"
        const val KEY_MESSAGE = "KEY_MESSAGE"
        const val KEY_INPUT_DATA = "KEY_INPUT_DATA"
    }
}

enum class SyncState{
    WithTrx,
    SuccessEmpty,
    Error
}