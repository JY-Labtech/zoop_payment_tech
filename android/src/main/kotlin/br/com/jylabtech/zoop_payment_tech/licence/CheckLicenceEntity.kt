package br.com.jylabtech.zoop_payment_tech.licence

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class CheckLicenceEntity {
    var id: String? = null
    var uid: String? = null
    var deviceUid: String? = null
    var deviceSerialNumber: String? = null
    var userUid: String? = null
    var productId: String? = null
    var productName: String? = null
    var productDescription: String? = null
    var integrationLibId: String? = null
    var integrationLibName: String? = null
    var checkoutSessionId: String? = null
    var licenceKey: String? = null
    var licenceEncrypt: String? = null
    var expiredAt: Date? = null
    var isExpired: Boolean = false
    var isInactive: Boolean = false
    var isTrial: Boolean = false
    var subscriptionId: String? = null
    var checkoutType: String? = null
    var createdAt: Date? = null
    var updatedAt: Date? = null
    var savedLocalDatabaseAt: Date? = null
    var databaseVersion: String? = null
    var licenceInternalKey: String? = null

    companion object {
        fun fromJson(jsonStr: String): CheckLicenceEntity? {
            return try {
                val json = JSONObject(jsonStr)
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

                val entity = CheckLicenceEntity()
                entity.id = json.optString("id")
                entity.uid = json.optString("uid")
                entity.deviceUid = json.optString("deviceUid")
                entity.deviceSerialNumber = json.optString("deviceSerialNumber")
                entity.userUid = json.optString("userUid")
                entity.productId = json.optString("productId")
                entity.productName = json.optString("productName")
                entity.productDescription = json.optString("productDescription")
                entity.integrationLibId = json.optString("integrationLibId")
                entity.integrationLibName = json.optString("integrationLibName")
                entity.checkoutSessionId = json.optString("checkoutSessionId")
                entity.licenceKey = json.optString("licenceKey")
                entity.licenceEncrypt = json.optString("licenceEncrypt")
                entity.isExpired = json.optBoolean("isExpired")
                entity.isInactive = json.optBoolean("isInactive")
                entity.isTrial = json.optBoolean("isTrial")
                entity.subscriptionId = json.optString("subscriptionId")
                entity.checkoutType = json.optString("checkoutType")
                entity.databaseVersion = json.optString("databaseVersion")
                entity.licenceInternalKey = json.optString("licenceInternalKey")

                val expiredAtStr = json.optString("expiredAt")
                if (expiredAtStr.isNotEmpty()) {
                    entity.expiredAt = sdf.parse(expiredAtStr)
                }

                val createdAtStr = json.optString("createdAt")
                if (createdAtStr.isNotEmpty()) {
                    entity.createdAt = sdf.parse(createdAtStr)
                }

                val updatedAtStr = json.optString("updatedAt")
                if (updatedAtStr.isNotEmpty()) {
                    entity.updatedAt = sdf.parse(updatedAtStr)
                }

                val savedLocalStr = json.optString("savedLocalDatabaseAt")
                if (savedLocalStr.isNotEmpty()) {
                    entity.savedLocalDatabaseAt = sdf.parse(savedLocalStr)
                }

                entity
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun toJson(): String {
        return try {
            val json = JSONObject()
            json.put("id", id)
            json.put("uid", uid)
            json.put("deviceUid", deviceUid)
            json.put("deviceSerialNumber", deviceSerialNumber)
            json.put("userUid", userUid)
            json.put("productId", productId)
            json.put("productName", productName)
            json.put("productDescription", productDescription)
            json.put("integrationLibId", integrationLibId)
            json.put("integrationLibName", integrationLibName)
            json.put("checkoutSessionId", checkoutSessionId)
            json.put("licenceKey", licenceKey)
            json.put("licenceEncrypt", licenceEncrypt)
            json.put("isExpired", isExpired)
            json.put("isInactive", isInactive)
            json.put("isTrial", isTrial)
            json.put("subscriptionId", subscriptionId)
            json.put("checkoutType", checkoutType)
            json.put("databaseVersion", databaseVersion)
            json.put("licenceInternalKey", licenceInternalKey)

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            if (expiredAt != null) json.put("expiredAt", sdf.format(expiredAt))
            if (createdAt != null) json.put("createdAt", sdf.format(createdAt))
            if (updatedAt != null) json.put("updatedAt", sdf.format(updatedAt))
            if (savedLocalDatabaseAt != null) json.put("savedLocalDatabaseAt", sdf.format(savedLocalDatabaseAt))

            json.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}

