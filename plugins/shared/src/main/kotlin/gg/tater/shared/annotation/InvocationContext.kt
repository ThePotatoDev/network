package gg.tater.shared.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class InvocationContext(val type: InvocationContextType)

enum class InvocationContextType {
    ASYNC,
    SYNC
}