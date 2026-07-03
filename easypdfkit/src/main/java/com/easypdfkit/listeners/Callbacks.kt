package com.easypdfkit.listeners

/** All v1.0 callbacks. Every listener is optional. */
fun interface OnLoadCompleteListener { fun onLoadComplete(pageCount: Int) }
fun interface OnPageChangeListener { fun onPageChanged(page: Int, pageCount: Int) }
fun interface OnErrorListener { fun onError(t: Throwable) }
fun interface OnPasswordRequiredListener { fun onPasswordRequired(retry: (String) -> Unit) }
fun interface OnZoomChangeListener { fun onZoomChanged(zoom: Float) }
fun interface OnTapListener { fun onTap(x: Float, y: Float): Boolean }
