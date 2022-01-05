package remote.common.firebase.billing

import android.content.Context
import androidx.room.*
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object BillingCache {
    @Entity(tableName = "sku_info")
    @TypeConverters(SkuStateConverter::class)
    data class SkuInfo (
        @PrimaryKey
        var sku: String,
        var status: BillingDataSource.SkuState,
    )

    class SkuStateConverter {
        @TypeConverter
        fun skuStateToInt(skuState: BillingDataSource.SkuState): Int {
            return skuState.ordinal
        }

        @TypeConverter
        fun intToSkuState(skuState: Int): BillingDataSource.SkuState {
            return BillingDataSource.SkuState.values()[skuState]
        }
    }

    @Dao
    interface SkuInfoDao {
        @Query("SELECT * FROM sku_info")
        suspend fun getAll(): List<SkuInfo>

        @Insert
        suspend fun insert(skuInfo: SkuInfo)

        @Update
        suspend fun update(skuInfo: SkuInfo)
    }

    @Database(entities = [SkuInfo::class], version = 1, exportSchema = false)
    abstract class PurchaseDatabase : RoomDatabase() {
        val skuInfoDao: SkuInfoDao by lazy { createSkuInfoDao() }
        abstract fun createSkuInfoDao(): SkuInfoDao
    }

    private lateinit var db: PurchaseDatabase

    fun init(context: Context) {
        db = Room.databaseBuilder(context, PurchaseDatabase::class.java, "purchase.db")
            .build()
    }

    fun updateSkuState(sku: String, skuState: BillingDataSource.SkuState) {
        GlobalScope.launch(Dispatchers.IO) {
            if (!checkExist(sku)) {
                db.skuInfoDao.insert(SkuInfo(sku, BillingDataSource.SkuState.SKU_STATE_UNPURCHASED))
            }
            db.skuInfoDao.update(SkuInfo(sku, skuState))
        }
    }

    fun getAllSkuInfo(onResult: (List<SkuInfo>) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            var allList = withContext(Dispatchers.IO) {
                db.skuInfoDao.getAll()
            }
            onResult.invoke(allList)
        }
    }

    private suspend fun checkExist(sku: String): Boolean {
        var allList = db.skuInfoDao.getAll()
        allList.forEach {
            if (sku == it.sku) {
                return true
            }
        }
        return false
    }
}