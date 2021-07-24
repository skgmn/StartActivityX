package com.github.skgmn.rapidstartactivity

import android.app.Activity
import android.content.Context
import android.content.Intent

class ExplicitIntent<T : Activity>(context: Context, cls: Class<T>) : Intent(context, cls)
