# CamKey - Cinematic Camera Tool

CamKey is a lightweight NeoForge 1.21.1 mod built for recording sessions. It allows production staff to capture specific keyframes in the world and automatically play back a smooth, cinematic camera pan between them over a set duration.

## How to Build and Run

This project uses the standard Gradle wrapper. 

**Build the mod:**
1. Open a terminal at the root of the project folder.
2. Run `.\gradlew build` (Windows) or `./gradlew build` (Mac/Linux).
3. The compiled `.jar` will output to `build/libs/`.

**Run the test client:**
1. Run `.\gradlew runClient` (Windows) or `./gradlew runClient` (Mac/Linux).

## Command Reference

The mod operates entirely through the `/camkey` command tree:

* `/camkey add <name>` - Captures your current position and rotation, adding it to the end of the specified sequence.
* `/camkey play <name> <seconds>` - Teleports you to the start of the sequence and plays the camera movement over the given duration.
* `/camkey delete <name>` - Deletes a sequence from active memory and permanently removes its `.json` file from the disk.
* `/camkey smoothing` - Toggles manual cinematic camera smoothing on or off.
* `/camkey autosmooth` - Toggles whether smoothing is automatically applied during playback (Default: ON).
* `/camkey speed [value|default]` - Adjusts camera smoothing responsiveness (e.g., `/camkey speed 0.15`).

---

## Architectural Decisions

The codebase is split into four distinct packages to keep concerns separated and make future expansion easier:

1. **`model` (Data):** Uses Java Records to handle immutable keyframe data. 
2. **`storage` (Persistence):** Uses `Gson` to read/write sequences to standard `.json` files in `config/camkey/`. Sequences save to disk immediately when a keyframe is added, ensuring no work is lost if the client crashes during a recording session.
3. **`command` (Interface):** Built on Mojang's Brigadier system. Includes basic safety checks, like returning graceful error messages if a user tries to play a sequence that doesn't exist or doesn't have enough frames.
4. **`playback` (Engine):** Separated into two classes to handle Minecraft's split tick/render loops:
   * `PlaybackEngine`: Hooks into `ClientTickEvent` to calculate the math for the sequence over time using Smoothstep interpolation.
   * `CameraSmoother`: Hooks into `ViewportEvent.ComputeCameraAngles` to manage the actual camera rendering independently of the player body.

---

## Production Safeguards

The tool is built to be resilient during live recording days:

* **Crash Resilience:** Sequences save to disk the exact millisecond a frame is captured. If a client crashes during a live setup, zero work is lost.
* **Collision & HUD Management:** When a sequence starts, the mod temporarily forces the player into Spectator mode. This prevents the camera from getting snagged on blocks (which causes intense collision jitter) and hides the player's HUD for a clean cinematic shot. The player's original gamemode is seamlessly restored the moment playback ends.
* **Graceful Failures:** The command tree intercepts bad inputs (e.g., playing a sequence that doesn't exist, or playing a sequence with less than two frames) and returns clear chat warnings rather than throwing console errors.
* **State Restoration:** Teleporting normally causes smoothed cameras to violently drag across the screen. The playback engine specifically resets the smoother's memory right before teleportation to ensure a clean cut. It also tracks the user's manual smoothing preferences and restores them exactly as they were when playback finishes.

---

## Technical Challenges Solved

Getting the camera to move was straightforward, but making it look smooth required working around a few specific Minecraft engine quirks:

1. **The 20 TPS Jitter:** Camera rotation was initially calculated in the `ClientTickEvent`. Because game logic runs at 20 Ticks Per Second, this locked the camera movement to 20 FPS, making the pan look incredibly choppy. We fixed this by moving the visual rotation logic to the `ViewportEvent` on the render thread. This decouples the camera from the player entity's body tick, allowing it to interpolate at the monitor's native refresh rate.
2. **Teleport Dragging:** When `/camkey play` is triggered, the player teleports to the first frame. However, the camera smoother still retained the old look angle in memory, causing the camera to violently drag across the screen to catch up. We fixed this by implementing a state reset that wipes the smoother's memory right before the teleport triggers.

---

## AI-Usage Notes

**Tool used:** Gemini 3.1 Pro (via Google AI Studio).

**Workflow:** I provided the initial architecture outline and structure for the mod, and Gemini ended up doing far more of the heavy lifting than I expected. For the most part, I just needed to guide it, handle formatting changes, and tweak the output to fit the project structure.

**Corrections made:** Where I really had to step in and correct the AI was regarding Minecraft engine fluency. 
* It originally tried to force camera smoothing by manually overwriting the `player.xo` (Old X) variables during the Pre-Tick phase. I caught that this was breaking Minecraft's internal partial-tick rendering and had to direct the shift over to `ClientTickEvent.Post` and eventually `ViewportEvent` to fix the frame rate. 
* It also initially caused a hard crash on load by trying to register an empty main class to the NeoForge Event Bus before any `@SubscribeEvent` methods actually existed, which I had to debug and remove.

---

## Future Scope

If I had another week to expand this into a larger studio tool, I would look into adding:

* **Continuous capture:** A command to automatically capture a keyframe every *X* milliseconds over *Y* seconds (rather than clicking manually for every point).
* **Timeline UI:** A graphical interface to scrub through the sequence timeline and easily delete or adjust specific frames.
* **Screen effects:** The ability to add visual triggers based on percentage timestamps (e.g., fade to black, FOV zooms, or cinematic letterboxing).
* **Decoupled capture:** Separating the camera from the player entity entirely, allowing you to record your own character in third-person while the camera flies the sequence.
* **Better splines:** Upgrading the math from linear/smoothstep interpolation to Catmull-Rom or Bezier splines for curved tracking shots.

## Video Preview

https://www.youtube.com/watch?v=w3IGv-kLLM8


