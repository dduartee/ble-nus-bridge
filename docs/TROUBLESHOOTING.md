# Troubleshooting

## Build Errors

See [Architecture](ARCHITECTURE.md#build-architecture) for platform version
constraints (aapt2 compatibility).

| Symptom | Cause | Fix |
|---------|-------|-----|
| `NullPointerException` (d8) | d8 on Termux ARM64 crashes on inner classes | Use `dx` instead of d8 (build.sh defaults to dx) |
| `Source option 7 is no longer supported` | Java 21 dropped source/target 7 | Use `--release 8` for javac |
| `cannot find symbol: method metafactory` | Lambda expressions not supported by manual dx | Replace lambdas with anonymous/named inner classes |
| `problem parsing the package` | Activity missing `android:exported` | Add `android:exported="true"` to `<activity>` |
| `App not compatible with your phone` | minSdkVersion too low | Set minSdkVersion to 26 or higher |

## Runtime Errors

### Scan returns no devices

| Cause | Fix |
|-------|-----|
| Missing `ACCESS_FINE_LOCATION` permission | Grant location permission in Settings |
| Missing `BLUETOOTH_SCAN` permission (Android 12+) | Grant scan permission via runtime dialog |
| BLE device not advertising | Verify BLE device firmware is running and advertising |

### BLE GATT connection fails

| Cause | Fix |
|-------|-----|
| Missing `BLUETOOTH_CONNECT` permission | Request permission before connecting |
| MTU request before discovery | `discoverServices()` is called from `onMtuChanged`, not `onConnectionStateChange` |
| Device out of range | Move phone closer to BLE device |
| BLE adapter disabled | Enable Bluetooth in Settings |

### TCP bridge not connecting

| Cause | Fix |
|-------|-----|
| App not running or not connected | Open app and verify notification says "TCP :8090" |
| Wrong port | Default is 8090. Verify with `ss -tlnp \| grep 8090` |
| Firewall blocking | Termux does not have firewall restrictions on Android |

### Data not flowing

| Cause | Fix |
|-------|-----|
| BLE notifications not enabled | Verify CCCD descriptor write completed |
| TCP client not connected | Run `nc localhost 8090` and verify no error |
| Fragment reassembly buffer full | Reconnect to clear state |

## Related Docs

- [Architecture](ARCHITECTURE.md) — build constraints and toolchain details
- [Installation](INSTALL.md) — setup prerequisites
