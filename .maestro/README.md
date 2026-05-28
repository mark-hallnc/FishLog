# FishLog Maestro End-to-End Tests

This directory contains [Maestro](https://maestro.mobile.dev/) flow files for end-to-end UI testing of the FishLog application.

## Prerequisites

1.  **Maestro CLI**: Install Maestro on your machine.
    *   **macOS / Linux**: `curl -Ls "https://get.maestro.mobile.dev" | bash`
    *   **Windows**: Follow the instructions at [maestro.mobile.dev](https://maestro.mobile.dev/getting-started/installing-maestro).
2.  **Running Emulator/Device**: An Android emulator or physical device must be connected and reachable via `adb`.
3.  **App Installed**: The FishLog app must be installed on the device.

## Running Tests

### Running all tests
To run all flows in the `.maestro` directory:
```powershell
maestro test .maestro
```

### Running a specific test
To run a specific flow:
```powershell
maestro test .maestro/00_smoke_launch.yaml
```

## Flow Overview

- `00_smoke_launch.yaml`: Basic launch test, handles the welcome screen, and verifies the Home screen appears.
- `01_first_run.yaml`: Verifies the first-run welcome screen (clears app state first).
- `02_start_trip_log_catch_end_trip.yaml`: Core flow: starting a trip, logging a catch, and ending the trip.
- `03_log_no_catch.yaml`: Logs a standalone "No Catch" record.
- `04_trip_history_open_detail.yaml`: Verifies the Trip History screen and opens a trip detail.
- `05_catch_history_open_detail.yaml`: Verifies the Catch History screen and opens a catch detail.
- `06_settings_basic_navigation.yaml`: Verifies the Settings screen and ensures technical technical implementation details are hidden.

## Implementation Details

- **Test Tags**: Some UI elements use `androidx.compose.ui.platform.testTag` for stable selection.
- **Offline First**: These tests do not depend on real Supabase login, GPS, or weather APIs.
- **Welcome Screen**: Most flows use a subflow (`subflows/dismiss_welcome.yaml`) to handle the first-run screen if it appears.
