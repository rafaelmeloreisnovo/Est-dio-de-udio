# SENSOR_PHYSICAL_INVENTORY_V1

Status boundary: SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.

This change declares the Android manifest sensor feature descriptors used for compatibility filtering as optional and preserves runtime discovery through `SensorManager.getSensorList(Sensor.TYPE_ALL)`.

Optional manifest features:
- accelerometer
- compass / geomagnetic field
- gyroscope
- light
- proximity
- barometer

Runtime inventory additionally observes any platform-reported sensor type, including ambient temperature and relative humidity when exposed by the device.

Permission boundary:
- accelerometer, magnetometer, gyroscope, light, proximity, pressure, ambient temperature and relative humidity: no additional runtime permission is requested by this project path;
- step counter/detector: permission-specific path (`ACTIVITY_RECOGNITION`) remains intentionally not auto-requested;
- body/health sensors: permission-specific path remains intentionally not auto-requested;
- high-rate motion sampling above the current platform-bounded profile is not enabled.

Physical device results remain NOT_RUN until a matching APK is installed and a new ZIPRAF receipt is generated.

EXTERNAL_STANDARD_AUDIT=NOT_AUDITED
CLAIM_ALLOWED=false
