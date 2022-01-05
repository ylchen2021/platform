package remote.common.firebase.billing

class BillingResultCode {
    companion object {
        var SERVICE_TIMEOUT = -3
        var FEATURE_NOT_SUPPORTED = -2
        var SERVICE_DISCONNECTED = -1
        var OK = 0
        var USER_CANCELED = 1
        var SERVICE_UNAVAILABLE = 2
        var BILLING_UNAVAILABLE = 3
        var ITEM_UNAVAILABLE = 4
        var DEVELOPER_ERROR = 5
        var ERROR = 6
        var ITEM_ALREADY_OWNED = 7
        var ITEM_NOT_OWNED = 8
    }
}