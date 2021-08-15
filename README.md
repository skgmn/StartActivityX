# Setup

```
dependencies {
    implementation "com.github.skgmn:startactivityx:1.0.0"
}
```
If you don't know how to access to GitHub Packges, please refer to [this](https://gist.github.com/skgmn/79da4a935e904078491e932bd5b327c7).

# Features

## startActivityForResult

```kotlin
lifecycleScope.launch {
    val activityResult = startActivityForResult(intent)
}
```
That's it.
It provides some extension methods of `ActivityResult` for the convenience.

```kotlin
val ActivityResult.isOk: Boolean
val ActivityResult.isCanceled: Boolean
fun ActivityResult.getDataIfOk(): Intent?
```
So returned `ActivityResult` can be used like this in more kotlin way.

```kotlin
lifecycleScope.launch {
    startActivityForResult(intent).getDataIfOk()?.let { open(it) }
}
```

## requestPermissions

```kotlin
lifecycleScope.launch {
    if (requestPermissions(Manifest.permission.CAMERA).granted) {
        // Permissions are granted here
        startCamera()
    }
}
```
Simple again.
This single method also handles rationale dialog and _do not ask again_ cases so there are no other things to acquire permissions.

There are some more features which are not documented yet. Please refer to source code and sample code to know about them.

## PermissionStatus

Sometimes there needs to show or hide views according to whether permissions are granted or not. `listenPermissionStatus()` has been introduced to manage this case. It returns `Flow<Boolean>` which infinitely emits boolean values that indicate whether required permissions are granted.
```kotlin
lifecycleScope.launch {
    listenPermissonStatus(Manifest.permission.CAMERA).collect {
        binding.permissionsGranted = it.granted
    }
}
```
```xml
<Button
    android:text="Grant permissions"
    android:visibility="@{permissionsGranted ? View.GONE : View.VISIBLE}" />
```

Or `getPermissionStatus()` can be used to get `PermissionStatus` just once.

## startActivityForInstance

This is some kind of bonus feature. It starts an Activity and returns its instance.
```kotlin
lifecycleScope.launch {
    val intent = ExplicitIntent(context, MyActivity::class.java)
    val activity = startActivityForInstance(intent)
}
```
