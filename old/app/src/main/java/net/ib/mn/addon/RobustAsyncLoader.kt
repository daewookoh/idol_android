package net.ib.mn.addon

import android.content.Context

/**
 * Eat all exception and return default value
 * Initially thought by sean
 * @author vulpes
 *
 * @param <D>
</D> */
abstract class RobustAsyncLoader<D> @JvmOverloads constructor(
    context: Context?, /// Variables
    // (Private)
    private val mDefaultValue: D? = null
) : AsyncLoader<D>(context) {
    /// Methods
    // (Public)
    //   :Default value will be returned whenever there exists exception
    var exception: Exception? = null
        private set
    var exceptionContext: Any? = null
        private set

    override fun loadInBackground(): D? {
        var loadedValue = mDefaultValue
        try {
            loadedValue = load()
        } catch (e: Exception) {
            handleException(e)
        }
        return loadedValue
    }

    fun clearException() {
        exception = null
        exceptionContext = null
    }

    // (Protected)
    @Throws(Exception::class)
    protected abstract fun load(): D
    protected fun setException(exception: Exception?, exceptionContext: Any?) {
        this.exception = exception
        this.exceptionContext = exceptionContext
    }

    protected fun handleException(exception: Exception?) {

        // Set exception
        setException(exception, null)
    }
}
