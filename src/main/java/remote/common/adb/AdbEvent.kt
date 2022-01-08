package remote.common.adb

data class AdbEvent(
    val ip: String,
    val eventType: AdbEventType,
    val subType: Any?,
    val param: Any?
)

enum class AdbEventType {
    STATUS,
    ERROR,
    RESPONSE,
    FILE_PUSHED
}

enum class AdbStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
}

enum class AdbErrorType {
    CONNECT_FAILED,
    CLOSED
}