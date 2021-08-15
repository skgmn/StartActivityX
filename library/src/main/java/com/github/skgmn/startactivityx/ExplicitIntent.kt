package com.github.skgmn.startactivityx

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable
import java.util.*

class ExplicitIntent<T : Any> : Intent {
    private val cls: Class<T>

    constructor(context: Context, cls: Class<T>) : super(context, cls) {
        this.cls = cls
    }

    private constructor(intent: ExplicitIntent<T>) : super(intent) {
        this.cls = intent.cls
    }

    private constructor(intent: Intent) : super(intent) {
        @Suppress("UNCHECKED_CAST")
        this.cls = intent.component?.className?.let {
            Class.forName(it) as Class<T>
        } ?: throw IllegalArgumentException()
    }

    override fun setClass(packageContext: Context, cls: Class<*>): ExplicitIntent<T> {
        if (!this.cls.isAssignableFrom(cls)) {
            throw IllegalArgumentException("Illegal class")
        }
        super.setClass(packageContext, cls)
        return this
    }

    override fun setClassName(packageContext: Context, className: String): ExplicitIntent<T> {
        val cls = try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            null
        }

        if (cls == null || !this.cls.isAssignableFrom(cls)) {
            throw IllegalArgumentException("Illegal class name")
        }

        super.setClassName(packageContext, className)
        return this
    }

    override fun setClassName(packageName: String, className: String): ExplicitIntent<T> {
        val cls = try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            null
        }

        if (cls == null || !this.cls.isAssignableFrom(cls)) {
            throw IllegalArgumentException("Illegal class name")
        }

        super.setClassName(packageName, className)
        return this
    }

    override fun setComponent(component: ComponentName?): ExplicitIntent<T> {
        component ?: throw IllegalArgumentException("component should not be null")

        val cls = try {
            Class.forName(component.className)
        } catch (e: ClassNotFoundException) {
            null
        }

        if (cls == null || !this.cls.isAssignableFrom(cls)) {
            throw IllegalArgumentException("Illegal class name")
        }

        super.setComponent(component)
        return this
    }

    override fun addCategory(category: String?): ExplicitIntent<T> {
        super.addCategory(category)
        return this
    }

    override fun addFlags(flags: Int): ExplicitIntent<T> {
        super.addFlags(flags)
        return this
    }

    override fun clone(): Any {
        return ExplicitIntent(this)
    }

    override fun cloneFilter(): ExplicitIntent<T> {
        return ExplicitIntent(super.cloneFilter())
    }

    override fun putCharSequenceArrayListExtra(
            name: String,
            value: ArrayList<CharSequence>?
    ): ExplicitIntent<T> {
        super.putCharSequenceArrayListExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Array<out CharSequence>?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Array<out Parcelable>?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Boolean): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Byte): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Char): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Short): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Int): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Long): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Float): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Double): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: String?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: CharSequence?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Parcelable?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Serializable?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: BooleanArray?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: ByteArray?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: ShortArray?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: CharArray?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: IntArray?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: LongArray?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: FloatArray?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: DoubleArray?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Array<out String>?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun putExtra(name: String, value: Bundle?): ExplicitIntent<T> {
        super.putExtra(name, value)
        return this
    }

    override fun setAction(action: String?): ExplicitIntent<T> {
        super.setAction(action)
        return this
    }

    override fun setData(data: Uri?): ExplicitIntent<T> {
        super.setData(data)
        return this
    }

    override fun setDataAndNormalize(data: Uri): ExplicitIntent<T> {
        super.setDataAndNormalize(data)
        return this
    }

    override fun setType(type: String?): ExplicitIntent<T> {
        super.setType(type)
        return this
    }

    override fun setTypeAndNormalize(type: String?): ExplicitIntent<T> {
        super.setTypeAndNormalize(type)
        return this
    }

    override fun setDataAndType(data: Uri?, type: String?): ExplicitIntent<T> {
        super.setDataAndType(data, type)
        return this
    }

    override fun setDataAndTypeAndNormalize(data: Uri, type: String?): ExplicitIntent<T> {
        super.setDataAndTypeAndNormalize(data, type)
        return this
    }

    override fun setIdentifier(identifier: String?): ExplicitIntent<T> {
        super.setIdentifier(identifier)
        return this
    }

    override fun putParcelableArrayListExtra(
            name: String,
            value: ArrayList<out Parcelable>?
    ): ExplicitIntent<T> {
        super.putParcelableArrayListExtra(name, value)
        return this
    }

    override fun putIntegerArrayListExtra(
            name: String,
            value: ArrayList<Int>?
    ): ExplicitIntent<T> {
        super.putIntegerArrayListExtra(name, value)
        return this
    }

    override fun putStringArrayListExtra(
            name: String,
            value: ArrayList<String>?
    ): ExplicitIntent<T> {
        super.putStringArrayListExtra(name, value)
        return this
    }

    override fun putExtras(src: Intent): ExplicitIntent<T> {
        super.putExtras(src)
        return this
    }

    override fun putExtras(extras: Bundle): ExplicitIntent<T> {
        super.putExtras(extras)
        return this
    }

    override fun replaceExtras(src: Intent): ExplicitIntent<T> {
        super.replaceExtras(src)
        return this
    }

    override fun replaceExtras(extras: Bundle?): ExplicitIntent<T> {
        super.replaceExtras(extras)
        return this
    }

    override fun setPackage(packageName: String?): ExplicitIntent<T> {
        super.setPackage(packageName)
        return this
    }
}
