package com.albaz.appuser.Constants;

import com.google.android.gms.maps.model.LatLng;
import static java.lang.Math.*;

/**
* Class Detected Positioning User With Lat & Lng
*/

public interface LatLngInterpolator {

    LatLng interpolate(float fraction, LatLng a, LatLng b);

    class Spherical implements LatLngInterpolator {

        /* Library From github.com/googlemaps/android-maps-utils */
        @Override
        public LatLng interpolate(float fraction, LatLng from, LatLng to) {

            double fromLat = toRadians(from.latitude);
            double fromLng = toRadians(from.longitude);
            double toLat = toRadians(to.latitude);
            double toLng = toRadians(to.longitude);
            double cosFromLat = cos(fromLat);
            double cosToLat = cos(toLat);

            // Computes Spherical interpolation coefficients.
            double angle = computeAngleBetween(fromLat, fromLng, toLat, toLng);
            double sinAngle = sin(angle);
            if (sinAngle < 1E-6) {
                return from;
            }
            double a = sin((1 - fraction) * angle) / sinAngle;
            double b = sin(fraction * angle) / sinAngle;

            // Converts from polar to vector and interpolate.
            double x = a * cosFromLat * cos(fromLng) + b * cosToLat * cos(toLng);
            double y = a * cosFromLat * sin(fromLng) + b * cosToLat * sin(toLng);
            double z = a * sin(fromLat) + b * sin(toLat);

            // Converts interpolated vector back to polar.
            double lat = atan2(z, sqrt(x * x + y * y));
            double lng = atan2(y, x);
            return new LatLng(toDegrees(lat), toDegrees(lng));
        }
        private double computeAngleBetween(double fromLat, double fromLng, double toLat, double toLng) {
            // Haversine's formula
            double dLat = fromLat - toLat;
            double dLng = fromLng - toLng;
            return 2 * asin(sqrt(pow(sin(dLat / 2), 2) + cos(fromLat) * cos(toLat) * pow(sin(dLng / 2), 2)));
        }
    }
}
