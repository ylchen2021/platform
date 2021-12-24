package remote.common.adb

data class AdbEvent(
    val eventType: AdbEventType,
    val status: Any?,
    val param: Any?
)

enum class AdbEventType {
    STATUS,
    ERROR,
    RESPONSE,
    FILE_PUSHED
}

enum class AdbStatus {
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

enum class AdbErrorType {
    CONNECT_FAILED,
    CLOSED
}