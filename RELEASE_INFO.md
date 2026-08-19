# Meloqis Music 0.1.14

- Repairs the widespread YouTube Music source failures caused by the August player rotation.
- Bundles 278 current player mappings through STS 20683, including all 39 mappings added after 0.1.13.
- Moves live playback configuration updates to the Axenora-controlled Meloqis repository.
- Adds the maintained Zemer configuration feed as an automatic fallback if the primary source is unavailable.
- Keeps the last verified configuration active when either network source is unavailable or returns invalid data.
