const { Driver } = require("zwave-js");
const mqtt = require("mqtt");

/* ==========================
   CONFIG
========================== */
const ZWAVE_PORT = "/dev/ttyUSB0";
const MQTT_URL = "mqtt://localhost";

/**
 * ⚠️ ADAPTE LES nodeId APRÈS INCLUSION
 */
const CASINO_DEVICES = {
  JACKPOT_LIGHT: 3, // Ampoule ABUS (DIMMABLE)
  POWER_PLUG: 4,    // Fibaro Wall Plug
  SIREN: 2          // Aeotec Siren 6
};

/* ==========================
   RUNTIME CONFIG (ADMIN)
========================== */
const DIM_CONFIG = {
  min: 10,
  max: 99,
  step: 8,
  delay: 40,
  cycles: 4
};

const SIREN_CONFIG = {
  volume: 1,   // 1 = très faible
  soundId: 26   // son victoire
};

/* ==========================
   UTILS
========================== */
const sleep = ms => new Promise(r => setTimeout(r, ms));

function isDimmable(node) {
  return !!node.commandClasses["Multilevel Switch"];
}

async function setBrightness(node, level) {
  if (!isDimmable(node)) return;
  level = Math.max(0, Math.min(99, level));
  await node.commandClasses["Multilevel Switch"].set(level);
}

/* ===== BLINK PAR INTENSITÉ ===== */
async function intensityBlink(node) {
  if (!isDimmable(node)) return;

  for (let c = 0; c < DIM_CONFIG.cycles; c++) {
    for (let v = DIM_CONFIG.min; v <= DIM_CONFIG.max; v += DIM_CONFIG.step) {
      await setBrightness(node, v);
      await sleep(DIM_CONFIG.delay);
    }
    for (let v = DIM_CONFIG.max; v >= DIM_CONFIG.min; v -= DIM_CONFIG.step) {
      await setBrightness(node, v);
      await sleep(DIM_CONFIG.delay);
    }
  }
}

/* ==========================
   SIREN 6 FUNCTIONS
========================== */
async function setSirenVolume(node, volume) {
  volume = Math.max(1, Math.min(5, volume));

  console.log(`🔉 Volume sirène → ${volume}`);

  await node.commandClasses.Configuration.set({
    parameter: 37,
    value: volume
  });
}

async function playSirenSound(node, soundId) {
  const cc = node.commandClasses["Sound Switch"];
  if (!cc || !cc.play) {
    console.warn("⚠️ Sound Switch non disponible");
    return;
  }

  console.log(`🎵 Siren Sound Switch → sound ${soundId}`);
  await cc.play(soundId);
}

async function stopSiren(node) {
  const cc = node.commandClasses["Sound Switch"];
  if (!cc || !cc.stop) return;

  await cc.stop();
}

/* Son de victoire */
async function victorySound(node) {
  await playSirenSound(node, SIREN_CONFIG.soundId);
  await sleep(2000);
  await stopSiren(node);
}

/* ==========================
   MAIN
========================== */
async function main() {
  const mqttClient = mqtt.connect(MQTT_URL);

  mqttClient.on("connect", () => {
    console.log("📡 MQTT connecté");
    mqttClient.subscribe("casino/command/#");
    mqttClient.subscribe("casino/admin/#");
  });

  const driver = new Driver(ZWAVE_PORT, {
    logConfig: { enabled: true, level: "info" },
    securityKeys: {
    S0_Legacy: Buffer.from("DA6E697B76EAE6ED7ED4B74F5AC53B3F", "hex"),
    S2_Unauthenticated: Buffer.from("E5DCFBD223AB6A1440CA899FB9515506", "hex"),
    S2_Authenticated: Buffer.from("AC52690F0B83154DF6B6B51EBFBA0443", "hex"),
    S2_AccessControl: Buffer.from("6FDA476B344B4A98BFDF20FDF4E5465B", "hex")
  }    
  });

  driver.on("node added", node => {
    console.log(`➕ Node ajouté: ${node.id}`);
    console.log(
      "Command Classes:",
      [...node.getSupportedCCs()].map(cc => cc.name)
    );
  });

  /* MQTT HANDLER */
  mqttClient.on("message", async (topic, message) => {
    const payload = JSON.parse(message.toString());

    /* ===== ADMIN DIM ===== */
    if (topic === "casino/admin/dim") {
      if (payload.delay) DIM_CONFIG.delay = Math.max(10, payload.delay);
      if (payload.step) DIM_CONFIG.step = Math.max(1, payload.step);
      if (payload.cycles) DIM_CONFIG.cycles = Math.max(1, payload.cycles);

      mqttClient.publish(
        "casino/admin/dim/state",
        JSON.stringify(DIM_CONFIG)
      );
      return;
    }

    /* ===== ADMIN SIREN ===== */
    if (topic === "casino/admin/siren") {
      if (payload.volume !== undefined) {
        SIREN_CONFIG.volume = Math.max(1, Math.min(5, payload.volume));

        const sirenNode = driver.controller.nodes.get(CASINO_DEVICES.SIREN);
        if (sirenNode) {
          await setSirenVolume(sirenNode, SIREN_CONFIG.volume);
        }
      }

      if (payload.soundId !== undefined) {
        SIREN_CONFIG.soundId = payload.soundId;
      }

      mqttClient.publish(
        "casino/admin/siren/state",
        JSON.stringify(SIREN_CONFIG)
      );
      return;
    }

    /* ===== Z-WAVE ADMIN ===== */
    if (topic === "casino/admin/zwave") {
      if (payload.mode === "INCLUDE") {
        await driver.controller.beginInclusion();
        setTimeout(() => driver.controller.stopInclusion(), 30000);
      }
      if (payload.mode === "EXCLUDE") {
        await driver.controller.beginExclusion();
        setTimeout(() => driver.controller.stopExclusion(), 30000);
      }
      return;
    }

    /* ===== COMMANDES CASINO ===== */
    if (!topic.startsWith("casino/command")) return;

    const nodeId = CASINO_DEVICES[payload.device];
    if (!nodeId) return;

    const node = driver.controller.nodes.get(nodeId);
    if (!node) return;

    switch (payload.action) {
      case "ON":
        await node.commandClasses["Binary Switch"].set(true);
        break;

      case "OFF":
        await node.commandClasses["Binary Switch"].set(false);
        break;

      case "DIM":
        await setBrightness(node, payload.level);
        break;

      case "BLINK":
        await intensityBlink(node);
        break;

      case "VICTORY":
        await victorySound(node);
        break;
    }
  });

  await driver.start();
  console.log("✅ Z-Wave driver démarré");
}

main().catch(console.error);
