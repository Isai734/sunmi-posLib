package com.pagatodo.sunmi.poslib.harmonizer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pagatodo.sigmalib.transacciones.AbstractTransaccion
import com.pagatodo.sigmalib.transacciones.TransaccionFactory
import com.pagatodo.sunmi.poslib.R
import com.pagatodo.sunmi.poslib.harmonizer.db.Sync
import com.pagatodo.sunmi.poslib.harmonizer.db.SyncDao
import com.pagatodo.sunmi.poslib.harmonizer.db.SyncDatabase
import com.pagatodo.sunmi.poslib.model.SyncData
import com.pagatodo.sunmi.poslib.posInstance
import com.pagatodo.sunmi.poslib.util.LazyStore
import com.pagatodo.sunmi.poslib.util.MoshiInstance
import com.pagatodo.sunmi.poslib.util.PosLogger
import com.pagatodo.sunmi.poslib.view.AbstractEmvFragment
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import net.fullcarga.android.api.data.respuesta.RespuestaTrxCierreTurno
import net.fullcarga.android.api.oper.TipoOperacion

class SyncService(appContext: Context, workerParams: WorkerParameters) :
    RxWorker(appContext, workerParams) {

    private val NOTIFICATION_ID = 102
    var syncDao: SyncDao = SyncDatabase.getDatabase(appContext).databaseDao()

    private val TAG = "SyncService.LOG"
    override fun createWork(): Single<Result> {
        val sync = inputData.getString(KEY_INPUT_DATA) ?: ""
        return doSyncIO(sync)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.single())
            .toList()
            .map {
               it.first()
            }
    }

    private fun doSyncIO(syncString: String) = Observable.create<Result> { emitter ->
        try {
            PosLogger.d(TAG, "sync $syncString")
            val syncObject = MoshiInstance.create().adapter(Sync::class.java).fromJson(syncString)
            PosLogger.d(TAG, "syncData ${syncObject?.data}")
            val syncData = MoshiInstance.create().adapter(SyncData::class.java).fromJson(syncObject?.data!!)
            TransaccionFactory.crearTransacion<AbstractTransaccion>(TipoOperacion.PCI_SINCRONIZACION,
                { response ->
                    LazyStore.response = response
                    syncDao.deleteByDate(syncObject.dateTime)
                    when {
                        response.isCorrecta -> {
                            createStaticNotification("Venta Cancelada")
                            PosLogger.d(TAG, "response.isCorrecta ${response.isCorrecta}")
                            if(response is RespuestaTrxCierreTurno)
                                emitter.onNext(Result.success(workDataOf(KEY_MESSAGE to SyncState.WithTrx.name, KEY_RESPONSE_MSG to syncObject.data!!)))
                            else
                                emitter.onNext(Result.success(workDataOf(KEY_MESSAGE to SyncState.SuccessEmpty.name, KEY_RESPONSE_MSG to syncObject.data!!)))
                            emitter.onComplete()
                        }
                        response.msjError.trim() == "La operacion esta anulada" -> {
                            emitter.onNext(Result.success(workDataOf(KEY_MESSAGE to SyncState.SuccessEmpty.name, KEY_RESPONSE_MSG to syncObject.data!!)))
                            emitter.onComplete()
                        }
                        else -> {
                            emitter.onNext(Result.failure(workDataOf(KEY_MESSAGE to SyncState.ErrorWithResponse.name, KEY_RESPONSE_MSG to syncObject.data!!)))
                            emitter.onComplete()
                        }
                    }
                },
                { error ->
                    PosLogger.d(TAG, "error ${error.message}")
                    syncDao.deleteByDate(syncObject.dateTime)
                    emitter.onNext(Result.failure(workDataOf(KEY_MESSAGE to error.message, KEY_RESPONSE_MSG to syncObject.data!!)))
                    emitter.onComplete()
                }
            ).withProcod(syncData?.product)
                .withFields(syncData?.params)
                .withStan(syncData?.stan)
                .withDatosOpTarjeta(AbstractEmvFragment.createDataOpTarjeta(syncData?.dataCard, syncData?.transactionData))
                .withUser(posInstance().user)
                .realizarOperacion()
        } catch (e: Exception) {
            emitter.onNext(Result.failure(workDataOf(KEY_MESSAGE to e.message)))
            emitter.onComplete()
        }
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

enum class SyncState {
    WithTrx,
    SuccessEmpty,
    Error,
    ErrorWithResponse
}