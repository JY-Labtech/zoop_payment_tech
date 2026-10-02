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
    var isCanceled: Boolean = false
    var isTrial: Boolean = false
    var subscriptionId: String? = null
    var checkoutType: String? = null
    var createdAt: Date? = null
    var updatedAt: Date? = null
    var savedLocalDatabaseAt: Date? = null
    var databaseVersion: String? = null
    var licenceInternalKey: String? = null

    companion object {
        private const val DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

        /** optString devolve "null" para JSONObject.NULL; aqui null/ausente vira "". */
        private fun JSONObject.optStringOrEmpty(name: String): String {
            return if (isNull(name)) "" else optString(name)
        }

        fun fromJson(jsonStr: String): CheckLicenceEntity? {
            return try {
                val json = JSONObject(jsonStr)
                val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US)

                val entity = CheckLicenceEntity()
                entity.id = json.optStringOrEmpty("id")
                entity.uid = json.optStringOrEmpty("uid")
                entity.deviceUid = json.optStringOrEmpty("deviceUid")
                entity.deviceSerialNumber = json.optStringOrEmpty("deviceSerialNumber")
                entity.userUid = json.optStringOrEmpty("userUid")
                entity.productId = json.optStringOrEmpty("productId")
                entity.productName = json.optStringOrEmpty("productName")
                entity.productDescription = json.optStringOrEmpty("productDescription")
                entity.integrationLibId = json.optStringOrEmpty("integrationLibId")
                entity.integrationLibName = json.optStringOrEmpty("integrationLibName")
                entity.checkoutSessionId = json.optStringOrEmpty("checkoutSessionId")
                entity.licenceKey = json.optStringOrEmpty("licenceKey")
                entity.licenceEncrypt = json.optStringOrEmpty("licenceEncrypt")
                entity.isExpired = json.optBoolean("isExpired")
                entity.isInactive = json.optBoolean("isInactive")
                entity.isCanceled = json.optBoolean("isCanceled")
                entity.isTrial = json.optBoolean("isTrial")
                entity.subscriptionId = json.optStringOrEmpty("subscriptionId")
                entity.checkoutType = json.optStringOrEmpty("checkoutType")
                entity.databaseVersion = json.optStringOrEmpty("databaseVersion")
                entity.licenceInternalKey = json.optStringOrEmpty("licenceInternalKey")

                val expiredAtStr = json.optStringOrEmpty("expiredAt")
                if (expiredAtStr.isNotEmpty()) {
                    entity.expiredAt = sdf.parse(expiredAtStr)
                }

                val createdAtStr = json.optStringOrEmpty("createdAt")
                if (createdAtStr.isNotEmpty()) {
                    entity.createdAt = try { sdf.parse(createdAtStr) } catch (e: Exception) { null }
                }

                val updatedAtStr = json.optStringOrEmpty("updatedAt")
                if (updatedAtStr.isNotEmpty()) {
                    entity.updatedAt = try { sdf.parse(updatedAtStr) } catch (e: Exception) { null }
                }

                val savedLocalStr = json.optStringOrEmpty("savedLocalDatabaseAt")
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
            json.put("isCanceled", isCanceled)
            json.put("isTrial", isTrial)
            json.put("subscriptionId", subscriptionId)
            json.put("checkoutType", checkoutType)
            json.put("databaseVersion", databaseVersion)
            json.put("licenceInternalKey", licenceInternalKey)

            val sdf = SimpleDateFormat(DATE_PATTERN, Locale.US)
            expiredAt?.let { json.put("expiredAt", sdf.format(it)) }
            createdAt?.let { json.put("createdAt", sdf.format(it)) }
            updatedAt?.let { json.put("updatedAt", sdf.format(it)) }
            savedLocalDatabaseAt?.let { json.put("savedLocalDatabaseAt", sdf.format(it)) }

            json.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}

