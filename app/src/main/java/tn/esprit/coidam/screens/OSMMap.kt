package tn.esprit.coidam.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.content.Context
import android.preference.PreferenceManager

@Composable
fun OSMMap(
    context: Context,
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // Configuration osmdroid
            Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

            val mapView = MapView(ctx)
            mapView.setMultiTouchControls(true)
            val startPoint = GeoPoint(latitude, longitude)
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(startPoint)

            // Marker
            val marker = Marker(mapView)
            marker.position = startPoint
            marker.title = "Alerte"
            mapView.overlays.add(marker)

            mapView
        }
    )
}
