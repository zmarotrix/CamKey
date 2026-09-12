# CamKey - Cinematic Camera Tool

CamKey is a lightweight NeoForge 1.21.1 mod built for production recording sessions. It provides a simple command-line interface for capturing spatial keyframes and executing smooth, cinematic camera sweeps between them.

## Build and Run Instructions

This project uses the standard Gradle wrapper.

**To build:**
1. Open a terminal at the project root.
2. Run `.\gradlew build` (Windows) or `./gradlew build` (Mac/Linux).
3. The compiled `.jar` will output to `build/libs/`.

**To run the test client:**
1. Run `.\gradlew runClient` (Windows) or `./gradlew runClient` (Mac/Linux).

## Command Reference

The tool is driven by the `/camkey` Brigadier command tree:

* `/camkey add <name>` - Captures current position and rotation, appending it to the named sequence.
* `/camkey play <name> <seconds>` - Teleports to the start of the sequence and plays the camera movement over the given duration.
* `/camkey smoothing` - Toggles manual cinematic camera smoothing on or off.
* `/camkey autosmooth` - Toggles whether smoothing is automatically forced during playback (Default: ON).
* `/camkey speed [value|default]` - Adjusts camera smoothing responsiveness (e.g., `/camkey speed 0.15`).

---

## Architectural Decisions

To ensure a clean separation of concerns, the codebase is modularized into four packages (`model`, `storage`, `command`, and `playback`). 

**Data & Persistence (Systems Thinking)**
I opted to serialize the Java Record keyframes to standard `.json` files via Gson rather than Minecraft's native NBT format. JSON is human-readable, meaning a Production Associate can easily open a sequence file in a text editor to manually tweak a coordinate, verify data, or share a sequence with another team member over Slack without needing special NBT tools. 

**Engine & Playback (Minecraft Fluency)**
The playback system is intentionally split across two distinct event buses to bypass Minecraft's rigid 20 TPS engine limit:
* `PlaybackEngine` hooks into `ClientTickEvent.Post` to calculate the mathematical Smoothstep progression of the sequence over time.
* `CameraSmoother` hooks into `ViewportEvent.ComputeCameraAngles` on the render thread. By completely decoupling the visual rotation from the player's physical body tick, the camera interpolates at the monitor's native refresh rate (e.g., 144hz), resulting in a buttery-smooth pan.

**Scalability**
For this lightweight implementation, the playback engine uses static state fields to manipulate the local player's camera. If the scope eventually expands to require multiple simultaneous cameras (e.g., server-side camera entities or AI bots), this architecture allows the static fields to be easily swapped for an instanced `CameraManager` that tracks playback state per-entity.

---

## Production Safeguards

The tool is built to be resilient during live recording days:
* **Crash Resilience:** Sequences save to disk the exact millisecond a frame is captured (`/camkey add`). If a client crashes during a live setup, zero work is lost.
* **Graceful Failures:** The command tree intercepts bad inputs (e.g., playing a sequence that doesn't exist, or playing a sequence with less than two frames) and returns clear chat warnings rather than throwing console errors.
* **State Restoration:** Teleporting normally causes smoothed cameras to violently drag across the screen. The playback engine specifically resets the smoother's memory right before teleportation to ensure a clean cut. It also tracks the user's manual smoothing preferences and restores them exactly as they were when playback finishes.

---

## AI-Assisted Development Notes

**Tool used:** Gemini 3.1 Pro (via Google AI Studio).

**Workflow:** I provided the initial architecture outline, and Gemini ended up doing a significant amount of the heavy lifting. It was highly effective at scaffolding the Brigadier command tree, setting up the basic NeoForge boilerplate, and generating the complex math required for 3D interpolation (such as the logic required to prevent the camera from spinning wildly when crossing the -180/180 degree yaw boundary).

**Corrections made:** The AI required correction primarily around Minecraft engine fluency. 
* It initially tried to achieve camera smoothing by manually overwriting the `player.xo` (Old X) variables during the Pre-Tick phase. I recognized that this was breaking Minecraft's internal partial-tick rendering and making the camera choppy, so I directed a shift over to `ClientTickEvent.Post` and eventually `ViewportEvent` to fix the frame rate.
* It also initially caused a hard crash on load by trying to register an empty main class to the NeoForge Event Bus before any `@SubscribeEvent` methods existed, which I had to debug and remove.

---

## Future Scope (If I had another week)

If given more time to expand this into a larger studio tool, I would build:
* **Continuous Capture:** A command to automatically capture a keyframe every *X* milliseconds over *Y* seconds, rather than requiring manual clicks for every point.
* **Timeline UI:** A graphical interface to visually scrub through the sequence timeline, adjust point timings, or delete errant frames.
* **Screen Effects:** Integrating post-processing effects based on percentage start/end times (e.g., fading to black, FOV zooms, or cinematic letterboxing).
* **Decoupled Capture:** Separating the camera entity from the player entity entirely, allowing users to record their own character in third-person while the camera flies the predefined sequence.
* **Advanced Splines:** Upgrading the math from linear/smoothstep interpolation to Catmull-Rom or Bezier splines for perfectly curved tracking shots.