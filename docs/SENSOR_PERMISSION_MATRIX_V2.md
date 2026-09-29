# Sensor Permission Matrix V2

## Principle

Request only permissions required by an explicit user action.

| Capability | Android permission | Request policy |
|---|---|---|
| microphone/recording | RECORD_AUDIO | ask on REC/CAL/narration |
| active-network metadata | ACCESS_NETWORK_STATE | normal permission; passive metadata only |
| accelerometer | none at current profile | direct local sensor observation |
| magnetometer | none | direct local sensor observation |
| light | none | direct local sensor observation |
| proximity | none | direct local sensor observation |
| step/activity family | ACTIVITY_RECOGNITION on Android 10+ | ask only from SYS optional sensor action |
| >200 Hz sensor sampling | HIGH_SAMPLING_RATE_SENSORS on applicable Android versions | not requested by current ~50 Hz profile |
| RF transmit/modem control | none because feature does not exist | NOT_IMPLEMENTED |

## SYS workspace

The adaptive `SYS` workspace shows:

- permission state;
- sensor presence;
- project/platform/toolchain origin state;
- signing state.

It provides one explicit action for optional motion-sensor access.

## Safety / regulatory boundary

```text
sensor permission != RF permission
network metadata != spectrum scan
telephony feature != modem control
```

The application does not request permissions for active RF manipulation.
